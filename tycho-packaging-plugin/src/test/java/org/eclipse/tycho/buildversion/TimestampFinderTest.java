package org.eclipse.tycho.buildversion;

import static org.junit.Assert.assertEquals;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.junit.Test;

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

    private Date utcTimestamp(int year, int month, int day, int hourOfDay, int minute) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.clear();
        month--; // month in Calendar is 0-based
        calendar.set(year, month, day, hourOfDay, minute);
        return calendar.getTime();
    }

}
