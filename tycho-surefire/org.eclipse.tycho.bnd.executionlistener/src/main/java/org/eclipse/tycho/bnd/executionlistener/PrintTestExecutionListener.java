/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 ******************************************************************************/
package org.eclipse.tycho.bnd.executionlistener;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.osgi.service.component.annotations.Component;

@Component(service = TestExecutionListener.class, immediate = true)
public class PrintTestExecutionListener implements TestExecutionListener {
	@Override
	public void executionStarted(TestIdentifier testIdentifier) {
		System.out.println(testIdentifier.getDisplayName() + ": STARTED");
	}

	@Override
	public void executionSkipped(TestIdentifier testIdentifier, String reason) {
		System.out.println(testIdentifier.getDisplayName() + ": SKIPPED");
	}

	@Override
	public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
		System.out.println(testIdentifier.getDisplayName() + ": " + testExecutionResult.getStatus());
	}
}
