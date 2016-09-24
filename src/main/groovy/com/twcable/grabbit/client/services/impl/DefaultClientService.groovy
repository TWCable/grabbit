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

package com.twcable.grabbit.client.services.impl

import com.twcable.grabbit.GrabbitConfiguration
import com.twcable.grabbit.GrabbitConfiguration.PathConfiguration
import com.twcable.grabbit.client.batch.ClientBatchJob
import com.twcable.grabbit.client.services.ClientService
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.felix.scr.annotations.Activate
import org.apache.felix.scr.annotations.Component
import org.apache.felix.scr.annotations.Reference
import org.apache.felix.scr.annotations.Service
import org.apache.sling.jcr.api.SlingRepository
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobInstance
import org.springframework.batch.core.explore.JobExplorer
import org.springframework.context.ConfigurableApplicationContext

@Slf4j
@CompileStatic
@Component(label = "Grabbit Client Service", description = "Grabbit Client Service", immediate = true, metatype = true, enabled = true)
@Service(ClientService)
@SuppressWarnings(['GroovyUnusedDeclaration', 'GrMethodMayBeStatic'])
class DefaultClientService implements ClientService {

    @Reference(bind = 'setSlingRepository')
    SlingRepository slingRepository

    @Reference(bind = 'setConfigurableApplicationContext')
    ConfigurableApplicationContext configurableApplicationContext


    @Activate
    void activate() {
        log.info "Activate\n\n"
    }


    @Override
    Collection<Long> initiateGrab(GrabbitConfiguration configuration, String clientUsername) {

        Collection<Long> jobExecutionIds = []

        for (PathConfiguration pathConfig : configuration.pathConfigurations) {
            try {
                final clientBatchJob = new ClientBatchJob.ServerBuilder(configurableApplicationContext)
                    .andServer(configuration.serverScheme, configuration.serverHost, configuration.serverPort)
                    .andCredentials(clientUsername, configuration.serverUsername, configuration.serverPassword)
                    .andClientJobExecutions(fetchAllClientJobExecutions())
                    .withTransactionID(configuration.transactionID)
                    .andConfiguration(pathConfig)
                    .build()
                final Long currentJobExecutionId = clientBatchJob.start()
                jobExecutionIds << currentJobExecutionId
            }
            catch (Exception e) {
                log.error "Error while requesting a content sync for current Path: ${[pathConfig.path]}", e
                throw new IllegalStateException("Failed to initiate job for path: ${pathConfig.path}")
            }
        }
        return jobExecutionIds

    }


    private List<JobExecution> fetchAllClientJobExecutions() {
        final explorer = configurableApplicationContext.getBean("clientJobExplorer", JobExplorer)
        final instances = explorer.getJobInstances("clientJob", 0, Integer.MAX_VALUE - 1) ?: [] as List<JobInstance>
        final executions = instances.collect { explorer.getJobExecutions(it) }.flatten() as List<JobExecution>
        executions
    }
}
