/*******************************************************************************
 * Copyright (c) 2012 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.jarcomparator.testdata;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.codehaus.plexus.component.annotations.Component;

@Component(role = JavaClass.class)
public class JavaClass {
    public Set<String> getStrings() throws IOException {
        return new LinkedHashSet<String>() {
            private static final long serialVersionUID = 711052831237148688L;
        };
    }
}
