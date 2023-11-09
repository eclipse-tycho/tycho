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

import org.eclipse.jdt.core.dom.Message;

/**
 * A compiled expression can be compiled once and evaluated multiple times in a
 * runtime context.
 *
 * @see org.eclipse.jdt.debug.eval.IAstEvaluationEngine
 * @since 2.0
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */

public interface ICompiledExpression {

	/**
	 * Returns the source snippet from which this compiled expression was
	 * created.
	 *
	 * @return the source snippet from which this compiled expression was
	 *         created
	 */
	public String getSnippet();

	/**
	 * Returns whether this compiled expression has any compilation errors.
	 *
	 * @return whether this compiled expression has any compilation errors
	 */
	public boolean hasErrors();

	/**
	 * Returns any errors which occurred while creating this compiled
	 * expression.
	 *
	 * @return any errors which occurred while creating this compiled expression
	 * @deprecated use getErrorMessages()
	 */
	@Deprecated
	public Message[] getErrors();

	/**
	 * Returns an array of problem messages. Each message describes a problem
	 * that occurred while while creating this compiled expression.
	 *
	 * @return error messages, or an empty array if no errors occurred
	 * @since 2.1
	 */
	public String[] getErrorMessages();

}
