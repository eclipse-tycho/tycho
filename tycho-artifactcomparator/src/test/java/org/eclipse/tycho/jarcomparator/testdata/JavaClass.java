/*******************************************************************************
 * Copyright (c) 2012 Sonatype Inc. and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
