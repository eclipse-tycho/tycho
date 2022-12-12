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
package org.eclipse.tycho.buildversion;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TargetPlatformService;
import org.eclipse.tycho.core.ArtifactDependencyVisitor;
import org.eclipse.tycho.core.FeatureDescription;
import org.eclipse.tycho.core.PluginDescription;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.osgi.framework.Version;

/**
 * <p>
 * This mojo calculates build timestamp as the latest timestamp of the project itself and timestamps
 * of bundles and features directly included in the project. This is meant to work with custom
 * timestamp providers and generate build qualifier based on build contents, i.e. the source code,
 * and not the time the build was started; rebuilding the same source code will result in the same
 * version qualifier.
 * </p>
 * <p>
 * Timestamp of included bundles and features is determined by parsing their respective version
 * qualifiers. Qualifiers that cannot be parsed are silently ignored, which can result in old
 * version qualifier used even when aggregator project contents actually changed. In this case
 * aggregator project timestamp will have to be increased manually, using artificial SCM commit for
 * example.
 * </p>
 * <p>
 * Qualifier aggregation is enabled only for projects with custom timestamp provider, i.e.
 * &lt;timestampProvider&gt; is set in pom.xml to a value other than "default". The default build
 * timestamp provider uses build start time as build timestamp, which should be newer or equal than
 * timestamp of any included bundle/feature project, which makes qualifier aggregation redundant.
 * </p>
 */
@Mojo(name = "build-qualifier-aggregator", defaultPhase = LifecyclePhase.VALIDATE, threadSafe = true)
public class BuildQualifierAggregatorMojo extends BuildQualifierMojo {

    private final TimestampFinder timestampFinder = new TimestampFinder();

	@Component
	private TargetPlatformService platformService;

    @Override
    protected Date getBuildTimestamp() throws MojoExecutionException {
        Date timestamp = super.getBuildTimestamp();

        if (timestampProvider == null) {
            // default timestamp is essentially this build start time
            // no included bundle/feature can have more recent timestamp
            return timestamp;
        }

        final Date[] latestTimestamp = new Date[] { timestamp };

        TychoProject projectType = projectTypes.get(project.getPackaging());
        if (projectType == null) {
            throw new IllegalStateException("Unknown or unsupported packaging type " + packaging);
        }

        final ReactorProject thisProject = DefaultReactorProject.adapt(project);
		// TODO we need to trigger TP loading now, probably we also should better use
		// the target platform instead of walker of the project type?
		platformService.getTargetPlatform(thisProject);

        projectType.getDependencyWalker(thisProject).walk(new ArtifactDependencyVisitor() {
            @Override
            public boolean visitFeature(FeatureDescription feature) {
                if (feature.getFeatureRef() == null || thisProject.equals(feature.getMavenProject())) {
                    // 'this' feature
                    return true; // visit immediately included features
                }
                visitArtifact(feature);
                return false; // do not visit indirectly included features/bundles
            }

            @Override
            public void visitPlugin(PluginDescription plugin) {
                if (plugin.getPluginRef() == null || thisProject.equals(plugin.getMavenProject())) {
                    // 'this' bundle
                    return;
                }
                visitArtifact(plugin);
            }

            private void visitArtifact(ArtifactDescriptor artifact) {
                ReactorProject otherProject = artifact.getMavenProject();
                String otherVersion = (otherProject != null) ? otherProject.getExpandedVersion()
                        : artifact.getKey().getVersion();
                Version v = Version.parseVersion(otherVersion);
                String otherQualifier = v.getQualifier();
                if (otherQualifier != null) {
                    Date timestamp = parseQualifier(otherQualifier);
                    if (timestamp != null) {
                        if (latestTimestamp[0].compareTo(timestamp) < 0) {
                            if (getLog().isDebugEnabled()) {
                                getLog().debug("Found '" + format.format(timestamp) + "' from qualifier '"
                                        + otherQualifier + "' for artifact " + artifact);
                            }
                            latestTimestamp[0] = timestamp;
                        }
                    } else {
                        getLog().debug("Could not parse qualifier timestamp " + otherQualifier);
                    }
                }
            }

            private Date parseQualifier(String qualifier) {
                Date timestamp = parseQualifier(qualifier, format);
                if (timestamp != null) {
                    return timestamp;
                }
                return discoverTimestamp(qualifier);
            }

            private Date parseQualifier(String qualifier, SimpleDateFormat format) {
                ParsePosition pos = new ParsePosition(0);
                Date timestamp = format.parse(qualifier, pos);
                if (timestamp != null && pos.getIndex() == qualifier.length()) {
                    return timestamp;
                }
                return null;
            }

            private Date discoverTimestamp(String qualifier) {
                return timestampFinder.findInString(qualifier);
            }
        });

        return latestTimestamp[0];
    }
}
