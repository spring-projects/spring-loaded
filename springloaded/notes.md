
Implementation details:

catchers
- A catcher is added to a reloadable type as it is being loaded for the first time. A reloadable type gets
a catcher for each method it inherits from a parent but does not override, or for an abstract class each
method it receives from an interface.  They basically stand in for methods that could be added in later
reloads. The catchers 'catch' virtual method dispatches. Their job is either to (a) make a simple super
call if the reloadable type does not implement the specified method (yet), or (b) if the method
has been implemented by a reload, forward to the new implementation.  Catchers are not created for
private, final or static methods since those cannot be overridden.  If you modify a final method in a reloadable
type to make it non final and then override it, it will be handled in a different way than catchers.

superdispatchers
- Where a method is protected in a non-reloadable type it is necessary to add a superdispatcher method to the
subtype so that when the executor for a new version is running it can access that protected method. The 
superdispatcher is simply a public method on a type that calls super.



Helpful snippets when debugging tests:
	ClassPrinter.print(z.getLatestExecutorBytes());
	Utils.dump("foo/SubControllerB", rtype.bytesLoaded);
		


		
		