/*******************************************************************************
 * Copyright (c) 2012 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
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
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.artifactcomparator.ArtifactComparator;
import org.eclipse.tycho.artifactcomparator.ArtifactDelta;
import org.eclipse.tycho.core.resolver.shared.MavenRepositoryLocation;
import org.eclipse.tycho.p2.metadata.IP2Artifact;
import org.eclipse.tycho.p2.repository.RepositoryLayoutHelper;
import org.eclipse.tycho.p2.tools.baseline.facade.BaselineService;
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
    }

    @Requirement
    private Logger log;

    @Requirement(hint = "zip")
    private ArtifactComparator zipComparator;

    @Requirement
    private EquinoxServiceFactory equinox;

    public Map<String, IP2Artifact> validateAndReplace(MavenProject project, MojoExecution execution,
            Map<String, IP2Artifact> reactorMetadata, List<Repository> baselineRepositories, BaselineMode baselineMode,
            BaselineReplace baselineReplace) throws IOException, MojoExecutionException {

        Map<String, IP2Artifact> result = reactorMetadata;

        if (baselineMode != disable && baselineRepositories != null && !baselineRepositories.isEmpty()) {
            List<MavenRepositoryLocation> _repositories = new ArrayList<>();
            for (Repository repository : baselineRepositories) {
                if (repository.getUrl() != null) {
                    _repositories.add(new MavenRepositoryLocation(repository.getId(), repository.getUrl()));
                }
            }

            File baselineBasedir = new File(project.getBuild().getDirectory(), "baseline");

            BaselineService baselineService = getService(BaselineService.class);

            Map<String, IP2Artifact> baselineMetadata = baselineService.getProjectBaseline(_repositories,
                    reactorMetadata, baselineBasedir);

            if (baselineMetadata != null) {
                CompoundArtifactDelta delta = getDelta(baselineService, baselineMetadata, reactorMetadata, execution);
                if (delta != null) {
                    if (System.getProperties().containsKey("tycho.debug.artifactcomparator")) {
                        File logdir = new File(project.getBuild().getDirectory(), "artifactcomparison");
                        log.info("Artifact comparison detailed log directory " + logdir.getAbsolutePath());
                        for (Map.Entry<String, ArtifactDelta> classifier : delta.getMembers().entrySet()) {
                            if (classifier.getValue() instanceof CompoundArtifactDelta) {
                                ((CompoundArtifactDelta) classifier.getValue())
                                        .writeDetails(new File(logdir, classifier.getKey()));
                            }
                        }
                    }
                    if (baselineMode == fail || (baselineMode == failCommon && !isMissingOnlyDelta(delta))) {
                        throw new MojoExecutionException(delta.getDetailedMessage());
                    } else {
                        log.warn(project.toString() + ": " + delta.getDetailedMessage());
                    }
                }

                if (baselineReplace != none) {
                    result = new LinkedHashMap<>();

                    // replace reactor artifacts with baseline
                    ArrayList<String> replaced = new ArrayList<>();
                    for (Map.Entry<String, IP2Artifact> artifact : baselineMetadata.entrySet()) {
                        File baseLineFile = artifact.getValue().getLocation();
                        String classifier = artifact.getKey();
                        File reactorFile = reactorMetadata.get(classifier).getLocation();
                        if (baseLineFile.isFile() && baseLineFile.length() == 0L) {
                            // workaround for possibly corrupted download - bug 484003
                            log.error("baseline file " + baseLineFile.getAbsolutePath() + " is empty. Will not replace "
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
                    ArrayList<String> removed = new ArrayList<>();
                    ArrayList<String> inconsistent = new ArrayList<>();
                    for (Map.Entry<String, IP2Artifact> entry : reactorMetadata.entrySet()) {
                        String classifier = entry.getKey();
                        IP2Artifact artifact = entry.getValue();
                        if (classifier == null || artifact == null) {
                            continue;
                        }
                        if (baselineReplace == all && !baselineMetadata.containsKey(classifier)) {
                            List<Artifact> attachedArtifacts = project.getAttachedArtifacts();
                            ListIterator<Artifact> iterator = attachedArtifacts.listIterator();
                            while (iterator.hasNext()) {
                                if (classifier.equals(iterator.next().getClassifier())) {
                                    iterator.remove();
                                    break;
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
                log.info("No baseline version " + project);
            }
        }
        return result;
    }

    private boolean isMissingOnlyDelta(ArtifactDelta delta) {
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
            Map<String, IP2Artifact> generatedMetadata, MojoExecution execution) throws IOException {

        Map<String, ArtifactDelta> result = new LinkedHashMap<>();

        // baseline never includes more artifacts
        for (Entry<String, IP2Artifact> classifierEntry : generatedMetadata.entrySet()) {
            // the following types of artifacts are produced/consumed by tycho as of 0.16
            // - bundle jar and jar.pack.gz artifacts
            // - feature jar artifacts
            // - feature rootfiles zip artifacts
            String classifier = classifierEntry.getKey();

            if (RepositoryLayoutHelper.PACK200_CLASSIFIER.equals(classifier)) {
                // in the unlikely event that reactor and baseline pack200 files have different contents
                // but bundle jar files are the same, the build will silently use baseline pack200 file
                continue;
            }

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
                        reactorArtifact.getLocation(), execution);
                if (delta != null) {
                    result.put(deltaKey, delta);
                }
            } catch (IOException e) {
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

    private <T> T getService(Class<T> type) {
        T service = equinox.getService(type);
        if (service == null) {
            throw new IllegalStateException("Could not acquire service " + type);
        }
        return service;
    }

}
