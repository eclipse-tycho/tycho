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
 * The class of an object on a Java debug target.
 *
 * @see IJavaValue
 * @since 2.0
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IJavaClassType extends IJavaReferenceType {

	/**
	 * Returns a new instance of this class by invoking the constructor with the
	 * given signature and arguments in the specified thread. The given thread
	 * is resumed to perform this method invocation and suspends in its original
	 * location when this method invocation is complete. This method does not
	 * return until the method invocation is complete. Resuming the specified
	 * thread can result in breakpoints being hit, infinite loops, and deadlock.
	 *
	 * @param signature
	 *            the JNI style signature of the method to be invoked
	 * @param args
	 *            the arguments of the constructor, which can be
	 *            <code>null</code> or empty if there are none
	 * @param thread
	 *            the thread in which to invoke the constructor
	 * @return the result of invoking the constructor
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                <li>This type does not implement the specified constructor
	 *                </li>
	 *                <li>An exception occurs while invoking the specified
	 *                constructor</li>
	 *                <li>The given thread is already performing a message send,
	 *                (status code
	 *                <code>IJavaThread.ERR_NESTED_METHOD_INVOCATION</code>)</li>
	 *                <li>The given thread is not currently suspended (status
	 *                code <code>IJavaThread.ERR_THREAD_NOT_SUSPENDED</code>)</li>
	 *                <li>The given thread was explicitly suspended (status code
	 *                <code>IJavaThread.ERR_INCOMPATIBLE_THREAD_STATE</code>)</li>
	 *                </ul>
	 */
	public IJavaObject newInstance(String signature, IJavaValue[] args,
			IJavaThread thread) throws DebugException;

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
	 */
	public IJavaValue sendMessage(String selector, String signature,
			IJavaValue[] args, IJavaThread thread) throws DebugException;

	/**
	 * Returns the superclass of this class type, or <code>null</code> if no
	 * such class exists.
	 *
	 * @return the superclass of this class type, or <code>null</code>
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                </ul>
	 */
	public IJavaClassType getSuperclass() throws DebugException;

	/**
	 * Returns the interface objects associated with the interfaces this class
	 * directly implements. Only those interfaces declared in the
	 * <code>implements</code> clause for this class are included.
	 *
	 * @return the interface objects associated with the interfaces this class
	 *         directly implements
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                </ul>
	 * @since 3.0
	 */
	public IJavaInterfaceType[] getInterfaces() throws DebugException;

	/**
	 * Returns the interface objects associated with <em>all</em> interfaces
	 * this class implements, directly or indirectly.
	 *
	 * @return the interface objects associated with the interfaces this class
	 *         directly implements, directly or indirectly
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                </ul>
	 * @since 3.0
	 */
	public IJavaInterfaceType[] getAllInterfaces() throws DebugException;

	/**
	 * Returns whether this type is declared as a type safe enumeration.
	 *
	 * @return <code>true</code> if this type is a type safe enumeration,
	 *         <code>false</code> otherwise.
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                </ul>
	 * @since 3.1
	 */
	public boolean isEnum() throws DebugException;
}
