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
package org.eclipse.equinox.p2.internal.repository.tools;

import java.net.URI;
import org.eclipse.equinox.internal.p2.repository.helpers.RepositoryHelper;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.osgi.util.NLS;

public class RepositoryDescriptor {

	public static final int TYPE_BOTH = -1;
	public static final String KIND_ARTIFACT = "A"; //$NON-NLS-1$
	public static final String KIND_METADATA = "M"; //$NON-NLS-1$

	private boolean compressed = true;
	private boolean append = true;
	private String name = null;
	private URI location = null;
	private URI format = null;
	private int kind = TYPE_BOTH;
	private URI originalLocation = null;
	private boolean optional = false;
	private String atomic = null;

	public void setCompressed(boolean compress) {
		compressed = compress;
	}

	public void setName(String repoName) {
		name = repoName;
	}

	public void setOptional(boolean optional) {
		this.optional = optional;
	}

	public boolean isOptional() {
		return optional;
	}

	public void setLocation(URI repoLocation) {
		originalLocation = repoLocation;
		location = RepositoryHelper.localRepoURIHelper(repoLocation);
	}

	public void setFormat(URI format) {
		this.format = RepositoryHelper.localRepoURIHelper(format);
	}

	public void setAppend(boolean appendMode) {
		append = appendMode;
	}

	public boolean isCompressed() {
		return compressed;
	}

	public boolean isAppend() {
		return append;
	}

	public String getName() {
		return name;
	}

	public URI getRepoLocation() {
		return location;
	}

	public URI getOriginalRepoLocation() {
		return originalLocation;
	}

	public URI getFormat() {
		return format;
	}

	public int getKind() {
		return kind;
	}

	public boolean isBoth() {
		return kind == TYPE_BOTH;
	}

	public boolean isArtifact() {
		return kind == TYPE_BOTH || kind == IRepository.TYPE_ARTIFACT;
	}

	public boolean isMetadata() {
		return kind == TYPE_BOTH || kind == IRepository.TYPE_METADATA;
	}

	public void setKind(String repoKind) {
		kind = determineKind(repoKind);
	}

	public void setAtomic(String booleanForAtomic) {
		atomic = booleanForAtomic;
	}

	public String getAtomic() {
		return atomic;
	}

	/*
	 * Determine the repository type
	 */
	public static int determineKind(String repoKind) {
		if (kindMatches(repoKind, KIND_METADATA))
			return IRepository.TYPE_METADATA;
		else if (kindMatches(repoKind, KIND_ARTIFACT))
			return IRepository.TYPE_ARTIFACT;

		throw new IllegalArgumentException(NLS.bind(Messages.unknown_repository_type, repoKind));
	}

	/*
	 * Determine if the repository kind matches the identifier kind
	 */
	public static boolean kindMatches(String repoKind, String kindIdentifier) {
		return repoKind.startsWith(kindIdentifier) || repoKind.startsWith(kindIdentifier.toLowerCase());
	}
}
