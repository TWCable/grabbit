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

import groovy.transform.CompileStatic

import javax.annotation.Nonnull
import java.security.Principal


@CompileStatic
class AuthorizablePrincipal implements Principal {

    final private String principalName

    AuthorizablePrincipal(@Nonnull final String principalName) {
        this.principalName = principalName
    }

    @Override
    boolean equals(Object other) {
        if(other == null) return false
        return this.hashCode() == other.hashCode()
    }

    @Override
    String getName() {
        return principalName
    }

    @Override
    int hashCode() {
        return principalName.hashCode()
    }
}
