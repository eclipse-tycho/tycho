/*******************************************************************************
 * Copyright (c) 2008, 2026 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.verifier;

/**
 * Signals a failure of a {@link Verifier} check, or of the forked Maven build it launched.
 * <p>
 * This mirrors the exception shape of the retired {@code org.apache.maven.it.VerificationException}
 * (maven-verifier 1.8.0) so that porting call sites is a package rename only.
 */
public class VerificationException extends Exception {

	private static final long serialVersionUID = 1L;

	public VerificationException() {
		super();
	}

	public VerificationException(String message) {
		super(message);
	}

	public VerificationException(Throwable cause) {
		super(cause);
	}

	public VerificationException(String message, Throwable cause) {
		super(message, cause);
	}

}
