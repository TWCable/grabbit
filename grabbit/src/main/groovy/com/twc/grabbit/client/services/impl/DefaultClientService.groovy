package com.twc.grabbit.client.services.impl

import com.twc.grabbit.client.batch.ClientBatchJob
import com.twc.grabbit.client.services.ClientService
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.felix.scr.annotations.Activate
import org.apache.felix.scr.annotations.Component
import org.apache.felix.scr.annotations.Property as ScrProperty
import org.apache.felix.scr.annotations.Reference
import org.apache.felix.scr.annotations.Service
import org.apache.sling.jcr.api.SlingRepository
import org.osgi.service.component.ComponentContext
import org.springframework.context.ConfigurableApplicationContext

@Slf4j
@CompileStatic
@Component(label = "Grabbit Client Service", description = "Grabbit Client Service", immediate = true, metatype = true, enabled = true)
@Service(ClientService)
@SuppressWarnings('GroovyUnusedDeclaration')
class DefaultClientService implements ClientService {

    public static final int BATCH_SIZE = 1000

    @ScrProperty(label = "Grabbit Server Hostname", description = "Grabbit Server Hostname")
    public static final String GRAB_SERVER_HOSTNAME = "grab.server.hostname"
    private String grabServerHostname

    @ScrProperty(label = "Grabbit Server Port", description = "Grabbit Server Port")
    public static final String GRAB_SERVER_PORT = "grab.server.port"
    private String grabServerPort

    @ScrProperty(label = "Grabbit Server Username", description = "Grabbit Server Username")
    public static final String GRAB_SERVER_USERNAME = "grab.server.username"
    private String grabServerUsername

    @ScrProperty(label = "Grabbit Server Password", description = "Grabbit Server Password")
    public static final String GRAB_SERVER_PASSWORD = "grab.server.password"
    private String grabServerPassword

    @Reference(bind='setSlingRepository')
    SlingRepository slingRepository

    @Reference(bind='setConfigurableApplicationContext')
    ConfigurableApplicationContext configurableApplicationContext


    @Activate
    void activate(ComponentContext componentContext) {
        log.info "Activate\n\n"
        grabServerHostname = componentContext.properties[GRAB_SERVER_HOSTNAME] as String
        grabServerPort = componentContext.properties[GRAB_SERVER_PORT] as String
        grabServerUsername = componentContext.properties[GRAB_SERVER_USERNAME] as String
        grabServerPassword = componentContext.properties[GRAB_SERVER_PASSWORD] as String
    }

    @Override
    Collection<Long> initiateGrab(Collection<String> whiteList) {

        Collection<Long> jobExecutionIds = []
        for(String path: whiteList) {
            final Long currentJobExecutionId = initiate(path)
            if(currentJobExecutionId == -1) throw new IllegalStateException("Failed to initiate job for path: ${path}")
            jobExecutionIds << currentJobExecutionId
        }
        return jobExecutionIds

    }

    private Long initiate(String path) {
        try {
            ClientBatchJob batchJob = configuredClientBatchJob(grabServerHostname, grabServerPort, grabServerUsername, grabServerPassword, path)
            Long id = batchJob.start()
            return id
        }
        catch(Exception e) {
            log.error "Error while requesting a content sync for current Path: ${[path]}", e
            return -1
        }

    }

    private ClientBatchJob configuredClientBatchJob(String host, String port, String username, String password, String path) {
        ClientBatchJob batchJob = new ClientBatchJob.ServerBuilder(configurableApplicationContext)
                                    .andServer(host, port)
                                    .andCredentials(username, password)
                                    .andPath(path)
                                    .build()
        batchJob
    }

}
