/*
 * Copyright 2010-2012 VMware and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springsource.loaded;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Compute the differences between two versions of a type as a series of deltas. Entry point is the computeDifferences
 * method.
 * 
 * @author Andy Clement
 * @since 0.5.0
 */
public class TypeDiffComputer implements Opcodes {

	public static TypeDelta computeDifferences(byte[] oldbytes, byte[] newbytes) {
		ClassNode oldClassNode = new ClassNode();
		new ClassReader(oldbytes).accept(oldClassNode, 0);
		ClassNode newClassNode = new ClassNode();
		new ClassReader(newbytes).accept(newClassNode, 0);
		TypeDelta delta = computeDelta(oldClassNode, newClassNode);
		return delta;
	}

	private static TypeDelta computeDelta(ClassNode oldClassNode, ClassNode newClassNode) {
		// The type itself: (int version, int access, String name, String signature, String superName, String[] interfaces) {
		TypeDelta td = new TypeDelta();
		computeTypeDelta(oldClassNode, newClassNode, td);
		computeFieldDelta(oldClassNode, newClassNode, td);
		computeMethodDelta(oldClassNode, newClassNode, td);
		// TODO delta: implement the rest of computeDelta.  These methods from ClassVisitor should help in knowing what is left to do:
		//		public void visitSource(String source, String debug) {
		//		public void visitOuterClass(String owner, String name, String desc) {
		//		public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		//		public void visitAttribute(Attribute attr) {
		//		public void visitInnerClass(String name, String outerName, String innerName, int access) {
		//		public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		//		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		//		public void visitEnd() {
		return td;
	}

	@SuppressWarnings("unchecked")
	private static void computeMethodDelta(ClassNode oldClassNode, ClassNode newClassNode, TypeDelta td) {
		List<MethodNode> nMethods = newClassNode.methods;
		List<MethodNode> oMethods = new ArrayList<MethodNode>(oldClassNode.methods);

		// Going through the new methods and comparing them to the old
		if (nMethods != null) {
			for (MethodNode nMethod : nMethods) {
				MethodNode found = null;
				for (MethodNode oMethod : oMethods) {
					if (oMethod.name.equals(nMethod.name) && oMethod.desc.equals(nMethod.desc)) { // TODO modifiers compared?
						found = oMethod;
						computeAnyMethodDifferences(oMethod, nMethod, td);
					}
				}
				if (found == null) {
					td.addNewMethod(nMethod);
				}
				else {
					oMethods.remove(found);
				}
			}
		}
		for (MethodNode lostMethod : oMethods) {
			td.addLostMethod(lostMethod);
		}
	}

	@SuppressWarnings("unchecked")
	private static void computeFieldDelta(ClassNode oldClassNode, ClassNode newClassNode, TypeDelta td) {
		//		int oSize = oldClassNode.fields.size();
		int nSize = newClassNode.fields.size();

		// Take a copy as we are going to delete entries in the next loop
		List<FieldNode> oFields = new ArrayList<FieldNode>(oldClassNode.fields);

		// Going through the new fields comparing them to the old
		for (int n = 0; n < nSize; n++) {
			FieldNode nField = (FieldNode) newClassNode.fields.get(n);
			FieldNode found = null;
			for (FieldNode oField : oFields) {
				if (oField.name.equals(nField.name)) {
					// found it!
					found = oField;
					// is it exactly the same?
					computeAnyFieldDifferences(oField, nField, td);
				}
			}
			if (found == null) {
				// this is a new field
				td.addNewField(nField);
			}
			else {
				oFields.remove(found);
			}
		}

		// Those left in oFields were not in nFields so have been removed!
		for (FieldNode lostField : oFields) {
			td.addLostField(lostField);
		}
	}

