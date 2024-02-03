/*******************************************************************************
 * Copyright (c) 2022 Red Hat Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.tycho.target;

import java.io.File;
import java.net.URI;

import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.eclipse.tycho.p2.repository.GAV;

/**
 * This object tries to capture main settings of the <code>target</code> parameter of the
 * <code>target-platform-configuration</code> to facilitate edition.
 */
public class TargetParameterObject {

    public GAV target;
    public File file;
    public URI uri;
    public PlexusConfiguration location;
}
