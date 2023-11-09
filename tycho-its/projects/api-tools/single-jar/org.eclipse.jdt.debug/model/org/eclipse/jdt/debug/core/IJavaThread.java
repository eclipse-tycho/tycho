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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IFilteredStep;
import org.eclipse.debug.core.model.IThread;

/**
 * A thread in a Java virtual machine.
 *
 * @see org.eclipse.debug.core.model.IThread
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
@SuppressWarnings("deprecation")
public interface IJavaThread extends IThread, IFilteredStep {

	/**
	 * Status code indicating a request failed because a thread was not
	 * suspended.
	 */
	public static final int ERR_THREAD_NOT_SUSPENDED = 100;

	/**
	 * Status code indicating a request to perform a message send failed because
	 * a thread was already performing a message send.
	 *
	 * @see IJavaObject#sendMessage(String, String, IJavaValue[], IJavaThread,
	 *      boolean)
	 * @see IJavaClassType#sendMessage(String, String, IJavaValue[],
	 *      IJavaThread)
	 * @see IJavaClassType#newInstance(String, IJavaValue[], IJavaThread)
	 */
	public static final int ERR_NESTED_METHOD_INVOCATION = 101;

	/**
	 * Status code indicating a request to perform a message send failed because
	 * a thread was not suspended by a step or breakpoint event. When a thread
	 * is suspended explicitly via the <code>suspend()</code> method, it is not
	 * able to perform method invocations (this is a JDI limitation).
	 *
	 * @see IJavaObject#sendMessage(String, String, IJavaValue[], IJavaThread,
	 *      boolean)
	 * @see IJavaClassType#sendMessage(String, String, IJavaValue[],
	 *      IJavaThread)
	 * @see IJavaClassType#newInstance(String, IJavaValue[], IJavaThread)
	 */
	public static final int ERR_INCOMPATIBLE_THREAD_STATE = 102;

	/**
	 * Returns whether this thread is a system thread.
	 *
	 * @return whether this thread is a system thread
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                </ul>
	 */
	boolean isSystemThread() throws DebugException;

	/**
	 * Returns whether any of the stack frames associated with this thread are running code in the VM that is out of synch with the code in the
	 * workspace.
	 *
	 * @return whether this thread is out of synch with the workspace.
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The DebugException's status code contains the underlying exception responsible for the
	 *                failure.</li>
	 *                </ul>
	 * @since 2.0
	 */
	boolean isOutOfSynch() throws DebugException;

	/**
	 * Returns whether this thread may be running code in the VM that is out of synch with the code in the workspace.
	 *
	 * @return whether this thread may be out of synch with the workspace.
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The DebugException's status code contains the underlying exception responsible for the
	 *                failure.</li>
	 *                </ul>
	 * @since 2.0
	 */
	boolean mayBeOutOfSynch() throws DebugException;

	/**
	 * Returns whether this thread is currently performing an evaluation.
	 *
	 * @return whether this thread is currently performing an evaluation
	 * @since 2.0
	 */
	boolean isPerformingEvaluation();

	/**
	 * Returns the name of the thread group this thread belongs to, or
	 * <code>null</code> if none.
	 *
	 * @return thread group name, or <code>null</code> if none
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                </ul>
	 */
	String getThreadGroupName() throws DebugException;

	/**
	 * Returns a variable with the given name, or <code>null</code> if unable to
	 * resolve a variable with the name, or if this thread is not currently
	 * suspended.
	 * <p>
	 * Variable lookup works only when a thread is suspended. Lookup is
	 * performed in all stack frames, in a top-down order, returning the first
	 * successful match, or <code>null</code> if no match is found.
	 * </p>
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
	 *                </ul>
	 */
	IJavaVariable findVariable(String variableName) throws DebugException;

	/**
	 * Invokes the given evaluation with the specified progress monitor. This
	 * thread fires a resume event when the evaluation begins, and a suspend
	 * event when the evaluation completes or throws an exception. The events
	 * are given a detail as specified by <code>evaluationDetail</code> (one of
	 * <code>DebugEvent.EVALUATION</code> or
	 * <code>DebugEvent.EVALUATION_IMPLICIT</code>).
	 * <p>
	 * Since 3.5, the <code>org.eclipse.jdt.debug.breakpointListeners</code>
	 * extension point supports evaluation execution during a listener call
	 * back. Suspend and resume events are not fired during listener call backs.
	 * Unspecified model specific events are fired.
	 * </p>
	 *
	 * @param evaluation
	 *            the evaluation to perform
	 * @param monitor
	 *            progress monitor (may be <code>null</code>
	 * @param evaluationDetail
	 *            one of <code>DebugEvent.EVALUATION</code> or
	 *            <code>DebugEvent.EVALUATION_IMPLICIT</code>
	 * @param hitBreakpoints
	 *            whether or not breakpoints should be honored in this thread
	 *            during the evaluation. If <code>false</code>, breakpoints hit
	 *            in this thread during the evaluation will be ignored.
	 * @exception DebugException
	 *                if an exception occurs performing the evaluation
	 * @since 2.0
	 */
	public void runEvaluation(IEvaluationRunnable evaluation,
			IProgressMonitor monitor, int evaluationDetail,
			boolean hitBreakpoints) throws DebugException;

	/**
	 * Queues the given runnable with the list of runnables associated with this
	 * thread. Runnables are executed asynchronously in a separate thread. This
	 * method should be used to execute any code which performs an operation
	 * like a method invocation.
	 *
	 * @param runnable
	 *            the runnable to execute.
	 * @since 2.1
	 */
	public void queueRunnable(Runnable runnable);

	/**
	 * Attempts to terminate the currently executing
	 * <code>IEvaluationRunnable</code> in this thread, if any.
	 *
	 * Evaluations may be composed of a series of instructions. Terminating an
	 * evaluation means stopping the evaluation after the current instruction
	 * completes. A single instruction (such as a method invocation) cannot be
	 * interrupted.
	 *
	 * @exception DebugException
	 *                if an exception occurs while terminating the evaluation.
	 * @since 2.1
	 */
	public void terminateEvaluation() throws DebugException;

	/**
	 * Returns whether the currently executing <code>IEvaluationRunnable</code>
	 * supports termination. An IEvaluationRunnable supports termination if it
	 * implements <code>ITerminate</code>
	 *
	 * @return whether the current evaluation supports termination
	 * @since 2.1
	 */
	public boolean canTerminateEvaluation();

	/**
	 * Returns a Java object for the monitor for which this thread is currently
	 * waiting or <code>null</code>.
	 *
	 * @return IJavaObject the contended monitor object or <code>null</code> if
	 *         this thread is not waiting on a monitor.
	 * @exception DebugException
	 *                if an exception occurs while retrieving the contended
	 *                monitor.
	 * @since 2.1
	 */
	public IJavaObject getContendedMonitor() throws DebugException;

	/**
	 * Returns the monitors owned by this thread or <code>null</code> if this
	 * thread owns no monitors.
	 *
	 * @return the owned monitors
	 * @exception DebugException
	 *                if an exception occurs while retrieving the owned monitors
	 *                of this thread.
	 * @since 2.1
	 */
	public IJavaObject[] getOwnedMonitors() throws DebugException;

	/**
	 * Returns whether this threads owns at least one monitor.
	 *
	 * @return boolean whether this thread owns a monitor
	 * @exception DebugException
	 *                if an exception occurs determining if there are owned
	 *                monitors.
	 * @since 2.1
	 */
	public boolean hasOwnedMonitors() throws DebugException;

	/**
	 * Request to stops this thread with the given exception.<br>
	 * The result will be the same as calling java.lang.Thread#stop(java.lang.Throwable).<br>
	 * If the thread is suspended when the method is called, the thread must be resumed to complete the action.<br>
	 *
	 * <em>exception</em> must represent an exception.
	 *
	 * @param exception
	 *            the exception to throw.
	 * @exception DebugException
	 *                if the request fails
	 * @since 3.0
	 * @see java.lang.Thread#stop()
	 */
	public void stop(IJavaObject exception) throws DebugException;

	/**
	 * Returns the thread group this thread belongs to or <code>null</code> if
	 * none.
	 *
	 * @return thread group or <code>null</code>
	 * @throws DebugException
	 *             if the thread group cannot be computed
	 * @since 3.2
	 */
	public IJavaThreadGroup getThreadGroup() throws DebugException;

	/**
	 * Returns whether this thread is a daemon thread.
	 *
	 * @return whether this thread is a daemon thread
	 * @throws DebugException
	 *             if an exception occurs while determining status
	 * @since 3.3
	 */
	public boolean isDaemon() throws DebugException;

	/**
	 * Returns the number of frames in this thread.
	 *
	 * @return number of stack frames
	 * @throws DebugException
	 *             if an exception occurs while retrieving the count
	 * @since 3.3
	 */
	public int getFrameCount() throws DebugException;

	/**
	 * Returns the object reference associated with this thread.
	 *
	 * @return thread object reference
	 * @throws DebugException
	 *             if unable to retrieve an object reference
	 * @since 3.6
	 */
	public IJavaObject getThreadObject() throws DebugException;

}
