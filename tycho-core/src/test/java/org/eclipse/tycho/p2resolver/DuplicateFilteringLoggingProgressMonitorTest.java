package org.eclipse.tycho.p2resolver;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.tycho.core.shared.DuplicateFilteringLoggingProgressMonitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DuplicateFilteringLoggingProgressMonitorTest {

    private DuplicateFilteringLoggingProgressMonitor monitor;

    @BeforeEach
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
