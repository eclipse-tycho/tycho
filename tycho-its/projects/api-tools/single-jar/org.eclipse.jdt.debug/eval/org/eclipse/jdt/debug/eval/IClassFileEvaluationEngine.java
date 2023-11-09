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
import org.eclipse.jdt.debug.core.IJavaThread;

/**
 * An evaluation engine that performs evaluations by deploying and executing
 * class files locally.
 *
 * @since 2.0
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IClassFileEvaluationEngine extends IEvaluationEngine {
	/**
	 * Returns the import declarations for this evaluation context. An empty
	 * list indicates there are no imports. The syntax for the import
	 * corresponds to a fully qualified type name, or to an on-demand package
	 * name as defined by ImportDeclaration (JLS2 7.5). For example,
	 * <code>"java.util.Hashtable"</code> or <code>"java.util.*"</code>.
	 *
	 * @return the list of import names
	 */
	public String[] getImports();

	/**
	 * Sets the import declarations for this evaluation context. An empty list
	 * indicates there are no imports. The syntax for the import corresponds to
	 * a fully qualified type name, or to an on-demand package name as defined
	 * by ImportDeclaration (JLS2 7.5). For example,
	 * <code>"java.util.Hashtable"</code> or <code>"java.util.*"</code>.
	 *
	 * @param imports
	 *            the list of import names
	 */
	public void setImports(String[] imports);

	/**
	 * Asynchronously evaluates the given snippet in the specified target
	 * thread, reporting the result back to the given listener. The snippet is
	 * evaluated in the context of the Java project this evaluation engine was
	 * created on. If the snippet is determined to be a valid expression, the
	 * expression is evaluated in the specified thread, which resumes its
	 * execution from the location at which it is currently suspended. When the
	 * evaluation completes, the thread will be suspened at this original
	 * location. Compilation and runtime errors are reported in the evaluation
	 * result.
	 *
	 * @param snippet
	 *            code snippet to evaluate
	 * @param thread
	 *            the thread in which to run the evaluation, which must be
	 *            suspended
	 * @param listener
	 *            the listener that will receive notification when/if the
	 *            evalaution completes
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
	 *                <li>The specified thread is not currently suspended</li>
	 *                <li>The specified thread is not contained in the debug
	 *                target associated with this evaluation engine</li>
	 *                <li>The specified thread is suspended in the middle of an
	 *                evaluation that has not completed. It is not possible to
	 *                perform nested evaluations</li>
	 *                </ul>
	 */
	public void evaluate(String snippet, IJavaThread thread,
			IEvaluationListener listener, boolean hitBreakpoints)
			throws DebugException;

}
