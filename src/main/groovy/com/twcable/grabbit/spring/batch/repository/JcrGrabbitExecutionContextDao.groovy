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

import com.twcable.grabbit.jcr.JCRUtil
import com.twcable.grabbit.util.CryptoUtil
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.sling.api.SlingException
import org.apache.sling.api.resource.*
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.repository.ExecutionContextSerializer
import org.springframework.batch.core.repository.dao.ExecutionContextDao
import org.springframework.batch.item.ExecutionContext

import javax.annotation.Nonnull

import static JcrGrabbitStepExecutionDao.ID
import static org.apache.jackrabbit.JcrConstants.NT_UNSTRUCTURED
import static org.apache.sling.api.resource.ResourceUtil.getOrCreateResource

/**
 * JCR Based implementation of {@link ExecutionContextDao}
 * Uses {@link ResourceResolverFactory} Sling API to maintain ExecutionContext resources
 */
@CompileStatic
@Slf4j
class JcrGrabbitExecutionContextDao extends AbstractJcrDao implements GrabbitExecutionContextDao {

    public static final String EXECUTION_CONTEXT_ROOT = "${ROOT_RESOURCE_NAME}/executionContexts"
    public static final String JOB_EXECUTION_CONTEXT_ROOT = "${EXECUTION_CONTEXT_ROOT}/job"
    public static final String STEP_EXECUTION_CONTEXT_ROOT = "${EXECUTION_CONTEXT_ROOT}/step"

    public static final String EXECUTION_ID = "executionId"
    public static final String EXECUTION_CONTEXT = "context"

    private ResourceResolverFactory resourceResolverFactory
    private ExecutionContextSerializer contextSerializer


    JcrGrabbitExecutionContextDao(
        @Nonnull final ResourceResolverFactory rrf, @Nonnull final ExecutionContextSerializer serializer) {
        this.resourceResolverFactory = rrf
        this.contextSerializer = serializer
    }

    /**
     * Returns {@link ExecutionContext} for the given {@link JobExecution}
     * The ExecutionContext is retrieved from {@link #JOB_EXECUTION_CONTEXT_ROOT} in JCR
     *
     * @see ExecutionContextDao#getExecutionContext(JobExecution)
     */
    @Override
    ExecutionContext getExecutionContext(@Nonnull final JobExecution jobExecution) {
        if (!jobExecution) throw new IllegalArgumentException("jobExecution == null")
        get(JOB_EXECUTION_CONTEXT_ROOT, jobExecution.id)
    }

    /**
     * Returns {@link ExecutionContext} for the given {@link StepExecution}
     * The ExecutionContext is retrieved from {@link #STEP_EXECUTION_CONTEXT_ROOT} in JCR
     *
     * @see ExecutionContextDao#getExecutionContext(StepExecution)
     */
    @Override
    ExecutionContext getExecutionContext(@Nonnull final StepExecution stepExecution) {
        if (!stepExecution) throw new IllegalArgumentException("stepExecution == null")
        get(STEP_EXECUTION_CONTEXT_ROOT, stepExecution.id)
    }

    /**
     * Saves the {@link ExecutionContext} for given {@link JobExecution}
     * The ExecutionContext is persisted under {@link #JOB_EXECUTION_CONTEXT_ROOT} in JCR
     *
     * @see ExecutionContextDao#saveExecutionContext(JobExecution)
     */
    @Override
    void saveExecutionContext(@Nonnull final JobExecution jobExecution) {
        if (!jobExecution) throw new IllegalArgumentException("jobExecution == null")
        //TODO: Persisted JobExecution Context SHOULD NOT exist
        saveOrUpdate(JOB_EXECUTION_CONTEXT_ROOT, jobExecution.id, jobExecution.executionContext)
    }

    /**
     * Saves the {@link ExecutionContext} for given {@link StepExecution}
     * The ExecutionContext is persisted under {@link #STEP_EXECUTION_CONTEXT_ROOT} in JCR
     *
     * @see ExecutionContextDao#saveExecutionContext(StepExecution)
     */
    @Override
    void saveExecutionContext(@Nonnull final StepExecution stepExecution) {
        if (!stepExecution) throw new IllegalArgumentException("stepExecution == null")
        //TODO: Persisted StepExecution Context SHOULD NOT exist
        saveOrUpdate(STEP_EXECUTION_CONTEXT_ROOT, stepExecution.id, stepExecution.executionContext)
    }

