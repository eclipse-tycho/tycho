/*******************************************************************************
 * Copyright (c) 2023 Vaclav Hala and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * Contributors:
 *    Vaclav Hala - extraction code from TargetDefinitionResolver into dedicated component
 *
 *******************************************************************************/
package org.eclipse.tycho.p2resolver;

import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.SessionScoped;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.shared.MavenContext;

@Named
@SessionScoped
public class DefaultTargetDefinitionVariableResolver implements TargetDefinitionVariableResolver {

    private static final Pattern SYSTEM_PROPERTY_PATTERN = createVariablePatternArgument("system_property");
    private static final Pattern PROJECT_LOC_PATTERN = createVariablePatternArgument("project_loc");
    private static final Pattern ENV_VAR_PATTERN = createVariablePatternArgument("env_var");

    @Inject
    private MavenContext mavenContext;
    @Inject
    private Logger logger;
    private MavenSession mavenSession;

    @Inject
    public DefaultTargetDefinitionVariableResolver(MavenSession mavenSession) {
        this.mavenSession = mavenSession;
    }

    /** for tests */
    public DefaultTargetDefinitionVariableResolver(MavenContext mavenContext, Logger logger) {
        this.mavenContext = mavenContext;
        this.logger = logger;
    }

    @Override
    public String resolve(String raw) {
        raw = resolvePattern(raw, SYSTEM_PROPERTY_PATTERN, key -> property(key, ""));
        raw = resolvePattern(raw, ENV_VAR_PATTERN, key -> {
            String env = System.getenv(key);
            return env == null ? "" : env;
        });
        raw = resolvePattern(raw, PROJECT_LOC_PATTERN, this::findProjectLocation);
        return raw;
    }

    private String findProjectLocation(String projectName) {
        if (projectName.startsWith("/")) {
            projectName = projectName.substring(1);
        }
        logger.debug("Find project location for project " + projectName);
        Iterable<ReactorProject> projects = projects();
        for (ReactorProject project : projects) {
            String name = project.getName();
            logger.debug("check reactor project name: " + name);
            if (name.equals(projectName)) {
                return project.getBasedir().getAbsolutePath();
            }
        }
        for (ReactorProject project : projects) {
            String artifactId = project.getArtifactId();
            logger.debug("check reactor project artifact id: " + artifactId);
            if (artifactId.equals(projectName)) {
                return project.getBasedir().getAbsolutePath();
            }
        }
        for (ReactorProject project : projects) {
            String name = project.getBasedir().getName();
            logger.debug("check reactor project base directory: " + name);
            if (name.equals(projectName)) {
                return project.getBasedir().getAbsolutePath();
            }
        }
        //if we can't resolve this, we will return the original one as this might be intentional to not include the project in the build
        String defaultValue = "${project_loc:" + projectName + "}";
        logger.warn("Cannot resolve " + defaultValue + " target resolution might be incomplete");
        return defaultValue;
    }

    private String property(String key, String defaultValue) {
        return mavenContext.getSessionProperties().getProperty(key, defaultValue);
    }

    private Iterable<ReactorProject> projects() {
        if (mavenSession != null) {
            List<MavenProject> projects = mavenSession.getAllProjects();
            if (projects != null) {
                return projects.stream().map(DefaultReactorProject::adapt).toList();
            }
        }
        return mavenContext.getProjects();
    }

    private static String resolvePattern(String input, Pattern pattern, Function<String, String> parameterResolver) {
        Matcher matcher = pattern.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String group = matcher.group(1);
            String resolved = parameterResolver.apply(group);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(resolved));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static Pattern createVariablePatternArgument(String variableName) {
        return Pattern.compile("\\$\\{" + variableName + ":([^}]+)\\}", Pattern.CASE_INSENSITIVE);
    }
}
