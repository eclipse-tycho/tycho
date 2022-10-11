/*******************************************************************************
 * Copyright (c) 2013, 2021 SAP SE and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    SAP SE - initial API and implementation
 *    Christoph Läubrich - #225 MavenLogger is missing error method that accepts an exception
 *******************************************************************************/
package org.eclipse.tycho.test.util;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.logging.Logger;
import org.eclipse.tycho.core.shared.MavenLogger;
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

    private class MemoryLog implements Logger {

        final StringBuilder errors = new StringBuilder();
        final StringBuilder warnings = new StringBuilder();
        final StringBuilder infos = new StringBuilder();

        @Override
        public void error(String message) {
            errors.append(message);
            errors.append('\n');

            if (WRITE_TO_CONSOLE) {
                System.out.println("[ERROR] " + message);
            }
        }

        @Override
        public void error(String message, Throwable cause) {
            error(message);
        }

        @Override
        public void warn(String message) {
            warnings.append(message);
            warnings.append('\n');

            if (WRITE_TO_CONSOLE) {
                System.out.println("[WARNING] " + message);
            }
        }

        @Override
        public void warn(String message, Throwable cause) {
            warn(message);
        }

        @Override
        public void debug(String message, Throwable cause) {
            debug(message);
        }

        @Override
        public void info(String message) {
            infos.append(message);
            infos.append('\n');

            if (WRITE_TO_CONSOLE) {
                System.out.println("[INFO] " + message);
            }
        }

        @Override
        public void debug(String message) {
            if (WRITE_TO_CONSOLE) {
                System.out.println("[DEBUG] " + message);
            }
        }

        @Override
        public boolean isDebugEnabled() {
            return true;
        }

        @Override
        public void info(String message, Throwable throwable) {

        }

        @Override
        public boolean isInfoEnabled() {
            return true;
        }

        @Override
        public boolean isWarnEnabled() {
            return true;
        }

        @Override
        public boolean isErrorEnabled() {
            return true;
        }

        @Override
        public void fatalError(String message) {
            error(message);
        }

        @Override
        public void fatalError(String message, Throwable throwable) {
            error(message, throwable);
        }

        @Override
        public boolean isFatalErrorEnabled() {
            return true;
        }

        @Override
        public int getThreshold() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public void setThreshold(int threshold) {
            // TODO Auto-generated method stub

        }

        @Override
        public Logger getChildLogger(String name) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getName() {
            // TODO Auto-generated method stub
            return null;
        }

    }

    private class TestRunContext {

        MemoryLog logger;

        boolean expectNoErrors = true;
        List<Matcher<? super String>> loggedErrorsMatchers = new ArrayList<>();

        boolean expectNoWarnings = false;
        List<Matcher<? super String>> loggedWarningsMatchers = new ArrayList<>();

        List<Matcher<? super String>> loggedInfosMatchers = new ArrayList<>();

        MemoryLog getInitializedLogger() {
            if (logger == null) {
                logger = new MemoryLog();
            }
            return logger;
        }

        void checkLoggedErrors() {
            if (expectNoErrors) {
                assertEquals("", getLoggedErrors());
            } else {
                Matcher<String> combinedMatcher = CoreMatchers.allOf(loggedErrorsMatchers);
                assertThat(getLoggedErrors(), combinedMatcher);
            }
        }

        void checkLoggedWarnings() {
            if (expectNoWarnings) {
                assertEquals("", getLoggedWarnings());
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

    public Logger getLogger() {
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
