/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 * 
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 * Christoph Läubrich - initial API and implementation
 * 
 *******************************************************************************/
package org.eclipse.tycho.pomless;

import javax.annotation.Priority;

import org.apache.maven.project.ProjectBuilder;
import org.codehaus.plexus.component.annotations.Component;
import org.sonatype.maven.polyglot.TeslaProjectBuilder;

@Component(role = ProjectBuilder.class)
@Priority(10)
//Workaround for https://github.com/takari/polyglot-maven/pull/256
public class TychoTeslaProjectBuilder extends TeslaProjectBuilder {

}
