/*******************************************************************************
 * Copyright (c) 2008, 2022 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *    Christoph Läubrich -  [Bug 461284] - Improve discovery and attach of .target files in eclipse-target-definition
 *                          [Bug 567098] - pomDependencies=consider should wrap non-osgi jars
 *                          [Issue 792]  - Support exclusion of certain dependencies from pom dependency consideration 
 *******************************************************************************/
package org.eclipse.tycho.core.resolver;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.tycho.DefaultArtifactKey;
import org.eclipse.tycho.PlatformPropertiesUtils;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.TychoConstants;
import org.eclipse.tycho.artifacts.configuration.TargetPlatformFilterConfigurationReader;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.TargetPlatformConfiguration.BREEHeaderSelectionPolicy;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.resolver.shared.IncludeSourceMode;
import org.eclipse.tycho.core.resolver.shared.OptionalResolutionAction;
import org.eclipse.tycho.core.resolver.shared.PomDependencies;
import org.eclipse.tycho.core.shared.BuildFailureException;

@Component(role = DefaultTargetPlatformConfigurationReader.class)
public class DefaultTargetPlatformConfigurationReader {
    public static final String TARGET_DEFINITION_INCLUDE_SOURCE = "targetDefinitionIncludeSource";
    public static final String DEPENDENCY_RESOLUTION = "dependency-resolution";
    public static final String OPTIONAL_DEPENDENCIES = "optionalDependencies";
    public static final String FILTERS = "filters";
    public static final String RESOLVE_WITH_EXECUTION_ENVIRONMENT_CONSTRAINTS = "resolveWithExecutionEnvironmentConstraints";
    public static final String BREE_HEADER_SELECTION_POLICY = "breeHeaderSelectionPolicy";
    public static final String EXECUTION_ENVIRONMENT_DEFAULT = "executionEnvironmentDefault";
    public static final String EXECUTION_ENVIRONMENT = "executionEnvironment";
    public static final String ALLOW_CONFLICTING_DEPENDENCIES = "allowConflictingDependencies";
    public static final String POM_DEPENDENCIES = "pomDependencies";
    public static final String TARGET = "target";
    public static final String RESOLVER = "resolver";
    public static final String ENVIRONMENTS = "environments";
    public static final String EXCLUSIONS = "exclusions";
    private static final String OPTIONAL_RESOLUTION_REQUIRE = "require";
    private static final String OPTIONAL_RESOLUTION_IGNORE = "ignore";
    private static final String OPTIONAL_RESOLUTION_OPTIONAL = "optional";
    private static final String FILE_EXTENSION = ".target";

    @Requirement
    private Logger logger;

    @Requirement
    private RepositorySystem repositorySystem;

    @Requirement
    private Map<String, TychoProject> projectTypes;

    @Requirement
    private TargetPlatformFilterConfigurationReader filterReader;

    public TargetPlatformConfiguration getTargetPlatformConfiguration(MavenSession session, MavenProject project)
            throws BuildFailureException {
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

                setTargetPlatformResolver(result, configuration);

                try {
                    setTarget(result, session, project, configuration);
                } catch (MojoExecutionException e) {
                    throw new BuildFailureException("Setting target failed", e);
                }

                setPomDependencies(result, configuration);

                setAllowConflictingDependencies(result, configuration);

                setDisableP2Mirrors(configuration);

                setExecutionEnvironment(result, configuration);
                setExecutionEnvironmentDefault(result, configuration);
                setBREEHeaderSelectionPolicy(result, configuration);
                setResolveWithEEContraints(result, configuration);

                readFilters(result, configuration);
                try {
                    readExclusions(result, configuration);
                } catch (TargetPlatformConfigurationException e) {
                    throw new BuildFailureException("reading exclusions failed", e);
                }

                readDependencyResolutionConfiguration(result, configuration);

                setTargetDefinitionIncludeSources(result, configuration);
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
            Properties properties = (Properties) DefaultReactorProject.adapt(project)
                    .getContextValue(TychoConstants.CTX_MERGED_PROPERTIES);

            // Otherwise, use current system os/ws/nl/arch
            String os = PlatformPropertiesUtils.getOS(properties);
            String ws = PlatformPropertiesUtils.getWS(properties);
            String arch = PlatformPropertiesUtils.getArch(properties);

            result.addEnvironment(new TargetEnvironment(os, ws, arch));

            result.setImplicitTargetEnvironment(true);
        } else {
            result.setImplicitTargetEnvironment(false);
        }

