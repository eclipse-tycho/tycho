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
 *    
 */
package org.eclipse.tycho.testing.plugin;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class JUnitPlatformRunnerResult implements Serializable {

	private List<String> debugMessages = new ArrayList<>();
	private List<String> infoMessages = new ArrayList<>();
	private Exception exception;

	public void debug(String debugMessage) {
		debugMessages.add(debugMessage);
	}

	public void info(String debugMessage) {
		infoMessages.add(debugMessage);
	}

	public JUnitPlatformRunnerResult setException(Exception exception) {
		this.exception = exception;
		return this;
	}

	public JUnitPlatformRunnerResult failure(String message) {
		return setException(new JUnitPlatformFailureException(message));
	}

	public JUnitPlatformRunnerResult setExitCode(int exitCode) {
		if (exitCode == 0) {
			return this;
		}
		if (exitCode == 2) {
			return failure("No tests found!");
		}
		if (exitCode == 1) {
			return failure("There are test failures");
		}
		return failure("Tool execution failed with exit code " + exitCode);
	}

	public Stream<String> debugMessages() {
		return debugMessages.stream();
	}

	public Stream<String> infoMessages() {
		return infoMessages.stream();
	}

	public Exception getException() {
		return exception;
	}

}
