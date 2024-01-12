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
package org.eclipse.jdt.debug.eval;

import org.eclipse.debug.core.DebugException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.debug.core.IJavaDebugTarget;
import org.eclipse.jdt.debug.core.IJavaObject;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.debug.core.IJavaThread;

/**
 * An evaluation engine performs an evaluation of a code snippet or expression
 * in a specified thread of a debug target. An evaluation engine is associated
 * with a specific debug target and Java project on creation.
 *
 * @see IEvaluationResult
 * @see IEvaluationListener
 * @since 2.0
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */

public interface IEvaluationEngine {
	/**
	 * Asynchronously evaluates the given snippet in the context of the
	 * specified stack frame, reporting the result back to the given listener.
	 * The snippet is evaluated in the context of the Java project this
	 * evaluation engine was created on. If the snippet is determined to be a
	 * valid expression, the expression is evaluated in the thread associated
	 * with the given stack frame. The thread is resumed from the location at
	 * which it is currently suspended to perform the evaluation. When the
	 * evaluation completes, the thread will be suspended at this original
	 * location. The thread runs the evaluation with the given evaluation detail
	 * (@see IJavaThread#runEvaluation(IEvaluationRunnable, IProgressMonitor,
	 * int)). Compilation and runtime errors are reported in the evaluation
	 * result.
	 *
	 * @param snippet
	 *            code snippet to evaluate
	 * @param frame
	 *            the stack frame context in which to run the evaluation.
	 * @param listener
	 *            the listener that will receive notification when/if the
	 *            evaluation completes
	 * @param evaluationDetail
	 *            one of <code>DebugEvent.EVALUATION</code> or
	 *            <code>DebugEvent.EVALUATION_IMPLICIT</code>
	 * @param hitBreakpoints
	 *            whether or not breakpoints should be honored in the evaluation
	 *            thread during the evaluation. If <code>false</code>,
	 *            breakpoints hit in the evaluation thread will be ignored.
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                <li>The associated thread is not currently suspended</li>
	 *                <li>The stack frame is not contained in the debug target
	 *                associated with this evaluation engine</li>
	 *                <li>The associated thread is suspended in the middle of an
	 *                evaluation that has not completed. It is not possible to
	 *                perform nested evaluations</li>
	 *                </ul>
	 */
	public void evaluate(String snippet, IJavaStackFrame frame,
			IEvaluationListener listener, int evaluationDetail,
			boolean hitBreakpoints) throws DebugException;

	/**
	 * Asynchronously evaluates the given snippet in the context of the
	 * specified type, reporting the result back to the given listener. The
	 * snippet is evaluated in the context of the Java project this evaluation
	 * engine was created on. If the snippet is determined to be a valid
	 * expression, the expression is evaluated in the thread associated with the
	 * given stack frame. The thread is resumed from the location at which it is
	 * currently suspended to perform the evaluation. When the evaluation
	 * completes, the thread will be suspended at this original location. The
	 * thread runs the evaluation with the given evaluation detail (@see
	 * IJavaThread#runEvaluation(IEvaluationRunnable, IProgressMonitor, int)).
	 * Compilation and runtime errors are reported in the evaluation result.
	 *
	 * @param snippet
	 *            code snippet to evaluate
	 * @param thisContext
	 *            the 'this' context for the evaluation
	 * @param thread
	 *            the thread in which to run the evaluation, which must be
	 *            suspended
	 * @param listener
	 *            the listener that will receive notification when/if the
	 *            evaluation completes
	 * @param evaluationDetail
	 *            one of <code>DebugEvent.EVALUATION</code> or
	 *            <code>DebugEvent.EVALUATION_IMPLICIT</code>
	 * @param hitBreakpoints
	 *            whether or not breakpoints should be honored in the evaluation
	 *            thread during the evaluation. If <code>false</code>,
	 *            breakpoints hit in the evaluation thread will be ignored.
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                <li>The associated thread is not currently suspended</li>
	 *                <li>The specified thread is not contained in the debug
	 *                target associated with this evaluation engine</li>
	 *                <li>The associated thread is suspended in the middle of an
	 *                evaluation that has not completed. It is not possible to
	 *                perform nested evaluations</li>
	 *                </ul>
	 */
	public void evaluate(String snippet, IJavaObject thisContext,
			IJavaThread thread, IEvaluationListener listener,
			int evaluationDetail, boolean hitBreakpoints) throws DebugException;

	/**
	 * Returns the Java project in which expressions are compiled.
	 *
	 * @return Java project context
	 */
	public IJavaProject getJavaProject();

	/**
	 * Returns the debug target for which evaluations are executed.
	 *
	 * @return Java debug target
	 */
	public IJavaDebugTarget getDebugTarget();

	/**
	 * Disposes this evaluation engine. This causes the evaluation engine to
	 * cleanup any resources (such as threads) that it maintains. Clients should
	 * call this method when they are finished performing evaluations with this
	 * engine.
	 *
	 * This engine must not be used to perform evaluations after it has been
	 * disposed.
	 */
	public void dispose();

}
