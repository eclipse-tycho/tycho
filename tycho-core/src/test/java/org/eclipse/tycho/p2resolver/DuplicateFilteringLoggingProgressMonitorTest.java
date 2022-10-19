package org.eclipse.tycho.p2resolver;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.tycho.repository.util.DuplicateFilteringLoggingProgressMonitor;
import org.junit.Before;
import org.junit.Test;

public class DuplicateFilteringLoggingProgressMonitorTest {

    private DuplicateFilteringLoggingProgressMonitor monitor;

    @Before
    public void before() {
        monitor = new DuplicateFilteringLoggingProgressMonitor(null);
    }

    @Test
    public void testIgnoreNonFetchingMessages() {
        assertFalse(monitor.suppressOutputOf("Some other message"));
        assertFalse(monitor.suppressOutputOf("Some other message"));
    }

    @Test
    public void testRemoveDuplicatesFromBugReport() {
        String message = "[INFO] Fetching org.eclipse.xtend.ide.common_2.19.0.v20190626-0355.jar from https://ci-staging.eclipse.org/xtext//job/xtext-xtend/job/master/lastStableBuild/artifact/build/p2-repository/plugins/ (567.95kB)";
        assertFalse(monitor.suppressOutputOf(message));
        assertTrue(monitor.suppressOutputOf(message));
    }

    @Test
    public void testRemoveMultipleDuplicates() {
        String message = "Fetching X from Y";
        assertFalse(monitor.suppressOutputOf(message));
        assertTrue(monitor.suppressOutputOf(message));
        assertTrue(monitor.suppressOutputOf(message));
        assertTrue(monitor.suppressOutputOf(message));
    }

}
