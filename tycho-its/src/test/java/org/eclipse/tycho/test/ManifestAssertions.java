/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

/**
 * Fluent assertion helper for validating MANIFEST.MF version ranges after the
 * {@code check-dependencies} goal has applied suggestions. Uses
 * {@link ManifestElement} for header parsing and {@link VersionRange} /
 * {@link Version} for semantic version comparison.
 */
public class ManifestAssertions {

	private final Map<String, VersionRange> importPackageRanges;
	private final Map<String, VersionRange> requireBundleRanges;
	private final Map<String, String> requireBundleRawVersions;
	private final Map<String, String> importPackageRawVersions;
	private final String bundleName;

	private ManifestAssertions(Map<String, VersionRange> importPackageRanges,
			Map<String, VersionRange> requireBundleRanges, Map<String, String> importPackageRawVersions,
			Map<String, String> requireBundleRawVersions, String bundleName) {
		this.importPackageRanges = importPackageRanges;
		this.requireBundleRanges = requireBundleRanges;
		this.importPackageRawVersions = importPackageRawVersions;
		this.requireBundleRawVersions = requireBundleRawVersions;
		this.bundleName = bundleName;
	}

	/**
	 * Creates a {@link ManifestAssertions} instance by parsing the given manifest
	 * file.
	 *
	 * @param manifestFile the {@code META-INF/MANIFEST.MF} file to parse
	 * @return a new assertion helper wrapping the parsed manifest
	 * @throws IOException     if the file cannot be read
	 * @throws BundleException if a manifest header cannot be parsed
	 */
	public static ManifestAssertions of(File manifestFile) throws IOException, BundleException {
		Manifest manifest;
		try (FileInputStream fis = new FileInputStream(manifestFile)) {
			manifest = new Manifest(fis);
		}
		Attributes attrs = manifest.getMainAttributes();
		String symbolicName = attrs.getValue(Constants.BUNDLE_SYMBOLICNAME);
		if (symbolicName != null && symbolicName.contains(";")) {
			symbolicName = symbolicName.split(";")[0].trim();
		}
		Map<String, VersionRange> importRanges = parseRanges(
				attrs.getValue(Constants.IMPORT_PACKAGE), Constants.IMPORT_PACKAGE,
				Constants.VERSION_ATTRIBUTE);
		Map<String, String> importRaw = parseRawVersions(
				attrs.getValue(Constants.IMPORT_PACKAGE), Constants.IMPORT_PACKAGE,
				Constants.VERSION_ATTRIBUTE);
		Map<String, VersionRange> requireRanges = parseRanges(
				attrs.getValue(Constants.REQUIRE_BUNDLE), Constants.REQUIRE_BUNDLE,
				Constants.BUNDLE_VERSION_ATTRIBUTE);
		Map<String, String> requireRaw = parseRawVersions(
				attrs.getValue(Constants.REQUIRE_BUNDLE), Constants.REQUIRE_BUNDLE,
				Constants.BUNDLE_VERSION_ATTRIBUTE);
		return new ManifestAssertions(importRanges, requireRanges, importRaw, requireRaw, symbolicName);
	}

	/**
	 * Parses a manifest header into a map from entry name to
	 * {@link VersionRange}.
	 *
	 * @param headerValue the raw header value (may be {@code null})
	 * @param headerName  the header name for {@link ManifestElement#parseHeader}
	 * @param versionAttr the attribute name holding the version (e.g.
	 *                    {@code "version"} or {@code "bundle-version"})
	 * @return a map of entry names to their parsed {@link VersionRange}, only
	 *         containing entries that declare a version
	 * @throws BundleException if parsing fails
	 */
	private static Map<String, VersionRange> parseRanges(String headerValue, String headerName,
			String versionAttr) throws BundleException {
		Map<String, VersionRange> result = new HashMap<>();
		if (headerValue == null || headerValue.isBlank()) {
			return result;
		}
		ManifestElement[] elements = ManifestElement.parseHeader(headerName, headerValue);
		if (elements != null) {
			for (ManifestElement element : elements) {
				String versionStr = element.getAttribute(versionAttr);
				if (versionStr != null) {
					result.put(element.getValue(), new VersionRange(versionStr));
				}
			}
		}
		return result;
	}

	/**
	 * Parses a manifest header into a map from entry name to raw version string.
	 *
	 * @param headerValue the raw header value (may be {@code null})
	 * @param headerName  the header name for {@link ManifestElement#parseHeader}
	 * @param versionAttr the attribute name holding the version
	 * @return a map of entry names to their raw version strings
	 * @throws BundleException if parsing fails
	 */
	private static Map<String, String> parseRawVersions(String headerValue, String headerName,
			String versionAttr) throws BundleException {
		Map<String, String> result = new HashMap<>();
		if (headerValue == null || headerValue.isBlank()) {
			return result;
		}
		ManifestElement[] elements = ManifestElement.parseHeader(headerName, headerValue);
		if (elements != null) {
			for (ManifestElement element : elements) {
				String versionStr = element.getAttribute(versionAttr);
				if (versionStr != null) {
					result.put(element.getValue(), versionStr);
				}
			}
		}
		return result;
	}

