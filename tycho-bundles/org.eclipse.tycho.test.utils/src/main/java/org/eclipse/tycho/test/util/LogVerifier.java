/*******************************************************************************
 * Copyright (c) 2013 SAP AG and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.test.util;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.tycho.core.facade.MavenLogger;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.rules.Verifier;

/**
 * Verifier to be used with JUnit's {@link Rule} annotation which provides a {@link MavenLogger}
 * instance and performs assertions on the log entries written to that logger. By default, it is
 * expected that no errors are logged. The expectations can be modified (per test) by calling the
 * <code>expect</code> methods on this instance.
 */
public class LogVerifier extends Verifier {

    private static boolean WRITE_TO_CONSOLE = false;

    private class MemoryLog implements MavenLogger {

        final StringBuilder errors = new StringBuilder();
        final StringBuilder warnings = new StringBuilder();
        final StringBuilder infos = new StringBuilder();

        public void error(String message) {
            errors.append(message);
            errors.append('\n');

            if (WRITE_TO_CONSOLE) {
                System.out.println("[ERROR] " + message);
            }
        }

        public void warn(String message) {
            warnings.append(message);
            warnings.append('\n');

            if (WRITE_TO_CONSOLE) {
                System.out.println("[WARNING] " + message);
            }
        }

        public void warn(String message, Throwable cause) {
            warn(message);
        }

        public void info(String message) {
            infos.append(message);
            infos.append('\n');

            if (WRITE_TO_CONSOLE) {
                System.out.println("[INFO] " + message);
            }
        }

        public void debug(String message) {
            if (WRITE_TO_CONSOLE) {
                System.out.println("[DEBUG] " + message);
            }
        }

        public boolean isDebugEnabled() {
            return true;
        }

        public boolean isExtendedDebugEnabled() {
            // run through message preparation code in tests
            return true;
        }
    }

    private class TestRunContext {

        MemoryLog logger;

        boolean expectNoErrors = true;
        List<Matcher<? super String>> loggedErrorsMatchers = new ArrayList<Matcher<? super String>>();

        boolean expectNoWarnings = false;
        List<Matcher<? super String>> loggedWarningsMatchers = new ArrayList<Matcher<? super String>>();

        List<Matcher<? super String>> loggedInfosMatchers = new ArrayList<Matcher<? super String>>();

        MemoryLog getInitializedLogger() {
            if (logger == null) {
                logger = new MemoryLog();
            }
            return logger;
        }

        void checkLoggedErrors() {
            if (expectNoErrors) {
                assertThat(getLoggedErrors(), is(""));
            } else {
                Matcher<String> combinedMatcher = CoreMatchers.allOf(loggedErrorsMatchers);
                assertThat(getLoggedErrors(), combinedMatcher);
            }
        }

        void checkLoggedWarnings() {
            if (expectNoWarnings) {
                assertThat(getLoggedWarnings(), is(""));
            } else {
                Matcher<String> combinedMatcher = CoreMatchers.allOf(loggedWarningsMatchers);
                assertThat(getLoggedWarnings(), combinedMatcher);
            }
        }

        void checkLoggedInfos() {
            Matcher<String> combinedMatcher = CoreMatchers.allOf(loggedInfosMatchers);
            assertThat(getLoggedInfos(), combinedMatcher);
        }

        private String getLoggedErrors() {
            if (logger == null) {
                return "";
            } else {
                return logger.errors.toString();
            }
        }

        private String getLoggedWarnings() {
            if (logger == null) {
                return "";
            } else {
                return logger.warnings.toString();
            }
        }

        private String getLoggedInfos() {
            if (logger == null) {
                return "";
            } else {
                return logger.infos.toString();
            }
        }
    }

    private TestRunContext currentContext;

    private TestRunContext getInitializedContext() {
        if (currentContext == null) {
            currentContext = new TestRunContext();
        }
        return currentContext;
    }

    public MavenLogger getLogger() {
        return getInitializedContext().getInitializedLogger();
    }

    /**
     * Verify that the logged errors contain the given string.
     */
    public void expectError(String string) {
        expectError(containsString(string));
    }

    /**
     * Verify that the logged errors match the given matcher.
     */
    public void expectError(Matcher<String> matcher) {
        TestRunContext context = getInitializedContext();

        context.expectNoErrors = false;
        context.loggedErrorsMatchers.add(matcher);
    }

    /**
     * Verify that the logged warnings contain the given string.
     */
    public void expectWarning(String string) {
        expectWarning(containsString(string));
    }

    /**
     * Verify that the logged warnings match the given matcher.
     */
    public void expectWarning(Matcher<String> matcher) {
        TestRunContext context = getInitializedContext();

        context.expectNoWarnings = false;
        context.loggedWarningsMatchers.add(matcher);
    }

    /**
     * Fails the test if warnings were logged.
     */
    public void expectNoWarnings() {
        getInitializedContext().expectNoWarnings = true;
    }

    /**
     * Verify that the info output contain the given string.
     */
    public void expectInfo(String string) {
        expectInfo(containsString(string));
    }

    /**
     * Verify that the info output match the given matcher.
     */
    public void expectInfo(Matcher<String> matcher) {
        TestRunContext context = getInitializedContext();

        context.loggedInfosMatchers.add(matcher);
    }

    @Override
    protected void verify() throws Throwable {
        TestRunContext contextAfterTest = currentContext;

        // reset configuration and errors/warnings
        currentContext = null;

        if (contextAfterTest != null) {
            contextAfterTest.checkLoggedErrors();
            contextAfterTest.checkLoggedWarnings();
            contextAfterTest.checkLoggedInfos();
        }
    }

}
