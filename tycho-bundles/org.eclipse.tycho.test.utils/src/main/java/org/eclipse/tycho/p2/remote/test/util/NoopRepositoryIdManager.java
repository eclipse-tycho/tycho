/*******************************************************************************
 * Copyright (c) 2012 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.remote.test.util;

import java.net.URI;

import org.eclipse.tycho.p2.remote.IRepositoryIdManager;

public class NoopRepositoryIdManager implements IRepositoryIdManager {

    public void addMapping(String id, URI location) {
    }

}
