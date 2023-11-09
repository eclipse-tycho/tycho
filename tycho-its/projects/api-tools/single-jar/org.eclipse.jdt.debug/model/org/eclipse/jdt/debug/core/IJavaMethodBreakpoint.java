/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
import org.eclipse.debug.core.model.IDebugTarget;

/**
 * A method breakpoint suspends execution when a method is entered or exited.
 *
 * @since 2.0
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IJavaMethodBreakpoint extends IJavaLineBreakpoint {

	/**
	 * Returns the name of the method(s) this breakpoint suspends execution in,
	 * or <code>null</code> if this breakpoint does not suspend execution based
	 * on method name.
	 *
	 * @return the name of the method(s) this breakpoint suspends execution in,
	 *         or <code>null</code> if this breakpoint does not suspend
	 *         execution based on method name
	 * @exception CoreException
	 *                if unable to access the property from this breakpoint's
	 *                underlying marker
	 */
	public String getMethodName() throws CoreException;

	/**
	 * Returns the signature of the method(s) this breakpoint suspends execution
	 * in, or <code>null</code> if this breakpoint does not suspend execution
	 * based on method signature.
	 *
	 * @return the signature of the method(s) this breakpoint suspends execution
	 *         in, or <code>null</code> if this breakpoint does not suspend
	 *         execution based on method signature
	 * @exception CoreException
	 *                if unable to access the property from this breakpoint's
	 *                underlying marker
	 */
	public String getMethodSignature() throws CoreException;

	/**
	 * Returns the pattern specifying the fully qualified name of type(s) this
	 * breakpoint suspends execution in. Patterns are limited to exact matches
	 * and patterns that begin or end with '*'.
	 *
	 * @return the pattern specifying the fully qualified name of type(s) this
	 *         breakpoint suspends execution in
	 * @exception CoreException
	 *                if unable to access the property from this breakpoint's
	 *                underlying marker
	 * @see IJavaBreakpoint#getTypeName()
	 */
	@Override
	public String getTypeName() throws CoreException;

	/**
	 * Returns whether this breakpoint causes execution to suspend on entry to
	 * methods.
	 *
	 * @return whether this breakpoint causes execution to suspend on entry to
	 *         methods
	 * @exception CoreException
	 *                if unable to access the property from this breakpoint's
	 *                underlying marker
	 */
	public boolean isEntry() throws CoreException;

	/**
	 * Returns whether this breakpoint causes execution to suspend on exit of
	 * methods.
	 *
	 * @return whether this breakpoint causes execution to suspend on exit of
	 *         methods
	 * @exception CoreException
	 *                if unable to access the property from this breakpoint's
	 *                underlying marker
	 */
	public boolean isExit() throws CoreException;

	/**
	 * Sets whether this breakpoint causes execution to suspend on entry to
	 * methods.
	 *
	 * @param entry
	 *            whether this breakpoint causes execution to suspend on entry
	 *            to methods
	 * @exception CoreException
	 *                if unable to set the property on this breakpoint's
	 *                underlying marker
	 */
	public void setEntry(boolean entry) throws CoreException;

	/**
	 * Sets whether this breakpoint causes execution to suspend on exit of
	 * methods.
	 *
	 * @param exit
	 *            whether this breakpoint causes execution to suspend on exit of
	 *            methods
	 * @exception CoreException
	 *                if unable to set the property on this breakpoint's
	 *                underlying marker
	 */
	public void setExit(boolean exit) throws CoreException;

	/**
	 * Sets whether this breakpoint causes execution to suspend only on
	 * entry/exit of native methods.
	 *
	 * @param nativeOnly
	 *            whether this breakpoint causes execution to suspend only on
	 *            entry/exit of native methods
	 * @exception CoreException
	 *                if unable to set the property on this breakpoint's
	 *                underlying marker
	 */
	public void setNativeOnly(boolean nativeOnly) throws CoreException;

	/**
	 * Returns whether this breakpoint causes execution to suspend only on
	 * entry/exit of native methods.
	 *
	 * @return whether this breakpoint causes execution to suspend only on
	 *         entry/exit of native methods
	 * @exception CoreException
	 *                if unable to access the property on this breakpoint's
	 *                underlying marker
	 */
	public boolean isNativeOnly() throws CoreException;

	/**
	 * Returns whether this breakpoint last suspended in the given target due to
	 * a method entry (<code>true</code>) or exit (<code>false</code>).
	 *
	 * @param target
	 *            the debug target
	 *
	 * @return <code>true</code> if this breakpoint last suspended the given
	 *         target due to a method entry; <code>false</code> if this
	 *         breakpoint last suspended in the given target due to a method
	 *         exit or if this breakpoint hasn't suspended the given target.
	 */
	public boolean isEntrySuspend(IDebugTarget target);
}
