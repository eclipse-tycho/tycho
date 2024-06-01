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
import org.eclipse.debug.core.model.IValue;

/**
 * An object, primitive data type, or array, on a Java virtual machine.
 *
 * @see org.eclipse.debug.core.model.IValue
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IJavaValue extends IValue {
	/**
	 * Returns the JNI-style signature for the type of this value, or
	 * <code>null</code> if the value is <code>null</code>.
	 *
	 * @return signature, or <code>null</code> if signature is <code>null</code>
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                <li>The type associated with the signature is not yet
	 *                loaded</li>
	 *                </ul>
	 */
	public String getSignature() throws DebugException;

	/**
	 * Returns the generic signature as defined in the JVM specification for the
	 * type of this value. Returns <code>null</code> if the value is
	 * <code>null</code>, or if the type of this value is not a generic type.
	 *
	 * @return signature, or <code>null</code> if generic signature not
	 *         available
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                <li>The type associated with the signature is not yet
	 *                loaded</li>
	 *                </ul>
	 * @since 3.1
	 */
	public String getGenericSignature() throws DebugException;

	/**
	 * Returns the type of this value, or <code>null</code> if this value
	 * represents the <code>null</code> value
	 *
	 * @return the type of this value, or <code>null</code> if this value
	 *         represents the <code>null</code> value
	 * @throws DebugException
	 *             if the request fails
	 *
	 * @since 2.0
	 */
	public IJavaType getJavaType() throws DebugException;

	/**
	 * Returns whether this value represents <code>null</code>.
	 *
	 * @return whether this value represents <code>null</code>
	 * @since 3.5
	 */
	public boolean isNull();

}
