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

import org.eclipse.debug.core.DebugException;

/**
 * The type of an array on a Java debug target.
 *
 * @see IJavaValue
 * @since 2.0
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */

public interface IJavaArrayType extends IJavaReferenceType {

	/**
	 * Returns a new instance of an array of this type, with the specified
	 * length.
	 *
	 * @param size
	 *            the length of the new array
	 * @return a new array of the specified length
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                </ul>
	 */
	public IJavaArray newInstance(int size) throws DebugException;

	/**
	 * Returns the type of the elements in this array.
	 *
	 * @return type
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The exception's
	 *                status code contains the underlying exception responsible
	 *                for the failure.</li>
	 *                </ul>
	 */
	public IJavaType getComponentType() throws DebugException;

}
