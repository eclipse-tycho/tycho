/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.target;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.RepositoryConnector;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.transfer.NoRepositoryConnectorException;

@Component(role = RepositoryConnectorFactory.class, hint = TargetArtifactRepositoryLayout.ID)
public class TargetRepositoryConnectorFactory implements RepositoryConnectorFactory {

    @Override
    public float getPriority() {
        return 0;
    }

    @Override
    public RepositoryConnector newInstance(RepositorySystemSession session, RemoteRepository repository)
            throws NoRepositoryConnectorException {
        if (TargetArtifactRepositoryLayout.ID.equals(repository.getContentType())) {
            return new TargetRepositoryConnector(repository);
        }
        throw new NoRepositoryConnectorException(repository);
    }

}
