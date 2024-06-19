/*******************************************************************************
 * Copyright (c) 2022, 2024 Christoph Läubrich and others.
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
package org.eclipse.tycho.baseline;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Function;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.artifactcomparator.ArtifactComparator.ComparisonData;
import org.eclipse.tycho.artifactcomparator.ArtifactDelta;
import org.eclipse.tycho.artifactcomparator.ComparatorInputStream;
import org.eclipse.tycho.p2maven.repository.P2RepositoryManager;
import org.eclipse.tycho.zipcomparator.internal.ContentsComparator;
import org.eclipse.tycho.zipcomparator.internal.DefaultContentsComparator;
import org.eclipse.tycho.zipcomparator.internal.ManifestComparator;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

import aQute.bnd.differ.Baseline;
import aQute.bnd.differ.Baseline.BundleInfo;
import aQute.bnd.differ.Baseline.Info;
import aQute.bnd.differ.DiffPluginImpl;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Instructions;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.diff.Delta;
import aQute.bnd.service.diff.Diff;
import aQute.bnd.service.diff.Tree;
import aQute.bnd.service.diff.Type;
import aQute.lib.strings.Strings;
import de.vandermeer.asciitable.AT_Cell;
import de.vandermeer.asciitable.AT_Row;
import de.vandermeer.asciitable.AsciiTable;
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment;

@Named(ArtifactType.TYPE_ECLIPSE_PLUGIN)
@Singleton
public class BundleArtifactBaselineComparator implements ArtifactBaselineComparator {

	private static final String X_INTERNAL_ATTRIBUTE = "x-internal";

	private static final String X_FRIENDS_ATTRIBUTE = "x-friends";

	private static final String INDENT = "..";

	private static final int WIDTH = 160;

	@Inject
	private P2RepositoryManager repositoryManager;

	@Inject
	Map<String, ContentsComparator> contentComparators;

	@Override
	public boolean compare(MavenProject project, BaselineContext context) throws Exception {
		byte[] baselineData = getBaseline(context);
		if (baselineData == null) {
			return false;
		}
		File artifact = project.getArtifact().getFile();
		if (artifact == null) {
			throw new MojoExecutionException("Artifact is null");
		}
		if (!artifact.exists()) {
			throw new MojoExecutionException("Artifact (" + artifact + ") does not exists.");
		}
		try (Processor processor = new Processor();
				Jar projectJar = new Jar(artifact);
				Jar baselineJar = new Jar("baseline", new ByteArrayInputStream(baselineData))) {
			Baseline baseliner = createBaselineCompare(context, processor);
			Instructions packageFilters = getPackageFilters(context, processor, projectJar);
			List<Info> infos = baseliner.baseline(projectJar, baselineJar, packageFilters).stream()//
					.filter(info -> info.packageDiff.getDelta() != Delta.UNCHANGED) //
					.sorted(Comparator.comparing(info -> info.packageName))//
					.toList();
			BundleInfo bundleInfo = baseliner.getBundleInfo();
			boolean failed = false;
			Map<Diff, ArtifactDelta> resourcediffs = new LinkedHashMap<>();
			collectResources(baseliner.getDiff(), resourcediffs, baselineJar, projectJar, context);
			List<Diff> manifestdiffs = new ArrayList<>();
			collectManifest(baseliner.getDiff(), manifestdiffs);
			processManifestDiff(manifestdiffs);
			if (!infos.isEmpty() || !resourcediffs.isEmpty() || !manifestdiffs.isEmpty()) {
				AsciiTable at = new AsciiTable();
				at.addRule();
				at.addRow("Delta", "Type", "Name", "Project Version", "Baseline Version", "Suggested Version");
				for (Info info : infos) {
					Diff packageDiff = info.packageDiff;
					addDiff(packageDiff, info, at, 0);
					failed |= info.mismatch;
				}
				Version pv = getBaseVersion(projectJar.getVersion());
				Version bv = getBaseVersion(baselineJar.getVersion());
				aQute.bnd.version.Version sv = new aQute.bnd.version.Version(bv.getMajor(), bv.getMinor(),
						bv.getMicro() + context.getMicroIncrement());
				if (!manifestdiffs.isEmpty()) {
					failed |= requireVersionBump(bundleInfo, pv, bv, sv);
					at.addRule();
					at.addRow(Delta.MICRO, Type.MANIFEST, artifact.getName(), pv, bv, sv);
					for (Diff diff : manifestdiffs) {
						String name = diff.getName();
						at.addRule();
						at.addRow(INDENT + getResourceDeltaString(diff), diff.getType(), null, null, null, name);
					}
				}
				if (!resourcediffs.isEmpty()) {
					failed |= requireVersionBump(bundleInfo, pv, bv, sv);
					at.addRule();
					at.addRow(Delta.MICRO, Type.RESOURCES, artifact.getName(), pv, bv, sv);
					for (Entry<Diff, ArtifactDelta> entry : resourcediffs.entrySet()) {
						Diff diff = entry.getKey();
						String name = diff.getName();
						at.addRule();
						at.addRow(INDENT + getResourceDeltaString(diff), getResourceTypeString(diff), null, null, null,
								name);
						String message = entry.getValue().getDetailedMessage();
						if (message != null && !message.isBlank() && !message.equals("different")) {
							at.addRule();
							at.addRow(null, null, null, null, null, message.replace(System.lineSeparator(), "<br>"))
									.setTextAlignment(TextAlignment.LEFT);
						}
					}
				}
				if (failed) {
					at.addRule();
					Logger logger = context.getLogger();
					try {
						at.renderAsIterator(WIDTH).forEachRemaining(logger::error);
					} catch (RuntimeException e) {
						// Rendering sometimes fails if there is bad data (e.g. nulls) we fall back to
						// plain output then to allow debugging and error reporting...
						for (AT_Row row : at.getRawContent()) {
							LinkedList<AT_Cell> cells = row.getCells();
							if (cells == null) {
								continue;
							}
							StringBuilder sb = new StringBuilder();
							for (AT_Cell cell : cells) {
								sb.append(cell.getContent());
								sb.append(" |\t");
							}
							logger.error(sb.toString());
						}
					}
				}
			}
			if (failed) {
				StringBuilder message = new StringBuilder("Baseline problems found! ");
				message.append("Project version: ");
				message.append(projectJar.getVersion());
				message.append(", baseline version: ");
				message.append(baselineJar.getVersion());
				Version suggestedVersion;
				if (bundleInfo.suggestedVersion != null) {
					message.append(", suggested version: ");
					message.append(bundleInfo.suggestedVersion);
					suggestedVersion = new Version(bundleInfo.suggestedVersion.toString());
				} else {
					suggestedVersion = null;
				}
				if (bundleInfo.reason != null && !bundleInfo.reason.isBlank()) {
					message.append(", ");
					message.append(bundleInfo.reason);
				}
				context.reportBaselineProblem(message.toString(), suggestedVersion);
			}
		}
		return true;
	}

	private void processManifestDiff(List<Diff> manifestdiffs) {
		try {
			if (manifestdiffs.size() > 1) {
				@SuppressWarnings("deprecation")
				Diff breeDiff = manifestdiffs.stream().filter(
						diff -> Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT.equalsIgnoreCase(getHeaderName(diff)))
						.findFirst().orElse(null);
				if (breeDiff != null && breeDiff.getDelta() == Delta.REMOVED) {
					// deprecated header was removed... check if it was added to the ee instead?
					Map<Delta, Diff> capDiffMap = manifestdiffs.stream()
							.filter(diff -> Constants.REQUIRE_CAPABILITY.equalsIgnoreCase(getHeaderName(diff)))
							.collect(Collectors.toMap(Diff::getDelta, Function.identity()));
					if (!capDiffMap.isEmpty()) {
						Diff capDiff = capDiffMap.get(Delta.ADDED);
						if (capDiff != null) {
							if (ManifestComparator.isEquivialentBreeCap(getOldHeaderValue(breeDiff),
									getNewHeaderValue(capDiff))) {
								manifestdiffs.remove(capDiff);
								manifestdiffs.remove(breeDiff);
							}
							Diff capDiffPrev = capDiffMap.get(Delta.REMOVED);
							if (capDiffPrev != null) {
								if (ManifestComparator.isEquivialentBreeCap(getOldHeaderValue(breeDiff),
										getOldHeaderValue(capDiffPrev), getNewHeaderValue(capDiff))) {
									manifestdiffs.remove(capDiff);
									manifestdiffs.remove(breeDiff);
									manifestdiffs.remove(capDiffPrev);
								}
							}
						}
					}

				}
			}
		} catch (RuntimeException e) {
			// just in case something cannot be processed we don't want to fail here!
		}
	}

	private String getNewHeaderValue(Diff diff) {
		String name = diff.getName();
		String[] split = name.split(":", 2);
		return split[1].trim();
	}

	private String getOldHeaderValue(Diff diff) {
		Tree older = diff.getOlder();
		for (Tree child : older.getChildren()) {
			return child.getName();
		}
		return getNewHeaderValue(diff);
	}

	private boolean requireVersionBump(BundleInfo bundleInfo, Version projectVersion, Version baselineVersion,
			aQute.bnd.version.Version suggestedVersion) {
		if (baselineVersion.compareTo(projectVersion) >= 0) {
			// a version bump is required!
			if (bundleInfo.suggestedVersion == null || suggestedVersion.compareTo(bundleInfo.suggestedVersion) > 0) {
				bundleInfo.suggestedVersion = suggestedVersion;
			}
			return true;
		}
		return false;
	}

	private byte[] getBaseline(BaselineContext context) throws IOException {
		ArtifactKey key = context.getArtifactKey();
		org.osgi.framework.Version artifactVersion = org.osgi.framework.Version.parseVersion(key.getVersion());
		org.eclipse.equinox.p2.metadata.Version maxVersion = org.eclipse.equinox.p2.metadata.Version
				.createOSGi(artifactVersion.getMajor(), artifactVersion.getMinor(), artifactVersion.getMicro() + 1);
		IQueryResult<IInstallableUnit> result = context.getMetadataRepository()
				.query(QueryUtil.createLatestQuery(QueryUtil.createIUQuery(key.getId(), new VersionRange(
						org.eclipse.equinox.p2.metadata.Version.emptyVersion, true, maxVersion, false))), null);
		if (result.isEmpty()) {
			return null;
		}
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		repositoryManager.downloadArtifact(result.iterator().next(), context.getArtifactRepository(), outputStream);
		return outputStream.toByteArray();
	}

	private Instructions getPackageFilters(BaselineContext context, Processor processor, Jar projectJar) {
		Collection<String> packages;
		if (context.isExtensionsEnabled()) {
			packages = new LinkedHashSet<>();
			Map<String, Boolean> projectPackages = getPackagesMap(context, projectJar);
			Map<String, Boolean> baselinePackages = getPackagesMap(context, projectJar);
			// every package that was internal in the baseline can be ignored already
			// because it was never public
			Logger logger = context.getLogger();
			for (Entry<String, Boolean> entry : baselinePackages.entrySet()) {
				if (entry.getValue()) {
					logger.debug("Ignore package " + entry.getKey() + " as it is marked as internal in the baseline.");
					packages.add("!" + entry.getKey());
				}
			}
			// now we can ignore all packages that are internal, but not in the baseline,
			// these are packages that are new and marked as internal
			for (Entry<String, Boolean> entry : projectPackages.entrySet()) {
				if (entry.getValue() && !baselinePackages.containsKey(entry.getKey())) {
					logger.debug("Ignore package " + entry.getKey() + " as it is marked as internal in the project.");
					packages.add("!" + entry.getKey());
				}
			}
			// finally add everything that was supplied by the context...
			packages.addAll(context.getPackages());
		} else {
			packages = context.getPackages();
		}
		return new Instructions(new Parameters(Strings.join(packages), processor));
	}

	private Map<String, Boolean> getPackagesMap(BaselineContext context, Jar jar) {
		Map<String, Boolean> internalPackages = new HashMap<>();
		Logger logger = context.getLogger();
		try {
			Manifest manifest = jar.getManifest();
			if (manifest == null) {
				return Map.of();
			}
			String exportPackageHeader = manifest.getMainAttributes().getValue(Constants.EXPORT_PACKAGE);
			if (exportPackageHeader != null) {
				try {
					ManifestElement[] exports = ManifestElement.parseHeader(Constants.EXPORT_PACKAGE,
							exportPackageHeader);
					for (ManifestElement export : exports) {
						boolean internal = export.getDirective(X_FRIENDS_ATTRIBUTE) != null
								|| Boolean.parseBoolean(export.getDirective(X_INTERNAL_ATTRIBUTE));
						internalPackages.put(export.getValue(), internal);
					}
				} catch (BundleException e) {
					String message = "Cannot get export package from Manifest, extensions cannot be processed";
					if (logger.isDebugEnabled()) {
						logger.error(message, e);
					} else {
						logger.warn(message);
					}
				}
			}
		} catch (Exception e) {
			String message = "Cannot get manifest from jar, extensions cannot be processed";
			if (logger.isDebugEnabled()) {
				logger.error(message, e);
			} else {
				logger.warn(message);
			}
		}
		return internalPackages;
	}

	private Object getResourceDeltaString(Diff diff) {
		Delta delta = diff.getDelta();
		if (delta == Delta.MAJOR) {
			// BND reports a remove/add as a MAJOR change...?!
			return "CHANGED";
		}
		return delta.toString();
	}

	private Object getResourceTypeString(Diff diff) {
		Type type = diff.getType();
		String name = diff.getName();
		if (name != null && name.endsWith(".class")) {
			return "CLASS";
		}
		return type;
	}

	private Version getBaseVersion(String version) {
		Version fullVersion = new Version(version);
		return new Version(fullVersion.getMajor(), fullVersion.getMinor(), fullVersion.getMicro());

	}

	private void collectManifest(Diff diff, List<Diff> manifestdiffs) {
		if (diff.getDelta() == Delta.UNCHANGED) {
			return;
		}
		if (isValidHeaderDif(diff)) {
			manifestdiffs.add(diff);
		}
		for (Diff child : diff.getChildren()) {
			collectManifest(child, manifestdiffs);
		}
	}

	private boolean isValidHeaderDif(Diff diff) {
		return diff.getType() == Type.HEADER && !ManifestComparator.isIgnoredHeaderName(getHeaderName(diff));
	}

	private String getHeaderName(Diff diff) {
		if (diff == null) {
			return null;
		}
		String name = diff.getName();
		String[] split = name.split(":", 2);
		return split[0];
	}

	private void collectResources(Diff diff, Map<Diff, ArtifactDelta> resourcediffs, Jar baselineJar, Jar projectJar,
			BaselineContext baselineContext) {
		if (diff.getDelta() == Delta.UNCHANGED) {
			return;
		}
		if (diff.getType() == Type.RESOURCE) {
			ArtifactDelta delta = getDelta(diff, baselineJar, projectJar, baselineContext);
			if (delta != null) {
				resourcediffs.put(diff, delta);
			}
		}
		for (Diff child : diff.getChildren()) {
			collectResources(child, resourcediffs, baselineJar, projectJar, baselineContext);
		}
	}

	private ArtifactDelta getDelta(Diff diff, Jar baselineJar, Jar projectJar, BaselineContext baselineContext) {
		String name = diff.getName();
		ContentsComparator comparator = getComparator(name);
		if (comparator != null) {
			Resource baseResource = baselineJar.getResource(name);
			Resource currenttResource = projectJar.getResource(name);
			if (baseResource != null && currenttResource != null) {
				try (InputStream baseStream = baseResource.openInputStream();
						InputStream currentStream = currenttResource.openInputStream()) {
					ComparatorInputStream baseline = new ComparatorInputStream(baseStream);
					ComparatorInputStream reactor = new ComparatorInputStream(currentStream);
					if (baseline.size() < ContentsComparator.THRESHOLD
							&& reactor.size() < ContentsComparator.THRESHOLD) {
						return comparator.getDelta(baseline, reactor,
								new ComparisonData(baselineContext.getIgnores(), false));
					}
				} catch (Exception e) {
				}
			}
		}
		return ArtifactDelta.DEFAULT;
	}

	private ContentsComparator getComparator(String name) {
		String extension = FilenameUtils.getExtension(name).toLowerCase();
		ContentsComparator comparator = contentComparators.get(extension);
		if (comparator == null) {
			for (ContentsComparator alternative : contentComparators.values()) {
				if (alternative.matches(name) || alternative.matches(extension)) {
					return alternative;
				}
			}
			return contentComparators.get(DefaultContentsComparator.TYPE);
		}
		return comparator;
	}

	private void addDiff(Diff diff, Info info, AsciiTable at, int indent) {
//Delta | Type | Name | Project Version | Baseline Version | Suggested | Version
		Delta delta = diff.getDelta();
		if (delta == Delta.UNCHANGED) {
			return;
		}
		at.addRule();
		String deltaString = INDENT.repeat(indent) + delta;
		if (info != null) {
			Object[] row = { deltaString, diff.getType(), diff.getName(), info.newerVersion, info.olderVersion,
					Objects.requireNonNullElse(info.suggestedVersion, "-") };
			at.addRow(row);
		} else {
			Object[] row = { deltaString, diff.getType(), null, null, null, diff.getName() };
			at.addRow(row);
		}
		if (diff.getType() == Type.METHOD && delta == Delta.ADDED) {
			// if something is added, all childs do not really matter
			return;
		}
		if (diff.getType() == Type.PACKAGE && delta == Delta.REMOVED) {
			// if a package is removed there is no need to print what exactly was removed...
			return;
		}
		for (Diff child : diff.getChildren()) {
			addDiff(child, null, at, indent + 1);
		}
	}

	private Baseline createBaselineCompare(BaselineContext context, Processor processor) throws IOException {
		DiffPluginImpl differ = new DiffPluginImpl();
		differ.setIgnore(new Parameters(Strings.join(context.getIgnores()), processor));
		return new Baseline(processor, differ);
	}

}
