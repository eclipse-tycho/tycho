package org.eclipse.tycho.pomgenerator;

import static org.eclipse.tycho.PackagingType.TYPE_ECLIPSE_FEATURE;
import static org.eclipse.tycho.PackagingType.TYPE_ECLIPSE_PLUGIN;
import static org.eclipse.tycho.PackagingType.TYPE_ECLIPSE_TEST_PLUGIN;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Generates a custom pom.xml for Eclipse (test-) plugin and features to be used as metadata in the
 * packaged jars. This file contains a list of all dependencies that are required to execute this
 * artifact. Note that due to technical limitations, this only covers artifacts built in the same
 * reactor build. Dependencies that are contributed e.g. a target platform are not included.<br>
 * The generated file is stored under {@code /target/generated-pom/pom.xml} of the respective
 * bundle.<br>
 * In order to include this metadata in both plugin <b>and</b> feature artifacts, it is necessary
 * that the {@code addMavenDescriptor} flag is set to true in the
 * {@code tycho-packaging-plugin}.<br>
 * The main purpose of this Mojo is to simplify the process of deploying and consuming artifacts
 * from a Maven repository by automatically fetching all (transitive) dependencies instead of having
 * to manually add and maintain them.
 *
 */
@Mojo(name = "generate-dependency-poms", threadSafe = true)
public class GenerateDependencyPomsMojo extends AbstractGeneratePomsMojo {
    private static final Object LOCK = new Object();

    /**
     * The current project in the reactor session.
     */
    @Parameter(property = "project", readonly = true, required = true)
    private MavenProject project;

    /**
     * The reactor session over the entire build.
     */
    @Parameter(property = "session", readonly = true, required = true)
    private MavenSession session;

    /**
     * Specifies whether a pom.xml should be generated, even in a pom already exists in the bundle
     * directory.
     */
    @Parameter(property = "forceGeneration", defaultValue = "false")
    private boolean forceGeneration;

    /**
     * Generates the pom for the current reactor project in case it is an Eclipse feature or (test-)
     * plugin.
     * 
     * @throws MojoExecutionException
     */
    @Override
    public void execute() throws MojoExecutionException {
        synchronized (LOCK) {
            switch (project.getPackaging()) {
            case TYPE_ECLIPSE_PLUGIN:
                generatePom("test-plugin-pom.xml");
                break;
            case TYPE_ECLIPSE_TEST_PLUGIN:
                generatePom("plugin-pom.xml");
                break;
            case TYPE_ECLIPSE_FEATURE:
                generatePom("feature-pom.xml");
                break;
            default:
                // ignore
            }
        }
    }

    /**
     * Generates the enhanced pom.xml for the current reactor project. The file itself is stored
     * under "/target/generated-pom/pom.xml" and contains a list of all direct dependencies.<br>
     * <b>NOTE:</b> Handwritten poms take precedence. So in case the program detects that such a
     * file already exists in the base directory, the generation process is skipped and the existing
     * file is used instead. This behavior can be overwritten by setting the {@code forceGeneration}
     * flag.
     * 
     * @throws MojoExecutionException
     */
    private void generatePom(String templateName) throws MojoExecutionException {
        Path baseDir = project.getBasedir().toPath();
        Path outDir = baseDir.resolve("target/generated-pom");
        Path pom = baseDir.resolve("pom.xml");

        if (Files.exists(pom) && !forceGeneration) {
            getLog().debug("pom.xml already exists. Ignore...");
            return;
        }

        Model model = readPomTemplate(templateName);
        pom = outDir.resolve("pom.xml");
        try {
            Files.createDirectories(pom.getParent());
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

        model.setGroupId(project.getGroupId());
        model.setArtifactId(project.getArtifactId());
        model.setVersion(project.getVersion());
        model.setDependencies(getMinimalReactorDependencies());

        getLog().info("Write generated pom.xml to " + pom.toFile());
        // Write to disc
        writePom(outDir.toFile(), model);
        // Update the path of the pom.xml in the project
        project.setPomFile(pom.toFile());
    }

    /**
     * Calculates the direct dependencies of the current reactor project. I.e. the minimal amount of
     * dependencies required to include all (transitive) dependencies.
     *
     * #TODO project.getArtifacts() should be used instead. However, it returns an empty set, even
     * though it should return all transitive dependencies. Alternatively, a filter may be used to
     * only keep all direct dependencies. But that's Maven 4 magic.
     * 
     * @return All direct dependencies of the current project.
     */
    private List<Dependency> getMinimalReactorDependencies() {
        // Create a lookup-table for all reactor projects
        Map<Artifact, MavenProject> projects = new TreeMap<>(Comparator.comparing(Artifact::getId));

        session.getProjects().forEach(proj -> projects.put(proj.getArtifact(), proj));

        Set<Dependency> minimalSet = new TreeSet<>(this::compare);

        // Change the scope from "provided" to the default scope "compile".
        // Using clone() prevents those changes from leaking into other phases.
        for (Dependency dependency : project.getDependencies()) {
            // Dependencies coming from the target platform are OSGi bundles and can't be transformed
            // into valid Maven artifacts. Therefore, those have to be excluded...
            if (Artifact.SCOPE_SYSTEM.equals(dependency.getScope())) {
                continue;
            }

            Dependency copy = dependency.clone();
            copy.setScope(null);
            minimalSet.add(copy);
        }

        // Iterate over all dependent artifacts
        for (Artifact artifacts : project.getDependencyArtifacts()) {
            MavenProject source = projects.get(artifacts);

            // Might be an artifact outside of the reactor build
            if (source != null) {
                // Remove all transitive dependencies.
                // Note: We can't use removeAll due to inconsistently calling the comparator
                // @See https://bugs.java.com/bugdatabase/view_bug.do?bug_id=4730113
                source.getDependencies().forEach(minimalSet::remove);
            } else {
                getLog().debug("Unhandled dependency artifact: " + artifacts);
            }
        }

        // Immutable list
        return List.copyOf(minimalSet);
    }

    /**
     * Two dependencies are considered equal, if their {@code groupId}, {@code artifactId} and
     * {@code version} match perfectly. A {@code null} version matches every other version. Elements
     * are sorted in this exact order.
     * 
     * @param arg0
     *            the first object to be compared.
     * @param arg1
     *            the second object to be compared.
     * @return a negative integer, zero, or a positive integer as the first argument is less than,
     *         equal to, or greater than the second.
     * @see {@link Comparator#compare(Object, Object)}
     */
    private int compare(Dependency arg0, Dependency arg1) {
        Comparator<Dependency> byGroupId = Comparator.comparing(Dependency::getGroupId);
        Comparator<Dependency> byArtifactId = Comparator.comparing(Dependency::getArtifactId);
        Comparator<Dependency> byVersion = Comparator.comparing(Dependency::getVersion);

        return byGroupId.thenComparing(byArtifactId).thenComparing(byVersion).compare(arg0, arg1);
    }
}
