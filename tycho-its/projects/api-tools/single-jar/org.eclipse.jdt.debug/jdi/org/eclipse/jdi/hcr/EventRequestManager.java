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

import com.sun.jdi.ObjectCollectedException;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.request.DuplicateRequestException;
import com.sun.jdi.request.EventRequest;

/**
 * Hot code replacement extension to
 * <code>com.sun.jdi.request.EventRequestManager</code>.
 */
public interface EventRequestManager extends com.sun.jdi.Mirror {
	/**
	 * Creates a new disabled {@link ReenterStepRequest}. The new event request
	 * is added to the list managed by this EventRequestManager. Use
	 * {@link EventRequest#enable()} to activate this event request.
	 * <p>
	 * The returned request will control stepping only in the specified
	 * <code>thread</code>; all other threads will be unaffected.
	 * <p>
	 * Only one pending reenter step request is allowed per thread.
	 * <p>
	 * Note that enabling such a request can throw an
	 * <code>OperationRefusedException</code> if the VM refused to perform this
	 * operation. This in recognition that the VM may be in an awkward state and
	 * unable to comply. For example, execution is suspended in a native method
	 * and the arguments would be unavailable on return .
	 *
	 * @param thread
	 *            the thread in which to step
	 * @return the created {@link ReenterStepRequest}
	 * @throws DuplicateRequestException
	 *             if there is already a pending step request for the specified
	 *             thread.
	 * @throws ObjectCollectedException
	 *             if the thread object has been garbage collected.
	 */
	public ReenterStepRequest createReenterStepRequest(ThreadReference thread);
}
