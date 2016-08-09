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

package com.twcable.grabbit.jcr

import spock.lang.Specification

import javax.jcr.Property
import javax.jcr.nodetype.PropertyDefinition

import static org.apache.jackrabbit.JcrConstants.JCR_LASTMODIFIED
import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE

@SuppressWarnings("GroovyAssignabilityCheck")
class JcrPropertyDecoratorSpec extends Specification {

    def "check if property is transferable"() {
        given:
        Property property = Mock(Property) {
            getName() >> propertyName
            getDefinition() >> Mock(PropertyDefinition) {
                isProtected() >> protectedFlag
            }
        }

        when:
        final propertyDecorator = new JcrPropertyDecorator(property)

        then:
        expectedOutput == propertyDecorator.isTransferable()

        where:
        propertyName        |   protectedFlag   |   expectedOutput
        JCR_LASTMODIFIED    |   true            |   false
        JCR_PRIMARYTYPE     |   false           |   true
        JCR_MIXINTYPES      |   false           |   true
        "otherProperty"     |   true            |   false
        "otherProperty"     |   false           |   true
    }
}
