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
package com.twcable.grabbit.security

import spock.lang.Specification

class AuthorizablePrincipalSpec extends Specification {

    def "Can get the name of an AuthorizablePrincipal"() {
        given:
        final authorizablePrincipal = new AuthorizablePrincipal('principalName')

        expect:
        authorizablePrincipal.getName() == 'principalName'
    }

    def "One AuthorizablePrincipal is equal to another"() {
        expect:
        assert   new AuthorizablePrincipal('one').equals(new AuthorizablePrincipal('one'))
        assert !(new AuthorizablePrincipal('two').equals(new AuthorizablePrincipal('three')))
        assert !(new AuthorizablePrincipal('two').equals(null))
    }
}
