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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.equinox.internal.p2.metadata.IRequiredCapability;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.Version;
import org.eclipse.equinox.p2.metadata.VersionRange;
import org.eclipse.equinox.p2.query.CollectionResult;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.artifactcomparator.ArtifactComparator.ComparisonData;
import org.eclipse.tycho.artifactcomparator.ArtifactDelta;
import org.eclipse.tycho.artifactcomparator.ComparatorInputStream;
import org.eclipse.tycho.p2.metadata.IP2Artifact;
import org.eclipse.tycho.p2.metadata.P2Generator;
import org.eclipse.tycho.p2.metadata.PublisherOptions;
import org.eclipse.tycho.p2maven.io.MetadataIO;
import org.eclipse.tycho.p2maven.repository.P2RepositoryManager;
import org.eclipse.tycho.p2resolver.ArtifactFacade;
import org.eclipse.tycho.zipcomparator.internal.ContentsComparator;

import de.vandermeer.asciitable.AT_Cell;
import de.vandermeer.asciitable.AT_Row;
import de.vandermeer.asciitable.AsciiTable;
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment;

/**
 * Check features according to <a href=
 * "https://wiki.eclipse.org/Version_Numbering#Versioning_features">Versioning
 * features</a> that is:
 * <ul>
 * <li>Increment the feature's major number if any contained plug-in or feature
 * increases their major number, additionally removing an item is considered a
 * major change</li>
 * <li>Otherwise, increment the feature's minor number if any contained plug-in
 * or feature increases their minor number, additionally adding an item is
 * considered a minor change</li>
 * <li>Otherwise, increment the feature's micro number if any contained plug-in
 * or feature increases their micro number, additionally changing properties
 * will be considered a micro change</li>
 * <ul>
 */
@Component(role = ArtifactBaselineComparator.class, hint = ArtifactType.TYPE_ECLIPSE_FEATURE)
public class FeatureBaselineComparator implements ArtifactBaselineComparator {

	private static final int WIDTH = 160;

	private static final String GROUP_SUFFIX = ".feature.group";
	private static final String JAR_SUFFIX = ".feature.jar";
	private static final String SOURCE_SUFFIX = ".source";

	@Requirement(hint = "zip")
	ContentsComparator zipComparator;

	@Requirement
	MetadataIO metadataIO;

	@Requirement
	P2Generator p2generator;

	@Requirement
	P2RepositoryManager repositoryManager;

	@Override
	public boolean compare(MavenProject project, BaselineContext context) throws Exception {
		ArtifactKey key = context.getArtifactKey();
		IInstallableUnit baselineGroupUnit = getBaselineUnit(key, context.getMetadataRepository());
		if (baselineGroupUnit == null) {
			return false;
		}
		IInstallableUnit baselineJarUnit = getJarUnit(key, baselineGroupUnit, context.getMetadataRepository());
		if (baselineJarUnit == null) {
			return false;
		}
		IQueryable<IInstallableUnit> projectUnits = getProjectUnits(project);
		IInstallableUnit projectGroupUnit = getBaselineUnit(key, projectUnits);
		IInstallableUnit projectJarUnit = getJarUnit(key, projectGroupUnit, projectUnits);
		if (projectGroupUnit == null || projectJarUnit == null) {
			throw new MojoExecutionException("Can't find required project units!");
		}
		List<Diff> propertyDiffs = computePropertyDiff(baselineGroupUnit.getProperties(),
				projectGroupUnit.getProperties());
		List<Diff> jarDiffs = computeJarDelta(project, context, baselineJarUnit);
		List<Diff> requirementDiffs = computeRequirementDelta(getRequirements(baselineGroupUnit),
				getRequirements(projectGroupUnit));
		ImpliedVersionChange change = jarDiffs.isEmpty() && propertyDiffs.isEmpty() ? ImpliedVersionChange.UNCHANGED
				: ImpliedVersionChange.MICRO;
		for (Diff diff : requirementDiffs) {
			if (diff.change.ordinal() > change.ordinal()) {
				change = diff.change;
			}
		}
		if (needsVersionBump(baselineGroupUnit.getVersion(), projectGroupUnit.getVersion(), change, context)) {
			Version suggestedVersion = getSuggestedVersion(projectGroupUnit.getVersion(), change, context);
			AsciiTable at = new AsciiTable();
			at.addRule();
			at.addRow("Change", "Delta", "Type", "Name", "Project Version", "Baseline Version", "Suggested Version");
			at.addRule();
			at.addRow(change, Delta.CHANGED, "FEATURE", project.getName(), projectGroupUnit.getVersion(),
					baselineGroupUnit.getVersion(), suggestedVersion);
			ImpliedVersionChange threshold = computeThreshold(baselineGroupUnit.getVersion(),
					projectGroupUnit.getVersion());
			addDiffs(requirementDiffs, threshold, at);
			addDiffs(propertyDiffs, threshold, at);
			addDiffs(jarDiffs, threshold, at);
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
			StringBuilder message = new StringBuilder("Baseline problems found! ");
			message.append("Project version: ");
			message.append(projectGroupUnit.getVersion());
			message.append(", baseline version: ");
			message.append(baselineGroupUnit.getVersion());
			message.append(", suggested version: ");
			message.append(suggestedVersion);
			context.reportBaselineProblem(message.toString(), new org.osgi.framework.Version(suggestedVersion.toString()));
		}
		return true;
	}

