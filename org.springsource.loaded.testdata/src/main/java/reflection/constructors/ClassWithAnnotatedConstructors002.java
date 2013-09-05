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
public class ClassWithAnnotatedConstructors002 {

	// We want our reloaded version to have 
	//      - additional constructors (with annotations)
	//      - constructors with changed annotations
	
	//The annotation will be removed
	@SuppressWarnings("unused")
	private /* @AnnoT */ ClassWithAnnotatedConstructors002() {}
	
	//The attribute value will be changed
	public @AnnoT3(/*"first"*/ "second") ClassWithAnnotatedConstructors002(int x) {}

	//Annotations will be added
	protected @AnnoT @AnnoT3("haa002") ClassWithAnnotatedConstructors002(double x) {}

	//Annotations will be changed (some added some removed)
	protected /*@AnnoT*/ @AnnoT3("haa") /*+*/ @AnnoT2 ClassWithAnnotatedConstructors002(boolean x) {}
	
	//Annotations are not changed at all
	public @AnnoT @AnnoT2 @AnnoT3("haa") ClassWithAnnotatedConstructors002(char x) {}
	
	// Annotations in the parameters will change
	public ClassWithAnnotatedConstructors002(@AnnoT3("002") String x, @AnnoT2 double y, boolean z) {}
	
	// Annotations in the parameters will be removed
	public ClassWithAnnotatedConstructors002(double x, double y, boolean z) {}
	
	// Annotations in the parameters will be added
	public ClassWithAnnotatedConstructors002(@AnnoT char x, @AnnoT2 String y, @AnnoT2 @AnnoT3("bongo") @AnnoT boolean z) {}
	
	///////////////////////////////////////////
	// Some new constructors with and without annotations

	public @AnnoT @AnnoT2 @AnnoT3("haa") ClassWithAnnotatedConstructors002(String x) {}
	
	public ClassWithAnnotatedConstructors002(Float x) {}
	
	public @AnnoT2 ClassWithAnnotatedConstructors002(float x) {}
	
	public ClassWithAnnotatedConstructors002(float x, @AnnoT2 String y, @AnnoT2 @AnnoT3("bongo") @AnnoT boolean z) {}
	
	
}
