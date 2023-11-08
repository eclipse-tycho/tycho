/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
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

import org.eclipse.equinox.p2.metadata.ILicense;

/**
 * LicenseManager defines a service which records the licenses that have been
 * accepted in the course of installing or updating software. It can be used to determine
 * whether a particular license should be presented to a user for acceptance, and
 * to record the user's decision.
 *
 * @since 2.0
 */
public abstract class LicenseManager {

	/**
	 * Record the acceptance of the specified license.
	 *
	 * @param license the license to be accepted
	 *
	 * @return <code>true</code> if the license was recorded as accepted, <code>false</code> if
	 * it was not.
	 *
	 */
	public abstract boolean accept(ILicense license);

	/**
	 * Record the rejection of the specified license.
	 *
	 * @param license the license to be rejected
	 *
	 * @return <code>true</code> if the license was recorded as rejected, <code>false</code> if
	 * it was not.
	 *
	 */
	public abstract boolean reject(ILicense license);

	/**
	 * Return a boolean indicating whether a particular license has previously
	 * been accepted.
	 *
	 * @param license the license in question
	 *
	 * @return <code>true</code> if the license has previously been accepted,
	 * <code>false</code> if it has not been accepted before.
	 *
	 */
	public abstract boolean isAccepted(ILicense license);

	/**
	 * Return a boolean indicating whether any licenses have been
	 * accepted.
	 *
	 * @return <code>true</code> if accepted licenses have been recorded,
	 * <code>false</code> if there have been no licenses accepted.

	 */
	public abstract boolean hasAcceptedLicenses();
}
