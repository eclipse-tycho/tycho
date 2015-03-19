/*******************************************************************************
 * Copyright (c) 2015 SAP SE and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.pomless;

import org.codehaus.plexus.component.annotations.Component;
import org.sonatype.maven.polyglot.mapping.Mapping;
import org.sonatype.maven.polyglot.mapping.MappingSupport;

@Component(role = Mapping.class, hint = "tycho")
public class TychoMapping extends MappingSupport {

    public TychoMapping() {
        super("tycho");
        // can't check for META-INF/MANIFEST.MF as this is in a subfolder and maven (and tycho) assumes
        // in many places that the pom file is located in the project base dir, so we just use build.properties as a marker file
        setPomNames("build.properties");
        setAcceptLocationExtensions("build.properties");
        // make sure priority is lower than pom.xml (XmlMapping) so we can still override and use pom.xml if needed
        setPriority(-2);
    }

}
