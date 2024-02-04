/*******************************************************************************
 * Copyright (c) 2012, 2022 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Christoph Läubrich - BaselineValidator.validateAndReplace() fails with UnsupportedOperationException under Maven 3.8.2
 *******************************************************************************/
package org.eclipse.tycho.plugins.p2;

import static org.eclipse.tycho.plugins.p2.BaselineMode.disable;
import static org.eclipse.tycho.plugins.p2.BaselineMode.fail;
import static org.eclipse.tycho.plugins.p2.BaselineMode.failCommon;
import static org.eclipse.tycho.plugins.p2.BaselineReplace.all;
import static org.eclipse.tycho.plugins.p2.BaselineReplace.none;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.tycho.MavenRepositoryLocation;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.artifactcomparator.ArtifactComparator;
import org.eclipse.tycho.artifactcomparator.ArtifactComparator.ComparisonData;
import org.eclipse.tycho.artifactcomparator.ArtifactDelta;
import org.eclipse.tycho.core.osgitools.BaselineService;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.p2.metadata.IP2Artifact;
import org.eclipse.tycho.zipcomparator.internal.CompoundArtifactDelta;
import org.eclipse.tycho.zipcomparator.internal.SimpleArtifactDelta;

@Component(role = BaselineValidator.class)
public class BaselineValidator {

    private static class MissingArtifactDelta implements ArtifactDelta {
        @Override
        public String getMessage() {
            return "not present in baseline";
        }

        @Override
        public String getDetailedMessage() {
            return getMessage();
        }

        @Override
        public void writeDetails(File destination) throws IOException {

        }
    }

    @Requirement
    private Logger log;

    @Requirement(hint = "zip")
    private ArtifactComparator zipComparator;

    @Requirement
    BaselineService baselineService;

    public Map<String, IP2Artifact> validateAndReplace(MavenProject project, ComparisonData data,
            Map<String, IP2Artifact> reactorMetadata, List<Repository> baselineRepositories, BaselineMode baselineMode,
            BaselineReplace baselineReplace) throws IOException, MojoExecutionException {

        Map<String, IP2Artifact> result = reactorMetadata;

        if (baselineMode != disable && baselineRepositories != null && !baselineRepositories.isEmpty()) {
            List<MavenRepositoryLocation> repositories = new ArrayList<>();
            for (Repository repository : baselineRepositories) {
                if (repository.getUrl() != null) {
                    repositories.add(new MavenRepositoryLocation(repository.getId(), repository.getUrl()));
                }
            }

            File baselineBasedir = new File(project.getBuild().getDirectory(), "baseline");

            Map<String, IP2Artifact> baselineMetadata = baselineService.getProjectBaseline(repositories,
                    reactorMetadata, baselineBasedir);

            if (baselineMetadata != null) {
                CompoundArtifactDelta delta = getDelta(baselineService, baselineMetadata, reactorMetadata, data);
                if (delta != null) {
                    if (data.writeDelta()) {
                        File logdir = new File(project.getBuild().getDirectory(), "artifactcomparison");
                        log.info("Artifact comparison detailed log directory " + logdir.getAbsolutePath());
                        for (Map.Entry<String, ArtifactDelta> classifier : delta.getMembers().entrySet()) {
                            classifier.getValue().writeDetails(new File(logdir, classifier.getKey()));
                        }
                    }
                    if (shouldFail(baselineMode, delta)) {
                        throw new MojoExecutionException(delta.getDetailedMessage());
                    } else if (shouldWarn(baselineMode, delta)) {
                        log.warn(project.toString() + ": " + delta.getDetailedMessage());
                    }
                }

                if (baselineReplace != none) {
                    result = new LinkedHashMap<>();

                    // replace reactor artifacts with baseline
                    List<String> replaced = new ArrayList<>();
                    for (Map.Entry<String, IP2Artifact> artifact : baselineMetadata.entrySet()) {
                        File baseLineFile = artifact.getValue().getLocation();
                        String classifier = artifact.getKey();
                        File reactorFile = reactorMetadata.get(classifier).getLocation();
                        if (baseLineFile.isFile() && baseLineFile.length() == 0L) {
                            // workaround for possibly corrupted download - bug 484003
                            log.error("Baseline file " + baseLineFile.getAbsolutePath() + " is empty. Will not replace "
                                    + reactorFile);
                        } else {
                            FileUtils.copyFile(baseLineFile, reactorFile);
                            result.put(classifier, artifact.getValue());
                            if (classifier != null) {
                                replaced.add(classifier);
                            }
                        }
                    }

                    // un-attach and delete artifacts present in reactor but not in baseline
                    List<String> removed = new ArrayList<>();
                    List<String> inconsistent = new ArrayList<>();
                    for (Map.Entry<String, IP2Artifact> entry : reactorMetadata.entrySet()) {
                        String classifier = entry.getKey();
                        IP2Artifact artifact = entry.getValue();
                        if (classifier == null || artifact == null) {
                            continue;
                        }
                        if (baselineReplace == all && !baselineMetadata.containsKey(classifier)) {
                            List<Artifact> attachedArtifacts = project.getAttachedArtifacts();
                            try {
                                attachedArtifacts.removeIf(a -> classifier.equals(a.getClassifier()));
                            } catch (UnsupportedOperationException e) {
                                List<Artifact> list = attachedArtifacts.stream()
                                        .filter(a -> !classifier.equals(a.getClassifier()))
                                        .collect(Collectors.toCollection(ArrayList::new));
                                try {
                                    MethodUtils.invokeMethod(project, true, "setAttachedArtifacts", list);
                                } catch (ReflectiveOperationException ignored) {
                                    log.warn("The attached artifact " + classifier
                                            + " is not present in the baseline, but could not be removed");
                                }
                            }
                            artifact.getLocation().delete();
                            removed.add(classifier);
                        } else {
                            inconsistent.add(classifier);
                            result.put(classifier, artifact);
                        }
                    }

                    // Reactor build can have more or less artifacts than baseline 
                    // baselineReplace==all guarantees consistency of build artifacts with baseline repository
                    // baselineReplace==none build results are self-consistent, but maybe inconsistent with baseline
                    // baselineReplace==common build artifacts are inconsistent
                    DefaultReactorProject.adapt(project)
                            .setContextValue(TychoConstants.KEY_BASELINE_REPLACE_ARTIFACT_MAIN, true);
                    if (log.isInfoEnabled()) {
                        StringBuilder msg = new StringBuilder();
                        msg.append(project.toString());
                        msg.append("\n    The main artifact has been replaced with the baseline version.\n");
                        if (!replaced.isEmpty()) {
                            msg.append(
                                    "    The following attached artifacts have been replaced with the baseline version: ");
                            msg.append(replaced.toString());
                            msg.append("\n");
                        }
                        if (!removed.isEmpty()) {
                            msg.append(
                                    "    The following attached artifacts are not present in the baseline and have been removed: ");
                            msg.append(removed.toString());
                            msg.append("\n");
                        }
                        log.info(msg.toString());
                    }
                }
            } else {
                log.info("No baseline version " + project.getId());
            }
        }
        return result;
    }

