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

import com.twcable.grabbit.DateUtil
import com.twcable.grabbit.client.batch.ClientBatchJob
import com.twcable.grabbit.jcr.JCRUtil
import com.twcable.grabbit.util.CryptoUtil
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import org.apache.sling.api.resource.*
import org.springframework.batch.core.*
import org.springframework.batch.core.repository.dao.JobExecutionDao

import javax.annotation.Nonnull

import static JcrGrabbitJobInstanceDao.JOB_INSTANCE_ROOT
import static org.apache.jackrabbit.JcrConstants.NT_UNSTRUCTURED
import static org.apache.sling.api.resource.ResourceUtil.getOrCreateResource

/**
 * JCR Based implementation of {@link JobExecutionDao}
 * Uses {@link ResourceResolverFactory} Sling API to maintain JobExecution resources
 */
@CompileStatic
@Slf4j
class JcrGrabbitJobExecutionDao extends AbstractJcrDao implements GrabbitJobExecutionDao {

    public static final String JOB_EXECUTION_ROOT = "${ROOT_RESOURCE_NAME}/jobExecutions"

    public static final String EXECUTION_ID = "executionId"
    public static final String TRANSACTION_ID = "transactionId"
    public static final String INSTANCE_ID = "instanceId"
    public static final String START_TIME = "startTime"
    public static final String END_TIME = "endTime"
    public static final String STATUS = "status"
    public static final String EXIT_CODE = "exitCode"
    public static final String EXIT_MESSAGE = "exitMessage"
    public static final String CREATE_TIME = "createTime"
    public static final String LAST_UPDATED = "lastUpdated"
    public static final String JOB_NAME = "jobName"
    public static final String VERSION = "version"

    private ResourceResolverFactory resourceResolverFactory


    JcrGrabbitJobExecutionDao(ResourceResolverFactory rrf) {
        this.resourceResolverFactory = rrf
    }

    /**
     * Saves the {@link JobExecution} under {@link #JOB_EXECUTION_ROOT} in JCR
     *
     * @see JobExecutionDao#saveJobExecution(JobExecution)
     */
    @Override
    void saveJobExecution(@Nonnull final JobExecution jobExecution) {
        if (!jobExecution) throw new IllegalArgumentException("jobExecution == null")
        if (!jobExecution.jobInstance.id) throw new IllegalArgumentException("jobInstance for jobExecution must have an Id")

        //Create a new resource for the jobExecution (with the id)
        jobExecution.incrementVersion()
        JCRUtil.manageResourceResolver(resourceResolverFactory) { ResourceResolver resolver ->
            final id = CryptoUtil.generateNextId()
            jobExecution.id = id
            Resource rootResource = getOrCreateResource(resolver, JOB_EXECUTION_ROOT, NT_UNSTRUCTURED, NT_UNSTRUCTURED, true)
            final properties = getJobExecutionProperties(jobExecution) << ([
                (VERSION): jobExecution.version
            ] as Map<String, Object>)
            log.debug "Properties : ${properties}"
            final createdJobExecution = resolver.create(rootResource, "${id}", properties)
            resolver.commit()
            log.debug "JobExecution Created : $createdJobExecution"
        }
    }

    /**
     * Returns a Map<String, Object> given the JobExecution. Used as "properties map" when a new resource for given
     * JobExecution is created
     *
     * @see #saveJobExecution(JobExecution)
     * @see #updateJobExecution(JobExecution)
     */
    private static Map<String, Object> getJobExecutionProperties(JobExecution execution) {
        execution.with {
            [
                (EXECUTION_ID): id,
                (TRANSACTION_ID): execution.getJobParameters().getString(ClientBatchJob.TRANSACTION_ID),
                (INSTANCE_ID) : jobId,
                //Map implementation sets StartTime/EndTime to null .. but looks like I can't store nulls in JCR
                (START_TIME)  : startTime ? DateUtil.getISOStringFromDate(startTime) : "NULL",
                (END_TIME)    : endTime ? DateUtil.getISOStringFromDate(endTime) : "NULL",
                (STATUS)      : execution.status.toString(),
                (EXIT_CODE)   : execution.exitStatus.exitCode,
                (EXIT_MESSAGE): execution.exitStatus.exitDescription,
                (CREATE_TIME) : execution.createTime ? DateUtil.getISOStringFromDate(execution.createTime) : "NULL",
                (LAST_UPDATED): execution.lastUpdated ? DateUtil.getISOStringFromDate(execution.lastUpdated) : "NULL",
                (JOB_NAME)    : execution.jobInstance.jobName

            ] as Map<String, Object>
        }
    }

