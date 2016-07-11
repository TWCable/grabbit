/**
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

// **************************************************************************
//
// VERSION CLASS
//
// **************************************************************************


class Version {
    String originalVersion
    String thisVersion
    String status
    Date buildTime


    Version(String versionValue) {
        buildTime = new Date()
        originalVersion = versionValue
        if (originalVersion.endsWith('-SNAPSHOT')) {
            status = 'integration'
            thisVersion = originalVersion - 'SNAPSHOT' + getTimestamp()
        }
        else {
            status = 'release'
            thisVersion = versionValue
        }
    }


    @SuppressWarnings("UnnecessaryQualifiedReference")
    String getTimestamp() {
        // Convert local file timestamp to UTC
        def format = new java.text.SimpleDateFormat('yyyyMMddHHmmss')
        format.setCalendar(Calendar.getInstance(TimeZone.getTimeZone('UTC')));
        return format.format(buildTime)
    }


    String toString() {
        originalVersion
    }


    String getBintrayVersion() {
        thisVersion
    }

}
