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
package my.group.my.plugin2;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class Activator extends AbstractUIPlugin{
    public static final String PLUGIN_ID = "my.group.my.plugin2";

    public Activator(){
    }

    @Override
    public void start(final BundleContext context) throws Exception{
        super.start(context);
    }

    @Override
    public void stop(final BundleContext context) throws Exception{
        super.stop(context);
    }
}
