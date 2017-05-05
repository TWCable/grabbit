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

package com.twcable.grabbit.spring.batch.repository

import com.twcable.grabbit.jcr.JcrUtil
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.sling.api.resource.Resource
import org.apache.sling.api.resource.ResourceResolver
import org.apache.sling.api.resource.ResourceResolverFactory
import org.apache.sling.api.resource.ValueMap
import org.springframework.batch.core.DefaultJobKeyGenerator
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobInstance
import org.springframework.batch.core.JobKeyGenerator
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.repository.dao.JobInstanceDao

import javax.annotation.Nonnull

import static org.apache.jackrabbit.JcrConstants.NT_UNSTRUCTURED
import static org.apache.sling.api.resource.ResourceUtil.getOrCreateResource

/**
 * JCR Based implementation of {@link JobInstanceDao}
 * Uses {@link ResourceResolverFactory} Sling API to maintain JobInstance resources
 */
@CompileStatic
@Slf4j
class JcrGrabbitJobInstanceDao extends AbstractJcrDao implements GrabbitJobInstanceDao {

    public static final String JOB_INSTANCE_ROOT = "${ROOT_RESOURCE_NAME}/jobInstances"

    public static final String INSTANCE_ID = "id"
    public static final String KEY = "key"
    public static final String NAME = "name"
    public static final String VERSION = "version"
    public static final String PARAMETERS = "parameters"

    private ResourceResolverFactory resourceResolverFactory


    JcrGrabbitJobInstanceDao(ResourceResolverFactory rrf) {
        this.resourceResolverFactory = rrf
    }

    /**
     * Creates a new {@link JobInstance} in JCR under {@link #JOB_INSTANCE_ROOT} and returns the instance
     *
     * @see JobInstanceDao#createJobInstance(String, JobParameters)
     */
    @Override
    JobInstance createJobInstance(@Nonnull final String jobName, @Nonnull final JobParameters jobParameters) {
        if (!jobName) throw new IllegalArgumentException("jobName == null")
        if (!jobParameters) throw new IllegalArgumentException("jobParameters == null")
        if (getJobInstance(jobName, jobParameters)) throw new IllegalStateException("A JobInstance for jobName: ${jobName} must not already exist")

        JcrUtil.manageResourceResolver(resourceResolverFactory) { ResourceResolver resolver ->

            //TODO : This will need to be a "incrementing ID" after all and NOT use a UUID (for other APIs like
            //getJobInstances which require result to be sorted backwards by "id". If we use UUID, then the possibility of
            //Negative UUIDs will screw up the ordering)
            final instanceId = getNextJobInstanceId(resolver)

            JobInstance jobInstance = new JobInstance(instanceId, jobName)
            jobInstance.incrementVersion()

            JobKeyGenerator<JobParameters> jobKeyGenerator = new DefaultJobKeyGenerator()

            final paramsStrings = jobParameters.parameters.collect { key, value -> "${key}=${value.toString()}" } as String[]

            final properties = [
                (KEY)        : jobKeyGenerator.generateKey(jobParameters),
                (NAME)       : jobName,
                (INSTANCE_ID): instanceId,
                (VERSION)    : jobInstance.version,
                (PARAMETERS) : paramsStrings
            ] as Map<String, Object>

            final rootResource = getOrCreateResource(resolver, JOB_INSTANCE_ROOT, NT_UNSTRUCTURED, NT_UNSTRUCTURED, true)
            final jobInstanceResource = resolver.create(rootResource, "${instanceId}", properties)
            resolver.commit()
            log.debug "JobInstance Resource : $jobInstanceResource"
            return jobInstance
        }
    }

