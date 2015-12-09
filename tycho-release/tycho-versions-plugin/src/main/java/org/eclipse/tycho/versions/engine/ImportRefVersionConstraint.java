/*******************************************************************************
 * Copyright (c) 2015 Sebastien Arod and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sebastien Arod - Initial implementation
 *******************************************************************************/
package org.eclipse.tycho.versions.engine;

import java.util.Objects;

import org.osgi.framework.Version;

/**
 * Represent a version constraint using version and match attributes as defined
 * in feature manifest file (feature>requires>import)
 * {@link http://help.eclipse.org/mars/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Freference%2Fmisc%2Ffeature_manifest.html&cp=2_1_5_20}
 * 
 * @author sarod
 *
 */
public class ImportRefVersionConstraint {

	public static final String MATCH_GREATER_OR_EQUAL = "greaterOrEqual";
	public static final String MATCH_COMPATIBLE = "compatible";
	public static final String MATCH_EQUIVALENT = "equivalent";
	public static final String MATCH_PERFECT = "perfect";
	private final String version;
	private final String match;

	public ImportRefVersionConstraint(String version, String match) {
		this.version = version;
		this.match = match;
	}

	public String getVersion() {
		return version;
	}

	public String getMatch() {
		return match;
	}

	public ImportRefVersionConstraint withVersion(String newVersion) {
		return new ImportRefVersionConstraint(newVersion, getMatch());
	}

	/**
	 * Test whether otherVersion matches the current constraint.
	 * @return true if otherVersion matches the constraint.
	 */
	public boolean matches(String otherVersion) {
		if (version == null) {
			return true;
		}
		Version parsedLocalVersion = Version.parseVersion(version);
		Version parsedOtherVersion = Version.parseVersion(otherVersion);
		if (MATCH_PERFECT.equals(match)) {
			return isPerfectMatch(parsedLocalVersion, parsedOtherVersion);
		} else if (MATCH_EQUIVALENT.equals(match)) {
			return isEquivalent(parsedLocalVersion, parsedOtherVersion);
		} else if (MATCH_COMPATIBLE.equals(match)) {
			return isCompatible(parsedLocalVersion, parsedOtherVersion);
		} else if (MATCH_GREATER_OR_EQUAL.equals(match)) {
			return isGreaterOrEqual(parsedLocalVersion, parsedOtherVersion);
		} else {
			return isCompatible(parsedLocalVersion, parsedOtherVersion);
		}
	}

	private boolean isPerfectMatch(Version parsedLocalVersion, Version parsedOtherVersion) {
		return parsedLocalVersion.compareTo(parsedOtherVersion) == 0;
	}

	private boolean isEquivalent(Version parsedLocalVersion, Version parsedOtherVersion) {
		return isGreaterOrEqual(parsedLocalVersion, parsedOtherVersion)
				&& parsedLocalVersion.getMajor() == parsedOtherVersion.getMajor()
				&& parsedLocalVersion.getMinor() == parsedOtherVersion.getMinor();
	}

	private boolean isCompatible(Version parsedLocalVersion, Version parsedOtherVersion) {
		return isGreaterOrEqual(parsedLocalVersion, parsedOtherVersion)
				&& parsedLocalVersion.getMajor() == parsedOtherVersion.getMajor();
	}

	private boolean isGreaterOrEqual(Version parsedLocalVersion, Version parsedOtherVersion) {
		return parsedLocalVersion.compareTo(parsedOtherVersion) <= 0;
	}

	@Override
	public int hashCode() {
		return Objects.hash(version, match);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ImportRefVersionConstraint other = (ImportRefVersionConstraint) obj;
		return Objects.equals(version, other.version) && Objects.equals(match, other.match);
	}

	@Override
	public String toString() {
		if (version == null) {
			return "<no version>";
		} else {
			return version + "(match=" + match + ")";
		}
	}
}
