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

package com.twcable.grabbit.spring.batch.repository.services.impl

import com.twcable.grabbit.jcr.JCRUtil
import com.twcable.grabbit.spring.batch.repository.JcrJobRepositoryFactoryBean
import com.twcable.grabbit.spring.batch.repository.services.CleanJobRepository
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.felix.scr.annotations.Activate
import org.apache.felix.scr.annotations.Component
import org.apache.felix.scr.annotations.Reference
import org.apache.felix.scr.annotations.Service
import org.apache.sling.api.resource.PersistenceException
import org.apache.sling.api.resource.Resource
import org.apache.sling.api.resource.ResourceResolver
import org.apache.sling.api.resource.ResourceResolverFactory
import org.springframework.batch.core.BatchStatus
import org.springframework.context.ConfigurableApplicationContext

@Slf4j
@CompileStatic
@Component(label = "Grabbit Clean Job Repository Service", description = "Grabbit Clean Job Repository Service", immediate = true, metatype = true, enabled = true)
@Service(CleanJobRepository)
@SuppressWarnings(['GroovyUnusedDeclaration', 'GrMethodMayBeStatic'])
class DefaultCleanJobRepository implements CleanJobRepository {

    @Reference
    ResourceResolverFactory resourceResolverFactory

    @Reference
    ConfigurableApplicationContext configurableApplicationContext

    @Activate
    void activate() {
        log.info "CleanJobRepository Service activated"
    }

    @Override
    Collection<String> cleanJobRepository(int hours) {
        JcrJobRepositoryFactoryBean jobRepositoryFactoryBean = configurableApplicationContext.getBean(JcrJobRepositoryFactoryBean)

        if(!jobRepositoryFactoryBean) {
            log.error "Cannot get an instance of JcrJobRepositoryFactoryBean. Will not clean up Grabbit Jcr Job Repository"
            return []
        }

        Collection<String> jobExecutionPaths = jobRepositoryFactoryBean.jobExecutionDao.getJobExecutions([BatchStatus.FAILED, BatchStatus.COMPLETED])
        Collection<String> olderThanHoursJobExecutions = jobRepositoryFactoryBean.jobExecutionDao.getJobExecutions(hours, jobExecutionPaths)
        Collection<String> jobInstancesToRemove = jobRepositoryFactoryBean.jobInstanceDao.getJobInstancePaths(olderThanHoursJobExecutions)
        Collection<String> stepExecutionsToRemove = jobRepositoryFactoryBean.stepExecutionDao.getStepExecutionPaths(olderThanHoursJobExecutions)
        Collection<String> jobExecutionContextsToRemove = jobRepositoryFactoryBean.executionContextDao.getJobExecutionContextPaths(olderThanHoursJobExecutions)
        Collection<String> stepExecutionContextsToRemove = jobRepositoryFactoryBean.executionContextDao.getStepExecutionContextPaths(stepExecutionsToRemove)

        JCRUtil.manageResourceResolver(resourceResolverFactory) { ResourceResolver resolver ->

            log.debug "jobInstancesToRemove: $jobInstancesToRemove, size: ${jobInstancesToRemove.size()}"
            log.debug "jobExecutionsToRemove: $olderThanHoursJobExecutions, size: ${olderThanHoursJobExecutions.size()}"
            log.debug "stepExecutionsToRemove: $stepExecutionsToRemove, size: ${stepExecutionsToRemove.size()}"
            log.debug "jobExecutionContextsToRemove: $jobExecutionContextsToRemove, size: ${jobExecutionContextsToRemove.size()}"
            log.debug "stepExecutionContextsToResource: $stepExecutionContextsToRemove, size: ${stepExecutionContextsToRemove.size()}"

            log.info "Removing ${jobInstancesToRemove.size()} JobInstances"
            removeResources(jobInstancesToRemove, resolver)
            log.info "Removing ${olderThanHoursJobExecutions.size()} JobExecutions"
            removeResources(olderThanHoursJobExecutions, resolver)
            log.info "Removing ${stepExecutionsToRemove.size()} StepExecutions"
            removeResources(stepExecutionsToRemove, resolver)
            log.info "Removing ${jobExecutionContextsToRemove.size()} JobExecutionContexts"
            removeResources(jobExecutionContextsToRemove, resolver)
            log.info "Removing ${stepExecutionContextsToRemove.size()} StepExecutionContexts"
            removeResources(stepExecutionContextsToRemove, resolver)
        }

        Collection<String> removedJobExecutionIds = olderThanHoursJobExecutions.collect { it.split("/").last() }
        return removedJobExecutionIds
    }

    private void removeResources(Collection<String> resourcePathsToRemove, ResourceResolver resolver) {
        try {
            resourcePathsToRemove.each {
                Resource resourceToDelete = resolver.getResource(it)
                if(resourceToDelete) {
                    resolver.delete(resourceToDelete)
                    log.debug "Resource ${it} will be removed"
                }
                else {
                    log.warn "Resource ${it} doesn't exist. So cannot remove it"
                }
            }
            resolver.commit()
            resolver.refresh()
        }
        catch(PersistenceException e) {
            log.error "Exception while removing resources :", e
            if(resolver.hasChanges()) {
                resolver.revert()
            }
        }
    }
}
