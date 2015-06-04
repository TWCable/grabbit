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
    def "Resourceprovider returns #resourceType for #path"() {
        given:
        def provider = new GrabbitResourceProvider()

        when:
        final resource = provider.getResource(Mock(ResourceResolver), path)

        then:
        resource.path == path
        resource.resourceType == resourceType
        resource.resourceMetadata[JobResource.JOB_EXECUTION_ID] == jobExecutionId

        where:
        path                    | resourceType                                      | jobExecutionId
        '/grabbit/job'          | GrabbitResourceProvider.GRABBIT_JOB_RESOURCE_TYPE | null
        '/grabbit/job/all.json' | GrabbitResourceProvider.GRABBIT_JOB_RESOURCE_TYPE | "all"
        '/grabbit/job/1.json'   | GrabbitResourceProvider.GRABBIT_JOB_RESOURCE_TYPE | "1"
        '/grabbit/job/1.html'   | GrabbitResourceProvider.GRABBIT_JOB_RESOURCE_TYPE | "1"
    }
}
