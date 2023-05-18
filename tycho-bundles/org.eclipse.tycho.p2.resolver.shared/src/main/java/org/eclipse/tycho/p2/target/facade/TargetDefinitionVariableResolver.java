package org.eclipse.tycho.p2.target.facade;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.tycho.ReactorProject;
import org.eclipse.tycho.core.shared.MavenContext;
import org.eclipse.tycho.core.shared.MavenLogger;

public class TargetDefinitionVariableResolver {

    private static final Pattern SYSTEM_PROPERTY_PATTERN = createVariablePatternArgument("system_property");
    private static final Pattern PROJECT_LOC_PATTERN = createVariablePatternArgument("project_loc");
    private static final Pattern ENV_VAR_PATTERN = createVariablePatternArgument("env_var");

    private final MavenContext mavenContext;
    private final MavenLogger logger;

    public TargetDefinitionVariableResolver(MavenContext mavenContext) {
        this.mavenContext = mavenContext;
        this.logger = mavenContext.getLogger();
    }

    public URI resolveRepositoryLocation(String location) {
        location = resolvePattern(location, SYSTEM_PROPERTY_PATTERN,
                key -> mavenContext.getSessionProperties().getProperty(key, ""));
        location = resolvePattern(location, ENV_VAR_PATTERN, key -> {
            String env = System.getenv(key);
            return env == null ? "" : env;
        });

        try {
            return new URI(location);
        } catch (URISyntaxException e) {
            throw new TargetDefinitionSyntaxException("Invalid URI: " + location);
        }
    }

    public String resolvePath(String path, TargetDefinition definition) {
        path = resolvePattern(path, SYSTEM_PROPERTY_PATTERN,
                key -> mavenContext.getSessionProperties().getProperty(key, ""));
        path = resolvePattern(path, ENV_VAR_PATTERN, key -> {
            String env = System.getenv(key);
            return env == null ? "" : env;
        });
        path = resolvePattern(path, PROJECT_LOC_PATTERN, this::findProjectLocation);
        return path;
    }

    private String findProjectLocation(String projectName) {
        if (projectName.startsWith("/")) {
            projectName = projectName.substring(1);
        }
        logger.debug("Find project location for project " + projectName);
        for (ReactorProject project : mavenContext.getProjects()) {
            String name = project.getName();
            logger.debug("check reactor project name: " + name);
            if (name.equals(projectName)) {
                return project.getBasedir().getAbsolutePath();
            }
        }
        for (ReactorProject project : mavenContext.getProjects()) {
            String artifactId = project.getArtifactId();
            logger.debug("check reactor project artifact id: " + artifactId);
            if (artifactId.equals(projectName)) {
                return project.getBasedir().getAbsolutePath();
            }
        }
        for (ReactorProject project : mavenContext.getProjects()) {
            String name = project.getBasedir().getName();
            logger.debug("check reactor project base directory: " + name);
            if (name.equals(projectName)) {
                return project.getBasedir().getAbsolutePath();
            }
        }
        //if we can't resolve this, we will return the original one as this might be intentional to not include the project in the build
        String defaultValue = "${project_loc:" + projectName + "}";
        logger.warn("Can't resolve " + defaultValue + " target resoloution might be incomplete");
        return defaultValue;
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
