/*******************************************************************************
 * Copyright (c) 2010, 2011 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.p2.util.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.tycho.p2.util.StatusTool;
import org.junit.Test;

public class StatusToolTest {
    private static final String PLUGIN_ID = "test";

    @Test
    public void testSimpleStatus() {
        IStatus status = new Status(IStatus.ERROR, PLUGIN_ID, "Simple error");
        // no extra quotes if no MultiStatus
        assertEquals("Simple error", StatusTool.collectProblems(status));
        assertSame(null, StatusTool.findException(status));
    }

    @Test
    public void testSimpleStatusWithException() {
        Exception exception = new Exception();
        IStatus status = new Status(IStatus.ERROR, PLUGIN_ID, "Simple error", exception);
        assertSame(exception, StatusTool.findException(status));
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

        assertEquals("\"Complicated error. See children for details.\": [\"Detail 1\", \"Detail 2\", \"Detail 3\"]",
                StatusTool.collectProblems(status));

        // take first exception found
        assertSame(exceptionChild2, StatusTool.findException(status));
    }

    @Test
    public void testMultiStatusWithOwnException() throws Exception {
        Throwable exceptionRoot = new Exception();
        MultiStatus root = new MultiStatus(PLUGIN_ID, 0, "Root message", exceptionRoot);

        Throwable exceptionChild = new Exception();
        IStatus child = new Status(IStatus.ERROR, PLUGIN_ID, "Child", exceptionChild);
        root.add(child);

        // prefer root exceptions over child exceptions
        assertSame(exceptionRoot, StatusTool.findException(root));
    }

    @Test
    public void testDeepNesting() {
        MultiStatus root = new MultiStatus(PLUGIN_ID, 0, "Root message", null);

        IStatus child1 = new Status(IStatus.ERROR, PLUGIN_ID, "Child 1");
        root.add(child1);

        MultiStatus child2 = new MultiStatus(PLUGIN_ID, IStatus.ERROR, "Child 2", null);
        child2.add(new Status(IStatus.ERROR, PLUGIN_ID, "Child 2.1"));
        child2.add(new Status(IStatus.ERROR, PLUGIN_ID, "Child 2.2"));
        root.add(child2);

        assertEquals("\"Root message\": [\"Child 1\", \"Child 2\": [\"Child 2.1\", \"Child 2.2\"]]",
                StatusTool.collectProblems(root));
    }

    @Test
    public void testOKChildren() {
        IStatus error = new Status(IStatus.ERROR, PLUGIN_ID, "Error message");
        IStatus warning = new Status(IStatus.WARNING, PLUGIN_ID, "Warning message");
        IStatus info = new Status(IStatus.INFO, PLUGIN_ID, "Info message");
        IStatus[] children = new IStatus[] { Status.OK_STATUS, info, Status.OK_STATUS, error, warning };
        MultiStatus status = new MultiStatus(PLUGIN_ID, 0, children, "Root message", null);

        // extra comments are not really necessary, but not harmful either
        assertEquals("\"Root message\": [\"Info message\", \"Error message\", \"Warning message\"]",
                StatusTool.collectProblems(status));
    }

    @Test
    public void testOnlyOKChildren() {
        IStatus[] children = new IStatus[] { Status.OK_STATUS, Status.OK_STATUS };
        MultiStatus status = new MultiStatus(PLUGIN_ID, 0, children, "Root message", null);

        // this has potential to throw an exception
        assertEquals("\"Root message\": []", StatusTool.collectProblems(status));
    }
}
