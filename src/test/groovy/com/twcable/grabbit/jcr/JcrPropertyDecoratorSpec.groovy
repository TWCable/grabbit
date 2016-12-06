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

import javax.jcr.Property as JCRProperty
import javax.jcr.nodetype.PropertyDefinition
import spock.lang.Specification
import spock.lang.Unroll


import static javax.jcr.PropertyType.BINARY
import static javax.jcr.PropertyType.REFERENCE
import static javax.jcr.PropertyType.WEAKREFERENCE
import static org.apache.jackrabbit.JcrConstants.JCR_LASTMODIFIED
import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE

@SuppressWarnings("GroovyAssignabilityCheck")
class JCRPropertyDecoratorSpec extends Specification {

    @Unroll
    def "check if property is transferable"() {
        given:
        JCRProperty property = Mock(JCRProperty) {
            getName() >> propertyName
            getDefinition() >> Mock(PropertyDefinition) {
                isProtected() >> protectedFlag
            }
        }
        final nodeOwner = Mock(JCRNodeDecorator) {
            isAuthorizableType() >> authorizableType
            isACType() >> acType
        }

        when:
        final propertyDecorator = new JCRPropertyDecorator(property, nodeOwner)

        then:
        expectedOutput == propertyDecorator.isTransferable()

        where:
        propertyName        | protectedFlag | expectedOutput | authorizableType  | acType
        JCR_LASTMODIFIED    | true          | false          | false             | false
        JCR_PRIMARYTYPE     | false         | true           | false             | false
        JCR_MIXINTYPES      | false         | true           | false             | false
        'otherProperty'     | true          | false          | false             | false
        'otherProperty'     | false         | true           | false             | false
        'protectedProperty' | true          | true           | true              | false
        'protectedProperty' | true          | true           | true              | false
        'rep:privileges'    | true          | true           | false             | true

    }

    def "isReferenceType()"() {
        when:
        final JCRProperty property = Mock(JCRProperty) {
            getType() >> type
        }

        final jcrPropertyDecorator = new JCRPropertyDecorator(property, Mock(JCRNodeDecorator))

        then:
        jcrPropertyDecorator.isReferenceType() == expectedValue

        where:
        type            | expectedValue
        REFERENCE       | true
        WEAKREFERENCE   | true
        BINARY          | false
    }
}
