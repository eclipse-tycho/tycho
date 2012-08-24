/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc and others.
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.sisu.equinox.EquinoxServiceFactory;
import org.eclipse.tycho.core.ee.ExecutionEnvironmentUtils;
import org.eclipse.tycho.core.utils.PlatformPropertiesUtils;
import org.eclipse.tycho.osgi.adapters.MavenLoggerAdapter;
import org.eclipse.tycho.p2.resolver.TargetDefinitionFile;
import org.eclipse.tycho.p2.resolver.facade.P2ResolutionResult;
import org.eclipse.tycho.p2.resolver.facade.P2Resolver;
import org.eclipse.tycho.p2.resolver.facade.P2ResolverFactory;
import org.eclipse.tycho.p2.target.facade.TargetPlatformBuilder;

/**
 * Validates that specified target platforms (.target files) contents can be resolved.
 * 
 * @goal validate-target-platform
 * @phase validate
 */
public class TPValidationMojo extends AbstractMojo {
    /**
     * .target files to validate
     * 
     * @parameter
     * @required
     */
    private File[] targetFiles;

    /**
     * whether to fail build or just print a warning when a validation fails
     * 
     * @parameter default-value="true"
     */
    private boolean failOnError;

    /** @component */
    protected EquinoxServiceFactory equinox;

    /**
     * @parameter default-value="${project}"
     */
    private MavenProject project;

    /** @component */
    private Logger logger;

    private P2ResolverFactory factory;
    protected P2Resolver p2;
    protected List<Map<String, String>> environment;

    public void execute() throws MojoExecutionException {
        this.factory = this.equinox.getService(P2ResolverFactory.class);
        this.p2 = this.factory.createResolver(new MavenLoggerAdapter(this.logger, false));
        initializeEnvironments();
        p2.setEnvironments(environment);

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
            res.append(error.getMessage());
            res.append("\n");
        }
        return res.toString();
    }

    private void validateTarget(File targetFile) throws TPError {
        try {
            // create resolver
            this.logger.info("Validating " + targetFile + "...");
            TargetPlatformBuilder resolutionContext;
            resolutionContext = this.factory.createTargetPlatformBuilder(null);

            TargetDefinitionFile target = TargetDefinitionFile.read(targetFile);
            resolutionContext.addTargetDefinition(target, this.environment);
            P2ResolutionResult result = this.p2.resolveMetadata(resolutionContext, this.environment.get(0));
        } catch (Exception ex) {
            throw new TPError(targetFile, ex);
        }
    }

    /*
     * Copied from version-bump-plugin
     */
    protected void initializeEnvironments() {
        Properties properties = new Properties();
        properties.put(PlatformPropertiesUtils.OSGI_OS, PlatformPropertiesUtils.getOS(properties));
        properties.put(PlatformPropertiesUtils.OSGI_WS, PlatformPropertiesUtils.getWS(properties));
        properties.put(PlatformPropertiesUtils.OSGI_ARCH, PlatformPropertiesUtils.getArch(properties));

        // TODO this does not honour project <executionEnvironment> configuration
        ExecutionEnvironmentUtils.loadVMProfile(properties);

        // TODO does not belong here
        properties.put("org.eclipse.update.install.features", "true");

        Map<String, String> map = new LinkedHashMap<String, String>();
        for (Object key : properties.keySet()) {
            map.put(key.toString(), properties.getProperty(key.toString()));
        }

        this.environment = new ArrayList<Map<String, String>>();
        this.environment.add(map);
    }
}
