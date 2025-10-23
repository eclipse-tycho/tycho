/*******************************************************************************
 * Copyright (c) 2008, 2024 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Repository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.tycho.BuildFailureException;
import org.eclipse.tycho.DefaultArtifactKey;
import org.eclipse.tycho.OptionalResolutionAction;
import org.eclipse.tycho.TargetEnvironment;
import org.eclipse.tycho.artifacts.configuration.TargetPlatformFilterConfigurationReader;
import org.eclipse.tycho.core.TargetPlatformConfiguration;
import org.eclipse.tycho.core.TargetPlatformConfiguration.BREEHeaderSelectionPolicy;
import org.eclipse.tycho.core.TargetPlatformConfiguration.LocalArtifactHandling;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.TychoProjectManager;
import org.eclipse.tycho.core.resolver.shared.IncludeSourceMode;
import org.eclipse.tycho.core.resolver.shared.PomDependencies;
import org.eclipse.tycho.core.resolver.shared.ReferencedRepositoryMode;
import org.eclipse.tycho.p2resolver.TargetDefinitionResolver;
import org.eclipse.tycho.targetplatform.TargetDefinitionFile;
import org.eclipse.tycho.targetplatform.TargetPlatformArtifactResolver;
import org.eclipse.tycho.targetplatform.TargetResolveException;
import org.osgi.framework.Filter;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Named
@Singleton
public class DefaultTargetPlatformConfigurationReader {
    public static final String TARGET_DEFINITION_INCLUDE_SOURCE = "targetDefinitionIncludeSource";
    public static final String REFERENCED_REPOSITORY_MODE = "referencedRepositoryMode";
    public static final String DEPENDENCY_RESOLUTION = "dependency-resolution";
    public static final String OPTIONAL_DEPENDENCIES = "optionalDependencies";
    public static final String LOCAL_ARTIFACTS = "localArtifacts";
    public static final String LOCAL_ARTIFACTS_PROPERTY = "tycho.localArtifacts";

    public static final String FILTERS = "filters";
    public static final String RESOLVE_WITH_EXECUTION_ENVIRONMENT_CONSTRAINTS = "resolveWithExecutionEnvironmentConstraints";
    public static final String REQUIRE_EAGER_RESOLVE = "requireEagerResolve";
    public static final String PROPERTY_REQUIRE_EAGER_RESOLVE = "tycho.target.eager";
    public static final String PROPERTY_ALIAS_REQUIRE_EAGER_RESOLVE = "tycho.resolver.classic";
    public static final String BREE_HEADER_SELECTION_POLICY = "breeHeaderSelectionPolicy";
    public static final String EXECUTION_ENVIRONMENT_DEFAULT = "executionEnvironmentDefault";
    public static final String EXECUTION_ENVIRONMENT = "executionEnvironment";
    public static final String POM_DEPENDENCIES = "pomDependencies";
    public static final String PROPERTY_POM_DEPENDENCIES = "tycho.target.pomDependencies";
    public static final String TARGET = "target";
    public static final String RESOLVER = "resolver";
    public static final String ENVIRONMENTS = "environments";
    public static final String EXCLUSIONS = "exclusions";
    private static final String OPTIONAL_RESOLUTION_REQUIRE = "require";
    private static final String OPTIONAL_RESOLUTION_IGNORE = "ignore";
    private static final String OPTIONAL_RESOLUTION_OPTIONAL = "optional";
    @Inject
    private Logger logger;

    @Inject
    private TychoProjectManager projectManager;

    @Inject
    private TargetPlatformFilterConfigurationReader filterReader;

    @Inject
    private TargetPlatformArtifactResolver platformArtifactResolver;

    public TargetPlatformConfiguration getTargetPlatformConfiguration(MavenSession session, MavenProject project)
            throws TargetPlatformConfigurationException {
        TargetPlatformConfiguration result = new TargetPlatformConfiguration();
        //we first need to set all items from the system environment just in case a target platform is not defined at all
        setRequireEagerResolve(result,
                getStringValue(null, session, PROPERTY_REQUIRE_EAGER_RESOLVE, PROPERTY_ALIAS_REQUIRE_EAGER_RESOLVE));
        setLocalArtifacts(result, getStringValue(null, session, LOCAL_ARTIFACTS_PROPERTY, null));
        //Now use org.eclipse.tycho:target-platform-configuration if provided
        TychoProject tychoProject = projectManager.getTychoProject(project).orElse(null);
        Plugin plugin = project.getPlugin("org.eclipse.tycho:target-platform-configuration");
        if (plugin != null) {
            Xpp3Dom configuration = (Xpp3Dom) plugin.getConfiguration();
            if (configuration != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("target-platform-configuration for " + project.toString() + ":\n"
                            + configuration.toString());
                }

                addTargetEnvironments(result, project, configuration, tychoProject);

                setTargetPlatformResolver(result, configuration);

                setTarget(result, session, project, configuration);

                setPomDependencies(result, configuration, session);

                setDisableP2Mirrors(configuration);

                setExecutionEnvironment(result, configuration);
                setExecutionEnvironmentDefault(result, configuration);
                setBREEHeaderSelectionPolicy(result, configuration);
                setResolveWithEEContraints(result, configuration);
                setRequireEagerResolve(result, configuration, session);

                readFilters(result, configuration);
                try {
                    readExclusions(result, configuration);
                } catch (TargetPlatformConfigurationException e) {
                    throw new BuildFailureException("reading exclusions failed", e);
                }

                readDependencyResolutionConfiguration(result, configuration, session);

                setTargetDefinitionIncludeSources(result, configuration);
                setReferencedRepositoryMode(result, configuration);
            }
        }
        //consider items set in the pom repositories
        for (Repository repository : project.getRepositories()) {
            if ("target".equals(repository.getLayout())) {
                try {
                    result.addTarget(new URI(TargetDefinitionResolver.convertRawToUri(repository.getUrl())));
                } catch (URISyntaxException e) {
                    throw new TargetPlatformConfigurationException("reading <repository> target failed: " + e, e);
                }
            }
        }

        if (result.getEnvironments().isEmpty()) {
            if (tychoProject != null) {
                TargetEnvironment env = tychoProject.getImplicitTargetEnvironment(project);
                if (env != null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Implicit target environment for " + project.toString() + ": " + env.toString());
                    }

                    result.addEnvironment(env);
                }
            }
        }

        if (result.getEnvironments().isEmpty()) {
            result.addEnvironment(TargetEnvironment.getRunningEnvironment());
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

    private void setReferencedRepositoryMode(TargetPlatformConfiguration result, Xpp3Dom configuration)
            throws BuildFailureException {
        String value = getStringValue(configuration.getChild(REFERENCED_REPOSITORY_MODE));
        if (value == null) {
            return;
        }
        try {
            result.setReferencedRepositoryMode(ReferencedRepositoryMode.valueOf(value));
        } catch (IllegalArgumentException e) {
            throw new BuildFailureException("Illegal value of <" + REFERENCED_REPOSITORY_MODE
                    + "> target platform configuration parameter: " + value, e);
        }
    }

    protected void readDependencyResolutionConfiguration(TargetPlatformConfiguration result, Xpp3Dom configuration,
            MavenSession mavenSession) {
        Xpp3Dom resolverDom = configuration.getChild(DEPENDENCY_RESOLUTION);
        if (resolverDom == null) {
            //create an empty dom so we can take system properties into account in the following steps
            resolverDom = new Xpp3Dom(DEPENDENCY_RESOLUTION);
        }

        setOptionalDependencies(result, resolverDom);
        setLocalArtifacts(result, resolverDom, mavenSession);
        readExtraRequirements(result, resolverDom);
        readProfileProperties(result, resolverDom);

    }

    private void setLocalArtifacts(TargetPlatformConfiguration result, Xpp3Dom resolverDom, MavenSession mavenSession) {
        String value = getStringValue(resolverDom.getChild(LOCAL_ARTIFACTS), mavenSession, LOCAL_ARTIFACTS_PROPERTY,
                null);
        setLocalArtifacts(result, value);
    }

    private void setLocalArtifacts(TargetPlatformConfiguration result, String value) {
        if (value == null) {
            return;
        }
        if ("default".equalsIgnoreCase(value)) {
            //backward compatible... but default is not a valid name for an enum, so we handle it special here.
            result.setLocalArtifactHandling(LocalArtifactHandling.include);
            return;
        }
        try {
            result.setLocalArtifactHandling(LocalArtifactHandling.valueOf(value));
        } catch (IllegalArgumentException e) {
            throw new BuildFailureException("Invalid value for " + LOCAL_ARTIFACTS + " setting, given = " + value
                    + ", allowed = " + Arrays.toString(LocalArtifactHandling.values()));
        }
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
     * the <code>Bundle-RequiredExecutionEnvironment</code> header. When set to <code>true</code>,
     * the dependency resolution verifies that the bundle and all required bundles can be used in an
     * OSGi container with the configured execution environment.
     */
    private void setResolveWithEEContraints(TargetPlatformConfiguration result, Xpp3Dom resolverDom) {
        String value = getStringValue(resolverDom.getChild(RESOLVE_WITH_EXECUTION_ENVIRONMENT_CONSTRAINTS));
        if (value == null) {
            return;
        }
        result.setResolveWithEEContraints(Boolean.valueOf(value));
    }

    private void setRequireEagerResolve(TargetPlatformConfiguration result, Xpp3Dom resolverDom, MavenSession session) {
        Xpp3Dom child = resolverDom.getChild(REQUIRE_EAGER_RESOLVE);
        String value = getStringValue(child, session, PROPERTY_REQUIRE_EAGER_RESOLVE,
                PROPERTY_ALIAS_REQUIRE_EAGER_RESOLVE);
        setRequireEagerResolve(result, value);
    }

    private void setRequireEagerResolve(TargetPlatformConfiguration result, String value) {
        if (value == null) {
            return;
        }
        result.setRequireEagerResolve(Boolean.valueOf(value));
    }

    private void setDisableP2Mirrors(Xpp3Dom configuration) {
        Xpp3Dom disableP2mirrorsDom = configuration.getChild("disableP2Mirrors");
        if (disableP2mirrorsDom != null) {
            logger.warn(
                    "Unsupported target-platform-configuration <disableP2Mirrors>. Use tycho.disableP2Mirrors -D command line parameter or settings.xml property.");
        }
    }

    private void addTargetEnvironments(TargetPlatformConfiguration result, MavenProject project, Xpp3Dom configuration,
            TychoProject tychoProject) {
        try {
            Xpp3Dom environmentsDom = configuration.getChild(ENVIRONMENTS);
            if (environmentsDom != null) {
                Filter filter = getTargetEnvironmentFilter(tychoProject, project);
                for (Xpp3Dom environmentDom : environmentsDom.getChildren("environment")) {
                    TargetEnvironment environment = newTargetEnvironment(environmentDom);
                    if (environment.match(filter)) {
                        result.addEnvironment(environment);
                    } else {
                        result.addFilteredEnvironment(environment);
                    }
                }
                List<TargetEnvironment> filteredEnvironments = result.getFilteredEnvironments();
                if (!filteredEnvironments.isEmpty()) {
                    logger.debug(MessageFormat.format(
                            "Declared TargetEnvironment(s) {0} are skipped for {1} as they do not match the project filter {2}.",
                            filteredEnvironments.stream().map(TargetEnvironment::toFilterProperties)
                                    .map(String::valueOf).collect(Collectors.joining(", ")),
                            project.getId(), filter));
                }
            }
        } catch (TargetPlatformConfigurationException e) {
            throw new RuntimeException("target-platform-configuration error in project " + project.getId(), e);
        }
    }

    private static Filter getTargetEnvironmentFilter(TychoProject tychoProject, MavenProject project) {
        if (tychoProject != null) {
            return tychoProject.getTargetEnvironmentFilter(project);
        }
        return null;
    }

    private void setPomDependencies(TargetPlatformConfiguration result, Xpp3Dom configuration, MavenSession session) {
        String value = getStringValue(configuration.getChild(POM_DEPENDENCIES), session, PROPERTY_POM_DEPENDENCIES,
                null);
        if (value == null) {
            return;
        }
        result.setPomDependencies(PomDependencies.valueOf(value));
    }

    private void setTarget(TargetPlatformConfiguration result, MavenSession session, MavenProject project,
            Xpp3Dom configuration) throws TargetPlatformConfigurationException {
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
                if (TargetDefinitionFile.isTargetFile(target)) {
                    result.addTarget(target);
                    return;
                } else {
                    result.addLazyTargetFile(() -> {
                        if (TargetDefinitionFile.isTargetFile(target)) {
                            return target;
                        }
                        throw new TargetPlatformConfigurationException("target definition file '"
                                + target.getAbsolutePath() + "' not found for project '" + project.getName() + "'.");
                    });
                }
            }
        }
        Xpp3Dom[] uriDomArray = targetDom.getChildren("uri");
        if (uriDomArray != null && uriDomArray.length > 0) {
            for (Xpp3Dom uriDom : uriDomArray) {
                String uri = uriDom.getValue();
                try {
                    result.addTarget(new URI(TargetDefinitionResolver.convertRawToUri(uri)));
                } catch (URISyntaxException e) {
                    throw new TargetPlatformConfigurationException("target definition uri '" + uri
                            + "' can not be parsed for project '" + project.getName() + "'.");
                }
            }
        }
        Xpp3Dom[] locationsArray = targetDom.getChildren("location");
        if (locationsArray != null && locationsArray.length > 0) {
            for (Xpp3Dom locationDom : locationsArray) {
                result.addTargetLocation(locationDom);
            }
        }

    }

    protected void addTargetArtifact(TargetPlatformConfiguration result, MavenSession session, MavenProject project,
            Xpp3Dom artifactDom) throws TargetPlatformConfigurationException {
        Xpp3Dom groupIdDom = artifactDom.getChild("groupId");
        Xpp3Dom artifactIdDom = artifactDom.getChild("artifactId");
        Xpp3Dom versionDom = artifactDom.getChild("version");
        if (groupIdDom == null || artifactIdDom == null || versionDom == null) {
            throw new TargetPlatformConfigurationException(
                    "The target artifact configuration is invalid - <groupId>, <artifactId> and <version> are mandatory");
        }
        Xpp3Dom classifierDom = artifactDom.getChild("classifier");

        String groupId = groupIdDom.getValue();
        String artifactId = artifactIdDom.getValue();
        String version = versionDom.getValue();
        String classifier = classifierDom != null ? classifierDom.getValue() : null;
        result.addLazyTargetFile(() -> {
            try {
                return platformArtifactResolver.resolveTargetFile(groupId, artifactId, version, classifier, session,
                        project.getRemoteArtifactRepositories());
            } catch (TargetResolveException e) {
                throw new TargetPlatformConfigurationException("resolve target artifact " + groupId + ":" + artifactId
                        + ":" + version + ":" + Objects.requireNonNullElse(classifier, "no classifier") + " failed!",
                        e);
            }
        });
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
    private static String getStringValue(Xpp3Dom element, MavenSession session, String property, String alias) {

        if (session != null) {
            Properties userProperties = session.getUserProperties();
            String userProperty = userProperties.getProperty(property);
            if (userProperty != null) {
                return userProperty;
            }
            if (alias != null) {
                userProperty = userProperties.getProperty(alias);
                if (userProperty != null) {
                    return userProperty;
                }
            }
        }
        return getStringValue(element);
    }

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
            return TargetDefinitionFile.isTargetFile(otherTargetFiles[0]);
        }
        String name = targetFile.getName();
        if (name.toLowerCase().endsWith(TargetDefinitionFile.FILE_EXTENSION)) {
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
        throw new MojoExecutionException("One target file must be named  '" + project.getArtifactId()
                + TargetDefinitionFile.FILE_EXTENSION + "' when multiple targets are present");
    }

}
