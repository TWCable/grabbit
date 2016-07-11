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

import spock.lang.Specification

class VersionSpec extends Specification {

    def "version When Snapshot"(){
        given:
        final String ver = "Test-SNAPSHOT"
        when:
        Version version = new Version(ver)
        then:
        version.thisVersion == ver - "SNAPSHOT" + version.getTimestamp()
    }

    def "default Version"(){
        given:
        final String ver = "1.0.5"
        when:
        Version version = new Version(ver)
        then:
        version.thisVersion == ver
    }

}