    /**
     * Updates the JobExecution resource under {@link #JOB_EXECUTION_ROOT} for given {@link JobExecution}
     *
     * @see JobExecutionDao#updateJobExecution(JobExecution)
     */
    @Override
    void updateJobExecution(@Nonnull final JobExecution jobExecution) {
        if (!jobExecution) throw new IllegalArgumentException("jobExecution == null")
        if (!jobExecution.id) throw new IllegalArgumentException("jobExecution must have an id")

        JCRUtil.manageResourceResolver(resourceResolverFactory) { ResourceResolver resolver ->
            final rootResource = getOrCreateResource(resolver, JOB_EXECUTION_ROOT, NT_UNSTRUCTURED, NT_UNSTRUCTURED, true)

            //Get the Execution with jobExecution.id from jobExecution root resource
            final jobExecutionResource = resolver.getResource(rootResource, "${jobExecution.id}")

            ModifiableValueMap map = jobExecutionResource.adaptTo(ModifiableValueMap)

            //Oh .. mutation :-)
            jobExecution.incrementVersion()

            final properties = getJobExecutionProperties(jobExecution) << ([
                (VERSION): jobExecution.version
            ] as Map<String, Object>)

            map.putAll(properties)
            resolver.commit()
            log.debug "Updated JobExecution $jobExecution with new properties : $properties"
        }
    }

    /**
     * Returns all {@link JobExecution}s for given {@link JobInstance}
     * The returned list is sorted backwards by creation time
     *
     * @see JobExecutionDao#findJobExecutions(JobInstance)
     */
    @Override
    List<JobExecution> findJobExecutions(@Nonnull final JobInstance jobInstance) {
        if (!jobInstance) throw new IllegalArgumentException("jobInstance == null")

        JCRUtil.manageResourceResolver(resourceResolverFactory) { ResourceResolver resolver ->
            final Resource jobExecutionRootResource = getOrCreateResource(resolver, JOB_EXECUTION_ROOT, NT_UNSTRUCTURED, NT_UNSTRUCTURED, true)
            List<Resource> resources = jobExecutionRootResource?.children?.asList()
            if (!resources) return Collections.EMPTY_LIST

            List<Resource> neededResources = resources.findAll { Resource resource ->
                final properties = resource.adaptTo(ValueMap)
                (properties[INSTANCE_ID] as Long) == jobInstance.id
            } as List<Resource>

            final jobExecutions = neededResources.collect { Resource resource ->
                //map jobExecution resources to the JobExecution POJO
                final properties = resource.adaptTo(ValueMap)
                final jobExecution = mapJobExecution(resolver, properties, jobInstance)
                jobExecution
            } as List<JobExecution>
            log.debug "JobExecutions : $jobExecutions"

            final sorted = jobExecutions.sort(false) { a, b ->
                ((JobExecution)b).createTime <=> ((JobExecution)a).createTime
            }

            return sorted
        }
    }

    /**
     * Returns JobParameters using the given instanceId from {@link JcrGrabbitJobInstanceDao#JOB_INSTANCE_ROOT}
     *
     * @see #mapJobExecution(ResourceResolver, ValueMap, JobInstance)
     */
    private static JobParameters getJobParameters(ResourceResolver resolver, Long instanceId) {
        final jobInstanceRoot = getOrCreateResource(resolver, JOB_INSTANCE_ROOT, NT_UNSTRUCTURED, NT_UNSTRUCTURED, true)
        final instanceResource = resolver.getResource(jobInstanceRoot, "${instanceId}")

        final properties = instanceResource.adaptTo(ValueMap)
        final params = properties[JcrGrabbitJobInstanceDao.PARAMETERS] as String[]

        final paramsMap = params?.collectEntries { String entry ->
            final pair = entry.split("=")
            if (pair.size() == 1) [pair[0], new JobParameter("No Value")]
            else [pair[0], getJobParameter(pair[1])]
        }

        paramsMap ? new JobParameters(paramsMap) : new JobParameters()
    }

    /**
     * Gets a single {@link JobParameter} instance given a String value
     * <b> Only Supports Long, String and Doubles right now. Does NOT support Date </b>
     *
     * @see #getJobParameters(ResourceResolver, Long)
     */
    private static JobParameter getJobParameter(String value) {
        if (StringUtils.isNumeric(value)) {
            return new JobParameter(Long.valueOf(value))
        }
        else {
            try {
                final doubleValue = Double.parseDouble(value)
                return new JobParameter(doubleValue)
            }
            catch (NumberFormatException ignored) { //not a double

                //Falling back to String
                return new JobParameter(value)

                //TODO : Implement Conversion of String to Date
            }
        }
    }

