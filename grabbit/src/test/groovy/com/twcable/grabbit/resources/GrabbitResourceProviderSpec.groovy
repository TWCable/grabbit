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

package com.twcable.grabbit.resources

import org.apache.sling.api.resource.ResourceResolver
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

@Subject(GrabbitResourceProvider)
class GrabbitResourceProviderSpec extends Specification {

    @Unroll
    def "Can provide a job resource from path #path"() {
        given:
        GrabbitResourceProvider provider = new GrabbitResourceProvider()

        when:
        final resource = provider.getResource(Mock(ResourceResolver), path)

        then:
        resource instanceof JobResource

        where:
        path << [
            '/grabbit/job/all.json',
            '/grabbit/job/1.json',
            '/grabbit/job/1.html',
            '/grabbit/job/1',
            '/grabbit/job',
            '/grabbit/job/'
        ]
    }


    @Unroll
    def "Can provide a transaction resource from path #path"() {
        given:
        GrabbitResourceProvider provider = new GrabbitResourceProvider()

        when:
        final resource = provider.getResource(Mock(ResourceResolver), path)

        then:
        resource instanceof TransactionResource

        where:
        path << [
                '/grabbit/transaction/123.json',
                '/grabbit/transaction/123.html',
                '/grabbit/transaction/123',
                '/grabbit/transaction',
                '/grabbit/transaction/'
        ]
    }


    def "Can provide a content resource from path #path"() {
        given:
        GrabbitResourceProvider provider = new GrabbitResourceProvider()

        when:
        final resource = provider.getResource(Mock(ResourceResolver), path)

        then:
        resource instanceof ContentResource

        where:
        path << [
            '/grabbit/content',
            '/grabbit/content/'
        ]
    }
}
