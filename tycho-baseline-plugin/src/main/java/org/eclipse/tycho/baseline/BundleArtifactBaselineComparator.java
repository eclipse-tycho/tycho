/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.ReactorProject;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.sonatype.plexus.build.incremental.BuildContext;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;

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
import aQute.bnd.service.diff.Type;
import aQute.lib.strings.Strings;
import de.vandermeer.asciitable.AT_Cell;
import de.vandermeer.asciitable.AT_Row;
import de.vandermeer.asciitable.AsciiTable;

@Component(role = ArtifactBaselineComparator.class, hint = ArtifactType.TYPE_ECLIPSE_PLUGIN)
public class BundleArtifactBaselineComparator implements ArtifactBaselineComparator {

	private static final String X_INTERNAL_ATTRIBUTE = "x-internal";

	private static final String X_FRIENDS_ATTRIBUTE = "x-friends";

	private static final String INDENT = "..";

	private static final int WIDTH = 160;

	@Requirement
	BuildContext buildContext;

	@Override
	public IInstallableUnit selectIU(IQueryable<IInstallableUnit> result) {
		// TODO should we create a bundle query instead?
		return result.query(QueryUtil.createLatestIUQuery(), null).iterator().next();
	}

	@Override
	public void compare(ReactorProject project, Supplier<InputStream> baselineArtifact, BaselineContext context)
			throws Exception {
		File artifact = project.getArtifact();
		if (artifact == null) {
			throw new MojoExecutionException("Artifact is null");
		}
		if (!artifact.exists()) {
			throw new MojoExecutionException("Artifact (" + artifact + ") does not exists.");
		}
		try (Processor processor = new Processor();
				Jar projectJar = new Jar(artifact);
				Jar baselineJar = new Jar("baseline", baselineArtifact.get())) {
			Baseline baseliner = createBaselineCompare(context, processor);
			Instructions packageFilters = getPackageFilters(context, processor, projectJar, baselineJar);
			List<Info> infos = baseliner.baseline(projectJar, baselineJar, packageFilters).stream()//
					.filter(info -> info.packageDiff.getDelta() != Delta.UNCHANGED) //
					.sorted(Comparator.comparing(info -> info.packageName))//
					.toList();
			BundleInfo bundleInfo = baseliner.getBundleInfo();
			boolean failed = bundleInfo.mismatch;
			ArrayList<Diff> resourcediffs = new ArrayList<Diff>();
			collectResources(baseliner.getDiff(), resourcediffs);
			if (!infos.isEmpty() || !resourcediffs.isEmpty()) {
				AsciiTable at = new AsciiTable();
				at.addRule();
				at.addRow("Delta", "Type", "Name", "Project Version", "Baseline Version", "Suggested Version");
				for (Info info : infos) {
					Diff packageDiff = info.packageDiff;
					addDiff(packageDiff, info, at, 0);
					failed |= info.mismatch;
				}
				if (!resourcediffs.isEmpty()) {
					Version pv = getBaseVersion(projectJar.getVersion());
					Version bv = getBaseVersion(baselineJar.getVersion());
					aQute.bnd.version.Version sv = new aQute.bnd.version.Version(bv.getMajor(), bv.getMinor(),
							bv.getMicro() + 100);
					if (bv.compareTo(pv) >= 0) {
						// a version bump is required!
						failed = true;
						if (failed && bundleInfo.suggestedVersion == null
								|| sv.compareTo(bundleInfo.suggestedVersion) > 0) {
							bundleInfo.suggestedVersion = sv;
						}
					}
					at.addRule();
					at.addRow(Delta.MICRO, Type.RESOURCES, artifact.getName(), pv, bv, sv);
					for (Diff diff : resourcediffs) {
						String name = diff.getName();
						at.addRule();
						at.addRow(INDENT + getResourceDeltaString(diff), diff.getType(), null, null, null, name);
						if (isComparable(name)) {
							List<String> source = getLines(baselineJar.getResource(name));
							List<String> target = getLines(projectJar.getResource(name));
							Patch<String> patch = DiffUtils.diff(source, target);
							List<String> unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff(name, name, source, patch,
									3);
							String collect = unifiedDiff.stream().collect(Collectors.joining("<br>"));
							if (!collect.isBlank()) {
								at.addRule();
								at.addRow(null, null, null, null, null, collect);
							}
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
				if (bundleInfo.suggestedVersion != null) {
					message.append(", suggested version: ");
					message.append(bundleInfo.suggestedVersion);
				}
				if (bundleInfo.reason != null && !bundleInfo.reason.isBlank()) {
					message.append(", ");
					message.append(bundleInfo.reason);
				}
				context.reportBaselineProblem(message.toString());
			}

		}

	}

	private Instructions getPackageFilters(BaselineContext context, Processor processor, Jar projectJar,
			Jar baselineJar) {
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
				if (entry.getValue()) {
					if (!baselinePackages.containsKey(entry.getKey())) {
						logger.debug(
								"Ignore package " + entry.getKey() + " as it is marked as internal in the project.");
						packages.add("!" + entry.getKey());
					}
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
					String message = "Can't get export package from manifest, extensions cannot be processed!";
					if (logger.isDebugEnabled()) {
						logger.error(message, e);
					} else {
						logger.warn(message);
					}
				}
			}
		} catch (Exception e) {
			String message = "Can't get manifest from jar, extensions cannot be processed!";
			if (logger.isDebugEnabled()) {
				logger.error(message, e);
			} else {
				logger.warn(message);
			}
		}
		return internalPackages;
	}

	private List<String> getLines(Resource resource) throws IOException, Exception {
		if (resource == null) {
			return List.of();
		}
		try (InputStream stream = resource.openInputStream()) {
			return IOUtils.readLines(stream, StandardCharsets.UTF_8);
		}
	}

	private boolean isComparable(String name) {
		return name != null && name.toLowerCase().endsWith(".xml");
	}

	private Object getResourceDeltaString(Diff diff) {
		Delta delta = diff.getDelta();
		if (delta == Delta.MAJOR) {
			// BND reports a remove/add as a MAJOR change...?!
			return "CHANGED";
		}
		return delta.toString();
	}

	private Version getBaseVersion(String version) {
		Version fullVersion = new Version(version);
		return new Version(fullVersion.getMajor(), fullVersion.getMinor(), fullVersion.getMicro());

	}

	private void collectResources(Diff diff, Collection<Diff> resourcediffs) {
		if (diff.getDelta() == Delta.UNCHANGED) {
			return;
		}
		if (diff.getType() == Type.RESOURCE && !diff.getName().endsWith(".class")) {
			resourcediffs.add(diff);
		}
		for (Diff child : diff.getChildren()) {
			collectResources(child, resourcediffs);
		}
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
		Baseline baseliner = new Baseline(processor, differ);
		return baseliner;
	}

}
