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

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.sling.api.resource.ResourceResolverFactory
import org.springframework.batch.core.repository.ExecutionContextSerializer
import org.springframework.batch.core.repository.dao.ExecutionContextDao
import org.springframework.batch.core.repository.dao.JobExecutionDao
import org.springframework.batch.core.repository.dao.JobInstanceDao
import org.springframework.batch.core.repository.dao.StepExecutionDao
import org.springframework.batch.core.repository.support.AbstractJobRepositoryFactoryBean
import org.springframework.batch.support.transaction.ResourcelessTransactionManager
import org.springframework.transaction.PlatformTransactionManager


/**
 * A {@link org.springframework.beans.factory.FactoryBean} that automates the creation of a
 * {@link org.springframework.batch.core.repository.support.SimpleJobRepository} using persistent JCR DAO
 * implementations. Requires user to provide a reference to {@link ResourceResolverFactory} and
 * a {@link ExecutionContextSerializer}
 */
@CompileStatic
@Slf4j
class JcrJobRepositoryFactoryBean extends AbstractJobRepositoryFactoryBean {

    private JcrJobExecutionDao jobExecutionDao

    private JcrJobInstanceDao jobInstanceDao

    private JcrStepExecutionDao stepExecutionDao

    private JcrExecutionContextDao executionContextDao

    private ResourceResolverFactory resourceResolverFactory

    private ExecutionContextSerializer executionContextSerializer

    /**
     * Create a new instance with a {@link org.springframework.batch.support.transaction.ResourcelessTransactionManager}.
     */
    public JcrJobRepositoryFactoryBean() {
        this(new ResourcelessTransactionManager())
    }


    public JcrJobRepositoryFactoryBean(PlatformTransactionManager transactionManager) {
        setTransactionManager(transactionManager)
    }


    void setResourceResolverFactory(ResourceResolverFactory resourceResolverFactory) {
        log.info "Setting RRF : ${resourceResolverFactory}"
        this.resourceResolverFactory = resourceResolverFactory
    }


    void setExecutionContextSerializer(ExecutionContextSerializer executionContextSerializer) {
        log.info "Setting ExecutionContextSerializer : ${executionContextSerializer}"
        this.executionContextSerializer = executionContextSerializer
    }


    JobExecutionDao getJobExecutionDao() {
        jobExecutionDao
    }


    JobInstanceDao getJobInstanceDao() {
        jobInstanceDao
    }


    StepExecutionDao getStepExecutionDao() {
        stepExecutionDao
    }


    ExecutionContextDao getExecutionContextDao() {
        executionContextDao
    }


    @Override
    protected JobInstanceDao createJobInstanceDao() throws Exception {
        log.info "Create JobInstance"
        jobInstanceDao = new JcrJobInstanceDao(resourceResolverFactory)
        jobInstanceDao.ensureRootResource()
        jobInstanceDao
    }


    @Override
    protected JobExecutionDao createJobExecutionDao() throws Exception {
        log.info "Create JobExecution"
        jobExecutionDao = new JcrJobExecutionDao(resourceResolverFactory)
        jobExecutionDao.ensureRootResource()
        jobExecutionDao
    }


    @Override
    protected StepExecutionDao createStepExecutionDao() throws Exception {
        log.info "Create StepExecution"
        stepExecutionDao = new JcrStepExecutionDao(resourceResolverFactory)
        stepExecutionDao.ensureRootResource()
        stepExecutionDao
    }


    @Override
    protected ExecutionContextDao createExecutionContextDao() throws Exception {
        log.info "Create ExecutionContext"
        executionContextDao = new JcrExecutionContextDao(resourceResolverFactory, executionContextSerializer)
        executionContextDao.ensureRootResource()
        executionContextDao
    }
}
