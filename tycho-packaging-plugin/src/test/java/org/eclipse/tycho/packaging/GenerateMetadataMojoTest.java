package org.eclipse.tycho.packaging;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.tycho.core.utils.TychoVersion;
import org.eclipse.tycho.testing.AbstractTychoMojoTestCase;

public class GenerateMetadataMojoTest extends AbstractTychoMojoTestCase {

	static final String GROUP_ID = "org.eclipse.tycho";
	static final String ARTIFACT_ID = "tycho-packaging-plugin";
	static final String VERSION = TychoVersion.getTychoVersion();
	static final String GOAL = "generate-metadata";

	MavenXpp3Reader modelReader = new MavenXpp3Reader();

    Dependency d000;
    Dependency d001;
    Dependency d002;

    File template;
    File buildDir;
    List<MavenProject> projects;
    MavenProject project;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        d000 = createDependency("test", "d000", "1.0.0");
        d001 = createDependency("test", "d001", "1.0.0");
        d002 = createDependency("test", "d002", "1.0.0");

        File baseDir = getBasedir("projects/withdependencies/p000");
        buildDir = new File(baseDir, "target");
        template = new File("src/test/resources/templates/pom.ftl");
        projects = getSortedProjects(baseDir);
        project = projects.get(0);

        project.setDependencies(List.of(d000, d001, d002));
    }

    private void generate(MavenProject project, boolean forceGenerate) throws Exception {
        Mojo generateMojo = lookupMojo(GROUP_ID, ARTIFACT_ID, VERSION, GOAL, null);

        MavenSession session = newMavenSession(project);
        
        setVariableValueToObject(generateMojo, "project", session.getCurrentProject());
        setVariableValueToObject(generateMojo, "session", session);
        setVariableValueToObject(generateMojo, "finalName", "out");
        setVariableValueToObject(generateMojo, "templatePath", "src/test/resources/templates/pom.ftl");
        setVariableValueToObject(generateMojo, "forceGenerate", forceGenerate);
        setVariableValueToObject(generateMojo, "buildDirectory", buildDir);
        
        generateMojo.execute();
    }

    public void testUseGeneratedPom() throws Exception {
        generate(project, true);

        Model model = readModel(buildDir, "out.pom");

        // Have the dependencies been added?
        List<Dependency> dependencies = model.getDependencies();
        assertEquals(dependencies.size(), 3);
        assertEquals(dependencies.get(0).getArtifactId(), d000.getArtifactId());
        assertEquals(dependencies.get(1).getArtifactId(), d001.getArtifactId());
        assertEquals(dependencies.get(2).getArtifactId(), d002.getArtifactId());

        // The licenses should not have been carried over
        List<License> licenses = model.getLicenses();
        assertEquals(licenses.size(), 0);

        // The name should not have been carried over
        assertNull(model.getName());
    }

    public void testUseExistingPom() throws Exception {
        generate(project, false);

        Model model = readModel(buildDir, "out.pom");

        // Have the dependencies been added?
        List<Dependency> dependencies = model.getDependencies();
        assertEquals(dependencies.size(), 3);
        assertEquals(dependencies.get(0).getArtifactId(), d000.getArtifactId());
        assertEquals(dependencies.get(1).getArtifactId(), d001.getArtifactId());
        assertEquals(dependencies.get(2).getArtifactId(), d002.getArtifactId());
        
        // Has the licenses remained?
        List<License> licenses = model.getLicenses();
        assertEquals(licenses.size(), 1);
        assertEquals(licenses.get(0).getName(), "Eclipse Public License - v 2.0");
        assertEquals(licenses.get(0).getUrl(), "https://www.eclipse.org/legal/epl-2.0/");

        // Has the name remained?
        assertEquals(model.getName(), "Tycho Test Plugin");
    }

    private Dependency createDependency(String groupId, String artifactId, String version) {
        Dependency dependency = new Dependency();
        dependency.setGroupId(groupId);
        dependency.setArtifactId(artifactId);
        dependency.setVersion(version);
        return dependency;
    }

    protected Model readModel(File baseDir, String name) throws IOException, XmlPullParserException {
        File pom = new File(baseDir, name);
        FileInputStream is = new FileInputStream(pom);
        try {
            return modelReader.read(ReaderFactory.newXmlReader(is));
        } finally {
            is.close();
        }
    }
}
