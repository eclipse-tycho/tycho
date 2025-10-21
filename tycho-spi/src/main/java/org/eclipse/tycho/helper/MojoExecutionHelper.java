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

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.maven.execution.MojoExecutionEvent;
import org.apache.maven.execution.MojoExecutionListener;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.logging.Logger;

@Named("helper")
public class MojoExecutionHelper implements MojoExecutionListener {
    private static boolean printMemoryInfo = Boolean.getBoolean("tycho.printMemory");
    private static boolean gc = Boolean.getBoolean("tycho.printMemory.gc");

    private static final ThreadLocal<MojoExecutionEvent> EVENT = new ThreadLocal<MojoExecutionEvent>();

    @Inject
    private Logger logger;

    @Override
    public void beforeMojoExecution(MojoExecutionEvent event) throws MojoExecutionException {
        EVENT.set(event);
        if (printMemoryInfo) {
            printMemory("Before", gc, event);
        }
    }

    @Override
    public void afterMojoExecutionSuccess(MojoExecutionEvent event) throws MojoExecutionException {
        EVENT.remove();
        if (printMemoryInfo) {
            printMemory("After", gc, event);
        }
    }

    @Override
    public void afterExecutionFailure(MojoExecutionEvent event) {
        EVENT.remove();
        if (printMemoryInfo) {
            printMemory("After", gc, event);
        }
    }

    public static Optional<MojoExecution> getExecution() {
        MojoExecutionEvent executionEvent = EVENT.get();
        if (executionEvent == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(executionEvent.getExecution());
    }

    private void printMemory(String state, boolean gc, MojoExecutionEvent evt) {
        Runtime runtime = Runtime.getRuntime();
        double usedBefore;
        if (gc) {
            usedBefore = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
            System.gc();
        } else {
            usedBefore = 0;
        }
        double totalMemory = runtime.totalMemory() / 1024 / 1024;
        double freeMemory = runtime.freeMemory() / 1024 / 1024;
        double usedMemory = totalMemory - freeMemory;
        StringBuilder builder = new StringBuilder();
        builder.append("---- ");
        builder.append(state);
        builder.append(" ");
        builder.append(evt.getProject().getId());
        builder.append(" - ");
        MojoExecution execution = evt.getExecution();
        builder.append(execution.getMojoDescriptor().getId());
        builder.append(" [");
        builder.append(execution.getExecutionId());
        builder.append("] ----");
        builder.append(System.lineSeparator());
        builder.append("Total Memory: ");
        builder.append(String.format("%.2f", totalMemory));
        builder.append("mb");
        builder.append(System.lineSeparator());
        builder.append("Free Memory:  ");
        builder.append(String.format("%.2f", freeMemory));
        builder.append("mb");
        builder.append(System.lineSeparator());
        builder.append("Used Memory:  ");
        builder.append(String.format("%.2f", usedMemory));
        builder.append("mb");
        builder.append(System.lineSeparator());
        double reclaimed = usedBefore - usedMemory;
        if (reclaimed > 0) {
            builder.append("GC Reclaimed: ");
            builder.append(String.format("%.2f", reclaimed));
            builder.append("mb");
            builder.append(System.lineSeparator());
        }
        logger.info(builder.toString());
    }

}
