package org.eclipse.tycho.packaging;

import static freemarker.template.Configuration.VERSION_2_3_31;
import static freemarker.template.TemplateExceptionHandler.RETHROW_HANDLER;
import static org.eclipse.tycho.PackagingType.TYPE_ECLIPSE_FEATURE;
import static org.eclipse.tycho.PackagingType.TYPE_ECLIPSE_PLUGIN;
import static org.eclipse.tycho.PackagingType.TYPE_ECLIPSE_TEST_PLUGIN;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import freemarker.cache.FileTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

@Mojo(name = "generate-metadata", threadSafe = true, defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class GenerateMetadataMojo extends AbstractTychoPackagingMojo {
    private static final Object LOCK = new Object();

    /**
     * Name of the generated JAR.
     */
    @Parameter(property = "project.build.finalName", alias = "jarName", required = true)
    protected String finalName;

    /**
     * Path to the FreeMarker template file, relative to the execution directory.
     */
    @Parameter(property = "templatePath", required = true)
    private String templatePath;

    /**
     * When set to {@code true}, a custom pom is always generated, even if the
     * project contains a handwritten pom.
     */
    @Parameter(property = "forceGenerate", defaultValue = "false")
    private boolean forceGenerate;

    /**
     * Path to the template file, relative to the current project.
     */
    private File template;

    /**
     * Generates the pom for the current reactor project in case it is an Eclipse
     * feature or (test-) plugin. The dependencies of a handwritten pom are updated
     * with the dependencies of the current reactor project. Otherwise a new pom is
     * generated using a custom FreeMarker template.
     * 
     * @throws MojoExecutionException
     */
    @Override
    public void execute() throws MojoExecutionException {
        synchronized (LOCK) {
            switch (project.getPackaging()) {
            case TYPE_ECLIPSE_PLUGIN:
            case TYPE_ECLIPSE_TEST_PLUGIN:
            case TYPE_ECLIPSE_FEATURE:
                try {
                    MavenProject copy;

                    // Create the project-relative template path
                    template = new File(templatePath);

                    if (hasHandwrittenPom() && !forceGenerate) {
                        getLog().debug("Handwritten pom detected");
                        copy = updatePom(finalName);
                    } else {
                        getLog().debug("Generate pom using " + template.getName());
                        copy = generatePom();
                    }

                    project.setPomFile(copy.getFile());
                } catch (IOException | TemplateException e) {
                    throw new MojoExecutionException(e.getMessage(), e);
                }
                break;
            default:
                // ignore
            }
        }
    }

    /**
     * Checks whether a handwritten pom.xml exists in the root director of the
     * current project. A handwritten file may contain additional project
     * information such as licenses, contributors, etc. In order to preserve this
     * data, the pom should be reused as much as possible, with the only
     * modification being the additional of the target-platform dependencies.
     * Otherwise a fresh pom is generated, using the template provided by the user.
     * 
     * @return {@code true}, in case a handwritten pom exists.
     */
    private boolean hasHandwrittenPom() {
        return new File(project.getBasedir(), "pom.xml").isFile();
    }

    /**
     * Generates the enhanced pom.xml for the current reactor project using a
     * FreeMarker template. The template takes (a copy of the) current project as an
     * argument and creates a user-specific pom.xml.<br>
     * The template is specified in {@link #templatePath}, relative to the root
     * directory of the current Maven execution (Usually the project root}.
     * 
     * @return A copy of the current Maven project, linking to the generated pom.xml
     *         file.
     * @throws IOException
     * @throws TemplateException If the specified template doesn't exist.
     */
    private MavenProject generatePom() throws IOException, TemplateException {
        MavenProject copy = project.clone();
        File pom = new File(buildDirectory, finalName + ".pom");

        // The path to the template directory should be relative to the
        // root directory, even in case of a multi-module project.
        String rootDirectory = session.getExecutionRootDirectory();
        File templateDirectory = new File(rootDirectory, template.getParent());

        Files.createDirectories(buildDirectory.toPath());

        // Setup FreeMarker
        Configuration cfg = new Configuration(VERSION_2_3_31);

        cfg.setTemplateLoader(new FileTemplateLoader(templateDirectory));
        cfg.setIncompatibleImprovements(VERSION_2_3_31);
        cfg.setDefaultEncoding("UTF-8");
        cfg.setLocale(Locale.ENGLISH);
        cfg.setTemplateExceptionHandler(RETHROW_HANDLER);

        Map<String, Object> args = new HashMap<>();
        args.put("project", copy);

        // Generate pom.xml
        Template ftl = cfg.getTemplate(template.getName());
        try (Writer out = new FileWriter(pom)) {
            getLog().info("Execute template" + ftl.getName());
            ftl.process(args, out);
        }

        getLog().debug("Update path of the project pom.xml to" + pom);
        // Link the project to the enhanced pom.xml
        copy.setPomFile(pom);
        return copy;
	}
}