    /**
     * Returns a fully hydrated {@link JobInstance} given JobName and {@link JobParameters}
     * Uses a "JobKey" to identify the JobInstance
     * @see DefaultJobKeyGenerator
     *
     * The {@link JobInstance} resource is retrieved from {@link #JOB_INSTANCE_ROOT}
     *
     * @see JobInstanceDao#getJobInstance(String, JobParameters)
     */
    @Override
    JobInstance getJobInstance(@Nonnull final String jobName, @Nonnull final JobParameters jobParameters) {
        if (!jobName) throw new IllegalArgumentException("jobName == null")
        if (!jobParameters) throw new IllegalArgumentException("jobParameters == null")

        final jobKey = new DefaultJobKeyGenerator().generateKey(jobParameters)
        //Find a resource under "/jobInstances" for the jobKey above
        JcrUtil.manageResourceResolver(resourceResolverFactory) { ResourceResolver resolver ->
            final rootResource = getOrCreateResource(resolver, JOB_INSTANCE_ROOT, NT_UNSTRUCTURED, NT_UNSTRUCTURED, true)
            final jobInstanceResource = rootResource?.children?.asList()?.find { Resource resource ->
                final properties = resource.adaptTo(ValueMap)
                (properties[KEY] as String) == jobKey
            }

            if (!jobInstanceResource) return null as JobInstance

            //map that resource's id and jobName to a JobInstance instance
            final jobId = jobInstanceResource.adaptTo(ValueMap).get(INSTANCE_ID) as Long
            final JobInstance jobInstance = new JobInstance(jobId, jobName)
            return jobInstance
        }
    }

    /**
     * Returns a fully hydrated {@link JobInstance} given an instanceId
     * The {@link JobInstance} resource is retrieved from {@link #JOB_INSTANCE_ROOT}
     *
     * @see JobInstanceDao#getJobInstance(Long)
     */
    @Override
    JobInstance getJobInstance(@Nonnull final Long instanceId) {
        if (!instanceId) throw new IllegalArgumentException("instanceId == null")
        //Get the "instanceId" node from JOB_INSTANCE_ROOT
        JcrUtil.manageResourceResolver(resourceResolverFactory) { ResourceResolver resolver ->
            final rootResource = getOrCreateResource(resolver, JOB_INSTANCE_ROOT, NT_UNSTRUCTURED, NT_UNSTRUCTURED, true)
            final jobInstanceResource = rootResource?.children?.asList()?.find { Resource resource ->
                final properties = resource.adaptTo(ValueMap)
                (properties[INSTANCE_ID] as Long) == instanceId
            }

            if (!jobInstanceResource) return null as JobInstance

            final jobName = jobInstanceResource.adaptTo(ValueMap).get(NAME) as String //Get the jobName Property from that
            final jobInstance = new JobInstance(instanceId, jobName)
            return jobInstance
        }
    }

    /**
     * Returns a fully hydrated {@link JobInstance} given a {@link JobExecution}
     *
     * @see JobInstanceDao#getJobInstance(JobExecution)
     */
    @Override
    JobInstance getJobInstance(@Nonnull final JobExecution jobExecution) {
        if (!jobExecution) throw new IllegalArgumentException("jobExecution == null")
        jobExecution?.jobInstance
    }

    /**
     * Returns all {@link JobInstance}s for given jobName and a start index + count
     *
     * @see JobInstanceDao#getJobInstances(String, int, int)
     * @see org.springframework.batch.core.repository.dao.MapJobInstanceDao#getJobInstances(String, int, int)
     * @see org.springframework.batch.core.repository.dao.JdbcJobInstanceDao#getJobInstances(String, int, int)
     */
    @Override
    List<JobInstance> getJobInstances(
        @Nonnull final String jobName, @Nonnull final int start, @Nonnull final int count) {
        if (!jobName) throw new IllegalArgumentException("jobName == null")
        if (start == null) throw new IllegalArgumentException("start == null")
        if (count == null) throw new IllegalArgumentException("count == null")
        if ((start + count) < start) throw new IllegalArgumentException("start (${start}) + count (${count}) causes an int overflow")

        //Get All jobInstances in JOB_INSTANCE_ROOT with jobName property = $jobName
        //Map jobInstances nodes to JobInstance object
        JcrUtil.manageResourceResolver(resourceResolverFactory) { ResourceResolver resolver ->
            final rootResource = getOrCreateResource(resolver, JOB_INSTANCE_ROOT, NT_UNSTRUCTURED, NT_UNSTRUCTURED, true)
            final jobInstanceResource = rootResource?.children?.asList()?.findAll { Resource resource ->
                final properties = resource.adaptTo(ValueMap)
                properties[NAME] == jobName
            }
            final jobInstances = jobInstanceResource?.collect { Resource resource ->
                final properties = resource.adaptTo(ValueMap)
                final jobInstance = new JobInstance(properties[INSTANCE_ID] as Long, jobName)
                jobInstance
            }

            if (!jobInstances) return Collections.EMPTY_LIST

            if (jobInstances.size() == 1) {
                //If only one jobInstance exists, no need to sort. Just return the list
                return jobInstances
            }

            final startIndex = Math.min(start, jobInstances.size());
            final endIndex = Math.min(start + count, jobInstances.size());

            //TODO : This is going to
            final sortedInstances = jobInstances.sort(false) { a, b ->
                ((JobInstance)b).id <=> ((JobInstance)a).id
            } as List<JobInstance>
            final subList = sortedInstances[startIndex..endIndex - 1]
            return subList
        }
    }

