/*******************************************************************************
 * Copyright (c) 2012 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
