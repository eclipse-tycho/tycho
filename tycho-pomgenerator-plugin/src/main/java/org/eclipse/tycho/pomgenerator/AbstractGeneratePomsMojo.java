package org.eclipse.tycho.pomgenerator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

public abstract class AbstractGeneratePomsMojo extends AbstractMojo {

    /**
     * Location of directory with template pom.xml file. pom.xml templates will be looked at this
     * directory first, default templates will be used if template directory and the template itself
     * does not exist.
     * 
     * See src/main/resources/templates for the list of supported template files.
     */
    @Parameter(property = "templatesDir", defaultValue = "${basedir}/pom-templates")
    private File templatesDir;

    private final MavenXpp3Reader modelReader = new MavenXpp3Reader();
    private final MavenXpp3Writer modelWriter = new MavenXpp3Writer();

    /* package */ void writePom(File dir, Model model) throws MojoExecutionException {
        writePom(dir, "pom.xml", model);
    }

    /* package */ void writePom(File dir, String filename, Model model) throws MojoExecutionException {
        try {
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(new File(dir, filename)),
                    StandardCharsets.UTF_8)) {
                modelWriter.write(writer, model);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Can't write pom.xml", e);
        }
    }

    /* package */ Model readPomTemplate(String name) throws MojoExecutionException {
        try {
            XmlStreamReader reader;

            File file = new File(templatesDir, name);
            if (file.canRead()) {
                // check custom templates dir first
                reader = ReaderFactory.newXmlReader(file);
            } else {
                // fall back to internal templates 
                ClassLoader cl = GeneratePomsMojo.class.getClassLoader();
                InputStream is = cl.getResourceAsStream("templates/" + name);
                reader = is != null ? ReaderFactory.newXmlReader(is) : null;
            }
            if (reader != null) {
                try {
                    return modelReader.read(reader);
                } finally {
                    reader.close();
                }
            } else {
                throw new MojoExecutionException("pom.xml template cannot be found " + name);
            }
        } catch (XmlPullParserException | IOException e) {
            throw new MojoExecutionException("Can't read pom.xml template " + name, e);
        }
    }
}