    /**
     * Returns the Latest {@link JobExecution} created for given {@link JobInstance}
     *
     * @see JobExecutionDao#getLastJobExecution(JobInstance)
     */
    @Override
    JobExecution getLastJobExecution(@Nonnull final JobInstance jobInstance) {
        if (!jobInstance) throw new IllegalArgumentException("jobInstance == null")

        final jobExecutions = findJobExecutions(jobInstance)
        if (!jobExecutions) return null

        //JobExecutions should already return sorted by most recent first
        jobExecutions.first()
    }

    /**
     * Returns all {@link JobExecution}s for given JobName that are running
     *
     * @see JobExecution#isRunning()
     * @see JobExecutionDao#findRunningJobExecutions(String)
     */
    @Override
    Set<JobExecution> findRunningJobExecutions(@Nonnull final String jobName) {
        if (!jobName) throw new IllegalArgumentException("jobName == null")

        JCRUtil.manageResourceResolver(resourceResolverFactory) { ResourceResolver resolver ->
            final jobExecutionRoot = getOrCreateResource(resolver, JOB_EXECUTION_ROOT, NT_UNSTRUCTURED, NT_UNSTRUCTURED, true)

            //find jobExecution root, then find all jobExecutions, then find the jobexecution
            //whose, jobInstance.jobName == jobName
            final jobExecutionResources = jobExecutionRoot?.children?.asList()?.findAll { Resource resource ->
                final properties = resource.adaptTo(ValueMap)
                properties[JOB_NAME] == jobName
            }
            if (!jobExecutionResources) return Collections.EMPTY_SET

            //Map to JobExecution POJO
            final jobExecutions = jobExecutionResources.collect { Resource resource ->
                final properties = resource.adaptTo(ValueMap)
                final jobExecution = mapJobExecution(resolver, properties)
                jobExecution
            }

            //Find all jobExecution.isRunning()
            final runningExecutions = jobExecutions.findAll { it.running } as Set<JobExecution>
            log.debug "All running executions : $runningExecutions"
            return runningExecutions
        }
    }

    /**
     * Returns a fully hydrated {@link JobExecution} for given executionId
     *
     * @see JobExecutionDao#getJobExecution(Long)
     */
    @Override
    JobExecution getJobExecution(@Nonnull final Long executionId) {
        if (!executionId) throw new IllegalArgumentException("executionId == null")

        //find jobExecution root, then find jobExecution with node name : "jobExecution${executionId}"
        JCRUtil.manageResourceResolver(resourceResolverFactory) { ResourceResolver resolver ->
            final jobExecutionRoot = getOrCreateResource(resolver, JOB_EXECUTION_ROOT, NT_UNSTRUCTURED, NT_UNSTRUCTURED, true)
            final jobExecutionResource = jobExecutionRoot?.children?.asList()?.find { Resource resource ->
                final properties = resource.adaptTo(ValueMap)
                (properties[EXECUTION_ID] as Long) == executionId
            }
            if (!jobExecutionResource) return null as JobExecution

            final properties = jobExecutionResource.adaptTo(ValueMap)
            final jobExecution = mapJobExecution(resolver, properties)
            log.debug "JobExecution : $jobExecution"
            return jobExecution
        }
    }

    /**
     * Maps given {@link ValueMap} to a new {@link JobExecution} instance
     *
     * @see #getJobExecution(Long)
     * @see #findJobExecutions(JobInstance)
     * @see #findRunningJobExecutions(String)
     */
    private
    static JobExecution mapJobExecution(ResourceResolver resolver, ValueMap properties, JobInstance jobInstance = null) {

        final id = properties[EXECUTION_ID] as Long

        GrabbitJobExecution jobExecution
        if (!jobInstance) {
            final JobInstance instance = new JobInstance(properties[INSTANCE_ID] as Long, properties[JOB_NAME] as String)
            final jobParameters = getJobParameters(resolver, properties[INSTANCE_ID] as Long)
            jobExecution = new GrabbitJobExecution(instance, id, jobParameters)
        }
        else {
            final jobParameters = getJobParameters(resolver, jobInstance.id)
            jobExecution = new GrabbitJobExecution(jobInstance, id, jobParameters)
        }

        jobExecution.startTime = getDate(properties[START_TIME] as String)
        jobExecution.endTime = getDate(properties[END_TIME] as String)
        jobExecution.status = BatchStatus.valueOf(properties[STATUS] as String)
        jobExecution.exitStatus = new ExitStatus(properties[EXIT_CODE] as String, properties[EXIT_MESSAGE] as String)
        jobExecution.createTime = getDate(properties[CREATE_TIME] as String)
        jobExecution.lastUpdated = getDate(properties[LAST_UPDATED] as String)
        jobExecution.version = properties[VERSION] as Integer ?: 0
        jobExecution.transactionID = properties[TRANSACTION_ID] as Long ?: 0L

        jobExecution
    }


