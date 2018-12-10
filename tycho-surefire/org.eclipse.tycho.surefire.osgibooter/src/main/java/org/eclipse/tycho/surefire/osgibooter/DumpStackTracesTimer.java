/*******************************************************************************
 * Copyright (c) 2018 Simeon Andreev and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Simeon Andreev - initial API and implementation
 *******************************************************************************/
package org.eclipse.tycho.surefire.osgibooter;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

/**
 * Starts a task which will dump stack trace information after some time. Mostly a copy of
 * org.eclipse.test.EclipseTestRunner.startStackDumpTimeoutTimer().
 * 
 * Necessary to know whether and where tests are hanging if a timeout occurred during tests.
 * 
 * @author sandreev
 *
 */
public class DumpStackTracesTimer extends TimerTask {

    /**
     * SECONDS_BEFORE_TIMEOUT_BUFFER is the time we allow ourselves to take stack traces delay
     * "SECONDS_BETWEEN_DUMPS", then do it again. On current build machine, it takes about 30
     * seconds to do all that, so 2 minutes should be sufficient time allowed for most machines.
     * Though, should increase, say, if we increase the "time between dumps" to a minute or more.
     */
    private static final int SECONDS_BEFORE_TIMEOUT_BUFFER = 120;

    /**
     * SECONDS_BETWEEN_DUMPS is the time we wait from first to second dump of stack trace. In most
     * cases, this should suffice to determine if still busy doing something, or, hung, or waiting
     * for user input.
     */
    private static final int SECONDS_BETWEEN_DUMPS = 5;

    private final String timeoutArg;

    private DumpStackTracesTimer(String timeoutArg) {
        this.timeoutArg = timeoutArg;
    }

    /**
     * Starts a timer that dumps interesting debugging information shortly before the given timeout
     * expires.
     *
     * @param timeoutArg
     *            the -timeout argument from the command line
     */
    static void startStackDumpTimeoutTimer(String timeoutArg) {
        try {
            /*
             * The delay (in ms) is the sum of - the expected time it took for launching the current
             * VM and reaching this method - the time it will take to run the garbage collection and
             * dump all the infos (twice)
             */
            int delay = SECONDS_BEFORE_TIMEOUT_BUFFER * 1000;

            int timeout = Integer.parseInt(timeoutArg) - delay;
            String time0 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(new Date());
            logInfo("starting DumpStackTracesTimer with timeout=" + timeout + " at " + time0);
            if (timeout > 0) {
                new Timer("DumpStackTracesTimer", true).schedule(new DumpStackTracesTimer(timeoutArg), timeout);
            } else {
                logWarning("DumpStackTracesTimer argument error: '-timeout " + timeoutArg
                        + "' was too short to accommodate time delay required (" + delay + ").");
            }
        } catch (NumberFormatException e) {
            logError("Error parsing timeout argument: " + timeoutArg, e);
        }
    }

    @Override
    public void run() {
        dump(0);
        try {
            Thread.sleep(SECONDS_BETWEEN_DUMPS * 1000);
        } catch (InterruptedException e) {
            // continue
        }
        dump(SECONDS_BETWEEN_DUMPS);
    }

    private void dump(final int num) {
        // Time elapsed time to do each dump, so we'll
        // know if/when we get too close to the 2
        // minutes we allow
        long start = System.currentTimeMillis();

        logStackTraces(num);

        // Elapsed time in milliseconds
        long elapsedTimeMillis = System.currentTimeMillis() - start;

        // Print in seconds
        float elapsedTimeSec = elapsedTimeMillis / 1000F;
        logInfo("Seconds to do dump " + num + ": " + elapsedTimeSec);
    }

    private void logStackTraces(int num) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        dumpStackTraces(num, new PrintStream(outputStream));
        logWarning(outputStream.toString());
    }

    private void dumpStackTraces(int num, PrintStream out) {
        out.println("DumpStackTracesTimer almost reached timeout '" + timeoutArg + "'.");
        out.println("totalMemory:            " + Runtime.getRuntime().totalMemory());
        out.println("freeMemory (before GC): " + Runtime.getRuntime().freeMemory());
        out.flush(); // https://bugs.eclipse.org/bugs/show_bug.cgi?id=420258: flush aggressively, we could be low on memory
        System.gc();
        out.println("freeMemory (after GC):  " + Runtime.getRuntime().freeMemory());
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(new Date());
        out.println("Thread dump " + num + " at " + time + ":");
        out.flush();
        Map<Thread, StackTraceElement[]> stackTraces = Thread.getAllStackTraces();
        for (Entry<Thread, StackTraceElement[]> entry : stackTraces.entrySet()) {
            String name = entry.getKey().getName();
            StackTraceElement[] stack = entry.getValue();
            Exception exception = new Exception("ThreadDump for thread \"" + name + "\"");
            exception.setStackTrace(stack);
            exception.printStackTrace(out);
        }
        out.flush();
    }

    private static void logInfo(String message) {
        IStatus warningStatus = new Status(IStatus.INFO, Activator.PLUGIN_ID, message);
        log(warningStatus);
    }

    private static void logWarning(String message) {
        IStatus warningStatus = new Status(IStatus.WARNING, Activator.PLUGIN_ID, message);
        log(warningStatus);
    }

    private static void logError(String message, Exception exception) {
        IStatus errorStatus = new Status(IStatus.ERROR, Activator.PLUGIN_ID, message, exception);
        log(errorStatus);
    }

    private static void log(IStatus warningStatus) {
        ILog log = Platform.getLog(Platform.getBundle(Activator.PLUGIN_ID));
        log.log(warningStatus);
    }
}
