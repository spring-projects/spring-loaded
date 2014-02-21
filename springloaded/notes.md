
Implementation details:

catchers
========
- A catcher is added to a reloadable type as it is being loaded for the first time. A reloadable type gets
a catcher for each method it inherits from a parent but does not override, or for an abstract class each
method it receives from an interface.  They basically stand in for methods that could be added in later
reloads. The catchers 'catch' virtual method dispatches. Their job is either to (a) make a simple super
call if the reloadable type does not implement the specified method (yet), or (b) if the method
has been implemented by a reload, forward to the new implementation.  Catchers are not created for
private, final or static methods since those cannot be overridden.  If you modify a final method in a reloadable
type to make it non final and then override it, it will be handled in a different way than catchers.

superdispatchers
================
- Where a method is protected in a non-reloadable type it is necessary to add a superdispatcher method to the
subtype so that when the executor for a new version is running it can access that protected method. The 
superdispatcher is simply a public method on a type that calls super.

Generic dispatcher method __execute
===================================

The way in which we handle new methods appearing on types is that all reloadable types get a generic handler method added to
them when first loaded - this can forward it on to the new method that has appeared. The method is like this:

__execute(Object[] params, Object target, String nameAndDescriptor)

All interfaces also get this method.

(#001) Lambdas introduce a problem here. The Lambda meta factory creates anonymous classes that forward to the lambda handling
method. It does this outside of our control using a more direct form of class defining which we don't see (can't instrument). This means
these generated classes don't get an __execute. This means if a new method is added to the SAM type, although we notice it
we can't generate an __execute in the meta factory created class.  This means the standard redirection of INVOKEINTERFACE which says:
does this method exist on the original form of the type? yes, then call it. no, then call the __execute telling it what we'd like to run.
Well that will fail because of the missing __execute. There are two solutions:
- modify the InnerClassLambdaMetaFactory to ensure an __execute (and relevant marker interface) are added
- change how we handle the INVOKEINTERFACE rewrite.

The second option is cheap to implement but performance will likely suck. The simplest way to do it is call the type registry to do the
suitable invoke via reflection - and it will recognize the lambda case and know what to do.



----------
Helpful snippets when debugging tests:
	ClassPrinter.print(z.getLatestExecutorBytes());
	Utils.dump("foo/SubControllerB", rtype.bytesLoaded);
		


		
		