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
package org.eclipse.tycho.security.storage;

import javax.crypto.spec.PBEKeySpec;

import org.eclipse.equinox.security.storage.provider.IPreferencesContainer;
import org.eclipse.equinox.security.storage.provider.PasswordProvider;

public class TychoPasswordProvider extends PasswordProvider {

    @Override
    public PBEKeySpec getPassword(IPreferencesContainer container, int passwordType) {
        return new PBEKeySpec("secret".toCharArray());
    }

}
