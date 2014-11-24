package com.twc.grabbit.spring.batch.repository

import groovy.transform.CompileStatic
import org.springframework.batch.core.explore.JobExplorer
import org.springframework.batch.core.explore.support.AbstractJobExplorerFactoryBean
import org.springframework.batch.core.explore.support.SimpleJobExplorer
import org.springframework.batch.core.repository.dao.ExecutionContextDao
import org.springframework.batch.core.repository.dao.JobExecutionDao
import org.springframework.batch.core.repository.dao.JobInstanceDao
import org.springframework.batch.core.repository.dao.StepExecutionDao
import org.springframework.beans.factory.InitializingBean
import org.springframework.util.Assert

/**
 * A {@link org.springframework.beans.factory.FactoryBean} that automates the creation of a
 * {@link SimpleJobExplorer} using JCR DAO implementations.
 *
 * @see JcrJobRepositoryFactoryBean
 */
@CompileStatic
public class JcrJobExplorerFactoryBean extends AbstractJobExplorerFactoryBean implements InitializingBean {

    private JcrJobRepositoryFactoryBean repositoryFactory

    public void setRepositoryFactory(JcrJobRepositoryFactoryBean repositoryFactory) {
        this.repositoryFactory = repositoryFactory;
    }

    JcrJobExplorerFactoryBean(JcrJobRepositoryFactoryBean repositoryFactory) {
        this.repositoryFactory = repositoryFactory
    }

    JcrJobExplorerFactoryBean() { }

    @Override
    protected JobInstanceDao createJobInstanceDao() throws Exception {
        repositoryFactory.jobInstanceDao
    }

    @Override
    protected JobExecutionDao createJobExecutionDao() throws Exception {
        repositoryFactory.jobExecutionDao
    }

    @Override
    protected StepExecutionDao createStepExecutionDao() throws Exception {
        repositoryFactory.stepExecutionDao
    }

    @Override
    protected ExecutionContextDao createExecutionContextDao() throws Exception {
        repositoryFactory.executionContextDao
    }

    @Override
    public JobExplorer getObject() throws Exception {
        return new SimpleJobExplorer(createJobInstanceDao(), createJobExecutionDao(), createStepExecutionDao(),
                createExecutionContextDao());

    }

    @Override
    void afterPropertiesSet() throws Exception {
        Assert.state(repositoryFactory != null, "A JcrJobRepositoryFactoryBean must be provided")
        repositoryFactory.afterPropertiesSet()
    }
}
