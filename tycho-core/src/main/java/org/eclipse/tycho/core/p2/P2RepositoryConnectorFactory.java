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
package org.eclipse.tycho.core.p2;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.RepositoryConnector;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.transfer.NoRepositoryConnectorException;

@Component(role = RepositoryConnectorFactory.class, hint = "p2")
public class P2RepositoryConnectorFactory implements RepositoryConnectorFactory {

    @Override
    public float getPriority() {
        return 0;
    }

    @Override
    public RepositoryConnector newInstance(RepositorySystemSession session, RemoteRepository repository)
            throws NoRepositoryConnectorException {
        if (P2ArtifactRepositoryLayout.ID.equals(repository.getContentType())) {
            return new P2RepositoryConnector(repository);
        }
        throw new NoRepositoryConnectorException(repository);
    }

}
