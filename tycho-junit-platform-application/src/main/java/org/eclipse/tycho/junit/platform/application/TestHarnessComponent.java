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

import org.eclipse.ui.testing.ITestHarness;
import org.eclipse.ui.testing.TestableObject;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * Implementation of the {@link TestHarnessHandler} that delegate to eclipse
 */
@Component(service = TestHarnessHandler.class)
public class TestHarnessComponent implements TestHarnessHandler {

	private TestableObject testableObject;

	/**
	 * Creates an instance that uses the given testable object
	 * 
	 * @param testableObject
	 */
	@Activate
	public TestHarnessComponent(@Reference TestableObject testableObject) {
		this.testableObject = testableObject;
	}

	@Override
	public void setStartRunnable(Runnable startRunnable) {
		testableObject.setTestHarness(new ITestHarness() {

			@Override
			public void runTests() {
				startRunnable.run();
			}
		});

	}

	@Override
	public void testingStarting() {
		testableObject.testingStarting();
	}

	@Override
	public void runTest(Runnable testRunnable) {
		testableObject.runTest(testRunnable);
	}

	@Override
	public void testingFinished() {
		testableObject.testingFinished();
	}

}
