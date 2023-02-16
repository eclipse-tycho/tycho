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

import java.lang.reflect.Field;

import javax.annotation.Priority;

import org.apache.maven.project.DefaultProjectBuilder;
import org.apache.maven.project.ProjectBuilder;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.sonatype.maven.polyglot.TeslaProjectBuilder;

@Component(role = ProjectBuilder.class)
@Priority(10)
public class TychoTeslaProjectBuilder extends TeslaProjectBuilder implements Initializable, ProjectBuilder {

    @Override
    public void initialize() throws InitializationException {
        try {
            // Workaround for https://github.com/takari/polyglot-maven/pull/256
        	// and https://github.com/takari/polyglot-maven/pull/257
            Field field = DefaultProjectBuilder.class.getDeclaredField("modelCacheFactory");
            field.setAccessible(true);
            Object value = field.get(this);
            if (value == null) {
                Class<?> clazz = getClass().getClassLoader()
                        .loadClass("org.apache.maven.repository.internal.DefaultModelCacheFactory");
                Object instance = clazz.getConstructor().newInstance();
                field.set(this, instance);
            }
        } catch (Exception e) {
        }
    }
}
