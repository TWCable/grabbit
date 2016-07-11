/*
 * Copyright 2015 Time Warner Cable, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.twcable.grabbit

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


    public static Date getDateFromISOString(String dateAsISO8601) {
        DateTime dateTime = new DateTime(dateAsISO8601)
        dateTime.toDate()
    }


    public static String getISOStringFromDate(Date date) {
        DateTime dateTime = new DateTime(date)
        final String dateAsISO8601 = ISODateTimeFormat.dateTime().print(dateTime)
        return dateAsISO8601
    }

}
