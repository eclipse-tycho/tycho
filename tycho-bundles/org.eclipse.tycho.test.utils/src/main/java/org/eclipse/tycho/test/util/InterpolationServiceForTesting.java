/*******************************************************************************
 * Copyright (c) 2014 Bachmann electronics GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Bachmann electronics GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.util;

import org.eclipse.tycho.core.shared.InterpolationService;

public class InterpolationServiceForTesting implements InterpolationService {

    @Override
    public String interpolate(String value) {
        return value;
    }

}
