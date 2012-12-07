package reflection.constructors;

import reflection.AnnoT;
import reflection.AnnoT2;
import reflection.AnnoT3;

/**
 * For testing constructor reloading and methods related to fetching annotation data from
 * constructors.
 * 
 * @author kdvolder
 */
public class ClassWithAnnotatedConstructors {

	// We want our reloaded version to have 
	//      - additional constructors (with annotations)
	//      - constructors with changed annotations
	
	//The annotation will be removed
	@SuppressWarnings("unused")
	private @AnnoT ClassWithAnnotatedConstructors() {}
	
	//The attribute value will be changed
	public @AnnoT3("first") ClassWithAnnotatedConstructors(int x) {}

	//Annotations will be added
	protected ClassWithAnnotatedConstructors(double x) {}

	//Annotations will be changed (some added some removed)
	protected @AnnoT @AnnoT3("haa") ClassWithAnnotatedConstructors(boolean x) {}
	
	//Annotations are not changed at all
	public @AnnoT @AnnoT2 @AnnoT3("haa") ClassWithAnnotatedConstructors(char x) {}

	// Annotations in the parameters will change
	public ClassWithAnnotatedConstructors(@AnnoT2 String x, @AnnoT3("bah") double y, @AnnoT boolean z) {}
	
	// Annotations in the parameters will be removed
	public ClassWithAnnotatedConstructors(@AnnoT2 double x, @AnnoT3("boohoo") double y, @AnnoT boolean z) {}
	
	// Annotations in the parameters will be added
	public ClassWithAnnotatedConstructors(char x, double y, boolean z) {}
}