	/**
	 * Check the properties of the field - if they have changed at all then record what kind of change for the field.
	 * Thinking the type delta should have a map from names to a delta describing (capturing) the change.
	 */
	@SuppressWarnings("unchecked")
	private static void computeAnyFieldDifferences(FieldNode oField, FieldNode nField, TypeDelta td) {
		// Want to record things that are different between these two fields...
		FieldDelta fd = new FieldDelta(oField.name);
		if (oField.access != nField.access) {
			// access changed
			fd.setAccessChanged(oField.access, nField.access);
		}
		if (!oField.desc.equals(nField.desc)) {
			// type changed
			fd.setTypeChanged(oField.desc, nField.desc);
		}
		String annotationChange = compareAnnotations(oField.invisibleAnnotations, nField.invisibleAnnotations);
		annotationChange = annotationChange + compareAnnotations(oField.visibleAnnotations, nField.visibleAnnotations);
		if (annotationChange.length() != 0) {
			fd.setAnnotationsChanged(annotationChange);
		}
		if (fd.hasAnyChanges()) {
			// it needs recording
			td.addChangedField(fd);
		}
	}

	/**
	 * Determine if there any differences between the methods supplied. A MethodDelta object is built to record any
	 * differences and stored against the type delta.
	 * 
	 * @param oMethod 'old' method
	 * @param nMethod 'new' method
	 * @param td the type delta where changes are currently being accumulated
	 */
	private static void computeAnyMethodDifferences(MethodNode oMethod, MethodNode nMethod, TypeDelta td) {
		MethodDelta md = new MethodDelta(oMethod.name, oMethod.desc);
		if (oMethod.access != nMethod.access) {
			md.setAccessChanged(oMethod.access, nMethod.access);
		}
		// TODO annotations
		InsnList oInstructions = oMethod.instructions;
		InsnList nInstructions = nMethod.instructions;
		if (oInstructions.size() != nInstructions.size()) {
			md.setInstructionsChanged(oInstructions.toArray(), nInstructions.toArray());
		}
		else {
			// TODO Just interested in constructors right now - should add others
			if (oMethod.name.charAt(0) == '<') {
				String oInvokeSpecialDescriptor = null;
				String nInvokeSpecialDescriptor = null;
				int oUninitCount = 0;
				int nUninitCount = 0;
				boolean codeChange = false;
				for (int i = 0, max = oInstructions.size(); i < max; i++) {
					AbstractInsnNode oInstruction = oInstructions.get(i);
					AbstractInsnNode nInstruction = nInstructions.get(i);
					if (!codeChange) {
						if (!sameInstruction(oInstruction, nInstruction)) {
							codeChange = true;
						}

					}
					if (oInstruction.getType() == AbstractInsnNode.TYPE_INSN) {
						if (oInstruction.getOpcode() == Opcodes.NEW) {
							oUninitCount++;
						}
					}
					if (nInstruction.getType() == AbstractInsnNode.TYPE_INSN) {
						if (nInstruction.getOpcode() == Opcodes.NEW) {
							nUninitCount++;
						}
					}
					if (oInstruction.getType() == AbstractInsnNode.METHOD_INSN) {
						MethodInsnNode mi = (MethodInsnNode) oInstruction;
						if (mi.getOpcode() == INVOKESPECIAL && mi.name.equals("<init>")) {
							if (oUninitCount == 0) {
								// this is the one!
								oInvokeSpecialDescriptor = mi.desc;
							}
							else {
								oUninitCount--;
							}
						}
					}
					if (nInstruction.getType() == AbstractInsnNode.METHOD_INSN) {
						MethodInsnNode mi = (MethodInsnNode) nInstruction;
						if (mi.getOpcode() == INVOKESPECIAL && mi.name.equals("<init>")) {
							if (nUninitCount == 0) {
								// this is the one!
								nInvokeSpecialDescriptor = mi.desc;
							}
							else {
								nUninitCount--;
							}
						}
					}
				}
				// Has the invokespecial changed?
				if (oInvokeSpecialDescriptor == null) {
					if (nInvokeSpecialDescriptor != null) {
						md.setInvokespecialChanged(oInvokeSpecialDescriptor, nInvokeSpecialDescriptor);
					}
				}
				else {
					if (!oInvokeSpecialDescriptor.equals(nInvokeSpecialDescriptor)) {
						md.setInvokespecialChanged(oInvokeSpecialDescriptor, nInvokeSpecialDescriptor);
					}
				}
				if (codeChange) {
					md.setCodeChanged(oInstructions.toArray(), nInstructions.toArray());
				}
			}
		}
		if (md.hasAnyChanges()) {
			// it needs recording
			td.addChangedMethod(md);
		}

	}

