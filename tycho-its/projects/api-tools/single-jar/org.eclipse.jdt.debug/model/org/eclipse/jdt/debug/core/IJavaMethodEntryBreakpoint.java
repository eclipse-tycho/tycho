/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.jdt.debug.core;

import org.eclipse.core.runtime.CoreException;

/**
 * A method entry breakpoint suspends execution on the first executable line of
 * a method when entered. Entry breakpoints can only be installed in methods
 * that have executable code (i.e. do not work in native methods).
 * <p>
 * This breakpoint provides a subset of the function provided by
 * <code>IJavaMethodBreakpoint</code> - i.e. break on enter. The implementation
 * of this breakpoint is more efficient than the general method breakpoint, as
 * the implementation is based on line breakpoints and does not require method
 * enter/exit tracing in the target VM.
 * </p>
 *
 * @since 2.0
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IJavaMethodEntryBreakpoint extends IJavaLineBreakpoint {

	/**
	 * Returns the name of the method this breakpoint suspends execution in.
	 *
	 * @return the name of the method this breakpoint suspends execution in
	 * @exception CoreException
	 *                if unable to access the property from this breakpoint's
	 *                underlying marker
	 */
	public String getMethodName() throws CoreException;

	/**
	 * Returns the signature of the method this breakpoint suspends execution
	 * in.
	 *
	 * @return the signature of the method this breakpoint suspends execution in
	 * @exception CoreException
	 *                if unable to access the property from this breakpoint's
	 *                underlying marker
	 */
	public String getMethodSignature() throws CoreException;

}
