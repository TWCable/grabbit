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
        for(PathConfiguration pathConfig : configuration.pathConfigurations) {
            final Long currentJobExecutionId = initiate(configuration.serverHost, configuration.serverPort, configuration.serverUsername,
                                                        configuration.serverPassword, pathConfig)
            if(currentJobExecutionId == -1) throw new IllegalStateException("Failed to initiate job for path: ${pathConfig}")
            jobExecutionIds << currentJobExecutionId
        }
        return jobExecutionIds

    }

    private Long initiate(String host, String port, String username, String password, PathConfiguration pathConfiguration) {
        try {
            ClientBatchJob batchJob = configuredClientBatchJob(host, port, username, password, pathConfiguration)
            Long id = batchJob.start()
            return id
        }
        catch(Exception e) {
            log.error "Error while requesting a content sync for current Path: ${[pathConfiguration]}", e
            return -1
        }

    }

    private ClientBatchJob configuredClientBatchJob(String host, String port, String username, String password,
                                                    PathConfiguration pathConfiguration) {
        ClientBatchJob batchJob = new ClientBatchJob.ServerBuilder(configurableApplicationContext)
                                    .andServer(host, port)
                                    .andCredentials(username, password)
                                    .andConfiguration(pathConfiguration)
                                    .build()
        batchJob
    }

}
