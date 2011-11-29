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
package org.eclipse.tycho.testing;

import java.io.File;
import java.util.List;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.sisu.equinox.embedder.EquinoxRuntimeLocator;

@Component(role = EquinoxRuntimeLocator.class, hint = "stub")
public class StubEquinoxRuntimeLocator implements EquinoxRuntimeLocator {

    public List<File> getRuntimeLocations() throws Exception {
        throw new UnsupportedOperationException();
    }

    public List<String> getSystemPackagesExtra() {
        throw new UnsupportedOperationException();
    }
}
