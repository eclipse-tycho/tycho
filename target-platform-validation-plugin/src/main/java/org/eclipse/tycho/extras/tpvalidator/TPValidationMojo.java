/*******************************************************************************
 * Copyright (c) 2012, 2014 Red Hat, Inc and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mickael Istria (Red Hat). - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.extras.tpvalidator;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.core.facade.TargetEnvironment;
import org.eclipse.tycho.osgi.adapters.MavenLoggerAdapter;
import org.eclipse.tycho.p2.resolver.TargetDefinitionFile;
import org.eclipse.tycho.p2.resolver.facade.P2Resolver;
import org.eclipse.tycho.p2.resolver.facade.P2ResolverFactory;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.InstallableUnitLocation;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.Location;
import org.eclipse.tycho.p2.target.facade.TargetDefinition.Unit;
import org.eclipse.tycho.p2.target.facade.TargetPlatformConfigurationStub;

/**
 * Validates that specified target platforms (.target files) contents can be resolved.
 */
@Mojo(name = "validate-target-platform", defaultPhase = LifecyclePhase.VALIDATE)
public class TPValidationMojo extends AbstractMojo {
    /**
     * .target files to validate
     */
    @Parameter(required = true)
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

    @Parameter(defaultValue = "JavaSE-1.6")
    private String executionEnvironment;

    @Component
    protected EquinoxServiceFactory equinox;

    @Component
    private Logger logger;

    private P2ResolverFactory factory;

    public void execute() throws MojoExecutionException {
        this.factory = this.equinox.getService(P2ResolverFactory.class);

        List<TPError> errors = new ArrayList<TPError>();
        for (File targetFile : this.targetFiles) {
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
            res.append(error.getMessage(this.logger.isDebugEnabled()));
            res.append("\n");
        }
        return res.toString();
    }

    private void validateTarget(File targetFile) throws TPError {
        try {
            // create resolver
            this.logger.info("Validating " + targetFile + "...");
            TargetPlatformConfigurationStub tpConfiguration = new TargetPlatformConfigurationStub();
            tpConfiguration.setEnvironments(Collections.singletonList(TargetEnvironment.getRunningEnvironment()));

            TargetDefinitionFile targetDefinition = TargetDefinitionFile.read(targetFile);
            tpConfiguration.addTargetDefinition(targetDefinition);

            P2Resolver resolver = this.factory.createResolver(new MavenLoggerAdapter(this.logger, false));
            if (checkDependencies) {
                for (Location location : targetDefinition.getLocations()) {
                    if (location instanceof InstallableUnitLocation) {
                        InstallableUnitLocation p2Loc = (InstallableUnitLocation) location;
                        for (Unit unit : p2Loc.getUnits()) {
                            // make dependency resolver resolve everything simultaneously
                            resolver.addDependency(P2Resolver.TYPE_INSTALLABLE_UNIT, unit.getId(), unit.getVersion());
                        }
                    }
                }
            }
            resolver.resolveMetadata(tpConfiguration, executionEnvironment);
        } catch (Exception ex) {
            throw new TPError(targetFile, ex);
        }
    }

}
