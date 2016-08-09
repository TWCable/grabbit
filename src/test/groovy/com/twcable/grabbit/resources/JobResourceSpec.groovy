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
import spock.lang.Unroll

import static com.twcable.grabbit.resources.JobResource.*

class JobResourceSpec extends Specification {

    @Unroll
    def "JobResource creation works as we expect with valid input path: #path"() {
        given:
        final jobResource = new JobResource(Mock(ResourceResolver), path)

        expect:
        jobResource.resourceMetadata[JOB_EXECUTION_ID_KEY] == "jobExecutionId"
        jobResource.getPath() == path
        jobResource.getResourceType() == JOB_RESOURCE_TYPE

        where:
        path  << [
            "/grabbit/job/jobExecutionId.json",
            "/grabbit/job/jobExecutionId.html",
            "/grabbit/job/jobExecutionId.doesntmatter",
            "/grabbit/job/jobExecutionId"
        ]
    }

    def "JobResource handles resource creation gracefully if no jobID was provided"() {
        given:
        final jobResource = new JobResource(Mock(ResourceResolver), path)

        expect:
        jobResource.resourceMetadata[JOB_EXECUTION_ID_KEY] == ""
        jobResource.getPath() == path
        jobResource.getResourceType() == JOB_RESOURCE_TYPE

        where:
        path << ["/grabbit/job/", "/grabbit/job"]
    }

}