    /**
     * Saves the {@link ExecutionContext}s for given {@link StepExecution}s
     *
     * @see #saveExecutionContext(StepExecution)
     * @see #saveExecutionContext(JobExecution)
     * @see ExecutionContextDao#saveExecutionContexts(java.util.Collection)
     */
    @Override
    void saveExecutionContexts(@Nonnull final Collection<StepExecution> stepExecutions) {
        if (!stepExecutions) throw new IllegalArgumentException("stepExecutions == null or empty")
        //TODO : Persisted StepExecutions SHOULD NOT exist yet

        stepExecutions.each { StepExecution stepExecution ->
            saveExecutionContext(stepExecution)
            saveExecutionContext(stepExecution.jobExecution)
        }
    }

    /**
     * Saves the Updated {@link ExecutionContext} for given {@link JobExecution}
     * The Updated ExecutionContext is persisted in {@link #JOB_EXECUTION_CONTEXT_ROOT} in JCR
     *
     * @see ExecutionContextDao#updateExecutionContext(JobExecution)
     */
    @Override
    void updateExecutionContext(@Nonnull final JobExecution jobExecution) {
        if (!jobExecution) throw new IllegalArgumentException("jobExecution == null")
        //TODO : JobExecutionContext SHOULD exist already
        saveOrUpdate(JOB_EXECUTION_CONTEXT_ROOT, jobExecution.id, jobExecution.executionContext)
    }

    /**
     * Saves the Updated {@link ExecutionContext} for given {@link StepExecution}
     * the Updated ExecutionContext is persisted in {@link #STEP_EXECUTION_CONTEXT_ROOT} in JCR
     *
     * @see ExecutionContextDao#updateExecutionContext(StepExecution)
     */
    @Override
    void updateExecutionContext(@Nonnull final StepExecution stepExecution) {
        if (!stepExecution) throw new IllegalArgumentException("stepExecution == null")
        //TODO : StepExecutionContext SHOULD exist already
        saveOrUpdate(STEP_EXECUTION_CONTEXT_ROOT, stepExecution.id, stepExecution.executionContext)
    }

    /**
     * Must be called when a new instance of JcrGrabbitExecutionContextDao is created.
     * Ensures that {@link #STEP_EXECUTION_CONTEXT_ROOT} and {@link #JOB_EXECUTION_CONTEXT_ROOT} exist on initialization
     */
    @Override
    protected void ensureRootResource() {
        JCRUtil.manageResourceResolver(resourceResolverFactory) { ResourceResolver resolver ->
            if (!getOrCreateResource(resolver, JOB_EXECUTION_CONTEXT_ROOT, NT_UNSTRUCTURED, NT_UNSTRUCTURED, true)) {
                //create the Root Resource
                throw new IllegalStateException("Cannot get or create RootResource for : ${JOB_EXECUTION_CONTEXT_ROOT}")
            }
            if (!getOrCreateResource(resolver, STEP_EXECUTION_CONTEXT_ROOT, NT_UNSTRUCTURED, NT_UNSTRUCTURED, true)) {
                //create the Root Resource
                throw new IllegalStateException("Cannot get or create RootResource for : ${STEP_EXECUTION_CONTEXT_ROOT}")
            }
        }
    }

