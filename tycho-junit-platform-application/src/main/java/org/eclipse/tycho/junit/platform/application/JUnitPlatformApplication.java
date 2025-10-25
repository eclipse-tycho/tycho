/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.junit.platform.application;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Application to run junit-platform inside OSGi
 */
public class JUnitPlatformApplication implements IApplication {

	private static final String APPLICATION_ARGS = "application.args";

	@Override
	public Object start(IApplicationContext context) throws Exception {
		Object argOption = context.getArguments().get(APPLICATION_ARGS);
		if (argOption instanceof String[]) {
			String[] args = (String[]) argOption;
			return runWithArguments(context, new ApplicationArguments(args));
		}
		throw new IllegalArgumentException("No application arguments where given!");
	}

	private Object runWithArguments(IApplicationContext context, ApplicationArguments args) throws Exception {

		Optional<String> harness = args.getArgument("--eclipse-testing-harness");
		List<String> junitArguments = args.getRemainingArguments("--junit-platform-arguments");
		if (harness.isPresent()) {
			boolean runInUI = args.hasArgument("--run-in-application-thread");
			BundleContext bundleContext = FrameworkUtil.getBundle(JUnitPlatformApplication.class).getBundleContext();
			ServiceTracker<TestHarnessHandler, TestHarnessHandler> tracker = new ServiceTracker<>(bundleContext,
					TestHarnessHandler.class, null);
			try {
				TestHarnessHandler harnesHandler = tracker.waitForService(TimeUnit.SECONDS.toMillis(10));
				if (harnesHandler == null) {
					throw new IllegalStateException("The Application Harness Service does not showed up in time!");
				}
				harnesHandler.setStartRunnable(() -> {
					harnesHandler.testingStarting();
					if (runInUI) {
						harnesHandler.runTest(() -> {
							runTests(junitArguments);
						});
					} else {
						runTests(junitArguments);
					}
					harnesHandler.testingFinished();
				});
				return runApplication(harness.get(), context, args.toArray());
			} finally {
				tracker.close();
			}
		} else {
			runTests(junitArguments);
			return IApplication.EXIT_OK;
		}
	}

	private void runTests(List<String> junitArguments) {
		// TODO Auto-generated method stub
		System.out.println("JUnitPlatformApplication.runTests()");
	}

	@SuppressWarnings("unchecked")
	private Object runApplication(String id, IApplicationContext context, String[] array) throws Exception {
		IExtension extension = RegistryFactory.getRegistry().getExtension("org.eclipse.core.runtime", "applications",
				id);
		IConfigurationElement[] elements = extension.getConfigurationElements();
		if (elements.length > 0) {
			IConfigurationElement[] runs = elements[0].getChildren("run");
			if (runs.length > 0) {
				Object executableExtension = runs[0].createExecutableExtension("class");
				if (executableExtension instanceof IApplication) {
					IApplication application = (IApplication) executableExtension;
					context.getArguments().put(APPLICATION_ARGS, array);
					return application.start(context);
				}
			}
		}
		throw new IllegalArgumentException("Application with ID '" + id + "' not found!");
	}

	@Override
	public void stop() {
	}

}
