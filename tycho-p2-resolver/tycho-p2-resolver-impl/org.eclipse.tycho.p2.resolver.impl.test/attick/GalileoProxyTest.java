/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package attick;

import java.io.File;
import java.net.URI;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.internal.p2.core.helpers.ServiceHelper;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryResult;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.junit.Test;
import org.eclipse.tycho.p2.impl.test.Activator;
import org.eclipse.tycho.p2.impl.test.ConsoleProgressMonitor;

@SuppressWarnings( "restriction" )
public class GalileoProxyTest
{
    private IProgressMonitor monitor = new ConsoleProgressMonitor();

    private URI location;

    public GalileoProxyTest()
        throws Exception
    {
        location = new URI( "http://download.eclipse.org/eclipse/updates/3.7milestones" );
    }

    // @Test
    // public void testArtifactRepository()
    // throws Exception
    // {
    // IArtifactRepositoryManager artifactRepositoryManager =
    // (IArtifactRepositoryManager) ServiceHelper.getService( Activator.getContext(),
    // IArtifactRepositoryManager.class.getName() );
    //
    // IArtifactRepository artifactRepository = artifactRepositoryManager.loadRepository( location, monitor );
    // SimpleArtifactRepository simple =
    // (SimpleArtifactRepository) artifactRepository.getAdapter( SimpleArtifactRepository.class );
    // new SimpleArtifactRepositoryIO().write( simple, System.out );
    //
    // System.out.println( simple );
    // }

    @Test
    public void testMetadataRepository()
        throws Exception
    {
        IProvisioningAgent agent =
            (IProvisioningAgent) ServiceHelper.getService( Activator.getContext(), IProvisioningAgent.class.getName() );

        IMetadataRepositoryManager repositoryManager =
            (IMetadataRepositoryManager) agent.getService( IMetadataRepositoryManager.class.getName() );

        IMetadataRepository repository = repositoryManager.loadRepository( location, monitor );

        File localFile = new File( "/tmp/xxx" );
        IMetadataRepository localRepository =
            repositoryManager.createRepository( localFile.toURI(), localFile.getName(),
                                                IMetadataRepositoryManager.TYPE_SIMPLE_REPOSITORY, null );
        repositoryManager.removeRepository( localFile.toURI() );

        IQueryResult<IInstallableUnit> result = repository.query( QueryUtil.ALL_UNITS, monitor );

        localRepository.addInstallableUnits( result.toUnmodifiableSet() );
    }
}
