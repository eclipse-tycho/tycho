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
 *     Jesper Steen Møller <jesper@selskabet.org> - Bug 430839
 *******************************************************************************/
package org.eclipse.jdt.debug.core;

import org.eclipse.debug.core.DebugException;

/**
 * An interface an object implements on a Java debug target.
 *
 * @see IJavaValue
 * @since 2.0
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IJavaInterfaceType extends IJavaReferenceType {

	/**
	 * Returns the class objects associated with the implementors of this
	 * interface type. Returns an empty array if there are none.
	 *
	 * @return the class objects associated with the implementors of this
	 *         interface type
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                </ul>
	 * @since 3.0
	 */
	public IJavaClassType[] getImplementors() throws DebugException;

	/**
	 * Returns the interface objects associated with the sub-interfaces of this
	 * interface type. Returns an empty array if there are none. The
	 * sub-interfaces are those interfaces that directly extend this interface,
	 * that is, those interfaces that declared this interface in their
	 * <code>extends</code> clause.
	 *
	 * @return the interface objects associated with the sub-interfaces of this
	 *         interface type
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                </ul>
	 * @since 3.0
	 */
	public IJavaInterfaceType[] getSubInterfaces() throws DebugException;

	/**
	 * Returns the interface objects associated with the super-interfaces of
	 * this interface type. Returns an empty array if there are none. The
	 * super-interfaces are those interfaces that are directly extended by this
	 * interface, that is, those interfaces that this interface declared to be
	 * extended.
	 *
	 * @return the interface objects associated with the super-interfaces of
	 *         this interface type
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                </ul>
	 * @since 3.0
	 */
	public IJavaInterfaceType[] getSuperInterfaces() throws DebugException;

	/**
	 * Returns the result of sending the specified message to this class with
	 * the given arguments in the specified thread (invokes a static method on
	 * this type). The given thread is resumed to perform this method invocation
	 * and suspends in its original location when this method invocation is
	 * complete. This method does not return until the method invocation is
	 * complete. Resuming the specified thread can result in breakpoints being
	 * hit, infinite loops, and deadlock.
	 *
	 * @param selector
	 *            the selector of the method to be invoked
	 * @param signature
	 *            the JNI style signature of the method to be invoked
	 * @param args
	 *            the arguments of the method, which can be <code>null</code> or
	 *            empty if there are none
	 * @param thread
	 *            the thread in which to invoke the method
	 * @return the result of invoking the method
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                <li>This object does not implement the specified method</li>
	 *                <li>An exception occurs while invoking the specified
	 *                method</li>
	 *                <li>The given thread is already performing a message send,
	 *                (status code
	 *                <code>IJavaThread.ERR_NESTED_METHOD_INVOCATION</code>)</li>
	 *                <li>The given thread is not currently suspended (status
	 *                code <code>IJavaThread.ERR_THREAD_NOT_SUSPENDED</code>)</li>
	 *                <li>The given thread was explicitly suspended (status code
	 *                <code>IJavaThread.ERR_INCOMPATIBLE_THREAD_STATE</code>)</li>
	 *                </ul>
	 *
	 * @since 3.10
	 */
	public IJavaValue sendMessage(String selector, String signature,
			IJavaValue[] args, IJavaThread thread) throws DebugException;

}