    private static Date getDate(String value) {
        value == "NULL" ? null : DateUtil.getDateFromISOString(value)
    }

    /**
     * Because it may be possible that the status of a JobExecution is updated
     * while running, the following method will synchronize only the status and
     * version fields.
     *
     * @see JobExecutionDao#synchronizeStatus(JobExecution)
     */
    @Override
    void synchronizeStatus(@Nonnull final JobExecution jobExecution) {
        if (!jobExecution) throw new IllegalArgumentException("jobExecution == null")

        final savedJobExecution = getJobExecution(jobExecution.id)
        if (savedJobExecution.version != jobExecution.version) {
            jobExecution.upgradeStatus(savedJobExecution.status)
            jobExecution.version = savedJobExecution.version
            log.info "Synchronized Status of ${jobExecution} with Saved Execution : ${savedJobExecution}"
        }
    }

    /**
     * Must be called when a new instance of JcrGrabbitJobExecutionDao is created.
     * Ensures that {@link #JOB_EXECUTION_ROOT} exists on initialization
     */
    @Override
    protected void ensureRootResource() {
        JCRUtil.manageResourceResolver(resourceResolverFactory) { ResourceResolver resolver ->
            if (!getOrCreateResource(resolver, JOB_EXECUTION_ROOT, NT_UNSTRUCTURED, NT_UNSTRUCTURED, true)) {
                //create the Root Resource
                throw new IllegalStateException("Cannot get or create RootResource for : ${JOB_EXECUTION_ROOT}")
            }
            if (!getOrCreateResource(resolver, JOB_INSTANCE_ROOT, NT_UNSTRUCTURED, NT_UNSTRUCTURED, true)) {
                //create the Root Resource
                throw new IllegalStateException("Cannot get or create RootResource for : ${JOB_INSTANCE_ROOT}")
            }
        }
    }

    @Override
    Collection<String> getJobExecutions(Collection<BatchStatus> batchStatuses) {
        String statusPredicate = batchStatuses.collect { "s.status = '${it}'" }.join(' or ')
        JCRUtil.manageResourceResolver(resourceResolverFactory) { ResourceResolver resolver ->
            String jobExecutionsQuery = "select * from [nt:unstructured] as s where " +
                    "ISDESCENDANTNODE(s,'${JOB_EXECUTION_ROOT}') AND ( ${statusPredicate} )"
            Collection<String> jobExecutions = resolver.findResources(jobExecutionsQuery, "JCR-SQL2")
                    .toList()
                    .collect { it.path }
                    .unique() as Collection<String>
            log.debug "JobExecutions: $jobExecutions, size: ${jobExecutions.size()}"
            return jobExecutions
        }

    }

    @Override
    Collection<String> getJobExecutions(int hours, Collection<String> jobExecutions) {
        JCRUtil.manageResourceResolver(resourceResolverFactory) { ResourceResolver resolver ->
            //Create a Date object that is "hours" ago from now
            Calendar olderThanHours = Calendar.getInstance()
            log.info "Current time: ${olderThanHours.time}"
            olderThanHours.add(Calendar.HOUR, -hours)
            log.info "Hours ${hours} .. OlderThanHours Time: ${olderThanHours.time}"

            //Find all resources that are older than "olderThanHours" Date
            Collection<String> olderResourcePaths = jobExecutions.findAll { String resourcePath ->
                Resource resource = resolver.getResource(resourcePath)
                ValueMap props = resource.adaptTo(ValueMap)
                String dateInIsoString = props[END_TIME] as String
                Date endTimeDate = DateUtil.getDateFromISOString(dateInIsoString)
                olderThanHours.time.compareTo(endTimeDate) > 0
            } as Collection<String>
            log.debug "JobExecutionsOlder than ${hours} hours: $olderResourcePaths , length: ${olderResourcePaths.size()}"
            return olderResourcePaths

        }

    }
}
