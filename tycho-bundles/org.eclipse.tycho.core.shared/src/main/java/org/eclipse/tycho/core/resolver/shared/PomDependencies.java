/*******************************************************************************
 * Copyright (c) 2020 Christoph Läubrich and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core.resolver.shared;

public enum PomDependencies {
    /**
     * pom dependencies are ignored
     */
    ignore,
    /**
     * pom dependencies are considered if the are already valid osgi artifacts. p2 metadata may be
     * generated if missing
     */
    consider,
    /**
     * pom dependencies are used and wrapped into OSGi bundles if necessary. p2 metadata may be
     * generated if missing.
     */
    wrapAsBundle;
}
