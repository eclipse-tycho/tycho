/*******************************************************************************
 * Copyright (c) 2012 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.remote.testutil;

import java.net.URI;

import org.eclipse.tycho.p2.remote.IRepositoryIdManager;

public class NoopRepositoryIdManager implements IRepositoryIdManager {

    @Override
    public void addMapping(String id, URI location) {
    }

}
