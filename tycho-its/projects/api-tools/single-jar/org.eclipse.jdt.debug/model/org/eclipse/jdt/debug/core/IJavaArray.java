/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
import org.eclipse.debug.core.model.IIndexedValue;

/**
 * A value referencing an array on a target VM.
 *
 * @see IJavaValue
 * @since 2.0
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */

public interface IJavaArray extends IJavaObject, IIndexedValue {

	/**
	 * Returns the values contained in this array.
	 *
	 * @return the values contained in this array
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                </ul>
	 */
	public IJavaValue[] getValues() throws DebugException;

	/**
	 * Returns the value at the given index in this array.
	 *
	 * @param index
	 *            the index of the value to return
	 * @return the value at the given index
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                </ul>
	 * @exception java.lang.IndexOutOfBoundsException
	 *                if the index is not within the bounds of this array.
	 */
	public IJavaValue getValue(int index) throws DebugException;

	/**
	 * Returns the length of this array.
	 *
	 * @return the length of this array
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The DebugException's status code contains the underlying exception responsible for the
	 *                failure.</li>
	 *                </ul>
	 */
	public int getLength() throws DebugException;

	/**
	 * Sets the value at the given index to the specified value.
	 *
	 * @param index
	 *            the index at which to assign a new value
	 * @param value
	 *            the new value
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                <li>The given value is not compatible with the type of
	 *                this array</li>
	 *                </ul>
	 * @exception java.lang.IndexOutOfBoundsException
	 *                if the index is not within the bounds of this array.
	 */
	public void setValue(int index, IJavaValue value) throws DebugException;

	/**
	 * Replaces values in this array. If the given replacement values length is
	 * less that the length of this array, only the number of values in the
	 * given array are replaced. If the given replacement values length is
	 * longer than the length of this array, values in positions greater than
	 * the length of this array are ignored.
	 *
	 * @param values
	 *            replacement values
	 * @exception DebugException
	 *                if an exception occurs replacing values
	 * @since 3.4
	 */
	public void setValues(IJavaValue[] values) throws DebugException;

	/**
	 * Replaces a range of values in this array.
	 *
	 * @param offset
	 *            offset in this array to start replacing values at
	 * @param length
	 *            the number of values to replace in this array
	 * @param values
	 *            replacement values
	 * @param startOffset
	 *            the first offset where values are copied from the given
	 *            replacement values
	 * @exception DebugException
	 *                if an exception occurs replacing values or if the given
	 *                offsets and length are not within the range of this array
	 *                or the replacement values
	 * @since 3.4
	 */
	public void setValues(int offset, int length, IJavaValue[] values,
			int startOffset) throws DebugException;

}
