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
import org.eclipse.jdt.core.dom.Message;

/**
 * Provides event and error notification for Java breakpoints. Listeners
 * register with the <code>JDIDebugModel</code>.
 * <p>
 * Since 3.5, clients can also register breakpoint listeners using the
 * <code>org.eclipse.jdt.debug.breakpointListeners</code> extension point. A
 * listener can be contributed to receive notifications from all Java
 * breakpoints or receive notifications about specific breakpoints by
 * programmatically registering the extension with a breakpoint.
 * </p>
 * <p>
 * Clients are intended to implement this interface.
 * </p>
 *
 * @since 2.0
 * @see JDIDebugModel
 * @see IJavaBreakpoint
 */
public interface IJavaBreakpointListener {

	/**
	 * Return code in response to a "breakpoint hit" notification, indicating a
	 * vote to suspend the associated thread.
	 *
	 * @since 3.0
	 */
	public static int SUSPEND = 0x0001;
	/**
	 * Return code in response to a "breakpoint hit" notification, indicating a
	 * vote to not suspend (i.e. resume) the associated thread.
	 *
	 * @since 3.0
	 */
	public static int DONT_SUSPEND = 0x0002;
	/**
	 * Return code in response to an "installing" notification, indicating a
	 * vote to install the associated breakpoint.
	 *
	 * @since 3.0
	 */
	public static int INSTALL = 0x0001;
	/**
	 * Return code in response to an "installing" notification, indicating a
	 * vote to not install the associated breakpoint.
	 *
	 * @since 3.0
	 */
	public static int DONT_INSTALL = 0x0002;
	/**
	 * Return code indicating that this listener should not be considered in a
	 * vote to suspend a thread or install a breakpoint.
	 *
	 * @since 3.0
	 */
	public static int DONT_CARE = 0x0004;

	/**
	 * Notification that the given breakpoint is about to be added to the
	 * specified target. This message is sent before the breakpoint is actually
	 * added to the debut target (i.e. this is a pre-notification).
	 *
	 * @param target
	 *            Java debug target
	 * @param breakpoint
	 *            Java breakpoint
	 */
	public void addingBreakpoint(IJavaDebugTarget target,
			IJavaBreakpoint breakpoint);

	/**
	 * Notification that the given breakpoint is about to be installed in the
	 * specified target, in the specified type. Allows this listener to vote to
	 * determine if the given breakpoint should be installed in the specified
	 * type and target. If at least one listener votes to <code>INSTALL</code>,
	 * the breakpoint will be installed. If there are no votes to install the
	 * breakpoint, there must be at least one <code>DONT_INSTALL</code> vote to
	 * cancel the installation. If all listeners vote <code>DONT_CARE</code>,
	 * the breakpoint will be installed by default.
	 *
	 * @param target
	 *            Java debug target
	 * @param breakpoint
	 *            Java breakpoint
	 * @param type
	 *            the type (class or interface) the breakpoint is about to be
	 *            installed in or <code>null</code> if the given breakpoint is
	 *            not installed in a specific type (one of
	 *            <code>IJavaClassType</code>, <code>IJavaInterfaceType</code>,
	 *            or <code>IJavaArrayType</code>)
	 * @return whether the the breakpoint should be installed in the given type
	 *         and target, or whether this listener doesn't care - one of
	 *         <code>INSTALL</code>, <code>DONT_INSTALL</code>, or
	 *         <code>DONT_CARE</code>
	 * @since 3.0
	 */
	public int installingBreakpoint(IJavaDebugTarget target,
			IJavaBreakpoint breakpoint, IJavaType type);

	/**
	 * Notification that the given breakpoint has been installed in the
	 * specified target.
	 *
	 * @param target
	 *            Java debug target
	 * @param breakpoint
	 *            Java breakpoint
	 */
	public void breakpointInstalled(IJavaDebugTarget target,
			IJavaBreakpoint breakpoint);

	/**
	 * Notification that the given breakpoint has been hit in the specified
	 * thread. Allows this listener to vote to determine if the given thread
	 * should be suspended in response to the breakpoint. If at least one
	 * listener votes to <code>SUSPEND</code>, the thread will suspend. If there
	 * are no votes to suspend the thread, there must be at least one
	 * <code>DONT_SUSPEND</code> vote to avoid the suspension (resume). If all
	 * listeners vote <code>DONT_CARE</code>, the thread will suspend by
	 * default.
	 * <p>
	 * The thread the breakpoint has been encountered in is now suspended.
	 * Listeners may query thread state and perform evaluations. All subsequent
	 * breakpoints in this thread will be ignored until voting has completed.
	 * </p>
	 *
	 * @param thread
	 *            Java thread
	 * @param breakpoint
	 *            Java breakpoint
	 * @return whether the thread should suspend or whether this listener
	 *         doesn't care - one of <code>SUSPEND</code>,
	 *         <code>DONT_SUSPEND</code>, or <code>DONT_CARE</code>
	 * @since 3.0
	 */
	public int breakpointHit(IJavaThread thread, IJavaBreakpoint breakpoint);

	/**
	 * Notification that the given breakpoint has been removed from the
	 * specified target.
	 *
	 * @param target
	 *            Java debug target
	 * @param breakpoint
	 *            Java breakpoint
	 */
	public void breakpointRemoved(IJavaDebugTarget target,
			IJavaBreakpoint breakpoint);

	/**
	 * Notification that the given breakpoint had runtime errors in its
	 * conditional expression.
	 *
	 * @param breakpoint
	 *            the breakpoint
	 * @param exception
	 *            the debug exception that occurred evaluating the breakpoint's
	 *            condition
	 */
	public void breakpointHasRuntimeException(IJavaLineBreakpoint breakpoint,
			DebugException exception);

	/**
	 * Notification that the given breakpoint has compilation errors in its
	 * conditional expression.
	 *
	 * @param breakpoint
	 *            the breakpoint
	 * @param errors
	 *            the compilation errors in the breakpoint's condition
	 */
	public void breakpointHasCompilationErrors(IJavaLineBreakpoint breakpoint,
			Message[] errors);
}
