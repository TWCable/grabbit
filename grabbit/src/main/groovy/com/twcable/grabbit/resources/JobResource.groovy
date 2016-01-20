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

import groovy.transform.CompileStatic
import org.apache.sling.api.resource.ResourceResolver
import org.apache.sling.api.resource.SyntheticResource

import javax.annotation.Nonnull
import java.util.regex.Matcher

/**
 * Simple representation of a Grabbit's Job Resource provided by {@link GrabbitResourceProvider}.
 * Queried from {@link com.twcable.grabbit.client.servlets.GrabbitJobServlet}.
 */
@CompileStatic
class JobResource extends SyntheticResource {

    public static final String JOB_RESOURCE_TYPE = "twcable:grabbit/job"
    public static final String JOB_EXECUTION_ID_KEY = "twcable:grabbit:jobresource.jobExecutionId"

    JobResource(@Nonnull final ResourceResolver resourceResolver, @Nonnull final String resolutionPath) {
        super(resourceResolver, resolutionPath, JOB_RESOURCE_TYPE)
        super.resourceMetadata.put(JOB_EXECUTION_ID_KEY, getJobIdFromPath(resolutionPath))
    }


    private static String getJobIdFromPath(String path) {
        Matcher matcher = path =~ /\/grabbit\/job\/(.+)$/
        if (matcher.matches()) {
            final matchedList = matcher[0] as Collection<String>
            return matchedList[1] - ~/\..+/
        }
        else {
            return ""
        }
    }


    @Override
    String getResourceType() {
        return JOB_RESOURCE_TYPE
    }
}