	private ImpliedVersionChange computeThreshold(Version baselineVersion, Version projectVersion) {
		org.osgi.framework.Version bv = baseVersion(baselineVersion);
		org.osgi.framework.Version pv = baseVersion(projectVersion);
		if (pv.compareTo(bv) > 0) {
			if (pv.getMinor() > bv.getMinor()) {
				// the minor version was already incremented compared to the baseline so only
				// major changes are of interest
				return ImpliedVersionChange.MAJOR;
			}
			if (pv.getMicro() > bv.getMicro()) {
				// the micro version was incremented compared to the baseline so only
				// minor changes are of interest
				return ImpliedVersionChange.MINOR;
			}
		}
		return ImpliedVersionChange.UNCHANGED;
	}

	private boolean needsVersionBump(Version baselineVersion, Version projectVersion, ImpliedVersionChange change,
			BaselineContext context) {
		if (change == ImpliedVersionChange.UNCHANGED) {
			return false;
		}
		org.osgi.framework.Version bv = baseVersion(baselineVersion);
		org.osgi.framework.Version minIncrement;
		switch (change) {
		case MAJOR: {
			minIncrement = new org.osgi.framework.Version(bv.getMajor() + 1, 0, 0);
			break;
		}
		case MINOR: {
			minIncrement = new org.osgi.framework.Version(bv.getMajor(), bv.getMinor() + 1, 0);
			break;
		}
		case MICRO: {
			minIncrement = new org.osgi.framework.Version(bv.getMajor(), bv.getMinor(),
					bv.getMicro() + context.getMicroIncrement());
			break;
		}
		default:
			throw new IllegalArgumentException("Unexpected value: " + change);
		}
		return baseVersion(projectVersion).compareTo(minIncrement) < 0;
	}

	private void addDiffs(List<Diff> diffs, ImpliedVersionChange threshold, AsciiTable at) {
		for (Diff diff : diffs) {
			if (diff.change.ordinal() < threshold.ordinal()) {
				continue;
			}
			at.addRule();
			at.addRow(diff.change, diff.delta, diff.type, null, null, null,
					diff.message.replace(System.lineSeparator(), "<br>")).setTextAlignment(TextAlignment.LEFT);
		}
	}

	private Version getSuggestedVersion(Version version, ImpliedVersionChange change, BaselineContext context) {
		if (change == ImpliedVersionChange.UNCHANGED) {
			return version;
		}
		org.osgi.framework.Version v = baseVersion(version);
		if (change == ImpliedVersionChange.MAJOR) {
			return Version.createOSGi(v.getMajor() + 1, 0, 0);
		}
		if (change == ImpliedVersionChange.MINOR) {
			return Version.createOSGi(v.getMajor(), v.getMinor() + 1, 0);
		}
		return Version.createOSGi(v.getMajor(), v.getMinor(), v.getMicro() + context.getMicroIncrement());
	}

	private Collection<IRequiredCapability> getRequirements(IInstallableUnit unit) {
		return Stream.concat(unit.getRequirements().stream(), unit.getMetaRequirements().stream())
				.filter(IRequiredCapability.class::isInstance).map(IRequiredCapability.class::cast)
				.filter(req -> isSingleVersionRequirement(req)).toList();
	}

	private boolean isSingleVersionRequirement(IRequiredCapability cap) {
		VersionRange range = cap.getRange();
		return range.getMinimum().equals(range.getMaximum());
	}

