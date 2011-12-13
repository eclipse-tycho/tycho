/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.resolver;

import java.io.File;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.tycho.core.TargetEnvironment;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.TychoConstants;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.UnknownEnvironmentException;
import org.eclipse.tycho.core.utils.ExecutionEnvironmentUtils;
import org.eclipse.tycho.core.utils.PlatformPropertiesUtils;

@Component(role = DefaultTargetPlatformConfigurationReader.class)
public class DefaultTargetPlatformConfigurationReader {
    @Requirement
    private Logger logger;

    @Requirement
    private RepositorySystem repositorySystem;

    @Requirement
    private Map<String, TychoProject> projectTypes;

    public TargetPlatformConfiguration getTargetPlatformConfiguration(MavenSession session, MavenProject project) {
        TargetPlatformConfiguration result = new TargetPlatformConfiguration();

        // Use org.eclipse.tycho:target-platform-configuration/configuration/environment, if provided
        Plugin plugin = project.getPlugin("org.eclipse.tycho:target-platform-configuration");

        if (plugin != null) {
            Xpp3Dom configuration = (Xpp3Dom) plugin.getConfiguration();
            if (configuration != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("target-platform-configuration for " + project.toString() + ":\n"
                            + configuration.toString());
                }

                addTargetEnvironments(result, project, configuration);

                addExtraRequirements(result, configuration);

                setOptionalResolutionAction(result, configuration);

                setTargetPlatformResolver(result, configuration);

                setTarget(result, session, project, configuration);

                setPomDependencies(result, configuration);

                setAllowConflictingDependencies(result, configuration);

                setDisableP2Mirrors(result, configuration);

                setExecutionEnvironment(result, configuration);
            }
        }

