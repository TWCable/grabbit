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
import com.twcable.grabbit.jcr.JCRUtil
import com.twcable.grabbit.util.CryptoUtil
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.sling.api.SlingException
import org.apache.sling.api.resource.ModifiableValueMap
import org.apache.sling.api.resource.Resource
import org.apache.sling.api.resource.ResourceResolver
import org.apache.sling.api.resource.ResourceResolverFactory
import org.apache.sling.api.resource.ValueMap
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.repository.dao.StepExecutionDao

import javax.annotation.Nonnull

import static JcrGrabbitJobExecutionDao.EXECUTION_ID
import static org.apache.jackrabbit.JcrConstants.NT_UNSTRUCTURED
import static org.apache.sling.api.resource.ResourceUtil.getOrCreateResource

/**
 * JCR Based implementation of {@link StepExecutionDao}
 * Uses {@link ResourceResolverFactory} Sling API to maintain StepExecution resources
 */
@CompileStatic
@Slf4j
public class JcrGrabbitStepExecutionDao extends AbstractJcrDao implements GrabbitStepExecutionDao {

    public static final String STEP_EXECUTION_ROOT = "${ROOT_RESOURCE_NAME}/stepExecutions"

    public static final String ID = "id"
    public static final String NAME = "name"
    public static final String JOB_EXECUTION_ID = "jobExecutionId"
    public static final String START_TIME = "startTime"
    public static final String END_TIME = "endTime"
    public static final String STATUS = "status"
    public static final String COMMIT_COUNT = "commitCount"
    public static final String READ_COUNT = "readCount"
    public static final String FILTER_COUNT = "filterCount"
    public static final String WRITE_COUNT = "writeCount"
    public static final String EXIT_CODE = "exitCode"
    public static final String EXIT_MESSAGE = "exitMessage"
    public static final String READ_SKIP_COUNT = "readSkipCount"
    public static final String WRITE_SKIP_COUNT = "writeSkipCount"
    public static final String PROCESS_SKIP_COUNT = "processSkipCount"
    public static final String ROLL_BACK_COUNT = "rollbackCount"
    public static final String LAST_UPDATED = "lastUpdated"
    public static final String VERSION = "version"

    private ResourceResolverFactory resourceResolverFactory


    JcrGrabbitStepExecutionDao(ResourceResolverFactory rrf) {
        this.resourceResolverFactory = rrf
    }

    /**
     * Saves the {@link StepExecution} under {@link #STEP_EXECUTION_ROOT} in JCR
     *
     * @see StepExecutionDao#saveStepExecution(StepExecution)
     */
    @Override
    void saveStepExecution(@Nonnull final StepExecution stepExecution) {
        if (!stepExecution) throw new IllegalArgumentException("stepExecution == null")
        if (stepExecution.id != null) throw new IllegalStateException("stepExecution.id must be null")

        final id = CryptoUtil.generateNextId()
        stepExecution.id = id
        saveOrUpdate("${id}", stepExecution)
    }

    /**
     * Saves the {@link StepExecution}s under {@link #STEP_EXECUTION_ROOT} in JCR
     *
     * @see #saveStepExecution(StepExecution)
     * @see StepExecutionDao#saveStepExecutions(Collection)
     */
    @Override
    void saveStepExecutions(@Nonnull final Collection<StepExecution> stepExecutions) {
        if (!stepExecutions) throw new IllegalArgumentException("stepExecutions == null or empty")
        if (stepExecutions.any { it.id != null }) throw new IllegalStateException("All stepExecution Ids must be null")
        stepExecutions.each { saveStepExecution(it) }
    }

    /**
     * Updates the {@link StepExecution} resource under {@link #STEP_EXECUTION_ROOT}
     *
     * @see StepExecutionDao#updateStepExecution(StepExecution)
     */
    @Override
    void updateStepExecution(@Nonnull final StepExecution stepExecution) {
        if (!stepExecution) throw new IllegalArgumentException("stepExecution == null")
        if (!stepExecution.id) throw new IllegalArgumentException("stepExecution == null")
        if (!stepExecution.jobExecutionId) throw new IllegalStateException("stepExecution.jobExecutionId == null")
        if (!stepExecution.stepName) throw new IllegalStateException("stepExecution.stepName == null")
        if (!stepExecution.startTime) throw new IllegalStateException("stepExecution.startTime == null")
        if (!stepExecution.status) throw new IllegalStateException("stepExecution.status == null")
        saveOrUpdate("${stepExecution.id}", stepExecution)
    }

