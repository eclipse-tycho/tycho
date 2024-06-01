/*******************************************************************************
 *  Copyright (c) 2000, 2011 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.debug.eval;

import org.eclipse.debug.core.DebugException;
import org.eclipse.jdt.core.dom.Message;
import org.eclipse.jdt.debug.core.IJavaThread;
import org.eclipse.jdt.debug.core.IJavaValue;

/**
 * The result of an evaluation. An evaluation result may contain problems and/or
 * a result value.
 *
 * @see IJavaValue
 * @since 2.0
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */

public interface IEvaluationResult {

	/**
	 * Returns the value representing the result of the evaluation, or
	 * <code>null</code> if the associated evaluation failed. If the associated
	 * evaluation failed, there will be problems, or an exception in this
	 * result.
	 * <p>
	 * Since 3.5, this method can also return null if the evaluation was
	 * terminated before it completed.
	 * </p>
	 *
	 * @return the resulting value, possibly <code>null</code>
	 */
	public IJavaValue getValue();

	/**
	 * Returns whether the evaluation had any problems or if an exception
	 * occurred while performing the evaluation.
	 *
	 * @return whether there were any problems.
	 * @see #getErrors()
	 * @see #getException()
	 */
	public boolean hasErrors();

	/**
	 * Returns an array of problem messages. Each message describes a problem
	 * that occurred while compiling the snippet.
	 *
	 * @return compilation error messages, or an empty array if no errors
	 *         occurred
	 * @deprecated use getErrorMessages()
	 */
	@Deprecated
	public Message[] getErrors();

	/**
	 * Returns an array of problem messages. Each message describes a problem
	 * that occurred while compiling the snippet.
	 *
	 * @return compilation error messages, or an empty array if no errors
	 *         occurred
	 * @since 2.1
	 */
	public String[] getErrorMessages();

	/**
	 * Returns the snippet that was evaluated.
	 *
	 * @return The string code snippet.
	 */
	public String getSnippet();

	/**
	 * Returns any exception that occurred while performing the evaluation or
	 * <code>null</code> if an exception did not occur. The exception will be a
	 * debug exception or a debug exception that wrappers a JDI exception that
	 * indicates a problem communicating with the target or with actually
	 * performing some action in the target.
	 *
	 * @return The exception that occurred during the evaluation
	 * @see com.sun.jdi.InvocationException
	 * @see org.eclipse.debug.core.DebugException
	 */
	public DebugException getException();

	/**
	 * Returns the thread in which the evaluation was performed.
	 *
	 * @return the thread in which the evaluation was performed
	 */
	public IJavaThread getThread();

	/**
	 * Returns the evaluation engine used to evaluate the original snippet.
	 *
	 * @return the evaluation engine used to evaluate the original snippet
	 */
	public IEvaluationEngine getEvaluationEngine();

	/**
	 * Returns whether this evaluation was terminated before it completed.
	 *
	 * @return whether terminated.
	 * @since 3.5
	 */
	public boolean isTerminated();
}
