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
 * A variable that contains the value of an instance or class variable.
 *
 * @see org.eclipse.debug.core.model.IVariable
 * @since 2.0
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IJavaFieldVariable extends IJavaVariable {

	/**
	 * Returns whether this variable is declared as transient.
	 *
	 * @return whether this variable has been declared as transient
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                </ul>
	 */
	public boolean isTransient() throws DebugException;

	/**
	 * Returns whether this variable is declared as volatile.
	 *
	 * @return whether this variable has been declared as volatile
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                </ul>
	 */
	public boolean isVolatile() throws DebugException;

	/**
	 * Returns the type that declares this variable.
	 *
	 * @return the type that declares this variable
	 */
	public IJavaType getDeclaringType();

	/**
	 * Returns the object that contains this field variable, or
	 * <code>null</code> if no object contains this field variable (static field
	 * variable).
	 *
	 * @return the object that contains this field variable
	 * @since 3.0
	 */
	public IJavaObject getReceiver();

	/**
	 * Returns the type that contains this field variable.
	 *
	 * @return the type that contains this field variable
	 * @since 3.0
	 */
	public IJavaReferenceType getReceivingType();

}
