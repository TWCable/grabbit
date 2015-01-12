package com.twc.grabbit.client.services.impl

import com.twc.grabbit.GrabbitConfiguration
import com.twc.grabbit.GrabbitConfiguration.PathConfiguration
import com.twc.grabbit.client.batch.ClientBatchJob
import com.twc.grabbit.client.services.ClientService
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
@SuppressWarnings(['GroovyUnusedDeclaration','GrMethodMayBeStatic'])
class DefaultClientService implements ClientService {

    public static final int BATCH_SIZE = 1000

    @Reference(bind='setSlingRepository')
    SlingRepository slingRepository

    @Reference(bind='setConfigurableApplicationContext')
    ConfigurableApplicationContext configurableApplicationContext


    @Activate
    void activate() {
        log.info "Activate\n\n"
    }

    @Override
    Collection<Long> initiateGrab(GrabbitConfiguration configuration) {

        Collection<Long> jobExecutionIds = []

        //Only fetch ClientJobExecutions if deltaContent is true in the GrabbitConfiguration
        List<JobExecution> clientJobExecutions = configuration.deltaContent ? fetchAllClientJobExecutions() : Collections.EMPTY_LIST

        //Do DeltaContent IFF there are previous Client JobExecutions AND DeltaContent flag is true in the GrabbitConfiguration
        final doDeltaContent = clientJobExecutions && configuration.deltaContent

        for(PathConfiguration pathConfig : configuration.pathConfigurations) {
            try {
                final clientBatchJob = new ClientBatchJob.ServerBuilder(configurableApplicationContext)
                        .andServer(configuration.serverHost, configuration.serverPort)
                        .andCredentials(configuration.serverUsername, configuration.serverPassword)
                        .andDoDeltaContent(doDeltaContent)
                        .andClientJobExecutions(clientJobExecutions)
                        .andConfiguration(pathConfig)
                        .build()
                final Long currentJobExecutionId = clientBatchJob.start()
                jobExecutionIds << currentJobExecutionId
            }
            catch(Exception e) {
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
