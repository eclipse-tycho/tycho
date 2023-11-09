/*******************************************************************************
 * Copyright (c) 2003, 2011 IBM Corporation and others.
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
 * A line breakpoint identified by its source file name and/or path, and stratum
 * that it is relative to.
 *
 * @since 3.0
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IJavaStratumLineBreakpoint extends IJavaLineBreakpoint {

	/**
	 * Returns the type name pattern this breakpoint uses to identify types in
	 * which to install itself. Patterns may begin or end with '*'. Will not
	 * return <code>null</code>. In the case that a stratum breakpoint was
	 * created with a <code>null</code> pattern, "*" is returned.
	 * <p>
	 * Multiple patterns can be specified in this breakpoint by delimiting the
	 * patterns with a comma - e.g. "x.y.z,a.b.c".
	 * </p>
	 *
	 * @return the type name patterns this breakpoint uses to identify types in
	 *         which to install itself.
	 * @exception CoreException
	 *                if unable to access the property from this breakpoint's
	 *                underlying marker
	 */
	public String getPattern() throws CoreException;

	/**
	 * Returns the simple name of the source file in which this breakpoint is
	 * set, or <code>null</code>. When this breakpoint specifies a source file
	 * name, this breakpoint is only installed in types whose source file name
	 * debug attribute matches this value, relative to this breakpoint's
	 * stratum.
	 *
	 * @return the source file name in which this breakpoint is set, or
	 *         <code>null</code>
	 * @exception CoreException
	 *                if unable to access the property from this breakpoint's
	 *                underlying marker
	 */
	public String getSourceName() throws CoreException;

	/**
	 * Returns the stratum that this breakpoint's source name, source path, and
	 * line number are relative to, or <code>null</code> if this breakpoint is
	 * relative to a type's default stratum.
	 *
	 * @return the stratum that this breakpoint's source name, source path, and
	 *         line number are relative to, or <code>null</code>
	 * @throws CoreException
	 *             if unable to access the property from this breakpoint's
	 *             underlying marker
	 */
	public String getStratum() throws CoreException;

	/**
	 * Returns the qualified source file path in which this breakpoint is set,
	 * or <code>null</code>. When specified, this breakpoint is only installed
	 * in types whose source file path debug attribute matches this value,
	 * relative to this breakpoint's stratum.
	 *
	 * @return the qualified source file path in which this breakpoint is set,
	 *         or <code>null</code>
	 * @throws CoreException
	 *             if unable to get the source path
	 */
	public String getSourcePath() throws CoreException;

}
