/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.helper;

import java.util.Optional;

import org.apache.maven.execution.MojoExecutionEvent;
import org.apache.maven.execution.MojoExecutionListener;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;

import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named("helper")
public class MojoExecutionHelper implements MojoExecutionListener {

    private static final ThreadLocal<MojoExecutionEvent> EVENT = new ThreadLocal<MojoExecutionEvent>();

    @Override
    public void beforeMojoExecution(MojoExecutionEvent event) throws MojoExecutionException {
        EVENT.set(event);
    }

    @Override
    public void afterMojoExecutionSuccess(MojoExecutionEvent event) throws MojoExecutionException {
        EVENT.remove();
    }

    @Override
    public void afterExecutionFailure(MojoExecutionEvent event) {
        EVENT.remove();
    }

    public static Optional<MojoExecution> getExecution() {
        MojoExecutionEvent executionEvent = EVENT.get();
        if (executionEvent == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(executionEvent.getExecution());
    }

}
