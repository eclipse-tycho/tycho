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

import java.util.List;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDropToFrame;
import org.eclipse.debug.core.model.IFilteredStep;
import org.eclipse.debug.core.model.IStackFrame;

/**
 * A stack frame in a thread on a Java virtual machine.
 * <p>
 * Since 3.1, <code>IJavaStackFrame</code> also implements
 * {@link org.eclipse.debug.core.model.IDropToFrame}.
 * </p>
 *
 * @see org.eclipse.debug.core.model.IStackFrame
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
@SuppressWarnings("deprecation")
public interface IJavaStackFrame extends IStackFrame, IJavaModifiers,
		IFilteredStep, IDropToFrame {

	/**
	 * Status code indicating a stack frame is invalid. A stack frame becomes
	 * invalid when the thread containing the stack frame resumes. A stack frame
	 * may or may not be valid if the thread subsequently suspends, depending on
	 * the location where the thread suspends.
	 *
	 * @since 3.1
	 */
	public static final int ERR_INVALID_STACK_FRAME = 130;

	/**
	 * Returns whether this stack frame currently supports the drop to frame
	 * operation. Note that not all VMs support the operation.
	 *
	 * @return whether this stack frame currently supports drop to frame
	 * @deprecated since 3.1, IJavaStackFrame extends
	 *             org.eclipse.debug.core.IDropToFrame which defines
	 *             canDropToFrame(). Use this method instead.
	 */
	@Deprecated
	boolean supportsDropToFrame();

	/**
	 * Returns whether the method associated with this stack frame is a
	 * constructor.
	 *
	 * @return whether this stack frame is associated with a constructor
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                <li>This stack frame is no longer valid. That is, the
	 *                thread containing this stack frame has since been resumed.
	 *                </li>
	 *                </ul>
	 */
	public boolean isConstructor() throws DebugException;

	/**
	 * Returns whether the method associated with this stack frame has been
	 * declared as native.
	 *
	 * @return whether this stack frame has been declared as native
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                <li>This stack frame is no longer valid. That is, the
	 *                thread containing this stack frame has since been resumed.
	 *                </li>
	 *                </ul>
	 */
	public boolean isNative() throws DebugException;

	/**
	 * Returns whether the method associated with this stack frame is a static
	 * initializer.
	 *
	 * @return whether this stack frame is a static initializer
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                <li>This stack frame is no longer valid. That is, the
	 *                thread containing this stack frame has since been resumed.
	 *                </li>
	 *                </ul>
	 */
	public boolean isStaticInitializer() throws DebugException;

	/**
	 * Returns whether the method associated with this stack frame has been
	 * declared as synchronized.
	 *
	 * @return whether this stack frame has been declared as synchronized
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                <li>This stack frame is no longer valid. That is, the
	 *                thread containing this stack frame has since been resumed.
	 *                </li>
	 *                </ul>
	 */
	public boolean isSynchronized() throws DebugException;

	/**
	 * Returns whether the method associated with this stack frame is running
	 * code in the VM that is out of synch with the code in the workspace.
	 *
	 * @return whether this stack frame is out of synch with the workspace.
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                <li>This stack frame is no longer valid. That is, the
	 *                thread containing this stack frame has since been resumed.
	 *                </li>
	 *                </ul>
	 * @since 2.0
	 */
	public boolean isOutOfSynch() throws DebugException;

	/**
	 * Returns whether the method associated with this stack frame is obsolete,
	 * that is, it is running old byte codes that have been replaced in the VM.
	 * This can occur when a hot code replace succeeds but the VM is unable to
	 * pop a call to an affected method from the call stack.
	 *
	 * @return whether this stack frame's method is obsolete
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                <li>This stack frame is no longer valid. That is, the
	 *                thread containing this stack frame has since been resumed.
	 *                </li>
	 *                </ul>
	 * @since 2.0
	 */
	public boolean isObsolete() throws DebugException;

	/**
	 * Returns the fully qualified name of the type that declares the method
	 * associated with this stack frame.
	 *
	 * @return declaring type name
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                <li>This stack frame is no longer valid. That is, the
	 *                thread containing this stack frame has since been resumed.
	 *                </li>
	 *                </ul>
	 */
	public String getDeclaringTypeName() throws DebugException;

	/**
	 * Returns the fully qualified name of the type that is the receiving object
	 * associated with this stack frame
	 *
	 * @return receiving type name
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                <li>This stack frame is no longer valid. That is, the
	 *                thread containing this stack frame has since been resumed.
	 *                </li>
	 *                </ul>
	 */
	public String getReceivingTypeName() throws DebugException;

	/**
	 * Returns the JNI signature for the method this stack frame is associated
	 * with.
	 *
	 * @return signature
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                <li>This stack frame is no longer valid. That is, the
	 *                thread containing this stack frame has since been resumed.
	 *                </li>
	 *                </ul>
	 */
	public String getSignature() throws DebugException;

	/**
	 * Returns a list of fully qualified type names of the arguments for the
	 * method associated with this stack frame.
	 *
	 * @return argument type names, or an empty list if this method has no
	 *         arguments
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                <li>This stack frame is no longer valid. That is, the
	 *                thread containing this stack frame has since been resumed.
	 *                </li>
	 *                </ul>
	 */
	public List<String> getArgumentTypeNames() throws DebugException;

	/**
	 * Returns the name of the method associated with this stack frame
	 *
	 * @return method name
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                <li>This stack frame is no longer valid. That is, the
	 *                thread containing this stack frame has since been resumed.
	 *                </li>
	 *                </ul>
	 */
	public String getMethodName() throws DebugException;

	/**
	 * Returns the local, static, or "this" variable with the given name, or
	 * <code>null</code> if unable to resolve a variable with the name.
	 *
	 * @param variableName
	 *            the name of the variable to search for
	 * @return a variable, or <code>null</code> if none
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                <li>This stack frame is no longer valid. That is, the
	 *                thread containing this stack frame has since been resumed.
	 *                </li>
	 *                </ul>
	 */
	public IJavaVariable findVariable(String variableName)
			throws DebugException;

	/**
	 * Returns the line number of the instruction pointer in this stack frame
	 * that corresponds to the line in the associated source element in the
	 * specified stratum, or <code>-1</code> if line number information is
	 * unavailable.
	 *
	 * @param stratum
	 *            the stratum to use.
	 * @return line number of instruction pointer in this stack frame, or
	 *         <code>-1</code> if line number information is unavailable
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the debug target. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                </ul>
	 *
	 * @since 3.0
	 */
	public int getLineNumber(String stratum) throws DebugException;

	/**
	 * Returns the source name debug attribute associated with the declaring
	 * type of this stack frame, or <code>null</code> if the source name debug
	 * attribute not present.
	 *
	 * @return source name debug attribute, or <code>null</code>
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                <li>This stack frame is no longer valid. That is, the
	 *                thread containing this stack frame has since been resumed.
	 *                </li>
	 *                </ul>
	 */
	public String getSourceName() throws DebugException;

	/**
	 * Returns the source name debug attribute associated with the declaring
	 * type of this stack frame in the specified stratum, or <code>null</code>
	 * if the source name debug attribute not present.
	 *
	 * @param stratum
	 *            the stratum to use.
	 * @return source name debug attribute, or <code>null</code>
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                <li>This stack frame is no longer valid. That is, the
	 *                thread containing this stack frame has since been resumed.
	 *                </li>
	 *                </ul>
	 *
	 * @since 3.0
	 */
	public String getSourceName(String stratum) throws DebugException;

	/**
	 * Returns the source path debug attribute associated with this stack frame
	 * in the specified stratum, or <code>null</code> if the source path is not
	 * known.
	 *
	 * @param stratum
	 *            the stratum to use.
	 * @return source path debug attribute, or <code>null</code>
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                <li>This stack frame is no longer valid. That is, the
	 *                thread containing this stack frame has since been resumed.
	 *                </li>
	 *                </ul>
	 * @since 3.0
	 */
	public String getSourcePath(String stratum) throws DebugException;

	/**
	 * Returns the source path debug attribute associated with this stack frame,
	 * or <code>null</code> if the source path is not known.
	 *
	 * @return source path debug attribute, or <code>null</code>
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                <li>This stack frame is no longer valid. That is, the
	 *                thread containing this stack frame has since been resumed.
	 *                </li>
	 *                </ul>
	 * @since 3.0
	 */
	public String getSourcePath() throws DebugException;

	/**
	 * Returns a collection of local variables that are visible at the current
	 * point of execution in this stack frame. The list includes arguments.
	 *
	 * @return collection of locals and arguments
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                <li>This stack frame is no longer valid. That is, the
	 *                thread containing this stack frame has since been resumed.
	 *                </li>
	 *                </ul>
	 * @since 2.0
	 */
	public IJavaVariable[] getLocalVariables() throws DebugException;

	/**
	 * Returns a reference to the receiver of the method associated with this
	 * stack frame, or <code>null</code> if this stack frame represents a static
	 * method.
	 *
	 * @return 'this' object, or <code>null</code>
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                <li>This stack frame is no longer valid. That is, the
	 *                thread containing this stack frame has since been resumed.
	 *                </li>
	 *                </ul>
	 */
	public IJavaObject getThis() throws DebugException;

	/**
	 * Returns the class in which this stack frame's method is declared.
	 *
	 * @return the class in which this stack frame's method is declared
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                <li>This stack frame is no longer valid. That is, the
	 *                thread containing this stack frame has since been resumed.
	 *                </li>
	 *                </ul>
	 * @since 2.0
	 * @deprecated Use <code>getReferenceType()</code> instead, as a method is
	 *             not restricted to occur in a class. An interface may contain
	 *             a synthetic class initializer methods. Since 3.1, this method
	 *             throws a <code>DebugException</code> when a stack frame's
	 *             method is contained in an interface.
	 */
	@Deprecated
	public IJavaClassType getDeclaringType() throws DebugException;

	/**
	 * Returns the type in which this stack frame's method is declared.
	 *
	 * @return the type in which this stack frame's method is declared
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                <li>This stack frame is no longer valid. That is, the
	 *                thread containing this stack frame has since been resumed.
	 *                </li>
	 *                </ul>
	 * @since 3.1
	 */
	public IJavaReferenceType getReferenceType() throws DebugException;

	/**
	 * Returns whether local variable information was available when local
	 * variables were retrieved from the target for this frame. Returns
	 * <code>true</code> if locals have never been retrieved. This data is
	 * available after the fact, since variable retrieval is expensive.
	 *
	 * @return whether local variable information was available when variables
	 *         were retrieved from the target. Returns <code>true</code> if
	 *         locals have never been retrieved
	 *
	 * @since 2.0
	 */
	public boolean wereLocalsAvailable();

	/**
	 * Returns whether the method associated with this stack frame accepts a
	 * variable number of arguments.
	 *
	 * @return <code>true</code> if the method associated with this stack frame
	 *         accepts a variable number of arguments, <code>false</code>
	 *         otherwise.
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                <li>This stack frame is no longer valid. That is, the
	 *                thread containing this stack frame has since been resumed.
	 *                </li>
	 *                </ul>
	 * @since 3.1
	 */
	public boolean isVarArgs() throws DebugException;

	/**
	 * Returns whether this frame currently supports a force return operation.
	 * That is, can this method force a return before it reaches a return
	 * statement. Not all VMs support this feature.
	 * <p>
	 * Force return is only available when a thread is suspended.
	 * </p>
	 *
	 * @return whether force return can be performed currently
	 * @since 3.3
	 */
	public boolean canForceReturn();

	/**
	 * Steps out of this frame's method returning the given value. No further
	 * instructions in the method are executed but locks acquired by entering
	 * synchronized blocks are released. The following conditions must be
	 * satisfied:
	 * <ul>
	 * <li>This frame must be suspended in a non-native method.</li>
	 * <li>The return value must be assignment compatible with this frame's
	 * method's return type. Use a void value when a method return type is void
	 * (see <code>IJavaDebugTarget.voidValue()</code>).</li>
	 * </ul>
	 *
	 * @param value
	 *            return value that must be assignment compatible with this
	 *            frame's method's return value
	 * @throws DebugException
	 *             if the operation fails
	 * @since 3.3
	 */
	public void forceReturn(IJavaValue value) throws DebugException;
}
