/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.tools.director;

import org.eclipse.equinox.internal.p2.director.app.DirectorApplication;
import org.eclipse.tycho.p2.tools.director.facade.DirectorApplicationWrapper;

@SuppressWarnings("restriction")
public final class DirectorApplicationWrapperImpl implements DirectorApplicationWrapper {

    public Object run(String[] args) {
        return new DirectorApplication().run(args);
    }

}