    /**
     * Saves or Updates given ExecutionContext for the executionId under the rootResourceName
     *
     * Checks if A resource under either {@link #STEP_EXECUTION_CONTEXT_ROOT} or {@link #JOB_EXECUTION_CONTEXT_ROOT} exists
     * which has the "executionId" property. If not present, creates a new resource with {@link com.twcable.grabbit.util.CryptoUtil#generateNextId()}.
     * Else, updates existing resource
     *
     * @see #saveExecutionContext(JobExecution)
     * @see #saveExecutionContext(StepExecution)
     * @see #saveExecutionContexts(Collection)
     * @see #updateExecutionContext(JobExecution)
     * @see #updateExecutionContext(StepExecution)
     */
    private void saveOrUpdate(@Nonnull final String rootResourceName, @Nonnull final Long executionId,
                              @Nonnull final ExecutionContext executionContext) {
        if (!rootResourceName) throw new IllegalArgumentException("rootResourceName == null")
        if (!executionId) throw new IllegalArgumentException("executionId == null")
        if (!executionContext) throw new IllegalArgumentException("executionContext == null")

        JCRUtil.manageResourceResolver(resourceResolverFactory) { ResourceResolver resolver ->
            final rootResource = getOrCreateResource(resolver, rootResourceName, NT_UNSTRUCTURED, NT_UNSTRUCTURED, true)

            final existingResource = rootResource.children.asList().find { Resource resource ->
                final properties = resource.adaptTo(ValueMap)
                (properties[EXECUTION_ID] as Long) == executionId
            }

            //Retrieve all properties from the ExecutionContext in a map
            final properties = contextAsResourceProperties(executionId, executionContext)

            if (!existingResource) {
                //Resource doesn't exist. Creating it
                log.debug "Resource for $executionId doesn't exist. Creating it..."
                final createdResource = resolver.create(rootResource, "${CryptoUtil.generateNextId()}", properties)
                resolver.commit()
                log.debug "Resource Created : ${createdResource}"
            }
            else {
                //Resource exists. Update its properties
                ModifiableValueMap map = existingResource.adaptTo(ModifiableValueMap)
                map.putAll(properties)
                resolver.commit()
                log.debug "Updated ExecutionContext : $existingResource"
            }
        }
    }

    /**
     * Returns a Map<String, Object> given the executionId and ExecutionContext
     * Uses {@link #contextSerializer} to serialize the Context to String
     *
     * @see #contextFromResourceProperties(ValueMap)
     * @see #saveOrUpdate(String, Long, ExecutionContext)
     * @see org.springframework.batch.core.repository.dao.DefaultExecutionContextSerializer#serialize(Object, OutputStream)
     */
    private Map contextAsResourceProperties(Long executionId, ExecutionContext context) {
        final contextAsMap = context.entrySet().iterator().collectEntries { key, value -> [key, value] } as Map<String, Object>

        String contextAsString
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream()
            contextSerializer.serialize(contextAsMap, out)
            contextAsString = new String(out.toByteArray(), "ISO-8859-1")
        }
        catch (IOException ioe) {
            throw new IllegalArgumentException("Could not serialize Execution Context: $context", ioe)
        }