    private static boolean shouldFail(BaselineMode baselineMode, CompoundArtifactDelta delta) {
        return baselineMode == fail || (baselineMode == failCommon && !isMissingOnlyDelta(delta));
    }

    private static boolean shouldWarn(BaselineMode baselineMode, CompoundArtifactDelta delta) {
        if (baselineMode == BaselineMode.warnCommon) {
            return !isMissingOnlyDelta(delta);
        }
        return true;
    }

    private static boolean isMissingOnlyDelta(ArtifactDelta delta) {
        if (delta instanceof MissingArtifactDelta) {
            return true;
        }
        if (delta instanceof CompoundArtifactDelta) {
            for (ArtifactDelta member : ((CompoundArtifactDelta) delta).getMembers().values()) {
                if (!(member instanceof MissingArtifactDelta)) {
                    return false;
                }
            }
        }
        return true;
    }

    private CompoundArtifactDelta getDelta(BaselineService baselineService, Map<String, IP2Artifact> baselineMetadata,
            Map<String, IP2Artifact> generatedMetadata, ComparisonData data) throws IOException {

        Map<String, ArtifactDelta> result = new LinkedHashMap<>();

        // baseline never includes more artifacts
        for (Entry<String, IP2Artifact> classifierEntry : generatedMetadata.entrySet()) {
            // the following types of artifacts are produced/consumed by tycho as of 0.16
            // - bundle jar artifacts
            // - feature jar artifacts
            // - feature rootfiles zip artifacts
            String classifier = classifierEntry.getKey();

            String deltaKey = classifier != null ? "classifier-" + classifier : "no-classifier";

            IP2Artifact baselineArtifact = baselineMetadata.get(classifier);
            IP2Artifact reactorArtifact = classifierEntry.getValue();

            if (baselineArtifact == null) {
                result.put(deltaKey, new MissingArtifactDelta());
                continue;
            }

            if (!baselineService.isMetadataEqual(baselineArtifact, reactorArtifact)) {
                result.put(deltaKey, new SimpleArtifactDelta("p2 metadata different"));
                continue;
            }

            try {
                ArtifactDelta delta = zipComparator.getDelta(baselineArtifact.getLocation(),
                        reactorArtifact.getLocation(), data);
                if (delta != null) {
                    result.put(deltaKey, delta);
                }
            } catch (IOException e) {
                log.warn("Elementwise comparison of zip-file failed", e);
                // do byte-to-byte comparison if jar comparison fails for whatever reason
                if (!FileUtils.contentEquals(baselineArtifact.getLocation(), reactorArtifact.getLocation())) {
                    result.put(deltaKey, new SimpleArtifactDelta("different"));
                }
            }
        }

        return !result.isEmpty()
                ? new CompoundArtifactDelta("baseline and build artifacts have same version but different contents",
                        result)
                : null;
    }

}
