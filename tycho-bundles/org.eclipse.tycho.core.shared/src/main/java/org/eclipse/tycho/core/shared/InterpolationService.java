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
package org.eclipse.tycho.core.shared;

/**
 * A Service that interpolates a given string. E.g. ${project.version} will end up e.g. in "1.0.0"
 *
 */
public interface InterpolationService {

    public String interpolate(String value);

}
