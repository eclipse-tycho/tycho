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
 * A line breakpoint installed in types associated with a specific source file
 * (based on source file name debug attribute) and whose fully qualified name
 * matches a specified pattern per target. The {target, type name pattern} pairs
 * are not persisted with this breakpoint, as targets are transient. Clients
 * that use this type of breakpoint are intended to be breakpoint listeners that
 * set a pattern per target as each breakpoint is added to a target.
 *
 * @see org.eclipse.jdt.debug.core.IJavaBreakpointListener
 * @since 2.0
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IJavaTargetPatternBreakpoint extends IJavaLineBreakpoint {

	/**
	 * Returns the type name pattern this breakpoint uses to identify types in
	 * which to install itself in the given target
	 *
	 * @param target
	 *            debug target
	 * @return the type name pattern this breakpoint uses to identify types in
	 *         which to install itself in the given target
	 */
	public String getPattern(IJavaDebugTarget target);

	/**
	 * Sets the type name pattern this breakpoint uses to identify types in
	 * which to install itself in the given target
	 *
	 * @param target
	 *            debug target
	 * @param pattern
	 *            type name pattern
	 * @exception CoreException
	 *                if changing the pattern for this breakpoint fails
	 */
	public void setPattern(IJavaDebugTarget target, String pattern)
			throws CoreException;

	/**
	 * Returns the source file name in which this breakpoint is set. When this
	 * breakpoint specifies a source file name, this breakpoint is only
	 * installed in types whose source file name debug attribute match this
	 * value.
	 *
	 * @return the source file name in which this breakpoint is set
	 * @exception CoreException
	 *                if unable to access the property from this breakpoint's
	 *                underlying marker
	 */
	public String getSourceName() throws CoreException;
}
