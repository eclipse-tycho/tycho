package org.eclipse.tycho.plugins.p2.repository;

import java.net.URISyntaxException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.tycho.p2tools.copiedfromp2.Repo2Runnable;
import org.eclipse.tycho.p2tools.copiedfromp2.RepositoryDescriptor;

/**
 * Mojo that provides the "repo2runnable" functionality described
 * <a href="https://wiki.eclipse.org/Equinox/p2/Ant_Tasks#Repo2Runnable">here</a>.
 */
@Mojo(name = "repo-to-runnable")
public class Repo2RunnableMojo extends AbstractMojo {

    @Component
    private IProvisioningAgent agent;
    @Parameter
    private boolean createFragments;
    @Parameter
    private boolean flagAsRunnable;
    @Parameter
    private String source;
    @Parameter
    private String destination;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Repo2Runnable repo2Runnable = new Repo2Runnable(agent);
        repo2Runnable.setCreateFragments(createFragments);
        repo2Runnable.setFlagAsRunnable(flagAsRunnable);
        RepositoryDescriptor source = new RepositoryDescriptor();
        try {
            source.setLocation(URIUtil.fromString(this.source));
        } catch (URISyntaxException e) {
            throw new MojoExecutionException("Invalid source: " + this.source, e);
        }
        repo2Runnable.addSource(source);
        RepositoryDescriptor destination = new RepositoryDescriptor();
        try {
            destination.setLocation(URIUtil.fromString(this.destination));
        } catch (URISyntaxException e) {
            throw new MojoExecutionException("Invalid destination: " + this.destination, e);
        }
        repo2Runnable.addDestination(destination);
    }

}