	private List<Diff> computeRequirementDelta(Collection<IRequiredCapability> baselineRequirements,
			Collection<IRequiredCapability> projectRequirements) {
		Map<RequirementId, List<IRequiredCapability>> baselineMap = baselineRequirements.stream()
				.collect(Collectors.groupingBy(FeatureBaselineComparator::idOf));
		Map<RequirementId, List<IRequiredCapability>> projectMap = projectRequirements.stream()
				.collect(Collectors.groupingBy(FeatureBaselineComparator::idOf));
		Set<RequirementId> allIds = new HashSet<>();
		allIds.addAll(baselineMap.keySet());
		allIds.addAll(projectMap.keySet());

		List<Diff> list = new ArrayList<>();
		for (RequirementId id : allIds) {
			List<IRequiredCapability> baselineValue = baselineMap.get(id);
			if (baselineValue == null || baselineValue.isEmpty()) {
				list.add(new Diff(ImpliedVersionChange.MINOR, Type.REQUIREMENT, Delta.ADDED,
						String.format("Requirement %s:%s not present in baseline version", id.namespace, id.name)));
				continue;
			}
			List<IRequiredCapability> projectValue = projectMap.get(id);
			if (projectValue == null || projectValue.isEmpty()) {
				// Source features are deprecated and should only trigger a minor version change
				ImpliedVersionChange change = id.name.endsWith(SOURCE_SUFFIX) ? ImpliedVersionChange.MINOR : ImpliedVersionChange.MAJOR;
				list.add(new Diff(change, Type.REQUIREMENT, Delta.REMOVED,
						String.format("Requirement %s:%s is removed from baseline version", id.namespace, id.name)));
				continue;
			}
			if (baselineValue.size() == 1 && projectValue.size() == 1) {
				IRequiredCapability baselineCapability = baselineValue.get(0);
				IRequiredCapability projectCapability = projectValue.get(0);
				org.osgi.framework.Version baselineVersion = baseVersion(baselineCapability);
				org.osgi.framework.Version projectVersion = baseVersion(projectCapability);
				ImpliedVersionChange versionChange = computeVersionChange(baselineVersion, projectVersion);
				if (versionChange != ImpliedVersionChange.UNCHANGED) {
					list.add(new Diff(versionChange, Type.REQUIREMENT, Delta.CHANGED,
							String.format("Requirement %s:%s changed version from %s > %s", id.namespace, id.name,
									baselineVersion.toString(), projectVersion.toString())));
				}
			} else {
				// TODO how should this be handled?
			}
		}
		Collections.sort(list, Comparator.comparing(diff -> diff.change));
		return list;
	}

	private ImpliedVersionChange computeVersionChange(org.osgi.framework.Version baselineVersion,
			org.osgi.framework.Version projectVersion) {
		if (projectVersion.compareTo(baselineVersion) < 0) {
			return ImpliedVersionChange.MAJOR;
		}
		if (projectVersion.getMajor() > baselineVersion.getMajor()) {
			return ImpliedVersionChange.MAJOR;
		} else if (projectVersion.getMinor() > baselineVersion.getMinor()) {
			return ImpliedVersionChange.MINOR;
		} else if (projectVersion.getMicro() > baselineVersion.getMicro()) {
			return ImpliedVersionChange.MICRO;
		}
		return ImpliedVersionChange.UNCHANGED;
	}

	private org.osgi.framework.Version baseVersion(IRequiredCapability baselineCapability) {
		return baseVersion(baselineCapability.getRange().getMinimum());
	}

	private org.osgi.framework.Version baseVersion(Version p2Version) {
		org.osgi.framework.Version version = new org.osgi.framework.Version(p2Version.toString());
		return new org.osgi.framework.Version(version.getMajor(), version.getMinor(), version.getMicro());
	}

	private static RequirementId idOf(IRequiredCapability cap) {
		return new RequirementId(cap.getNamespace(), cap.getName());
	}

	private List<Diff> computeJarDelta(MavenProject project, BaselineContext context, IInstallableUnit baselineJarUnit)
			throws IOException {
		File file = project.getArtifact().getFile();
		try (FileInputStream reactor = new FileInputStream(file)) {
			List<String> ignores = new ArrayList<>();
			ignores.add("feature.xml"); // we compare the feature not on the file level but on the requirements level
										// here!
			ignores.addAll(context.getIgnores());
			ArtifactDelta artifactDelta = zipComparator.getDelta(getStream(baselineJarUnit, context),
					new ComparatorInputStream(reactor), new ComparisonData(ignores, false));
			if (artifactDelta == null) {
				return List.of();
			}
			// TODO can we get a list of individual items?
			return List.of(new Diff(ImpliedVersionChange.MICRO, Type.CONTENT, Delta.CHANGED,
					artifactDelta.getDetailedMessage()));
		}
	}

