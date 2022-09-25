/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho;

import java.net.URI;
import java.util.stream.Stream;

public interface IRepositoryIdManager {

    String SERVICE_NAME = IRepositoryIdManager.class.getName();

    void addMapping(String id, URI location);

    URI getEffectiveLocation(URI location);

    URI getEffectiveLocationAndPrepareLoad(URI location);

    Stream<MavenRepositoryLocation> getKnownMavenRepositoryLocations();

}
