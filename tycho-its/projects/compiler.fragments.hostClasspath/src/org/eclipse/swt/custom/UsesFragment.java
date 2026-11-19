/*******************************************************************************
 * Copyright (c) 2026 Chris Lake and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Chris Lake - initial API and implementation
 *******************************************************************************/
package org.eclipse.swt.custom;

import org.eclipse.swt.internal.DPIUtil;

public class UsesFragment {
	public static int zoom() {
		return DPIUtil.getDeviceZoom();
	}
}
