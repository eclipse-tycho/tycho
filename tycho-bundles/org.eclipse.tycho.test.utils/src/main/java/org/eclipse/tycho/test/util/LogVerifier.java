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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.containsString;

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
 * <code>expect</code> on this instance.
 */
public class LogVerifier extends Verifier {

    private class MemoryLog implements MavenLogger {

        final StringBuilder errors = new StringBuilder();
        final StringBuilder warnings = new StringBuilder();

        public void error(String message) {
            errors.append(message);
            errors.append('\n');
        }

        public void warn(String message) {
            warnings.append(message);
            warnings.append('\n');
        }

        public void warn(String message, Throwable cause) {
            warn(message);
        }

        public void info(String message) {
        }

        public void debug(String message) {
        }

        public boolean isDebugEnabled() {
            return true;
        }

        public boolean isExtendedDebugEnabled() {
            return true;
        }
    }

    private class TestRunContext {

        MemoryLog logger;

        boolean expectNoErrors = true;
        List<Matcher<? extends String>> loggedErrorsMatchers = new ArrayList<Matcher<? extends String>>();
        boolean expectNoWarnings = false;
        List<Matcher<? extends String>> loggedWarningsMatchers = new ArrayList<Matcher<? extends String>>();

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

    @Override
    protected void verify() throws Throwable {
        TestRunContext contextAfterTest = currentContext;

        // reset configuration and errors/warnings
        currentContext = null;

        if (contextAfterTest != null) {
            contextAfterTest.checkLoggedErrors();
            contextAfterTest.checkLoggedWarnings();
        }
    }

}
