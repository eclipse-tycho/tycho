package org.eclipse.tycho.versions;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.tycho.versions.engine.ProjectMetadataReader;

abstract class AbstractVersionsMojo extends AbstractMojo {

    /**
     * @parameter expression="${session}"
     * @readonly
     */
    protected MavenSession session;

    /**
     * @component
     */
    private PlexusContainer plexus;

    protected <T> T lookup(Class<T> clazz) throws MojoFailureException {
        try {
            return plexus.lookup(clazz);
        } catch (ComponentLookupException e) {
            throw new MojoFailureException("Could not lookup required component", e);
        }
    }

    protected ProjectMetadataReader newProjectMetadataReader() throws MojoFailureException {
        return lookup(ProjectMetadataReader.class);
    }

}
