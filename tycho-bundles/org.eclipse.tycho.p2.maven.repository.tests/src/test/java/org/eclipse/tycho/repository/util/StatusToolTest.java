/*******************************************************************************
 * Copyright (c) 2010, 2013 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP SE - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.repository.util;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.junit.Test;

public class StatusToolTest {
    private static final String PLUGIN_ID = "test";

    @Test
    public void testSimpleStatus() {
        IStatus status = new Status(IStatus.ERROR, PLUGIN_ID, "Simple error");

        assertThat(StatusTool.toLogMessage(status), is("Simple error"));
        assertThat(StatusTool.collectProblems(status), is("Simple error"));
        assertThat(StatusTool.findException(status), is(nullValue()));
    }

    @Test
    public void testSimpleStatusWithException() {
        Throwable exception = new Exception();
        IStatus status = new Status(IStatus.ERROR, PLUGIN_ID, "Simple error", exception);

        assertThat(StatusTool.findException(status), is(sameInstance(exception)));
    }

    @Test
    public void testMultiStatus() {
        Throwable exceptionChild2 = new Exception();
        Throwable exceptionChild3 = new Exception();
        IStatus child1 = new Status(IStatus.ERROR, PLUGIN_ID, "Detail 1");
        IStatus child2 = new Status(IStatus.ERROR, PLUGIN_ID, "Detail 2", exceptionChild2);
        IStatus child3 = new Status(IStatus.ERROR, PLUGIN_ID, "Detail 3", exceptionChild3);
        IStatus[] children = new IStatus[] { child1, child2, child3 };
        MultiStatus status = new MultiStatus(PLUGIN_ID, 0, children, "Complicated error. See children for details.",
                null);

        assertThat(StatusTool.toLogMessage(status),
                is("Complicated error. See children for details.:\n   Detail 1\n   Detail 2\n   Detail 3"));
        assertThat(StatusTool.collectProblems(status),
                is("Complicated error. See children for details.: [Detail 1; Detail 2; Detail 3]"));

        // use the first exception found
        assertThat(StatusTool.findException(status), is(sameInstance(exceptionChild2)));
    }

    @Test
    public void testMultiStatusWithOwnException() throws Exception {
        Throwable exceptionRoot = new Exception();
        MultiStatus root = new MultiStatus(PLUGIN_ID, 0, "Root message", exceptionRoot);

        Throwable exceptionChild = new Exception();
        IStatus child = new Status(IStatus.ERROR, PLUGIN_ID, "Child", exceptionChild);
        root.add(child);

        // prefer root exceptions over child exceptions
        assertThat(StatusTool.findException(root), is(sameInstance(exceptionRoot)));
    }

    @Test
    public void testDeepNesting() {
        MultiStatus root = new MultiStatus(PLUGIN_ID, 0, "Root message", null);

        MultiStatus child1 = new MultiStatus(PLUGIN_ID, IStatus.ERROR, "Child 1", null);
        child1.add(new Status(IStatus.ERROR, PLUGIN_ID, "Child 1.1"));
        child1.add(new Status(IStatus.ERROR, PLUGIN_ID, "Child 1.2"));
        root.add(child1);

        MultiStatus child2 = new MultiStatus(PLUGIN_ID, IStatus.ERROR, "Child 2", null);
        child2.add(new Status(IStatus.ERROR, PLUGIN_ID, "Child 2.1"));
        root.add(child2);

        assertThat(StatusTool.toLogMessage(root),
                is("Root message:\n   Child 1:\n      Child 1.1\n      Child 1.2\n   Child 2:\n      Child 2.1"));
        assertThat(StatusTool.collectProblems(root),
                is("Root message: [Child 1: [Child 1.1; Child 1.2]; Child 2: [Child 2.1]]"));
    }

    @Test
    public void testOKChildren() {
        IStatus error = new Status(IStatus.ERROR, PLUGIN_ID, "Error message");
        IStatus warning = new Status(IStatus.WARNING, PLUGIN_ID, "Warning message");
        IStatus info = new Status(IStatus.INFO, PLUGIN_ID, "Info message");
        IStatus[] children = new IStatus[] { Status.OK_STATUS, info, Status.OK_STATUS, error, warning };
        MultiStatus status = new MultiStatus(PLUGIN_ID, 0, children, "Root message", null);

        assertThat(StatusTool.collectProblems(status),
                is("Root message: [Info message; Error message; Warning message]"));
    }

    @Test
    public void testOnlyOKChildren() {
        IStatus[] children = new IStatus[] { Status.OK_STATUS, Status.OK_STATUS };
        MultiStatus status = new MultiStatus(PLUGIN_ID, 0, children, "Root message", null);

        // use case should not occur -> just make sure that it wouldn't throw an exception
        assertThat(StatusTool.toLogMessage(status), containsString("Root message"));
        assertThat(StatusTool.collectProblems(status), containsString("Root message"));
    }
}
