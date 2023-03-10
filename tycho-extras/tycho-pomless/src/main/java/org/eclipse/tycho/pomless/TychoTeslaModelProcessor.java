package org.eclipse.tycho.pomless;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Map;

import javax.annotation.Priority;

import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelProcessor;
import org.apache.maven.model.io.ModelParseException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.util.ReaderFactory;
import org.sonatype.maven.polyglot.TeslaModelProcessor;

//This is a hack until https://github.com/takari/polyglot-maven/pull/259 is fixed 
@Component(role = ModelProcessor.class)
@Priority(10)
public class TychoTeslaModelProcessor extends TeslaModelProcessor {
    @Override
    public Model read(final File input, final Map<String, ?> options) throws IOException, ModelParseException {
        Model model;
        try (Reader reader = ReaderFactory.newXmlReader(input)) {
            model = read(reader, options);
            model.setPomFile(input);
        }
        return model;
    }

    @Override
    public Model read(final InputStream input, final Map<String, ?> options) throws IOException, ModelParseException {
        try (Reader reader = ReaderFactory.newXmlReader(input)) {
            return read(reader, options);
        }
    }
}
