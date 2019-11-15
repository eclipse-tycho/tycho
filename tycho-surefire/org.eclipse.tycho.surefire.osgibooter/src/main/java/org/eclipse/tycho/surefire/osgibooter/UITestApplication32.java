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
package org.eclipse.tycho.surefire.osgibooter;

import org.eclipse.core.runtime.IPlatformRunnable;
import org.eclipse.core.runtime.Platform;

@SuppressWarnings("deprecation")
public class UITestApplication32 extends AbstractUITestApplication implements IPlatformRunnable {

    @Override
    public Object run(Object args) throws Exception {
        return run(Platform.getApplicationArgs());
    }

    @Override
    protected void runApplication(Object application, String[] args) throws Exception {
        ((IPlatformRunnable) application).run(args);
    }

}
