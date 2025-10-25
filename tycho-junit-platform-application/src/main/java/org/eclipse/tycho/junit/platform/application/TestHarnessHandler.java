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

/**
 * Facade to use the Eclipse "Testing Harness" facility without directly depend
 * on it.
 */
public interface TestHarnessHandler {

	/**
	 * Sets a runnable that is invoked once the harness is ready to accept test runs
	 * 
	 * @param startRunnable
	 */
	public void setStartRunnable(Runnable startRunnable);

	/**
	 * Notifies the harness that we are starting the test and it should invoke the
	 * startRunnable when ready.
	 */
	public void testingStarting();

	/**
	 * Runs the given test runnable in the context of the harness (usually the UI thread)
	 *
	 * @param testRunnable the test runnable to run
	 */
	public void runTest(Runnable testRunnable);

	/**
	 * Notifies the harness that it should shutdown
	 */
	public void testingFinished();
}
