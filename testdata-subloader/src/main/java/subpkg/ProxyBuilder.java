package subpkg;

import java.lang.reflect.UndeclaredThrowableException;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.transform.impl.UndeclaredThrowableStrategy;

public class ProxyBuilder {

	static <T> T createProxyFor(Class<T> clazz, MethodInterceptor mi) {
		Enhancer enhancer = new Enhancer();
		//		if (classLoader != null) {
		//			enhancer.setClassLoader(classLoader);
		//			if (classLoader instanceof SmartClassLoader &&
		//					((SmartClassLoader) classLoader).isClassReloadable(proxySuperClass)) {
		//				enhancer.setUseCache(false);
		//			}
		//		}
		enhancer.setSuperclass(clazz);
		enhancer.setStrategy(new UndeclaredThrowableStrategy(UndeclaredThrowableException.class));
		enhancer.setInterfaces(null);//AopProxyUtils.completeProxiedInterfaces(this.advised));
		enhancer.setInterceptDuringConstruction(false);

		Callback[] callbacks = new Callback[] { mi };//getCallbacks(rootClass);
		enhancer.setCallbacks(callbacks);
		//		enhancer.setCallbackFilter(new ProxyCallbackFilter(
		//				this.advised.getConfigurationOnlyCopy(), this.fixedInterceptorMap, this.fixedInterceptorOffset));

		Class<?>[] types = new Class[callbacks.length];
		for (int x = 0; x < types.length; x++) {
			types[x] = callbacks[x].getClass();
		}
		enhancer.setCallbackTypes(types);

		// Generate the proxy class and create a proxy instance.
		//		Object proxy;
		//		if (this.constructorArgs != null) {
		//			proxy = enhancer.create(this.constructorArgTypes, this.constructorArgs);
		//		}
		//		else {
		@SuppressWarnings("unchecked")
		T proxy = (T) enhancer.create();
		//		}
		return proxy;
	}
}
