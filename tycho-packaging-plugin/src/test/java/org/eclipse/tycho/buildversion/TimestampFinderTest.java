package org.eclipse.tycho.buildversion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

public class TimestampFinderTest {

    @Test
    public void testFindInString() throws Exception {
        DefaultTimestampFinder finder = new DefaultTimestampFinder();

        assertEquals(utcTimestamp(2012, 05, 06, 22, 00), finder.findInString("N201205062200"));
        assertEquals(utcTimestamp(2012, 05, 06, 22, 00), finder.findInString("I201205062200"));
        assertEquals(utcTimestamp(2012, 05, 06, 22, 00), finder.findInString("R201205062200"));

        assertEquals(utcTimestamp(2012, 05, 06, 22, 00), finder.findInString("N20120506-2200"));
        assertEquals(utcTimestamp(2012, 05, 06, 22, 00), finder.findInString("I20120506-2200"));
        assertEquals(utcTimestamp(2012, 05, 06, 22, 00), finder.findInString("R20120506-2200"));

        assertEquals(utcTimestamp(2012, 05, 06, 00, 00), finder.findInString("N20120506"));
        assertEquals(utcTimestamp(2012, 05, 06, 00, 00), finder.findInString("I20120506"));
        assertEquals(utcTimestamp(2012, 05, 06, 00, 00), finder.findInString("R20120506"));

        assertEquals(utcTimestamp(2012, 05, 06, 22, 00), finder.findInString("v201205062200"));
        assertEquals(utcTimestamp(2012, 05, 06, 22, 00), finder.findInString("v20120506-2200"));

        assertEquals(utcTimestamp(2012, 05, 06, 22, 00), finder.findInString("20120506220000"));
        assertEquals(utcTimestamp(2012, 05, 06, 22, 00), finder.findInString("20120506-220000"));

        assertEquals(utcTimestamp(2012, 05, 06, 22, 00),
                finder.findInString("scdasdcasdc.sd0320-sdva-201205062200-dscsadvj0239inacslj"));
    }

    /**
     * {@link DefaultTimestampFinder} is a {@code @Singleton} component, but it keeps a map of
     * {@link java.text.SimpleDateFormat} instances that it reuses for every parse operation.
     * {@code SimpleDateFormat} is not thread safe, so as soon as two threads parse concurrently they
     * corrupt each other's parse state.
     * <p>
     * This happens in real builds: {@code BuildQualifierAggregatorMojo} is declared
     * {@code threadSafe = true}, so with a parallel build ({@code -T 1C}) several
     * {@code eclipse-repository} projects run {@code build-qualifier-aggregator} at the same time and
     * all of them call into the single shared {@code TimestampFinder}, which fails with e.g.
     *
     * <pre>
     * java.lang.NumberFormatException: multiple points
     *     at java.text.DigitList.getDouble (DigitList.java:173)
     *     at java.text.DecimalFormat.parse (DecimalFormat.java:2303)
     *     at java.text.SimpleDateFormat.subParse (SimpleDateFormat.java:1976)
     *     at org.eclipse.tycho.buildversion.DefaultTimestampFinder.parseTimestamp (...)
     * </pre>
     */
    @Test
    @Timeout(value = 2, unit = TimeUnit.MINUTES)
    public void testFindInStringIsThreadSafe() throws Exception {
        final DefaultTimestampFinder finder = new DefaultTimestampFinder();
        final int threads = 8;
        final int iterations = 50000;

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch startSignal = new CountDownLatch(1);
            List<Future<Throwable>> results = new ArrayList<>();
            for (int thread = 0; thread < threads; thread++) {
                // every thread uses its own qualifier so that corruption also shows up as a wrong result
                final int day = 1 + thread;
                final String qualifier = String.format("v201205%02d2200", day);
                final Date expected = utcTimestamp(2012, 05, day, 22, 00);
                results.add(executor.submit(() -> {
                    startSignal.await();
                    for (int i = 0; i < iterations; i++) {
                        Date actual;
                        try {
                            actual = finder.findInString(qualifier);
                        } catch (RuntimeException e) {
                            return e;
                        }
                        if (!expected.equals(actual)) {
                            return new AssertionError(
                                    "parsing '" + qualifier + "' returned " + actual + " instead of " + expected);
                        }
                    }
                    return null;
                }));
            }
            startSignal.countDown();

            for (Future<Throwable> result : results) {
                Throwable failure = result.get();
                if (failure != null) {
                    fail("DefaultTimestampFinder is not thread safe: " + failure, failure);
                }
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private Date utcTimestamp(int year, int month, int day, int hourOfDay, int minute) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.clear();
        month--; // month in Calendar is 0-based
        calendar.set(year, month, day, hourOfDay, minute);
        return calendar.getTime();
    }

}