    /**
     * Returns a fully hydrated {@link StepExecution} instance given its parent {@link JobExecution} and a stepExecutionId
     *
     * @see StepExecutionDao#getStepExecution(JobExecution, Long)
     */
    @Override
    StepExecution getStepExecution(@Nonnull final JobExecution jobExecution, @Nonnull final Long stepExecutionId) {
        if (!jobExecution) throw new IllegalArgumentException("jobExecution == null")
        if (!stepExecutionId) throw new IllegalArgumentException("stepExecutionId == null")

        JCRUtil.manageResourceResolver(resourceResolverFactory) { ResourceResolver resolver ->
            final Resource rootResource = getOrCreateResource(resolver, STEP_EXECUTION_ROOT, NT_UNSTRUCTURED, NT_UNSTRUCTURED, true)
            final stepExecutionResource = rootResource?.children?.asList()?.find { Resource resource ->
                final properties = resource.adaptTo(ValueMap)
                (properties[ID] as Long) == stepExecutionId
            }
            if (!stepExecutionResource) return null as StepExecution

            final properties = stepExecutionResource.adaptTo(ValueMap)

            final stepExecution = mapStepExecution(properties, jobExecution)
            return stepExecution
        }
    }

    /**
     * Hydrates all the {@link JobExecution#stepExecutions} by calling {@link JobExecution#addStepExecutions(List)}
     *
     * Retrieves all {@link StepExecution}s under {@link #STEP_EXECUTION_ROOT} for given {@link JobExecution#id}
     *
     * @see StepExecutionDao#addStepExecutions(JobExecution)
     */
    @Override
    void addStepExecutions(@Nonnull final JobExecution jobExecution) {
        if (!jobExecution) throw new IllegalArgumentException("jobExecution == null")
        //TODO : JOB_EXECUTION's ID must exist

        JCRUtil.manageResourceResolver(resourceResolverFactory) { ResourceResolver resolver ->
            final rootResource = getOrCreateResource(resolver, STEP_EXECUTION_ROOT, NT_UNSTRUCTURED, NT_UNSTRUCTURED, true)
            final stepExecutionResources = rootResource.children.asList().findAll { Resource resource ->
                final properties = resource.adaptTo(ValueMap)
                (properties[JOB_EXECUTION_ID] as Long) == jobExecution.id
            }

            final stepExecutions = stepExecutionResources.collect { Resource resource ->
                final properties = resource.adaptTo(ValueMap)
                mapStepExecution(properties, jobExecution)
            } as List<StepExecution>

            log.debug "StepExecutions for the JobExecution : ${jobExecution} ==> $stepExecutions"

            //THIS IS VERY WEIRD!
            jobExecution.addStepExecutions(stepExecutions)
        }
    }

    /**
     * Saves or Updates given StepExecution for the executionId under {@link #STEP_EXECUTION_ROOT} in JCR
     *
     * Checks if A resource for ExecutionId exists which has the "executionId" resource name.
     * If not present, creates a new resource with executionId as name.
     * Else, updates existing resource
     *
     * @see #saveStepExecution(StepExecution)
     * @see #updateStepExecution(StepExecution)
     */
    private void saveOrUpdate(@Nonnull final String executionId, @Nonnull final StepExecution execution) {
        if (!executionId) throw new IllegalArgumentException("executionId == null")
        if (!execution) throw new IllegalArgumentException("execution == null")

        JCRUtil.manageResourceResolver(resourceResolverFactory) { ResourceResolver resolver ->
            final rootResource = getOrCreateResource(resolver, STEP_EXECUTION_ROOT, NT_UNSTRUCTURED, NT_UNSTRUCTURED, true)
            final existingResource = resolver.getResource(rootResource, executionId)

            //Retrieve all properties from the ExecutionContext in a map
            final properties = getStepExecutionProperties(execution) << ([
                (ResourceResolver.PROPERTY_RESOURCE_TYPE): NT_UNSTRUCTURED
            ] as Map<String, Object>)

            if (!existingResource) {
                //Resource doesn't exist. Creating it

                final createdResource = resolver.create(rootResource, executionId, properties)
                resolver.commit()
                log.debug "Resource Created : ${createdResource}"
            }
            else {
                //Resource exists. Update its properties

                ModifiableValueMap map = existingResource.adaptTo(ModifiableValueMap)
                map.putAll(properties)
                resolver.commit()
                log.debug "Updated StepExecution : $existingResource"
                execution.incrementVersion()
            }
        }
    }

    /**
     * Returns a Map<String, Object> given a {@link StepExecution}. Used as Properties for a StepExecution resource in JCR
     *
     * @see #saveOrUpdate(String, StepExecution)
     */
    private static Map<String, Object> getStepExecutionProperties(StepExecution execution) {
        execution.incrementVersion()
        [
            (ID)                : execution.id,
            (NAME)              : execution.stepName,
            (JOB_EXECUTION_ID)  : execution.jobExecutionId,
            //Map implementation sets StartTime/EndTime to null .. but looks like I can't store nulls in JCR
            (START_TIME)        : execution.startTime ? DateUtil.getISOStringFromDate(execution.startTime) : "NULL",
            (END_TIME)          : execution.endTime ? DateUtil.getISOStringFromDate(execution.endTime) : "NULL",
            (STATUS)            : execution.status.toString(),
            (COMMIT_COUNT)      : execution.commitCount,
            (READ_COUNT)        : execution.readCount,
            (FILTER_COUNT)      : execution.filterCount,
            (WRITE_COUNT)       : execution.writeCount,
            (EXIT_CODE)         : execution.exitStatus.exitCode,
            (EXIT_MESSAGE)      : execution.exitStatus.exitDescription,
            (READ_SKIP_COUNT)   : execution.readSkipCount,
            (WRITE_SKIP_COUNT)  : execution.writeSkipCount,
            (PROCESS_SKIP_COUNT): execution.processSkipCount,
            (ROLL_BACK_COUNT)   : execution.rollbackCount,
            (LAST_UPDATED)      : execution.lastUpdated ? DateUtil.getISOStringFromDate(execution.lastUpdated) : "NULL",
            (VERSION)           : execution.version

        ] as Map<String, Object>
    }

