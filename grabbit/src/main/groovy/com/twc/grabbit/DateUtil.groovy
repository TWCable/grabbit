package com.twc.grabbit

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

@CompileStatic
@Slf4j
class DateUtil {

    public static Calendar getCalendarFromISOString(String calendarAsISO8601) {
        DateTime dateTime = new DateTime(calendarAsISO8601)
        dateTime.toGregorianCalendar()
    }

    public static String getISOStringFromCalendar(Calendar calendar) {
        DateTime dateTime = new DateTime(calendar)
        final String calendarAsISO8601 = ISODateTimeFormat.dateTime().print(dateTime)
        return calendarAsISO8601

    }
}