        return result;
    }

    private void setTargetDefinitionIncludeSources(TargetPlatformConfiguration result, Xpp3Dom configuration)
            throws BuildFailureException {
        String value = getStringValue(configuration.getChild(TARGET_DEFINITION_INCLUDE_SOURCE));

        if (value == null) {
            return;
        }
        try {
            result.setTargetDefinitionIncludeSourceMode(IncludeSourceMode.valueOf(value));
        } catch (IllegalArgumentException e) {
            throw new BuildFailureException(
                    "Illegal value of <targetDefinitionIncludeSource> target platform configuration parameter: "
                            + value,
                    e);
        }
    }

    protected void readDependencyResolutionConfiguration(TargetPlatformConfiguration result, Xpp3Dom configuration) {
        Xpp3Dom resolverDom = configuration.getChild(DEPENDENCY_RESOLUTION);
        if (resolverDom == null) {
            return;
        }

        setOptionalDependencies(result, resolverDom);
        readExtraRequirements(result, resolverDom);
        readProfileProperties(result, resolverDom);

    }

    private void setOptionalDependencies(TargetPlatformConfiguration result, Xpp3Dom resolverDom) {
        String value = getStringValue(resolverDom.getChild(OPTIONAL_DEPENDENCIES));

        if (value == null) {
            return;
        } else if (OPTIONAL_RESOLUTION_REQUIRE.equals(value)) {
            result.setOptionalResolutionAction(OptionalResolutionAction.REQUIRE);
        } else if (OPTIONAL_RESOLUTION_IGNORE.equals(value)) {
            result.setOptionalResolutionAction(OptionalResolutionAction.IGNORE);
        } else if (OPTIONAL_RESOLUTION_OPTIONAL.equals(value)) {
            result.setOptionalResolutionAction(OptionalResolutionAction.OPTIONAL);
        } else {
            throw new BuildFailureException(
                    "Illegal value of <optionalDependencies> dependency resolution parameter: " + value);
        }
    }

    protected void readExtraRequirements(TargetPlatformConfiguration result, Xpp3Dom resolverDom)
            throws BuildFailureException {
        Xpp3Dom requirementsDom = resolverDom.getChild("extraRequirements");
        if (requirementsDom == null) {
            return;
        }

        for (Xpp3Dom requirementDom : requirementsDom.getChildren("requirement")) {
            if (requirementDom.getChild("type") == null) {
                throw new BuildFailureException(
                        "Element <type> is missing in <extraRequirements><requirement> section.");
            }
            if (requirementDom.getChild("id") == null) {
                throw new BuildFailureException("Element <id> is missing in <extraRequirements><requirement> section.");
            }
            if (requirementDom.getChild("versionRange") == null) {
                throw new BuildFailureException(
                        "Element <versionRange> is missing in <extraRequirements><requirement> section.");
            }
            result.addExtraRequirement(new DefaultArtifactKey(requirementDom.getChild("type").getValue(),
                    requirementDom.getChild("id").getValue(), requirementDom.getChild("versionRange").getValue()));
        }
    }

    private void readProfileProperties(TargetPlatformConfiguration result, Xpp3Dom resolverDom) {
        Xpp3Dom propertiesDom = resolverDom.getChild("profileProperties");
        if (propertiesDom == null) {
            return;
        }

        Xpp3Dom[] propertyDomList = propertiesDom.getChildren();
        for (Xpp3Dom propertyDom : propertyDomList) {
            result.addProfileProperty(propertyDom.getName(), propertyDom.getValue().trim());
        }
    }

    private void setExecutionEnvironment(TargetPlatformConfiguration result, Xpp3Dom configuration) {
        String value = getStringValue(configuration.getChild(EXECUTION_ENVIRONMENT));

        if (value == null) {
            return;
        }
        result.setExecutionEnvironment(value);
    }

    private void setExecutionEnvironmentDefault(TargetPlatformConfiguration result, Xpp3Dom configuration) {
        String value = getStringValue(configuration.getChild(EXECUTION_ENVIRONMENT_DEFAULT));

        if (value == null) {
            return;
        }
        result.setExecutionEnvironmentDefault(value);
    }

    private void setBREEHeaderSelectionPolicy(TargetPlatformConfiguration result, Xpp3Dom configuration) {
        String value = getStringValue(configuration.getChild(BREE_HEADER_SELECTION_POLICY));

        if (value == null) {
            return;
        }
        try {
            result.setBREEHeaderSelectionPolicy(BREEHeaderSelectionPolicy.valueOf(value));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(
                    "Illegal value of <breeHeaderSelectionPolicy> target platform parameter: " + value);
        }
    }

    /**
     * Take the constraints of the configured execution environment into account when resolving
     * dependencies or target definitions. These constraints include the list of system packages and
     * the <tt>Bundle-RequiredExecutionEnvironment</tt> header. When set to <code>true</code>, the
     * dependency resolution verifies that the bundle and all required bundles can be used in an
     * OSGi container with the configured execution environment.
     */
    private void setResolveWithEEContraints(TargetPlatformConfiguration result, Xpp3Dom resolverDom) {
        String value = getStringValue(resolverDom.getChild(RESOLVE_WITH_EXECUTION_ENVIRONMENT_CONSTRAINTS));
        if (value == null) {
            return;
        }
        result.setResolveWithEEContraints(Boolean.valueOf(value));
    }

    private void setDisableP2Mirrors(Xpp3Dom configuration) {
        Xpp3Dom disableP2mirrorsDom = configuration.getChild("disableP2Mirrors");
        if (disableP2mirrorsDom != null) {
            logger.warn(
                    "Unsupported target-platform-configuration <disableP2Mirrors>. Use tycho.disableP2Mirrors -D command line parameter or settings.xml property.");
        }
    }

    private void setAllowConflictingDependencies(TargetPlatformConfiguration result, Xpp3Dom configuration) {
        String value = getStringValue(configuration.getChild(ALLOW_CONFLICTING_DEPENDENCIES));

        if (value == null) {
            return;
        }
        result.setAllowConflictingDependencies(Boolean.parseBoolean(value));
    }

    private void addTargetEnvironments(TargetPlatformConfiguration result, MavenProject project,
            Xpp3Dom configuration) {
        try {
            TargetEnvironment deprecatedTargetEnvironmentSpec = getDeprecatedTargetEnvironment(configuration);
            if (deprecatedTargetEnvironmentSpec != null) {
                result.addEnvironment(deprecatedTargetEnvironmentSpec);
            }

            Xpp3Dom environmentsDom = configuration.getChild(ENVIRONMENTS);
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
        } catch (TargetPlatformConfigurationException e) {
            throw new RuntimeException("target-platform-configuration error in project " + project.getId(), e);
        }
    }

    protected TargetEnvironment getDeprecatedTargetEnvironment(Xpp3Dom configuration)
            throws TargetPlatformConfigurationException {
        Xpp3Dom environmentDom = configuration.getChild("environment");
        if (environmentDom != null) {
            logger.warn(
                    "target-platform-configuration <environment> element is deprecated; use <environments> instead");
            return newTargetEnvironment(environmentDom);
        }
        return null;
    }

    private void setPomDependencies(TargetPlatformConfiguration result, Xpp3Dom configuration) {
        String value = getStringValue(configuration.getChild(POM_DEPENDENCIES));
        if (value == null) {
            return;
        }
        result.setPomDependencies(PomDependencies.valueOf(value));
    }

    private void setTarget(TargetPlatformConfiguration result, MavenSession session, MavenProject project,
            Xpp3Dom configuration) throws MojoExecutionException {
        Xpp3Dom targetDom = configuration.getChild(TARGET);
        if (targetDom == null) {
            return;
        }

        Xpp3Dom[] artifactDomArray = targetDom.getChildren("artifact");
        if (artifactDomArray != null && artifactDomArray.length > 0) {
            for (Xpp3Dom artifactDom : artifactDomArray) {
                addTargetArtifact(result, session, project, artifactDom);
            }
        }
        Xpp3Dom[] fileDomArray = targetDom.getChildren("file");
        if (fileDomArray != null && fileDomArray.length > 0) {
            Path basedir = Paths.get(project.getBasedir().getAbsolutePath());
            for (Xpp3Dom fileDom : fileDomArray) {
                String file = fileDom.getValue();
                File target = basedir.resolve(file).toFile();
                if (isTargetFile(target)) {
                    result.addTarget(target);
                    return;
                } else {
                    throw new MojoExecutionException("target definition file '" + target.getAbsolutePath()
                            + "' not found for project '" + project.getName() + "'.");
                }
            }
        }
        Xpp3Dom[] uriDomArray = targetDom.getChildren("uri");
        if (uriDomArray != null && uriDomArray.length > 0) {
            for (Xpp3Dom uriDom : uriDomArray) {
                String uri = uriDom.getValue();
                try {
                    result.addTarget(new URI(uri));
                } catch (URISyntaxException e) {
                    throw new MojoExecutionException("target definition uri '" + uri
                            + "' can not be parsed for project '" + project.getName() + "'.");
                }
            }
        }
    }

    protected void addTargetArtifact(TargetPlatformConfiguration result, MavenSession session, MavenProject project,
            Xpp3Dom artifactDom) throws MojoExecutionException {
        Xpp3Dom groupIdDom = artifactDom.getChild("groupId");
        Xpp3Dom artifactIdDom = artifactDom.getChild("artifactId");
        Xpp3Dom versionDom = artifactDom.getChild("version");
        if (groupIdDom == null || artifactIdDom == null || versionDom == null) {
            throw new BuildFailureException(
                    "The target artifact configuration is invalid - <groupId>, <artifactId> and <version> are mandatory");
        }
        Xpp3Dom classifierDom = artifactDom.getChild("classifier");

        String groupId = groupIdDom.getValue();
        String artifactId = artifactIdDom.getValue();
        String version = versionDom.getValue();
        String classifier = classifierDom != null ? classifierDom.getValue() : null;

        //check if target is part of reactor-build
        for (MavenProject otherProject : session.getProjects()) {
            if (groupId.equals(otherProject.getGroupId()) && artifactId.equals(otherProject.getArtifactId())
                    && version.equals(otherProject.getVersion())) {
                if (classifier == null) {
                    File[] targetFiles = listTargetFiles(otherProject.getBasedir());
                    for (File targetFile : targetFiles) {
                        if (isPrimaryTarget(otherProject, targetFile, targetFiles)) {
                            result.addTarget(targetFile);
                            return;
                        }
                    }
                    throwNoPrimaryTargetFound(otherProject, targetFiles);
                } else {
                    File target = new File(otherProject.getBasedir(), classifier + FILE_EXTENSION);
                    if (isTargetFile(target)) {
                        result.addTarget(target);
                        return;
                    } else {
                        throw new MojoExecutionException("target definition file '" + target
                                + "' not found in project '" + project.getName() + "'.");
                    }
                }
            }
        }
        // resolve using maven

        Artifact artifact = repositorySystem.createArtifactWithClassifier(groupId, artifactId, version, TARGET,
                classifier);
        ArtifactResolutionRequest request = new ArtifactResolutionRequest();
        request.setArtifact(artifact);
        request.setLocalRepository(session.getLocalRepository());
        request.setRemoteRepositories(project.getRemoteArtifactRepositories());
        repositorySystem.resolve(request);

        if (artifact.isResolved()) {
            result.addTarget(artifact.getFile());
            return;
        }
        throw new RuntimeException("Could not resolve target platform specification artifact " + artifact);

    }

    private void setTargetPlatformResolver(TargetPlatformConfiguration result, Xpp3Dom configuration) {
        String value = getStringValue(configuration.getChild(RESOLVER));

        if (value == null) {
            return;
        }
        result.setResolver(value);
    }

    private void readFilters(TargetPlatformConfiguration result, Xpp3Dom configuration) {
        Xpp3Dom filtersDom = configuration.getChild(FILTERS);
        if (filtersDom != null) {
            result.setFilters(filterReader.parseFilterConfiguration(filtersDom));
        }
    }

    private void readExclusions(TargetPlatformConfiguration result, Xpp3Dom configuration)
            throws TargetPlatformConfigurationException {
        Xpp3Dom exclusionsDom = configuration.getChild(EXCLUSIONS);
        if (exclusionsDom != null) {
            Xpp3Dom[] children = exclusionsDom.getChildren("exclusion");
            for (Xpp3Dom exclusion : children) {
                Xpp3Dom groupId = exclusion.getChild("groupId");
                if (groupId == null) {
                    throw new TargetPlatformConfigurationException(
                            "<groupId> element is missing within target-platform-configuration (element <exclusion>)");
                }
                Xpp3Dom artifactId = exclusion.getChild("artifactId");
                if (artifactId == null) {
                    throw new TargetPlatformConfigurationException(
                            "<artifactId> element is missing within target-platform-configuration (element <exclusion>)");
                }
                result.addExclusion(groupId.getValue(), artifactId.getValue());
            }
        }

    }

    private static TargetEnvironment newTargetEnvironment(Xpp3Dom environmentDom)
            throws TargetPlatformConfigurationException {
        Xpp3Dom osDom = environmentDom.getChild("os");
        if (osDom == null) {
            String message = "<os> element is missing within target-platform-configuration (element <environment>)";
            throw new TargetPlatformConfigurationException(message);
        }

        Xpp3Dom wsDom = environmentDom.getChild("ws");
        if (wsDom == null) {
            String message = "<ws> element is missing within target-platform-configuration (element <environment>)";
            throw new TargetPlatformConfigurationException(message);
        }

        Xpp3Dom archDom = environmentDom.getChild("arch");
        if (archDom == null) {
            String message = "<arch> element is missing within target-platform-configuration (element <environment>)";
            throw new TargetPlatformConfigurationException(message);
        }

        return new TargetEnvironment(osDom.getValue(), wsDom.getValue(), archDom.getValue());
    }

    /**
     * Returns the string value of the given node, with all "value not set" cases normalized to
     * <code>null</code>.
     */
    private static String getStringValue(Xpp3Dom element) {
        if (element == null) {
            return null;
        }

        String value = element.getValue().trim();
        if ("".equals(value)) {
            return null;
        } else {
            return value;
        }
    }

    /**
     * List all target files in the given folder
     * 
     * @param folder
     * @return the found target files or empty array if nothing was found, folder is not a directory
     *         or the directory could not be read
     */
    public static File[] listTargetFiles(File folder) {
        if (folder.isDirectory()) {
            File[] targetFiles = folder.listFiles(DefaultTargetPlatformConfigurationReader::isTargetFile);
            if (targetFiles != null) {
                return targetFiles;
            }
        }
        return new File[0];
    }

    /**
     * 
     * @param file
     * @return <code>true</code> if the given files likely denotes are targetfile based on file
     *         naming, <code>false</code> otherwise
     */
    public static boolean isTargetFile(File file) {
        return file != null && file.isFile() && file.getName().toLowerCase().endsWith(FILE_EXTENSION)
                && !file.getName().startsWith(".polyglot.");
    }

    /**
     * Checks if the given target file is the "primary" target artifact file among others
     * 
     * @param project
     * @param targetFile
     *            the target file to check
     * @param otherTargetFiles
     *            other target files to take into account
     * @return <code>true</code> if the target file is the primary artifact, <code>false</code>
     *         otherwise
     */
    public static boolean isPrimaryTarget(MavenProject project, File targetFile, File[] otherTargetFiles) {
        if (otherTargetFiles != null && otherTargetFiles.length == 1) {
            return isTargetFile(otherTargetFiles[0]);
        }
        String name = targetFile.getName();
        if (name.toLowerCase().endsWith(FILE_EXTENSION)) {
            String baseName = FilenameUtils.getBaseName(name);
            if (baseName.equalsIgnoreCase(project.getArtifactId())) {
                return true;
            }
        }
        return false;
    }

    public static void throwNoPrimaryTargetFound(MavenProject project, File[] targetFiles)
            throws MojoExecutionException {
        if (targetFiles == null || targetFiles.length == 0) {
            throw new MojoExecutionException(
                    "No target definition file(s) found in project '" + project.getName() + "'.");
        }
        throw new MojoExecutionException("One target file must be named  '" + project.getArtifactId() + FILE_EXTENSION
                + "' when multiple targets are present");
    }

}
