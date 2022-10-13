/*******************************************************************************
 * Copyright (c) 2003, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ui.testing;

import org.eclipse.core.runtime.AssertionFailedException;

/**
 * A testable object. Allows a test harness to register itself with a testable object. The test
 * harness is notified of test-related lifecycle events, such as when is an appropriate time to run
 * tests on the object. This also provides API for running tests as a runnable, and for signaling
 * when the tests are starting and when they are finished.
 * <p>
 * The workbench provides an implementation of this facade, available via
 * <code>PlatformUI.getTestableObject()</code>.
 * </p>
 *
 * @since 3.0
 */
public class TestableObject {

    private ITestHarness testHarness;

    public ITestHarness getTestHarness() {
        return testHarness;
    }

    /**
     * Sets the test harness.
     *
     * @param testHarness
     *            the test harness
     */
    public void setTestHarness(ITestHarness testHarness) {
        if (testHarness == null)
            throw new AssertionFailedException("null argument:");
        this.testHarness = testHarness;
    }

    /**
     * Runs the given test runnable. The default implementation simply invokes <code>run</code> on
     * the given test runnable. Subclasses may extend.
     *
     * @param testRunnable
     *            the test runnable to run
     */
    public void runTest(Runnable testRunnable) {
        testRunnable.run();
    }

    /**
     * Notification from the test harness that it is starting to run the tests. The default
     * implementation does nothing. Subclasses may override.
     */
    public void testingStarting() {
        // do nothing
    }

    /**
     * Notification from the test harness that it has finished running the tests. The default
     * implementation does nothing. Subclasses may override.
     */
    public void testingFinished() {
        // do nothing
    }
}
