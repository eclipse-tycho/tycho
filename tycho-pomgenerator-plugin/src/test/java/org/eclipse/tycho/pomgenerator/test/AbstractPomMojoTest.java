package org.eclipse.tycho.pomgenerator.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.tycho.core.utils.TychoVersion;
import org.eclipse.tycho.testing.AbstractTychoMojoTestCase;

public abstract class AbstractPomMojoTest extends AbstractTychoMojoTestCase {

    static final String GROUP_ID = "org.eclipse.tycho";
    static final String ARTIFACT_ID = "tycho-pomgenerator-plugin";
    static final String VERSION = TychoVersion.getTychoVersion();

    MavenXpp3Reader modelReader = new MavenXpp3Reader();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
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
