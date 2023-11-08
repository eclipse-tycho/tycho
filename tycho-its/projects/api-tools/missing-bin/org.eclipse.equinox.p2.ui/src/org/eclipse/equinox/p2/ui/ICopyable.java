/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
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
package org.eclipse.equinox.p2.ui;

import org.eclipse.swt.widgets.Control;

/**
 * ICopyable defines an interface for elements that provide
 * copy support in a UI.  The active control in the UI determines
 * what should be copied.
 *
 * @since 2.0
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface ICopyable {
	/**
	 * Copy text related to the active control to the clipboard.
	 *
	 * @param activeControl the active control
	 */
	public void copyToClipboard(Control activeControl);
}