	private static boolean sameInstruction(AbstractInsnNode o, AbstractInsnNode n) {
		if (o.getType() != o.getType() || o.getOpcode() != n.getOpcode()) {
			return false;
		}
		switch (o.getType()) {
			case (AbstractInsnNode.INSN): // 0
				if (!sameInsnNode(o, n)) {
					return false;
				}
				break;
			case (AbstractInsnNode.INT_INSN): // 1
				if (!sameIntInsnNode(o, n)) {
					return false;
				}
				break;
			case (AbstractInsnNode.VAR_INSN): // 2
				if (!sameVarInsn(o, n)) {
					return false;
				}
				break;
			case (AbstractInsnNode.TYPE_INSN):// 3
				if (!sameTypeInsn(o, n)) {
					return false;
				}
				break;
			case (AbstractInsnNode.FIELD_INSN): // 4
				if (!sameFieldInsn(o, n)) {
					return false;
				}
				break;
			case (AbstractInsnNode.METHOD_INSN): // 5
				if (!sameMethodInsnNode(o, n)) {
					return false;
				}
				break;
			case (AbstractInsnNode.JUMP_INSN): // 6
				if (!sameJumpInsnNode(o, n)) {
					return false;
				}
				break;
			case (AbstractInsnNode.LABEL): // 7
				if (!sameLabelNode(o, n)) {
					return false;
				}
				break;
			case (AbstractInsnNode.LDC_INSN): // 8
				if (!sameLdcInsnNode(o, n)) {
					return false;
				}
				break;
			case (AbstractInsnNode.IINC_INSN): // 9
				if (!sameIincInsn(o, n)) {
					return false;
				}
				break;
			case (AbstractInsnNode.TABLESWITCH_INSN): // 10
				if (!sameTableSwitchInsn(o, n)) {
					return false;
				}
				break;
			case (AbstractInsnNode.LOOKUPSWITCH_INSN): // 11
				if (!sameLookupSwitchInsn(o, n)) {
					return false;
				}
				break;
			case (AbstractInsnNode.MULTIANEWARRAY_INSN): // 12
				if (!sameMultiANewArrayInsn(o, n)) {
					return false;
				}
				break;
			case (AbstractInsnNode.FRAME): // 13
				if (!sameFrameInsn(o, n)) {
					return false;
				}
				break;
			case (AbstractInsnNode.LINE): // 14
				if (!sameLineNumberNode(o, n)) {
					return false;
				}
				break;
			default:
				throw new IllegalStateException("nyi " + o.getType());
		}
		return true;
	}

	private static boolean sameFrameInsn(AbstractInsnNode o, AbstractInsnNode n) {
		// given that these nodes are computed based on everything else.  if everything else is the same then these
		// must be the same.  A full comparison could be a little ugly as different frames can be equivalent (maybe
		// the compiler produces an incremental frame on one run then a full frame on the next).
		return true;
	}