	private ComparatorInputStream getStream(IInstallableUnit unit, BaselineContext context) throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		repositoryManager.downloadArtifact(unit, context.getArtifactRepository(), outputStream);
		return new ComparatorInputStream(outputStream.toByteArray());
	}

	private static final List<Diff> computePropertyDiff(Map<String, String> base, Map<String, String> project) {
		List<Diff> list = new ArrayList<>();
		Set<String> names = new LinkedHashSet<>();
		names.addAll(base.keySet());
		names.addAll(project.keySet());
		for (String name : names) {
			String baselineValue = base.get(name);
			if (baselineValue == null) {
				list.add(new Diff(ImpliedVersionChange.MICRO, Type.PROPERTY, Delta.ADDED,
						String.format("Property %s not present in baseline version", name)));
				continue;
			}
			String projectValue = project.get(name);
			if (projectValue == null) {
				list.add(new Diff(ImpliedVersionChange.MICRO, Type.PROPERTY, Delta.REMOVED,
						String.format("Property %s present in baseline version only", name)));
				continue;
			}
			if (!propertyEquals(baselineValue, projectValue)) {
				int indexOfDifference = indexOfDifference(baselineValue, projectValue);
				list.add(new Diff(ImpliedVersionChange.MICRO, Type.PROPERTY, Delta.CHANGED, String.format(
						"Property %s is different, baseline = %s, project = %s (first index of difference is %s", //
						name, baselineValue, projectValue, indexOfDifference)));
			}
		}
		return list;
	}

	private static boolean propertyEquals(String baselineValue, String projectValue) {
		if (baselineValue.equals(projectValue)) {
			// perfect match
			return true;
		}
		String normalizdBl = baselineValue.replaceAll("\\s", " ").replaceAll("\\s+", " ").trim();
		String normalizdPr = projectValue.replaceAll("\\s", " ").replaceAll("\\s+", " ").trim();
		return normalizdBl.equals(normalizdPr);
	}

	private static int indexOfDifference(CharSequence s1, CharSequence s2) {
		int length = Math.min(s1.length(), s2.length());
		for (int i = 0; i < length; i++) {
			char c1 = s1.charAt(i);
			char c2 = s2.charAt(i);
			if (c1 != c2) {
				return i;
			}
		}
		return length;
	}

	private IQueryable<IInstallableUnit> getProjectUnits(MavenProject project) throws IOException {
		// first check if there is anything attached yet...
		for (Artifact artifact : project.getAttachedArtifacts()) {
			if (TychoConstants.CLASSIFIER_P2_METADATA.equals(artifact.getClassifier())) {
				File file = artifact.getFile();
				if (file != null && file.exists()) {
					return new CollectionResult<>(metadataIO.readXML(file));
				}
			}
		}
		// generate metadata, nothing is attached yet...
		File targetDir = new File(project.getBuild().getDirectory());
		ArtifactFacade projectDefaultArtifact = new ArtifactFacade(project.getArtifact());
		Map<String, IP2Artifact> generatedMetadata = p2generator.generateMetadata(List.of(projectDefaultArtifact),
				new PublisherOptions(), targetDir);
		return new CollectionResult<>(
				generatedMetadata.values().stream().flatMap(a -> a.getInstallableUnits().stream()).toList());

	}

	private IInstallableUnit getJarUnit(ArtifactKey key, IInstallableUnit baselineUnit,
			IQueryable<IInstallableUnit> metadataRepository) {
		IQueryResult<IInstallableUnit> result = metadataRepository
				.query(QueryUtil.createIUQuery(key.getId() + JAR_SUFFIX, baselineUnit.getVersion()), null);
		if (result.isEmpty()) {
			return null;
		}
		return result.iterator().next();
	}

	private IInstallableUnit getBaselineUnit(ArtifactKey key, IQueryable<IInstallableUnit> metadataRepository) {
		org.osgi.framework.Version artifactVersion = org.osgi.framework.Version.parseVersion(key.getVersion());
		Version maxVersion = Version.createOSGi(artifactVersion.getMajor(), artifactVersion.getMinor(),
				artifactVersion.getMicro() + 1);
		IQueryResult<IInstallableUnit> result = metadataRepository
				.query(QueryUtil.createLatestQuery(QueryUtil.createIUQuery(key.getId() + GROUP_SUFFIX,
						new VersionRange(Version.emptyVersion, true, maxVersion, false))), null);
		if (result.isEmpty()) {
			return null;
		}
		return result.iterator().next();
	}

	private static final record Diff(ImpliedVersionChange change, Type type, Delta delta, String message) {

	}

	private static final record RequirementId(String namespace, String name) {

	}

	private enum Type {
		CONTENT, PROPERTY, REQUIREMENT;
	}

	private enum Delta {
		ADDED, REMOVED, CHANGED;
	}

	private enum ImpliedVersionChange {
		UNCHANGED, MICRO, MINOR, MAJOR;
	}
}