        if (result.getEnvironments().isEmpty()) {
            TychoProject projectType = projectTypes.get(project.getPackaging());
            if (projectType != null) {
                TargetEnvironment env = projectType.getImplicitTargetEnvironment(project);
                if (env != null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Implicit target environment for " + project.toString() + ": " + env.toString());
                    }

                    result.addEnvironment(env);
                }
            }
        }

        if (result.getEnvironments().isEmpty()) {
            // applying defaults
            logger.warn("No explicit target runtime environment configuration. Build is platform dependent.");

            // Otherwise, use project or execution properties, if provided
            Properties properties = (Properties) project.getContextValue(TychoConstants.CTX_MERGED_PROPERTIES);

            // Otherwise, use current system os/ws/nl/arch
            String os = PlatformPropertiesUtils.getOS(properties);
            String ws = PlatformPropertiesUtils.getWS(properties);
            String arch = PlatformPropertiesUtils.getArch(properties);

            result.addEnvironment(new TargetEnvironment(os, ws, arch, null /* nl */));

            result.setImplicitTargetEnvironment(true);
        } else {
            result.setImplicitTargetEnvironment(false);
        }

        if (result.getTarget() != null && result.getOptionalResolutionAction() != null) {
            throw new RuntimeException(
                    "<optationDependencies> and <target> target platform configuration parameters cannot be used simultaneusly "
                            + project.toString());
        }

        return result;
    }

    private void setOptionalResolutionAction(TargetPlatformConfiguration result, Xpp3Dom configuration) {
        Xpp3Dom optionalDependenciesDom = configuration.getChild("optionalDependencies");
        if (optionalDependenciesDom == null) {
            return;
        }

        String optionalDependencies = optionalDependenciesDom.getValue();

        if (!TargetPlatformConfiguration.OPTIONAL_RESOLUTION_REQUIRE.equals(optionalDependencies)
                && !TargetPlatformConfiguration.OPTIONAL_RESOLUTION_IGNORE.equals(optionalDependencies)) {
            throw new RuntimeException(
                    "Illegal value of <optationDependencies> target platform configuration parameter "
                            + optionalDependencies);
        }

        result.setOptionalResolutionAction(optionalDependencies);
    }
    
    private void setExecutionEnvironment(TargetPlatformConfiguration result, Xpp3Dom configuration) {
        Xpp3Dom eeDom = configuration.getChild("executionEnvironment");
        if (eeDom == null) {
            return;
        }
        String ee = eeDom.getValue().trim();
        if (!"".equals(ee)) {
            try {
                String profile = ee.startsWith("?") ? ee.substring(1) : ee;
                ExecutionEnvironmentUtils.getExecutionEnvironment(profile);
            } catch (UnknownEnvironmentException e) {
                throw new RuntimeException("Invalid execution environment profile name " + ee);
            }

            result.setExecutionEnvironment(ee);
        }
    }

    private void addExtraRequirements(TargetPlatformConfiguration result, Xpp3Dom configuration) {
        Xpp3Dom requirementsDom = configuration.getChild("extraRequirements");
        if (requirementsDom == null) {
            return;
        }
        for (Xpp3Dom requirementDom : requirementsDom.getChildren("requirement")) {
            result.addExtraRequirement(newRequirement(requirementDom));
        }
    }

    private Dependency newRequirement(Xpp3Dom requirementDom) {
        Dependency d = new Dependency();
        d.setType(requirementDom.getChild("type").getValue());
        d.setArtifactId(requirementDom.getChild("id").getValue());
        d.setVersion(requirementDom.getChild("versionRange").getValue());
        return d;
    }

    private void setDisableP2Mirrors(TargetPlatformConfiguration result, Xpp3Dom configuration) {
        Xpp3Dom disableP2mirrorsDom = configuration.getChild("disableP2Mirrors");
        if (disableP2mirrorsDom == null) {
            return;
        }

        result.setDisableP2Mirrors(Boolean.parseBoolean(disableP2mirrorsDom.getValue()));
    }

    private void setAllowConflictingDependencies(TargetPlatformConfiguration result, Xpp3Dom configuration) {
        Xpp3Dom allowConflictingDependenciesDom = configuration.getChild("allowConflictingDependencies");
        if (allowConflictingDependenciesDom == null) {
            return;
        }

        result.setAllowConflictingDependencies(Boolean.parseBoolean(allowConflictingDependenciesDom.getValue()));
    }

    private void addTargetEnvironments(TargetPlatformConfiguration result, MavenProject project, Xpp3Dom configuration) {
        TargetEnvironment deprecatedTargetEnvironmentSpec = getDeprecatedTargetEnvironment(configuration);
        if (deprecatedTargetEnvironmentSpec != null) {
            result.addEnvironment(deprecatedTargetEnvironmentSpec);
        }

        Xpp3Dom environmentsDom = configuration.getChild("environments");
        if (environmentsDom != null) {
            if (deprecatedTargetEnvironmentSpec != null) {
                String message = "Deprecated target-platform-configuration <environment> element must not be combined with new <environments> element; check the (inherited) configuration of "
                        + project.getId();
                throw new RuntimeException(message);
            }
            for (Xpp3Dom environmentDom : environmentsDom.getChildren("environment")) {
                result.addEnvironment(newTargetEnvironment(environmentDom));
            }
        }
    }

    protected TargetEnvironment getDeprecatedTargetEnvironment(Xpp3Dom configuration) {
        Xpp3Dom environmentDom = configuration.getChild("environment");
        if (environmentDom != null) {
            logger.warn("target-platform-configuration <environment> element is deprecated; use <environments> instead");
            return newTargetEnvironment(environmentDom);
        }
        return null;
    }

    private void setPomDependencies(TargetPlatformConfiguration result, Xpp3Dom configuration) {
        Xpp3Dom pomDependenciesDom = configuration.getChild("pomDependencies");
        if (pomDependenciesDom == null) {
            return;
        }

        result.setPomDependencies(pomDependenciesDom.getValue());
    }

    private void setTarget(TargetPlatformConfiguration result, MavenSession session, MavenProject project,
            Xpp3Dom configuration) {
        Xpp3Dom targetDom = configuration.getChild("target");
        if (targetDom == null) {
            return;
        }

        Xpp3Dom artifactDom = targetDom.getChild("artifact");
        if (artifactDom == null) {
            return;
        }

        Xpp3Dom groupIdDom = artifactDom.getChild("groupId");
        Xpp3Dom artifactIdDom = artifactDom.getChild("artifactId");
        Xpp3Dom versionDom = artifactDom.getChild("version");
        if (groupIdDom == null || artifactIdDom == null || versionDom == null) {
            return;
        }
        Xpp3Dom classifierDom = artifactDom.getChild("classifier");

        String groupId = groupIdDom.getValue();
        String artifactId = artifactIdDom.getValue();
        String version = versionDom.getValue();
        String classifier = classifierDom != null ? classifierDom.getValue() : null;

        File targetFile = null;
        for (MavenProject otherProject : session.getProjects()) {
            if (groupId.equals(otherProject.getGroupId()) && artifactId.equals(otherProject.getArtifactId())
                    && version.equals(otherProject.getVersion())) {
                targetFile = new File(otherProject.getBasedir(), classifier + ".target");
                break;
            }
        }

        if (targetFile == null) {
            Artifact artifact = repositorySystem.createArtifactWithClassifier(groupId, artifactId, version, "target",
                    classifier);
            ArtifactResolutionRequest request = new ArtifactResolutionRequest();
            request.setArtifact(artifact);
            request.setLocalRepository(session.getLocalRepository());
            request.setRemoteRepositories(project.getRemoteArtifactRepositories());
            repositorySystem.resolve(request);

            if (!artifact.isResolved()) {
                throw new RuntimeException("Could not resolve target platform specification artifact " + artifact);
            }

            targetFile = artifact.getFile();
        }

        result.setTarget(targetFile);
    }

    private void setTargetPlatformResolver(TargetPlatformConfiguration result, Xpp3Dom configuration) {
        Xpp3Dom resolverDom = configuration.getChild("resolver");

        if (resolverDom == null) {
            return;
        }

        result.setResolver(resolverDom.getValue());
    }

    private TargetEnvironment newTargetEnvironment(Xpp3Dom environmentDom) {
        Xpp3Dom osDom = environmentDom.getChild("os");
        if (osDom == null) {
            return null;
        }

        Xpp3Dom wsDom = environmentDom.getChild("ws");
        if (wsDom == null) {
            return null;
        }

        Xpp3Dom archDom = environmentDom.getChild("arch");
        if (archDom == null) {
            return null;
        }

        return new TargetEnvironment(osDom.getValue(), wsDom.getValue(), archDom.getValue(), null /* nl */);
    }

}
