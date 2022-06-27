/*******************************************************************************
 * Copyright (c) 2022 Christoph Läubrich and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.core;

import org.osgi.framework.Version;

public interface DeclarativeServicesConfiguration {

    /**
     * Controls if the DS components annotations are made available on the compile-classpath, this
     * means no explicit import is required.
     */
    boolean isAddToClasspath();

    /**
     * Controls the declarative services specification version to use.
     */
    Version getSpecificationVersion();

    /**
     * 
     * @return the path where generated data should be placed
     */
    String getPath();

}
