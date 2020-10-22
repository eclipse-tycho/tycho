/*******************************************************************************
 * Copyright (c) 2012, 2020 Red Hat, Inc and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Mickael Istria (Red Hat). - initial API and implementation
 *    Christoph Läubrich -  Bug 461284 - Improve discovery and attach of .target files in eclipse-target-definition
 *******************************************************************************/
package org.eclipse.tycho.extras.tpvalidator;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.ArtifactType;
import org.eclipse.tycho.PackagingType;
import org.eclipse.tycho.core.ee.TargetDefinitionFile;
import org.eclipse.tycho.core.resolver.DefaultTargetPlatformConfigurationReader;
import org.eclipse.tycho.core.shared.TargetEnvironment;
import org.eclipse.tycho.osgi.adapters.MavenLoggerAdapter;
import org.eclipse.tycho.p2.resolver.facade.P2Resolver;
import org.eclipse.tycho.p2.resolver.facade.P2ResolverFactory;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.InstallableUnitLocation;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.Location;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.Repository;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.Unit;
import org.eclipse.tycho.p2.target.facade.TargetPlatformConfigurationStub;
import org.eclipse.tycho.p2.tools.RepositoryReferences;
import org.eclipse.tycho.p2.tools.director.shared.DirectorRuntime;

/**
 * Validates that specified target platforms (.target files) contents can be resolved.
 */
@Mojo(name = "validate-target-platform", defaultPhase = LifecyclePhase.VALIDATE)
public class TPValidationMojo extends AbstractMojo {
    @Parameter(property = "project")
    private MavenProject project;

    /**
     * The .target files to validate. If not specified and the project packaging is
     * "eclipse-target-definition", the goal will validate project's primary target file will be
     * validated.
     */
    @Parameter
    private File[] targetFiles;

    /**
     * whether to fail build or just print a warning when a validation fails
     */
    @Parameter(defaultValue = "true")
    private boolean failOnError;

    /**
     * Check that, for each artifact from the target file, the dependencies of the artifact are also
     * contained in the target file. Also check that there are no conflicting artifacts, i.e.
     * artifact which could not be installed together. When this check passes, none of the artifacts
     * should lead to dependency resolution problems when used in a Tycho project.
     * 
     * @since 0.21.0
     */
    @Parameter(defaultValue = "false")
    private boolean checkDependencies;

    /**
     * Check that the content of the target-platform can be installed together in a same
     * provisioning operation.
     *
     * @since 0.26.0
     */
    @Parameter(defaultValue = "false")
    private boolean checkProvisioning;

    @Parameter(defaultValue = "JavaSE-1.7")
    private String executionEnvironment;

    @Component
    protected EquinoxServiceFactory equinox;

    @Component
    private Logger logger;

    private P2ResolverFactory factory;

    public void execute() throws MojoExecutionException {
        this.factory = this.equinox.getService(P2ResolverFactory.class);

        List<TPError> errors = new ArrayList<>();
        File[] targetFilesToValidate;
        if (this.targetFiles != null) {
            targetFilesToValidate = this.targetFiles;
        } else if (this.project != null
                && PackagingType.TYPE_ECLIPSE_TARGET_DEFINITION.equals(this.project.getPackaging())) {
            File[] targetFiles = DefaultTargetPlatformConfigurationReader.listTargetFiles(project.getBasedir());
            Optional<File> primaryTarget = Arrays.stream(targetFiles)
                    .filter(targetFile -> DefaultTargetPlatformConfigurationReader.isPrimaryTarget(project, targetFile,
                            targetFiles))
                    .findFirst();
            if (primaryTarget.isEmpty()) {
                if (failOnError) {
                    DefaultTargetPlatformConfigurationReader.throwNoPrimaryTargetFound(project, targetFiles);
                } else {
                    this.logger.error("No primary target found. Skipping execution.");
                }
                return;
            } else {
                targetFilesToValidate = new File[] { primaryTarget.get() };
            }
        } else {
            this.logger.info("No targetFiles configured. Skipping execution.");
            return;
        }

        for (File targetFile : targetFilesToValidate) {
            try {
                validateTarget(targetFile);
                this.logger.info("OK!");
            } catch (TPError ex) {
                this.logger.info("Failed, see Error log below");
                errors.add(ex);
            }
        }

        if (!errors.isEmpty()) {
            String message = createErrorMessage(errors);
            this.logger.error(message);
            if (this.failOnError) {
                throw new MojoExecutionException(message);
            }
        }
    }

    private String createErrorMessage(List<TPError> errors) {
        StringBuilder res = new StringBuilder();
        res.append("Validation found errors in ");
        res.append(errors.size());
        res.append(" .target files:");
        res.append("\n");
        for (TPError error : errors) {
            res.append(error.getMessage(true));
            res.append("\n");
        }
        return res.toString();
    }

    private void validateTarget(File targetFile) throws TPError {
        try {
            // create resolver
            this.logger.info("Validating " + targetFile + "...");
            RepositoryReferences ref = new RepositoryReferences();
            DirectorRuntime director = this.equinox.getService(DirectorRuntime.class);
            DirectorRuntime.Command directorCommand = director.newInstallCommand();

            TargetPlatformConfigurationStub tpConfiguration = new TargetPlatformConfigurationStub();
            tpConfiguration.setEnvironments(Collections.singletonList(TargetEnvironment.getRunningEnvironment()));

            TargetDefinitionFile targetDefinition = TargetDefinitionFile.read(targetFile);
            tpConfiguration.addTargetDefinition(targetDefinition);

            P2Resolver resolver = this.factory.createResolver(new MavenLoggerAdapter(this.logger, false));

            for (Location location : targetDefinition.getLocations()) {
                if (location instanceof InstallableUnitLocation) {
                    InstallableUnitLocation p2Loc = (InstallableUnitLocation) location;
                    for (Repository repo : p2Loc.getRepositories()) {
                        ref.addArtifactRepository(repo.getLocation());
                        ref.addMetadataRepository(repo.getLocation());
                    }
                    for (Unit unit : p2Loc.getUnits()) {
                        if (checkDependencies) {
                            // make dependency resolver resolve everything simultaneously
                            resolver.addDependency(ArtifactType.TYPE_INSTALLABLE_UNIT, unit.getId(), unit.getVersion());
                        }
                        if (checkProvisioning) {
                            directorCommand.addUnitToInstall(unit.getId() + '/' + unit.getVersion());
                        }
                    }
                }
            }
            resolver.resolveMetadata(tpConfiguration, executionEnvironment);
            if (checkProvisioning) {
                directorCommand.addMetadataSources(ref.getMetadataRepositories());
                directorCommand.addArtifactSources(ref.getArtifactRepositories());
                directorCommand.setDestination(
                        new File(project.getBuild().getDirectory(), targetFile.getName() + ".provisioning"));
                directorCommand.setProfileName(project.getArtifactId());
                directorCommand.setVerifyOnly(true);
                directorCommand.setInstallFeatures(true);
                directorCommand.execute();
            }
        } catch (Exception e) {
            throw new TPError(targetFile, e);
        }
    }

}