    /**
     * Returns all JobNames under {@link #JOB_INSTANCE_ROOT}
     *
     * @see JobInstanceDao#getJobNames()
     */
    @Override
    List<String> getJobNames() {
        JcrUtil.manageResourceResolver(resourceResolverFactory) { ResourceResolver resolver ->
            final jobInstanceResources = getOrCreateResource(resolver, JOB_INSTANCE_ROOT, NT_UNSTRUCTURED, NT_UNSTRUCTURED, true)?.children?.asList()
            if (!jobInstanceResources) return Collections.EMPTY_LIST

            final List<String> jobNames = jobInstanceResources.collect { Resource r ->
                final valueMap = r.adaptTo(ValueMap)
                valueMap[NAME] as String
            } as List<String>
            return jobNames
        }
    }

    /**
     * Must be called when a new instance of JcrGrabbitJobInstanceDao is created.
     * Ensures that {@link #JOB_INSTANCE_ROOT} exists on initialization
     */
    @Override
    protected void ensureRootResource() {
        JcrUtil.manageResourceResolver(resourceResolverFactory) { ResourceResolver resolver ->
            if (!getOrCreateResource(resolver, JOB_INSTANCE_ROOT, NT_UNSTRUCTURED, NT_UNSTRUCTURED, true)) {
                //create the Root Resource
                throw new IllegalStateException("Cannot get or create RootResource for : ${JOB_INSTANCE_ROOT}")
            }
        }
    }

    /**
     * Gets next Id to be assigned to JobInstanceId. This looks for Children under {@link #JOB_INSTANCE_ROOT}
     * This is different from {@link com.twcable.grabbit.util.CryptoUtil#generateNextId()} and is only used for JobInstanceId
     * @param resolver
     */
    private static Long getNextJobInstanceId(@Nonnull ResourceResolver resolver) {
        if (!resolver) throw new IllegalArgumentException("resolver == null")

        final rootResource = resolver.getResource(JOB_INSTANCE_ROOT)

        final lastInstance = rootResource?.children?.asList()?.max { Resource resource ->
            final properties = resource.adaptTo(ValueMap)
            properties[INSTANCE_ID] as Long
        }
        final lastInstanceProperties = lastInstance?.adaptTo(ValueMap)
        Long nextId = lastInstanceProperties?.get(INSTANCE_ID) as Long
        if (!nextId) {
            nextId = 1
        } else {
            nextId += 1
        }

        log.debug "Next JobInstance Id : $nextId"
        nextId
    }

    @Override
    Collection<String> getJobInstancePaths(Collection<String> jobExecutionResourcePaths) {
        JcrUtil.manageResourceResolver(resourceResolverFactory) { ResourceResolver resolver ->
            Collection<String> jobInstancesToRemove = []
            jobExecutionResourcePaths.each { String jobExecutionResourcePath ->
                Resource jobExecutionResource = resolver.getResource(jobExecutionResourcePath)
                ValueMap props = jobExecutionResource.adaptTo(ValueMap)
                Long instanceId = props[JcrGrabbitJobExecutionDao.INSTANCE_ID] as Long
                String jobInstanceToRemoveResourcePath = "${JOB_INSTANCE_ROOT}/${instanceId}".toString()
                Resource jobInstanceToRemove = resolver.getResource(jobInstanceToRemoveResourcePath)
                if (!jobInstanceToRemove) {
                    log.info "JobInstance with id : ${instanceId} is already removed"
                } else {
                    jobInstancesToRemove.add(jobInstanceToRemoveResourcePath)
                }
            }
            return jobInstancesToRemove


        }

    }
}
