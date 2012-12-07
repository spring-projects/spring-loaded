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

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Modify a class by changing it from one name to another. References to other types can also be changed. Basically used in the test
 * suite.
 * 
 * @author Andy Clement
 * @since 0.5.0
 */
public class ClassRenamer {

	/**
	 * Rename a type - changing it to specified new name (which should be the dotted form of the name). Retargets are an optional
	 * sequence of retargets to also perform during the rename. Retargets take the form of "a.b:a.c" which will change all
	 * references to a.b to a.c.
	 * 
	 * @param dottedNewName dotted name, e.g. com.foo.Bar
	 * @param classbytes the bytecode for the class to be renamed
	 * @param retargets retarget rules for references, of the form "a.b:b.a","c.d:d.c"
	 * @return bytecode for the modified class
	 */
	public static byte[] rename(String dottedNewName, byte[] classbytes, String... retargets) {
		ClassReader fileReader = new ClassReader(classbytes);
		RenameAdapter renameAdapter = new RenameAdapter(dottedNewName, retargets);
		fileReader.accept(renameAdapter, 0);
		byte[] renamed = renameAdapter.getBytes();
		return renamed;
	}

	static class RenameAdapter extends ClassAdapter implements Opcodes {

		private ClassWriter cw;
		private String oldname;
		private String newname;
		private Map<String, String> retargets = new HashMap<String, String>();

		public RenameAdapter(String newname, String[] retargets) {
			super(new ClassWriter(0));
			cw = (ClassWriter) cv;
			this.newname = newname.replace('.', '/');
			if (retargets != null) {
				for (String retarget : retargets) {
					int i = retarget.indexOf(":");
					this.retargets.put(retarget.substring(0, i).replace('.', '/'), retarget.substring(i + 1).replace('.', '/'));
				}
			}
		}

		public byte[] getBytes() {
			return cw.toByteArray();
		}

		private String retargetIfNecessary(String string) {
			String value = retargets.get(string);
			return value == null ? string : value;
		}

		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			oldname = name;
			if (superName != null) {
				superName = retargetIfNecessary(superName);
			}
			if (interfaces != null) {
				for (int i = 0; i < interfaces.length; i++) {
					interfaces[i] = retargetIfNecessary(interfaces[i]);
				}
			}
			super.visit(version, access, newname, signature, superName, interfaces);
		}

		@Override
		public MethodVisitor visitMethod(int flags, String name, String descriptor, String signature, String[] exceptions) {
			if (descriptor.indexOf(oldname) != -1) {
				descriptor = descriptor.replace(oldname, newname);
			} else {
				for (String s : retargets.keySet()) {
					if (descriptor.indexOf(s) != -1) {
						descriptor = descriptor.replace(s, retargets.get(s));
					}
				}
			}
			MethodVisitor mv = super.visitMethod(flags, name, descriptor, signature, exceptions);
			return new RenameMethodAdapter(mv, oldname, newname);
		}

		@Override
		public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
			if (desc.indexOf(oldname) != -1) {
				desc = desc.replace(oldname, newname);
			} else {
				for (String s : retargets.keySet()) {
					if (desc.indexOf(s) != -1) {
						desc = desc.replace(s, retargets.get(s));
					}
				}
			}
			return super.visitField(access, name, desc, signature, value);
		}

		class RenameMethodAdapter extends MethodAdapter implements Opcodes {

			String oldname;
			String newname;

			public RenameMethodAdapter(MethodVisitor mv, String oldname, String newname) {
				super(mv);
				this.oldname = oldname;
				this.newname = newname;
			}

			public void visitFieldInsn(int opcode, String owner, String name, String desc) {
				if (owner.equals(oldname)) {
					owner = newname;
				} else {
					String retarget = retargets.get(owner);
					if (retarget != null) {
						owner = retarget;
					}
				}
				if (desc.indexOf(oldname) != -1) {
					desc = desc.replace(oldname, newname);
				} else {
					desc = checkIfShouldBeRewritten(desc);
				}
				mv.visitFieldInsn(opcode, owner, name, desc);
			}

			public void visitTypeInsn(int opcode, String type) {
				if (type.equals(oldname)) {
					type = newname;
				} else {
					String retarget = retargets.get(type);
					if (retarget != null) {
						type = retarget;
					} else {
						if (type.startsWith("[")) {
							if (type.indexOf(oldname) != -1) {
								type = type.replaceFirst(oldname, newname);
							}
						}
					}
				}
				mv.visitTypeInsn(opcode, type);
			}

			@Override
			public void visitLdcInsn(Object obj) {
//				System.out.println("Possibly remapping "+obj);
				if (obj instanceof Type) {
					Type t = (Type) obj;
					String s = t.getInternalName();
					String retarget = retargets.get(s);
					if (retarget != null) {
						mv.visitLdcInsn(Type.getObjectType(retarget));
					} else {
						mv.visitLdcInsn(obj);
					}
				} else if (obj instanceof String) {
					String s = (String) obj;
					String retarget = retargets.get(s.replace('.', '/'));
					if (retarget != null) {
						mv.visitLdcInsn(retarget.replace('/', '.'));
					} else {
						String oldnameDotted = oldname.replace('/', '.');
						if (s.equals(oldnameDotted)) {
							String nname = newname.replace('/', '.');
							mv.visitLdcInsn(nname);
							return;
						} else if (s.startsWith("[")) {
							// might be array of oldname
							if (s.indexOf(oldnameDotted) != -1) {
								mv.visitLdcInsn(s.replaceFirst(oldnameDotted, newname.replace('/', '.')));
								return;
							}
						}
						mv.visitLdcInsn(obj);
					}
				} else {
					mv.visitLdcInsn(obj);
				}
			}

			public void visitMethodInsn(int opcode, String owner, String name, String desc) {
				if (owner.equals(oldname)) {
					owner = newname;
				} else {
					owner = retargetIfNecessary(owner);
				}
				if (desc.indexOf(oldname) != -1) {
					desc = desc.replace(oldname, newname);
				} else {
					desc = checkIfShouldBeRewritten(desc);
				}
				mv.visitMethodInsn(opcode, owner, name, desc);
			}

			private String checkIfShouldBeRewritten(String desc) {
				for (String s : retargets.keySet()) {
					if (desc.indexOf(s) != -1) {
						desc = desc.replace(s, retargets.get(s));
					}
				}
				return desc;
			}
		}
	}

}
