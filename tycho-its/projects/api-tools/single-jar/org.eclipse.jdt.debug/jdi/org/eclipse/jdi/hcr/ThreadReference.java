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
package org.eclipse.jdi.hcr;

import com.sun.jdi.Value;

/**
 * Hot code replacement extension to <code>com.sun.jdi.ThreadReference</code>.
 */
public interface ThreadReference {
	/**
	 * Resumes the execution of this thread as if the next instruction was a
	 * return instruction with the given value. This causes the top stack frame
	 * to be popped with the given value.
	 * <p>
	 * A breakpoint instruction at the current instruction is not triggered that
	 * is, this operation takes precedence over breakpoints.
	 * <code>try-finally</code> blocks enclosing the current location will be
	 * triggered in due course.
	 * <p>
	 * The triggerFinallyAndSynchronizedBlocks option on this operation controls
	 * whether <code>try-finally</code> and <code>synchronized</code> blocks
	 * enclosing the current location should be triggered:
	 * <ul>
	 * <li>If no, the stack frame is popped, the return value is returned, and
	 * execution continues back in the caller. Note that <code>finally</code>
	 * blocks are not run, and that if the code is nested within a
	 * <code>synchronized</code> statement, the monitor lock is not released
	 * (however, if the method is </code>synchronized</code> the monitor lock
	 * will be properly released). This mechanism is sure-fire, but at the risk
	 * of not letting the target program clean itself up (e.g., close its
	 * files).
	 * <li>If yes, the VM checks to see whether there might be a
	 * <code>finally</code> or <code>synchronized</code> block enclosing the
	 * current instruction.
	 * <ul>
	 * <li>If there is no enclosing <code>finally</code> block, the operation
	 * reduces to the above case.
	 * <li>If there is an enclosing <code>finally</code> block, the VM creates a
	 * VM exception and activates the <code>finally</code> block with it. If
	 * this exception eventually causes the stack frame to be popped, the
	 * exception is caught by the VM itself, the return value is returned, and
	 * execution continues back in the caller.
	 * <ul>
	 * </ul>
	 * <p>
	 * Note that a <code>finally</code> block manifests itself as (and is
	 * indistinguishable from) a <code>catch Throwable</code> block.
	 * <code>synchronized</code> statements also compile to a
	 * <code> catch Throwable block<code>.The target program may inadventently
	 * end up catching this exception.
	 *
	 * Since the choices each have their pros and cons, making the decision
	 * is left to the debugger. However the later option is the  recommended choice.
	 * <p>
	 * The reply to the operation contains a flag indicating whether any <code>finally</code>
	 * or <code>synchronized</code> blocks are enclosing the current
	 * instruction.
	 * <p>
	 * This operation is ignored if the thread was not suspended. If the thread
	 * was suspended multiple times, wait for the same number of resumes before
	 * executing the return instruction.
	 * <p>
	 * The returned value is ignored if the method returns void.
	 * <p>
	 * Throws an <code>OperationRefusedException</code> if the VM refused to
	 * perform this operation. This in recognition that the VM may be in an
	 * awkward state and unable to comply:
	 * <ul>
	 * <li>for example, execution is suspended in a native method,
	 * <li>for example, execution is suspended during class preparation.
	 * </ul>
	 * @param returnValue the value to return from the thread with
	 * @param triggerFinallyAndSynchronizedBlocks if finally / synchronization blocks should be executed before resuming
	 * @return if the forced return was successful
	 */
	public boolean doReturn(Value returnValue,
			boolean triggerFinallyAndSynchronizedBlocks);
}
