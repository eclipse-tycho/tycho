package org.eclipse.tycho.pomgenerator.test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.project.MavenProject;

public class GenerateDependencyPomsMojoTest extends AbstractPomMojoTest {

    static final String GOAL = "generate-dependency-poms";

    Path baseDir;
    File outDir;

    MavenProject p000;
    MavenProject p001;
    MavenProject p002;
    MavenProject p003;
    MavenProject p004;
    MavenProject p005_100;
    MavenProject p005_200;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        baseDir = getBasedir("projects/withdependencies").toPath();
        outDir = baseDir.resolve("p000/target/generated-pom").toFile();
        p000 = readProject("p000");
        p001 = readProject("p001");
        p002 = readProject("p002");
        p003 = readProject("p003");
        p004 = readProject("p004");
        p005_100 = readProject("p005-100");
        p005_200 = readProject("p005-200");
    }

    private void generate() throws Exception {
        Mojo generateMojo = lookupMojo(GROUP_ID, ARTIFACT_ID, VERSION, GOAL, null);

        MavenSession session = newMavenSession(p000, List.of(p000, p001, p002, p003, p005_100, p005_200));
        setVariableValueToObject(generateMojo, "session", session);
        setVariableValueToObject(generateMojo, "project", session.getCurrentProject());
        setVariableValueToObject(generateMojo, "forceGeneration", true);

        generateMojo.execute();
    }

    /**
     * p000 directly depends on p001 and p002.<br>
     * Check whether the generated pom contains both artifacts.
     * 
     * @throws Exception
     */
    public void testDirectDependencies() throws Exception {
        updateDependencies(p000, p001, p002);
        generate();
        Model model = readModel(outDir, "pom.xml");

        List<Dependency> dependencies = model.getDependencies();
        assertEquals(dependencies.size(), 2);
        assertEquals(dependencies.get(0).getArtifactId(), "p001");
        assertEquals(dependencies.get(1).getArtifactId(), "p002");
    }

    /**
     * p000 directly depends on p001 and indirectly on p002.<br>
     * p001 directly depends on p002.<br>
     * Check whether the generated pom only contains p001, as a dependency on p002 would be
     * redundant.
     * 
     * @throws Exception
     */
    public void testIndirectDependencies() throws Exception {
        updateDependencies(p000, p001, p002);
        updateDependencies(p001, p002);
        generate();
        Model model = readModel(outDir, "pom.xml");

        List<Dependency> dependencies = model.getDependencies();
        assertEquals(dependencies.size(), 1);
        assertEquals(dependencies.get(0).getArtifactId(), "p001");
    }

    /**
     * p000 directly depends on p001.<br>
     * p002 directly depends on p001 and p003.<br>
     * Check whether the generated pom excludes all artifacts which exists within the reactor
     * session but are not referenced by the artifact.
     * 
     * @throws Exception
     */
    public void testIgnoreUnrelatedDependencies() throws Exception {
        updateDependencies(p000, p001);
        updateDependencies(p002, p001, p003);
        generate();
        Model model = readModel(outDir, "pom.xml");

        List<Dependency> dependencies = model.getDependencies();
        assertEquals(dependencies.size(), 1);
        assertEquals(dependencies.get(0).getArtifactId(), "p001");
    }

    /**
     * p000 directly depends on p001 and p005 (v1.0.0).<br>
     * p001 directly depends on p005 (v.2.0.0).<br>
     * Check whether the generated pom is able to handle multiple versions of the same artifact.
     * Unlike Maven, OSGi allows an application to use multiple versions of the same library.
     * Therefore only artifacts with matching versions can be substituted and p000 should keep its
     * dependency on p005.
     * 
     * @throws Exception
     */
    public void testVersionAwareDependencies() throws Exception {
        updateDependencies(p000, p001, p005_100, p005_200);
        updateDependencies(p001, p005_200);
        generate();
        Model model = readModel(outDir, "pom.xml");

        List<Dependency> dependencies = model.getDependencies();
        assertEquals(dependencies.size(), 2);
        assertEquals(dependencies.get(0).getArtifactId(), "p001");
        assertEquals(dependencies.get(1).getArtifactId(), "p005");
        assertEquals(dependencies.get(1).getVersion(), "1.0.0");
    }

    /**
     * p000 directly depends on p001 twice.<br>
     * Check whether the generated pom is able handle duplicate dependencies. In such a case, all
     * but one instance should be removed.
     * 
     * @throws Exception
     */
    public void testDuplicateDependencies() throws Exception {
        updateDependencies(p000, p001, p001);
        generate();
        Model model = readModel(outDir, "pom.xml");

        List<Dependency> dependencies = model.getDependencies();
        assertEquals(dependencies.size(), 1);
        assertEquals(dependencies.get(0).getArtifactId(), "p001");
    }

    /**
     * p000 directly depends on p001 and p002.<br>
     * p001 directly depends on p002.<br>
     * Check whether the generated pom only contains p001, as is also contributes the dependency on
     * p002.
     * 
     * @throws Exception
     */
    public void testMinimalDependencies() throws Exception {
        updateDependencies(p000, p001, p002);
        updateDependencies(p001, p002);
        generate();
        Model model = readModel(outDir, "pom.xml");

        List<Dependency> dependencies = model.getDependencies();
        assertEquals(dependencies.size(), 1);
        assertEquals(dependencies.get(0).getArtifactId(), "p001");
    }

    /**
     * p000 directly depends on p001 and p003.<br>
     * p001 directly depends on p002.<br>
     * p003 directly depends on p001 and p003.<br>
     * Check whether the generated pom only contains is affected by a JDK bug (JDK-4744966) in the
     * TreeSet. When removing elements from a set using removeAll where the size of the argument is
     * larger than the size of the set, then elements are compared using equals(), rather than the
     * comparator. This does not work for instances of {@link Dependency}.
     * 
     * @throws Exception
     */
    public void testDeepDependencies() throws Exception {
        updateDependencies(p000, p001, p003);
        updateDependencies(p001, p002);
        updateDependencies(p003, p001, p004);
        generate();
        Model model = readModel(outDir, "pom.xml");

        List<Dependency> dependencies = model.getDependencies();
        assertEquals(dependencies.size(), 1);
        assertEquals(dependencies.get(0).getArtifactId(), "p003");
    }

    /**
     * p000 directly depends on p001 to p004.<br>
     * Check whether the dependencies use the correct scope. Artifacts within the same reactor build
     * are marked as {@code provided}. However, for the metadata generation, they should be in the
     * default scope.
     * 
     * @throws Exception
     */
    public void testDependencyScope() throws Exception {
        updateDependencies(p000, p001, p002, p003, p004);
        generate();
        Model model = readModel(outDir, "pom.xml");

        List<Dependency> dependencies = model.getDependencies();
        assertEquals(dependencies.size(), 4);

        for (Dependency dependency : dependencies) {
            // Null defaults to compile scope
            assertNull(dependency.getScope());
        }
    }

    private MavenProject readProject(String projectName) throws Exception {
        File projectDir = baseDir.resolve(projectName).toFile();
        File pom = projectDir.toPath().resolve("pom.xml").toFile();
        Model model = readModel(projectDir, "pom.xml");

        MavenProject project = new MavenProject(model);
        project.setFile(pom);
        project.setArtifact(mockArtifact(project));
        return project;
    }

    private Artifact mockArtifact(MavenProject project) {
        Artifact artifact = mock(Artifact.class);
        when(artifact.getGroupId()).thenReturn(project.getGroupId());
        when(artifact.getArtifactId()).thenReturn(project.getArtifactId());
        when(artifact.getVersion()).thenReturn(project.getVersion());
        when(artifact.getId()).thenReturn(project.getId());
        return artifact;
    }

    private void updateDependencies(MavenProject source, MavenProject... targets) {
        source.setDependencies(createDependencies(targets));
        source.setDependencyArtifacts(createDependencyArtifacts(targets));
    }

    private Set<Artifact> createDependencyArtifacts(MavenProject... projects) {
        return Arrays.stream(projects).map(MavenProject::getArtifact).collect(Collectors.toSet());
    }

    private List<Dependency> createDependencies(MavenProject... projects) {
        return Arrays.stream(projects).map(this::createDependency).collect(Collectors.toUnmodifiableList());
    }

    private Dependency createDependency(MavenProject project) {
        Dependency dependency = new Dependency();
        dependency.setGroupId(project.getGroupId());
        dependency.setArtifactId(project.getArtifactId());
        dependency.setVersion(project.getVersion());
        return dependency;
    }
}
