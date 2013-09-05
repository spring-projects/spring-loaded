/*******************************************************************************
 * Copyright (c) 2011 SpringSource.  All rights reserved.
 *******************************************************************************/
package com.test.plugins;

import org.springsource.loaded.ReloadEventProcessorPlugin;
import org.springsource.loaded.TypeDelta;
import org.springsource.loaded.UnableToReloadEventProcessorPlugin;

/**
 * 
 * @author Andy Clement
 * @since 0.7.2
 */
public class ReloadEventProcessorPlugin1 implements ReloadEventProcessorPlugin, UnableToReloadEventProcessorPlugin {

	public ReloadEventProcessorPlugin1() {
		System.out.println("Instantiated ReloadEventProcessorPlugin1");
	}

	public void reloadEvent(String typename, Class<?> clazz, String encodedTimestamp) {
		System.out.println("ReloadEventProcessorPlugin1: reloadEvent(" + typename + "," + clazz.getName() + "," + encodedTimestamp
				+ ")");
	}

	public void unableToReloadEvent(String typename, Class<?> clazz, TypeDelta typeDelta, String encodedTimestamp) {
		System.out.println("ReloadEventProcessorPlugin1: unableToReloadEvent(" + typename + "," + clazz.getName() + ","
				+ encodedTimestamp + ")");
	}

	@Override
	public boolean shouldRerunStaticInitializer(String typename, Class<?> clazz, String encodedTimestamp) {
		System.out.println("ReloadEventProcessorPlugin1: shouldRerunStaticInitializer(" + typename + "," + clazz.getName() + ","
				+ encodedTimestamp + ")");
		return false;
	}

}
