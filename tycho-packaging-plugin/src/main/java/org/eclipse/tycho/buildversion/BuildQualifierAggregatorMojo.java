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

import java.io.File;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.OptionalLong;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.TargetPlatformService;
import org.eclipse.tycho.TychoConstants;
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

	/**
	 * Controls if when a aggregation happens the mojo should use the embedded
	 * timestamps in the jar manifest or finally fall back to the last modified
	 * timestamps of the jar entries itself if it can't parse the qualifier from the
	 * file name.
	 */
	@Parameter(defaultValue = "true")
	private boolean useArtifactTimestamps = true;

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
				Date timestamp = getTimestamp(artifact);
				if (timestamp != null && latestTimestamp[0].compareTo(timestamp) < 0) {
					latestTimestamp[0] = timestamp;
				}
            }

        });

        return latestTimestamp[0];
    }

	private Date getTimestamp(ArtifactDescriptor artifact) {
		ReactorProject otherProject = artifact.getMavenProject();
		if (otherProject != null) {
			Object contextValue = otherProject.getContextValue(TychoConstants.BUILD_TIMESTAMP);
			if (contextValue instanceof Date date) {
				return date;
			}
		}
		String otherVersion = (otherProject != null) ? otherProject.getExpandedVersion()
				: artifact.getKey().getVersion();
		Version v = Version.parseVersion(otherVersion);
		String otherQualifier = v.getQualifier();
		if (otherQualifier != null) {
			Date parseQualifier = parseQualifier(otherQualifier);
			if (parseQualifier != null) {
				return parseQualifier;
			}
		}
		if (useArtifactTimestamps) {
			try {
				File file = artifact.fetchArtifact().get();
				try (JarFile jarFile = new JarFile(file)) {
					Manifest manifest = jarFile.getManifest();
					if (manifest != null) {
						Attributes attributes = manifest.getMainAttributes();
						String tychoTs = attributes.getValue(TychoConstants.HEADER_TYCHO_BUILD_TIMESTAMP);
						if (tychoTs != null) {
							return new Date(Long.parseLong(tychoTs));
						}
						String bndTs = attributes.getValue(TychoConstants.HEADER_BND_LAST_MODIFIED);
						if (bndTs != null) {
							return new Date(Long.parseLong(bndTs));
						}
					}
					OptionalLong max = jarFile.stream().mapToLong(JarEntry::getTime).filter(l -> l > 0).max();
					if (max.isPresent()) {
						return new Date(max.getAsLong());
					}
				}
			} catch (Exception e) {
				// can't use it then...
			}
		}
		return null;
	}

	private Date parseQualifier(String qualifier) {
		Date timestamp = parseQualifier(qualifier, format);
		if (timestamp != null) {
			return timestamp;
		}
		return timestampFinder.findInString(qualifier);
	}

	private Date parseQualifier(String qualifier, SimpleDateFormat format) {
		ParsePosition pos = new ParsePosition(0);
		Date timestamp = format.parse(qualifier, pos);
		if (timestamp != null && pos.getIndex() == qualifier.length()) {
			return timestamp;
		}
		return null;
	}
}
