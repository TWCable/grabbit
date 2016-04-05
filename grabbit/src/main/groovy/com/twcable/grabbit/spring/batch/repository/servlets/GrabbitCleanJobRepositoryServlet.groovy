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

package com.twcable.grabbit.spring.batch.repository.servlets

import com.twcable.grabbit.spring.batch.repository.services.CleanJobRepository
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.felix.scr.annotations.Reference
import org.apache.felix.scr.annotations.sling.SlingServlet
import org.apache.sling.api.SlingHttpServletRequest
import org.apache.sling.api.SlingHttpServletResponse
import org.apache.sling.api.servlets.SlingAllMethodsServlet
import org.apache.sling.api.servlets.SlingSafeMethodsServlet

import javax.annotation.Nonnull
import javax.servlet.http.HttpServletResponse

/**
 * This servlet is used for cleanup of the Grabbit's JobRepository stored under /var/grabbit/job.
 *
 * It is a handler for the {@link com.twcable.grabbit.resources.CleanJobRepositoryResource} resource.
 */
@Slf4j
@CompileStatic
@SlingServlet(methods = ['POST'], resourceTypes = ['twcable:grabbit/jobrepository/clean'])
class GrabbitCleanJobRepositoryServlet extends SlingAllMethodsServlet {

    @Reference
    CleanJobRepository cleanJobRepository

    @Override
    protected void doPost( @Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response) {
        String hoursParam = request.getParameter("hours") ?: ""
        if(!hoursParam.isInteger()) {
            log.warn "Parameter 'hours' must be an integer"
            response.status = HttpServletResponse.SC_BAD_REQUEST
            response.writer.write("Parameter 'hours' must be an integer")
            return
        }
        int hours = hoursParam.toInteger()
        Collection<String> removedJobExecutions = cleanJobRepository.cleanJobRepository(hours)
        response.status = HttpServletResponse.SC_OK
        response.writer.write ("JobExecutions and the corresponding JobInstances, StepExecutions and ExecutionContexts " +
                "were removed. JobExecutionsIds that were removed: ${removedJobExecutions}")
    }
}