    /**
     * Maps a {@link ValueMap} and {@link JobExecution} to a new {@link StepExecution}
     * @param properties
     * @param jobExecution
     * @return
     */
    private static StepExecution mapStepExecution(ValueMap properties, JobExecution jobExecution) {

        StepExecution stepExecution = new StepExecution(properties[NAME] as String, jobExecution, properties[ID] as Long)
        stepExecution.startTime = getDate(properties[START_TIME] as String)
        stepExecution.endTime = getDate(properties[END_TIME] as String)
        stepExecution.status = BatchStatus.valueOf(properties[STATUS] as String)
        stepExecution.commitCount = properties[COMMIT_COUNT] as Integer ?: 0
        stepExecution.readCount = properties[READ_COUNT] as Integer ?: 0
        stepExecution.filterCount = properties[FILTER_COUNT] as Integer ?: 0
        stepExecution.writeCount = properties[WRITE_COUNT] as Integer ?: 0
        stepExecution.exitStatus = new ExitStatus(properties[EXIT_CODE] as String, properties[EXIT_MESSAGE] as String)
        stepExecution.readSkipCount = properties[READ_SKIP_COUNT] as Integer ?: 0
        stepExecution.writeSkipCount = properties[WRITE_SKIP_COUNT] as Integer ?: 0
        stepExecution.processSkipCount = properties[PROCESS_SKIP_COUNT] as Integer ?: 0
        stepExecution.rollbackCount = properties[ROLL_BACK_COUNT] as Integer ?: 0
        stepExecution.lastUpdated = getDate(properties[LAST_UPDATED] as String)
        stepExecution.version = properties[VERSION] as Integer ?: 0
        log.debug "Mapped StepExecution : $stepExecution"
        stepExecution
    }


    private static Date getDate(String value) {
        value == "NULL" ? null : DateUtil.getDateFromISOString(value)
    }

    /**
     * Must be called when a new instance of JcrGrabbitStepExecutionDao is created.
     * Ensures that {@link #STEP_EXECUTION_ROOT} exists on initialization
     */
    @Override
    protected void ensureRootResource() {
        JCRUtil.manageResourceResolver(resourceResolverFactory) { ResourceResolver resolver ->
            //ResourceUtil.getOrCreateResource()
            if (!getOrCreateResource(resolver, STEP_EXECUTION_ROOT, NT_UNSTRUCTURED, NT_UNSTRUCTURED, true)) {
                //create the Root Resource
                throw new IllegalStateException("Cannot get or create RootResource for : ${STEP_EXECUTION_ROOT}")
            }
            if (!getOrCreateResource(resolver, JcrGrabbitJobExecutionDao.JOB_EXECUTION_ROOT, NT_UNSTRUCTURED, NT_UNSTRUCTURED, true)) {
                //create the Root Resource
                throw new IllegalStateException("Cannot get or create RootResource for : ${JcrGrabbitJobExecutionDao.JOB_EXECUTION_ROOT}")
            }
        }
    }

    @Override
    Collection<String> getStepExecutionPaths(Collection<String> jobExecutionResourcePaths) {
        JCRUtil.manageResourceResolver(resourceResolverFactory) { ResourceResolver resolver ->
            Collection<String> stepExecutionsToRemove = []
            jobExecutionResourcePaths.each { String jobExecutionResourcePath ->
                Resource jobExecutionResource = resolver.getResource(jobExecutionResourcePath)
                ValueMap props = jobExecutionResource.adaptTo(ValueMap)
                Long jobExecutionId = props[EXECUTION_ID] as Long
                String query = "select * from [nt:unstructured] as s " +
                        "where ISDESCENDANTNODE(s,'${STEP_EXECUTION_ROOT}') AND ( s.${JOB_EXECUTION_ID} = ${jobExecutionId})"
                try {
                    List<String> stepExecutions = resolver.findResources(query, "JCR-SQL2").toList().collect { it.path }
                    stepExecutionsToRemove.addAll(stepExecutions)
                } catch (SlingException | IllegalStateException e) {
                    log.error "Exception when executing Query: ${query}. \nException - ", e
                }
            }
            //There are 2 versions of Resources returned back by findResources
            //One for JcrNodeResource and one for SocialResourceWrapper
            //Hence, duplicates need to be removed
            return stepExecutionsToRemove.unique() as Collection<String>
        }
    }
}
