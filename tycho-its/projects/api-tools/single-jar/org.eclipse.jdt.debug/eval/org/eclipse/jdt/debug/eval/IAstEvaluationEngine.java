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

import java.util.Map;

import org.eclipse.debug.core.DebugException;
import org.eclipse.jdt.debug.core.IJavaObject;
import org.eclipse.jdt.debug.core.IJavaReferenceType;
import org.eclipse.jdt.debug.core.IJavaStackFrame;
import org.eclipse.jdt.debug.core.IJavaThread;

/**
 * An evaluation engine that performs evaluations by interpreting abstract
 * syntax trees. An AST evaluation engine is capable of creating compiled
 * expressions that can be evaluated multiple times in a given runtime context.
 *
 * @since 2.0
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IAstEvaluationEngine extends IEvaluationEngine {

	/**
	 * Instructs the evaluation engine to disable garbage collection on the result object. The caller is then responsible to call
	 * {@link IJavaObject#enableCollection()} on the evaluated result. Can be passed as a bit flag to the <code>evaluationDetail</code> parameter.
	 *
	 * @since 3.17
	 */
	int DISABLE_GC_ON_RESULT = 0x0100;

	/**
	 * Asynchronously evaluates the given expression in the context of the
	 * specified stack frame, reporting the result back to the given listener.
	 * The thread is resumed from the location at which it is currently
	 * suspended to perform the evaluation. When the evaluation completes, the
	 * thread will be suspended at this original location. The thread runs the
	 * evaluation with the given evaluation detail (@see
	 * IJavaThread#runEvaluation(IEvaluationRunnable, IProgressMonitor, int)).
	 * Compilation and runtime errors are reported in the evaluation result.
	 *
	 * @param expression
	 *            expression to evaluate
	 * @param frame
	 *            the stack frame context in which to run the evaluation.
	 * @param listener
	 *            the listener that will receive notification when/if the
	 *            evaluation completes
	 * @param evaluationDetail
	 *            bitmask of one of <code>DebugEvent.EVALUATION</code> or
	 *            <code>DebugEvent.EVALUATION_IMPLICIT</code> and
	 *            optionally <code>DISABLE_GC_ON_RESULT</code>
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
	public void evaluateExpression(ICompiledExpression expression,
			IJavaStackFrame frame, IEvaluationListener listener,
			int evaluationDetail, boolean hitBreakpoints) throws DebugException;

	/**
	 * Asynchronously evaluates the given expression in the context of the
	 * specified type, reporting the result back to the given listener. The
	 * expression is evaluated in the context of the Java project this
	 * evaluation engine was created on. If the expression is determined to have
	 * no errors, the expression is evaluated in the thread associated with the
	 * given stack frame. When the evaluation completes, the thread will be
	 * suspended at this original location. The thread runs the evaluation with
	 * the given evaluation detail (@see
	 * IJavaThread#runEvaluation(IEvaluationRunnable, IProgressMonitor, int)).
	 * Compilation and runtime errors are reported in the evaluation result.
	 *
	 * @param expression
	 *            the expression to evaluate
	 * @param object
	 *            the 'this' context for the evaluation
	 * @param thread
	 *            the thread in which to run the evaluation, which must be
	 *            suspended
	 * @param listener
	 *            the listener that will receive notification when/if the
	 *            evaluation completes
	 * @param evaluationDetail
	 *            bitmask of one of <code>DebugEvent.EVALUATION</code> or
	 *            <code>DebugEvent.EVALUATION_IMPLICIT</code> and
	 *            optionally <code>DISABLE_GC_ON_RESULT</code>
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
	public void evaluateExpression(ICompiledExpression expression,
			IJavaObject object, IJavaThread thread,
			IEvaluationListener listener, int evaluationDetail,
			boolean hitBreakpoints) throws DebugException;

	/**
	 * Synchronously generates a compiled expression from the given expression
	 * in the context of the specified stack frame. The generated expression can
	 * be stored and evaluated later in a valid runtime context. Compilation
	 * errors are reported in the returned compiled expression.
	 *
	 * @param expression
	 *            expression to compile
	 * @param frame
	 *            the context in which to compile the expression
	 * @return the compiled expression
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                <li>The associated thread is not currently suspended</li>
	 *                <li>The stack frame is not contained in the debug target
	 *                associated with this evaluation engine</li>
	 *                </ul>
	 */
	public ICompiledExpression getCompiledExpression(String expression,
			IJavaStackFrame frame) throws DebugException;

	/**
	 * Synchronously generates a compiled expression from the given expression
	 * in the context of the specified object. The generated expression can be
	 * stored and evaluated later in a valid runtime context. Compilation errors
	 * are reported in the returned compiled expression.
	 *
	 * @param expression
	 *            expression to compile
	 * @param object
	 *            the context in which to compile the expression
	 * @return the compiled epxression
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                <li>The associated thread is not currently suspended</li>
	 *                <li>The stack frame is not contained in the debug target
	 *                associated with this evaluation engine</li>
	 *                </ul>
	 */
	public ICompiledExpression getCompiledExpression(String expression,
			IJavaObject object) throws DebugException;

	/**
	 * Synchronously generates a compiled expression from the given expression
	 * in the context of the specified type. The generated expression can be
	 * stored and evaluated later in a valid runtime context. Compilation errors
	 * are reported in the returned compiled expression.
	 *
	 * @param expression
	 *            expression to compile
	 * @param type
	 *            the context in which to compile the expression
	 * @return the compiled expression
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The
	 *                DebugException's status code contains the underlying
	 *                exception responsible for the failure.</li>
	 *                <li>The associated thread is not currently suspended</li>
	 *                <li>The stack frame is not contained in the debug target
	 *                associated with this evaluation engine</li>
	 *                </ul>
	 * @since 3.1
	 */
	public ICompiledExpression getCompiledExpression(String expression,
			IJavaReferenceType type) throws DebugException;

	/**
	 * Synchronously generates a compiled expression from the given expression in the context of the specified type. The generated expression can be
	 * stored and evaluated later in a valid runtime context. Compilation errors are reported in the returned compiled expression.
	 *
	 * @param expression
	 *            expression to compile
	 * @param type
	 *            the context in which to compile the expression
	 * @param compileOptions
	 *            options to use during the compile
	 * @return the compiled expression
	 * @exception DebugException
	 *                if this method fails. Reasons include:
	 *                <ul>
	 *                <li>Failure communicating with the VM. The DebugException's status code contains the underlying exception responsible for the
	 *                failure.</li>
	 *                <li>The associated thread is not currently suspended</li>
	 *                <li>The stack frame is not contained in the debug target associated with this evaluation engine</li>
	 *                </ul>
	 * @since 3.13
	 */
	public ICompiledExpression getCompiledExpression(String expression,
			IJavaReferenceType type, Map<String, String> compileOptions) throws DebugException;

}