	private static boolean sameMultiANewArrayInsn(AbstractInsnNode o, AbstractInsnNode n) {
		if (!(n instanceof MultiANewArrayInsnNode)) {
			return false;
		}
		MultiANewArrayInsnNode mnao = (MultiANewArrayInsnNode) o;
		MultiANewArrayInsnNode mnan = (MultiANewArrayInsnNode) n;
		if (!mnao.desc.equals(mnan.desc)) {
			return false;
		}
		if (mnao.dims != mnan.dims) {
			return false;
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	private static boolean sameLookupSwitchInsn(AbstractInsnNode o, AbstractInsnNode n) {
		if (!(n instanceof LookupSwitchInsnNode)) {
			return false;
		}
		LookupSwitchInsnNode lsio = (LookupSwitchInsnNode) o;
		LookupSwitchInsnNode lsin = (LookupSwitchInsnNode) n;
		if (sameLabels(lsio.dflt, lsin.dflt)) {
			return false;
		}
		List<Integer> keyso = lsio.keys;
		List<Integer> keysn = lsin.keys;
		if (keyso.size() != keysn.size()) {
			return false;
		}
		for (int i = 0, max = keyso.size(); i < max; i++) {
			if (keyso.get(i) != keysn.get(i)) {
				return false;
			}
		}
		List<LabelNode> labelso = lsio.labels;
		List<LabelNode> labelsn = lsin.labels;
		if (labelso.size() != labelsn.size()) {
			return false;
		}
		for (int i = 0, max = labelso.size(); i < max; i++) {
			if (!sameLabelNode(labelso.get(i), labelsn.get(i))) {
				return false;
			}
		}
		return true;
	}

	@SuppressWarnings("unchecked")
	private static boolean sameTableSwitchInsn(AbstractInsnNode o, AbstractInsnNode n) {
		if (!(n instanceof TableSwitchInsnNode)) {
			return false;
		}
		TableSwitchInsnNode tsio = (TableSwitchInsnNode) o;
		TableSwitchInsnNode tsin = (TableSwitchInsnNode) n;
		if (sameLabels(tsio.dflt, tsin.dflt)) {
			return false;
		}
		if (tsio.min != tsin.min) {
			return false;
		}
		if (tsio.max != tsin.max) {
			return false;
		}
		List<LabelNode> labelso = tsio.labels;
		List<LabelNode> labelsn = tsin.labels;
		if (labelso.size() != labelsn.size()) {
			return false;
		}
		for (int i = 0, max = labelso.size(); i < max; i++) {
			if (!sameLabelNode(labelso.get(i), labelsn.get(i))) {
				return false;
			}
		}
		return true;
	}

	private static boolean sameLabels(LabelNode lno, LabelNode lnn) {
		// TODO implement?
		return false;
	}

	private static boolean sameFieldInsn(AbstractInsnNode o, AbstractInsnNode n) {
		FieldInsnNode oi = (FieldInsnNode) o;
		if (!(n instanceof FieldInsnNode)) {
			return false;
		}
		FieldInsnNode ni = (FieldInsnNode) n;
		return oi.name.equals(ni.name) && oi.desc.equals(ni.desc) && oi.owner.equals(ni.owner);
	}

	private static boolean sameMethodInsnNode(AbstractInsnNode o, AbstractInsnNode n) {
		MethodInsnNode oi = (MethodInsnNode) o;
		if (!(n instanceof MethodInsnNode)) {
			return false;
		}
		MethodInsnNode ni = (MethodInsnNode) n;
		return oi.name.equals(ni.name) && oi.desc.equals(ni.desc) && oi.owner.equals(ni.owner);
	}

	private static boolean sameVarInsn(AbstractInsnNode o, AbstractInsnNode n) {
		VarInsnNode oi = (VarInsnNode) o;
		if (!(n instanceof VarInsnNode)) {
			return false;
		}
		VarInsnNode ni = (VarInsnNode) n;
		return oi.var == ni.var;
	}

	private static boolean sameInsnNode(AbstractInsnNode o, AbstractInsnNode n) {
		InsnNode oi = (InsnNode) o;
		if (!(n instanceof InsnNode)) {
			return false;
		}
		InsnNode ni = (InsnNode) n;
		return oi.getOpcode() == ni.getOpcode();
	}

	private static boolean sameJumpInsnNode(AbstractInsnNode o, AbstractInsnNode n) {
		//		JumpInsnNode oJumpInsnNode = (JumpInsnNode) o;
		if (!(n instanceof JumpInsnNode)) {
			return false;
		}
		//		JumpInsnNode nJumpInsnNode = (JumpInsnNode) n;
		// TODO tricky to compare destinations when captured as labels with no exposed identifier/position
		return true;
	}

	private static boolean sameLdcInsnNode(AbstractInsnNode o, AbstractInsnNode n) {
		LdcInsnNode oi = (LdcInsnNode) o;
		if (!(n instanceof LdcInsnNode)) {
			return false;
		}
		LdcInsnNode ni = (LdcInsnNode) n;
		Object ocst = oi.cst;
		if (ocst instanceof Integer) {
			if (!(ni.cst instanceof Integer)) {
				return false;
			}
			return ((Integer) ocst).equals(ni.cst);
		}
		if (ocst instanceof Float) {
			if (!(ni.cst instanceof Float)) {
				return false;
			}
			return ((Float) ocst).equals(ni.cst);
		}
		if (ocst instanceof Long) {
			if (!(ni.cst instanceof Long)) {
				return false;
			}
			return ((Long) ocst).equals(ni.cst);
		}
		if (ocst instanceof Double) {
			if (!(ni.cst instanceof Double)) {
				return false;
			}
			return ((Double) ocst).equals(ni.cst);
		}
		if (ocst instanceof String) {
			if (!(ni.cst instanceof String)) {
				return false;
			}
			return ((String) ocst).equals(ni.cst);
		}
		// must be Type
		return ((Type) ocst).equals(ni.cst);
	}

	private static boolean sameIntInsnNode(AbstractInsnNode o, AbstractInsnNode n) {
		IntInsnNode oi = (IntInsnNode) o;
		if (!(n instanceof IntInsnNode)) {
			return false;
		}
		IntInsnNode ni = (IntInsnNode) n;
		return oi.operand == ni.operand;
	}

	private static boolean sameLineNumberNode(AbstractInsnNode o, AbstractInsnNode n) {
		LineNumberNode oi = (LineNumberNode) o;
		if (!(n instanceof LineNumberNode)) {
			return false;
		}
		LineNumberNode ni = (LineNumberNode) n;
		return oi.line == ni.line;
		// TODO check oi.start?
	}

	private static boolean sameIincInsn(AbstractInsnNode o, AbstractInsnNode n) {
		IincInsnNode oi = (IincInsnNode) o;
		if (!(n instanceof IincInsnNode)) {
			return false;
		}
		IincInsnNode ni = (IincInsnNode) n;
		return oi.var == ni.var && oi.incr == ni.incr;
	}

	private static boolean sameTypeInsn(AbstractInsnNode o, AbstractInsnNode n) {
		TypeInsnNode oi = (TypeInsnNode) o;
		if (!(n instanceof TypeInsnNode)) {
			return false;
		}
		TypeInsnNode ni = (TypeInsnNode) n;
		return oi.desc.equals(ni.desc);
	}

	/**
	 * Compare two labels to check they are the same.
	 * 
	 * @param o 'old' label
	 * @param n 'new' label
	 * @return true if they are different
	 */
	private static boolean sameLabelNode(AbstractInsnNode o, AbstractInsnNode n) {
		//		LabelNode oi = (LabelNode) o;
		if (!(n instanceof LabelNode)) {
			return false;
		}
		//		LabelNode ni = (LabelNode) n;

		// TODO tricky to get right.  Unfortunately the positions aren't always available - and we can't check if they are, we have to call the
		// getOffset() method on label and catch an exception if they aren't
		return true;
	}

	private static String compareAnnotations(List<AnnotationNode> oldAnnos, List<AnnotationNode> newAnnos) {
		if (oldAnnos == null) {
			if (newAnnos == null) {
				return "";
			}
			oldAnnos = Collections.emptyList();
		}
		if (newAnnos == null) {
			newAnnos = Collections.emptyList();
		}
		StringBuilder diff = new StringBuilder();
		// Which have been removed
		for (AnnotationNode o : oldAnnos) {
			boolean found = false;
			String oFormatted = Utils.annotationNodeFormat(o);
			for (AnnotationNode n : newAnnos) {
				String nFormatted = Utils.annotationNodeFormat(n);
				if (oFormatted.equals(nFormatted)) {
					found = true;
					break;
				}
			}
			if (!found) {
				diff.append("-").append(oFormatted);
			}
		}
		// Which have been added
		for (AnnotationNode n : newAnnos) {
			boolean found = false;
			String nFormatted = Utils.annotationNodeFormat(n);
			for (AnnotationNode o : oldAnnos) {
				String oFormatted = Utils.annotationNodeFormat(o);
				if (oFormatted.equals(nFormatted)) {
					found = true;
					break;
				}
			}
			if (!found) {
				diff.append("+").append(nFormatted);
			}
		}
		return diff.toString();
	}

	@SuppressWarnings("unchecked")
	private static void computeTypeDelta(ClassNode oldClassNode, ClassNode newClassNode, TypeDelta td) {
		//		if (oldClassNode.version != newClassNode.version) {
		//			td.setTypeVersionChange(oldClassNode.version, newClassNode.version);
		//		}
		if (oldClassNode.access != newClassNode.access) {
			// Is it only because of 0x20000 - that appears to represent Deprecated!
			if ((oldClassNode.access & 0xffff) != (newClassNode.access & 0xffff)) {
				td.setTypeAccessChange(oldClassNode.access, newClassNode.access);
			}
		}
		if (!oldClassNode.name.equals(newClassNode.name)) {
			td.setTypeNameChange(oldClassNode.name, newClassNode.name);
		}
		//		if (oldClassNode.signature == null) {
		//			if (newClassNode.signature != null) {
		//				td.setTypeSignatureChange(oldClassNode.signature, newClassNode.signature);
		//			}
		//		} else if (newClassNode.signature == null) {
		//			if (oldClassNode.signature != null) {
		//				td.setTypeSignatureChange(oldClassNode.signature, newClassNode.signature);
		//			}
		//		} else if (!oldClassNode.signature.equals(newClassNode.signature)) {
		//			td.setTypeSignatureChange(oldClassNode.signature, newClassNode.signature);
		//		}
		if (oldClassNode.superName == null) {
			if (newClassNode.superName != null) {
				td.setTypeSuperNameChange(oldClassNode.superName, newClassNode.superName);
			}
		}
		else if (newClassNode.superName == null) {
			if (oldClassNode.superName != null) {
				td.setTypeSuperNameChange(oldClassNode.superName, newClassNode.superName);
			}
		}
		else if (!oldClassNode.superName.equals(newClassNode.superName)) {
			td.setTypeSuperNameChange(oldClassNode.superName, newClassNode.superName);
		}
		if (oldClassNode.interfaces.size() == 0) {
			if (newClassNode.interfaces.size() != 0) {
				td.setTypeInterfacesChange(oldClassNode.interfaces, newClassNode.interfaces);
			}
		}
		else if (newClassNode.interfaces.size() == 0) {
			if (oldClassNode.interfaces.size() != 0) {
				td.setTypeInterfacesChange(oldClassNode.interfaces, newClassNode.interfaces);
			}
		}
		else {
			if (oldClassNode.interfaces.size() != newClassNode.interfaces.size()) {
				td.setTypeInterfacesChange(oldClassNode.interfaces, newClassNode.interfaces);
			}
			HashSet<String> oldInterfaceSet = new HashSet<String>(oldClassNode.interfaces);
			HashSet<String> newInterfaceSet = new HashSet<String>(newClassNode.interfaces);
			if (!oldInterfaceSet.equals(newInterfaceSet)) { // TODO expensive? keep the interfaces list sorted instead?
				td.setTypeInterfacesChange(oldClassNode.interfaces, newClassNode.interfaces);
			}
		}
	}
}
