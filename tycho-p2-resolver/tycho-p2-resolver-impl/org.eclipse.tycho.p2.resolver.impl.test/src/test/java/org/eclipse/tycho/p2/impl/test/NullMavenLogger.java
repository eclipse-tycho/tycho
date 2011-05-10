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
package org.eclipse.tycho.p2.impl.test;

import org.eclipse.tycho.core.facade.MavenLogger;

public class NullMavenLogger implements MavenLogger {

    public void info(String message) {
        // TODO Auto-generated method stub

    }

    public void debug(String message) {
        // TODO Auto-generated method stub

    }

    public boolean isDebugEnabled() {
        // TODO Auto-generated method stub
        return false;
    }

}
