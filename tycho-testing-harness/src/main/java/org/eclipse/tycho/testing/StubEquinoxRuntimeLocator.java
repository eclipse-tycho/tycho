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
package org.eclipse.tycho.testing;

import org.codehaus.plexus.component.annotations.Component;
import org.eclipse.sisu.equinox.embedder.EquinoxRuntimeLocator;

@Component(role = EquinoxRuntimeLocator.class, hint = "stub")
public class StubEquinoxRuntimeLocator implements EquinoxRuntimeLocator {
    @Override
    public void locateRuntime(EquinoxRuntimeDescription description) throws Exception {
        throw new UnsupportedOperationException();
    }
}