        final properties = ([
            (ResourceResolver.PROPERTY_RESOURCE_TYPE): NT_UNSTRUCTURED,
            (EXECUTION_ID)                           : executionId,
            (EXECUTION_CONTEXT)                      : contextAsString
        ] as Map<String, Object>)
        log.debug "Properties for ExecutionContext : ${context} : ${properties}"
        properties
    }

    /**
     * Returns a Fully hydrated {@link ExecutionContext} for given rootResourceName and executionId
     *
     * @see #getExecutionContext(JobExecution)
     * @see #getExecutionContext(StepExecution)
     */
    private ExecutionContext get(@Nonnull final String rootResourceName, @Nonnull final Long executionId) {
        if (!rootResourceName) throw new IllegalArgumentException("rootResourceName == null")
        if (!executionId) throw new IllegalArgumentException("executionId == null")

        JCRUtil.manageResourceResolver(resourceResolverFactory) { ResourceResolver resolver ->
            final rootResource = getOrCreateResource(resolver, rootResourceName, NT_UNSTRUCTURED, NT_UNSTRUCTURED, true)

            //find resource from the "rootResource" where the executionId property matches "executionid"
            final contextResource = rootResource.children.asList().find { Resource resource ->
                final properties = resource.adaptTo(ValueMap)
                (properties[EXECUTION_ID] as Long) == executionId
            }
            if (!contextResource) {
                log.error "Could not find executionContext : $executionId"
                return null as ExecutionContext
            }

            final properties = contextResource.adaptTo(ValueMap)
            log.debug "Properties for ExecutionContext OF $executionId : $properties"

            final context = contextFromResourceProperties(properties)
            log.debug "Context : ${context}"
            return context
        }
    }

    /**
     * Returns a new {@link ExecutionContext} given a {@link ValueMap}
     * Uses {@link #contextSerializer} to deserialize the context from String back to Map<String, Object>
     *
     * @see #contextAsResourceProperties(Long, ExecutionContext)
     * @see #get(String, Long)
     * @see org.springframework.batch.core.repository.dao.DefaultExecutionContextSerializer#deserialize(InputStream)
     */
    private ExecutionContext contextFromResourceProperties(ValueMap properties) {
        final contextAsString = properties[EXECUTION_CONTEXT] as String

        Map<String, Object> contextAsMap
        try {
            ByteArrayInputStream input = new ByteArrayInputStream(contextAsString.getBytes("ISO-8859-1"))
            contextAsMap = contextSerializer.deserialize(input) as Map<String, Object>
        }
        catch (IOException ioe) {
            throw new IllegalArgumentException("Could not deserialize Execution Context: $contextAsString", ioe)
        }

        ExecutionContext context = new ExecutionContext()
        contextAsMap.each { key, value -> context.put(key, value) }
        context
    }

    @Override
    Collection<String> getJobExecutionContextPaths(Collection<String> jobExecutionResourcePaths) {
        JCRUtil.manageResourceResolver(resourceResolverFactory) { ResourceResolver resolver ->
            Collection<String> jobExecutionContextPathsToRemove = []
            jobExecutionResourcePaths.each { String jobExecutionResourcePath ->
                Resource jobExecutionResource = resolver.getResource(jobExecutionResourcePath)
                ValueMap props = jobExecutionResource.adaptTo(ValueMap)
                Long jobExecutionId = props[JcrGrabbitJobExecutionDao.EXECUTION_ID] as Long
                String query = "select * from [nt:unstructured] as s " +
                        "where ISDESCENDANTNODE(s,'${JOB_EXECUTION_CONTEXT_ROOT}') AND ( s.${EXECUTION_ID} = ${jobExecutionId})"
                try {
                    List<String> jobExecutionContextPaths = resolver.findResources(query, "JCR-SQL2").toList().collect { it.path }
                    jobExecutionContextPathsToRemove.addAll(jobExecutionContextPaths)
                }
                catch(SlingException | IllegalStateException e) {
                    log.error "Exception when executing Query: ${query}. \nException - ", e
                }
            }
            //There are 2 versions of Resources returned back by findResources
            //One for JcrNodeResource and one for SocialResourceWrapper
            //Hence, duplicates need to be removed
            return jobExecutionContextPathsToRemove.unique() as Collection<String>
        }
    }

    @Override
    Collection<String> getStepExecutionContextPaths(Collection<String> stepExecutionResourcePaths) {
        JCRUtil.manageResourceResolver(resourceResolverFactory) { ResourceResolver resolver ->
            Collection<String> stepExecutionContextPathsToRemove = []
            stepExecutionResourcePaths.each { String stepExecutionResourcePath ->
                Resource stepExecutionResource = resolver.getResource(stepExecutionResourcePath)
                ValueMap props = stepExecutionResource.adaptTo(ValueMap)
                Long stepExecutionId = props[ID] as Long
                String query = "select * from [nt:unstructured] as s " +
                        "where ISDESCENDANTNODE(s,'${STEP_EXECUTION_CONTEXT_ROOT}') AND ( s.${EXECUTION_ID} = ${stepExecutionId})"
                try {
                    List<String> stepExecutionContextPaths = resolver.findResources(query, "JCR-SQL2").toList().collect { it.path }
                    stepExecutionContextPathsToRemove.addAll(stepExecutionContextPaths)
                } catch (SlingException | IllegalStateException e) {
                    log.error "Exception when executing Query: ${query}. \nException - ", e
                }
            }
            //There are 2 versions of Resources returned back by findResources
            //One for JcrNodeResource and one for SocialResourceWrapper
            //Hence, duplicates need to be removed
            return stepExecutionContextPathsToRemove.unique() as Collection<String>

        }

    }
}
