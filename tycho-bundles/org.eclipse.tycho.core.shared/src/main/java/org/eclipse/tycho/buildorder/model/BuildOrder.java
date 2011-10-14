/*******************************************************************************
 * Copyright (c) 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.buildorder.model;

public interface BuildOrder {

    // namespaces of entities also known to p2 are defined with the same values (see org.eclipse.equinox.spi.p2.publisher.PublisherHelper)
    String NAMESPACE_BUNDLE = "osgi.bundle";
    String NAMESPACE_FEATURE = "org.eclipse.update.feature";
    String NAMESPACE_PACKAGE = "java.package";

    interface Export {
        public String getNamespace();

        public String getId();

        // no version, because multiple versions of same ID can't be built in same reactor 
    }

    interface Import {
        public boolean isSatisfiedBy(Export export);
    }
}
