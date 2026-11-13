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
package compiler.fragments.accessrules;

import org.eclipse.swt.internal.DPIUtil;

@SuppressWarnings("restriction")
public class Suppressed {
	public static int zoom() {
		return DPIUtil.getDeviceZoom();
	}
}