	/**
	 * Asserts that the lower bound of the version range for an
	 * {@code Import-Package} equals the expected version.
	 *
	 * @param packageName   the imported package name
	 * @param expectedLower the expected lower bound version string (e.g.
	 *                      {@code "1.6.0"})
	 * @param message       assertion context message
	 * @return this instance for chaining
	 */
	public ManifestAssertions assertPackageLowerBound(String packageName, String expectedLower, String message) {
		VersionRange range = importPackageRanges.get(packageName);
		assertNotNull(message + " - Import-Package '" + packageName + "' should have a version range in " + bundleName,
				range);
		Version expected = Version.parseVersion(expectedLower);
		Version left = range.getLeft();
		assertTrue(message + " [Import-Package " + packageName + "] - expected lower bound " + expected
				+ " but was " + left, left.compareTo(expected) == 0);
		return this;
	}

	/**
	 * Asserts that the upper bound of the version range for an
	 * {@code Import-Package} equals the expected version.
	 *
	 * @param packageName   the imported package name
	 * @param expectedUpper the expected upper bound version string (e.g.
	 *                      {@code "2.0.0"})
	 * @param message       assertion context message
	 * @return this instance for chaining
	 */
	public ManifestAssertions assertPackageUpperBound(String packageName, String expectedUpper, String message) {
		VersionRange range = importPackageRanges.get(packageName);
		assertNotNull(message + " - Import-Package '" + packageName + "' should have a version range in " + bundleName,
				range);
		Version expected = Version.parseVersion(expectedUpper);
		Version right = range.getRight();
		assertNotNull(message + " [Import-Package " + packageName + "] - upper bound should exist: " + range, right);
		assertTrue(message + " [Import-Package " + packageName + "] - expected upper bound " + expected
				+ " but was " + right, right.compareTo(expected) == 0);
		return this;
	}

	/**
	 * Asserts that the lower bound of the version range for a
	 * {@code Require-Bundle} equals the expected version.
	 *
	 * @param bundleId      the required bundle symbolic name
	 * @param expectedLower the expected lower bound version string (e.g.
	 *                      {@code "3.5.0"})
	 * @param message       assertion context message
	 * @return this instance for chaining
	 */
	public ManifestAssertions assertBundleLowerBound(String bundleId, String expectedLower, String message) {
		VersionRange range = requireBundleRanges.get(bundleId);
		assertNotNull(message + " - Require-Bundle '" + bundleId + "' should have a version range in " + bundleName,
				range);
		Version expected = Version.parseVersion(expectedLower);
		Version left = range.getLeft();
		assertTrue(message + " [Require-Bundle " + bundleId + "] - expected lower bound " + expected
				+ " but was " + left, left.compareTo(expected) == 0);
		return this;
	}

	/**
	 * Asserts that the upper bound of the version range for a
	 * {@code Require-Bundle} equals the expected version.
	 *
	 * @param bundleId      the required bundle symbolic name
	 * @param expectedUpper the expected upper bound version string (e.g.
	 *                      {@code "4.0.0"})
	 * @param message       assertion context message
	 * @return this instance for chaining
	 */
	public ManifestAssertions assertBundleUpperBound(String bundleId, String expectedUpper, String message) {
		VersionRange range = requireBundleRanges.get(bundleId);
		assertNotNull(message + " - Require-Bundle '" + bundleId + "' should have a version range in " + bundleName,
				range);
		Version expected = Version.parseVersion(expectedUpper);
		Version right = range.getRight();
		assertNotNull(message + " [Require-Bundle " + bundleId + "] - upper bound should exist: " + range, right);
		assertTrue(message + " [Require-Bundle " + bundleId + "] - expected upper bound " + expected
				+ " but was " + right, right.compareTo(expected) == 0);
		return this;
	}

	/**
	 * Asserts that the raw version string for a {@code Require-Bundle} entry
	 * exactly matches the expected string. This verifies that the manifest was
	 * not modified when the existing range is already semantically correct.
	 *
	 * @param bundleId        the required bundle symbolic name
	 * @param expectedRawVersion the expected raw version string (e.g.
	 *                        {@code "[3.5.0,4)"})
	 * @param message         assertion context message
	 * @return this instance for chaining
	 */
	public ManifestAssertions assertBundleRawVersion(String bundleId, String expectedRawVersion, String message) {
		String rawVersion = requireBundleRawVersions.get(bundleId);
		assertNotNull(message + " - Require-Bundle '" + bundleId + "' should have a version in " + bundleName,
				rawVersion);
		assertTrue(message + " [Require-Bundle " + bundleId + "] - expected raw version '" + expectedRawVersion
				+ "' but was '" + rawVersion + "'", expectedRawVersion.equals(rawVersion));
		return this;
	}
}
