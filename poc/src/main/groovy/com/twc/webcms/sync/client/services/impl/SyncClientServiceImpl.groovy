package com.twc.webcms.sync.client.services.impl

import com.twc.webcms.sync.client.services.SyncClientService
import com.twc.webcms.sync.jcr.JcrUtil
import com.twc.webcms.sync.proto.NodeProtos
import com.twc.webcms.sync.proto.PreProcessorProtos
import com.twc.webcms.sync.client.unmarshaller.ProtobufUnmarshaller
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.felix.scr.annotations.Activate
import org.apache.felix.scr.annotations.Component
import org.apache.felix.scr.annotations.Property as ScrProperty
import org.apache.felix.scr.annotations.Reference
import org.apache.felix.scr.annotations.Service
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.CredentialsProvider
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.sling.jcr.api.SlingRepository
import org.osgi.service.component.ComponentContext
import org.springframework.util.StopWatch

import javax.jcr.Session

@Slf4j
@CompileStatic
@Component(label = "Content Sync Client Service", description = "Content Sync Client Service", immediate = true, metatype = true, enabled = true)
@Service(SyncClientService)
@SuppressWarnings('GroovyUnusedDeclaration')
class SyncClientServiceImpl implements SyncClientService {

    public static final int BATCH_SIZE = 1000

    @ScrProperty(label = "Sync Server Hostname", description = "Sync Server Hostname")
    public static final String SYNC_SERVER_HOSTNAME = "sync.server.hostname"
    private String syncServerHostname

    @ScrProperty(label = "Sync Server Port", description = "Sync Server Port")
    public static final String SYNC_SERVER_PORT = "sync.server.port"
    private String syncServerPort

    @ScrProperty(label = "Sync Server Username", description = "Sync Server Username")
    public static final String SYNC_SERVER_USERNAME = "sync.server.username"
    private String syncServerUsername

    @ScrProperty(label = "Sync Server Password", description = "Sync Server Password")
    public static final String SYNC_SERVER_PASSWORD = "sync.server.password"
    private String syncServerPassword

    @ScrProperty(label = "Sync Paths", description = "Sync Paths")
    public static final String SYNC_PATHS = "sync.paths"
    private String[] syncPaths

    @ScrProperty(boolValue = false, label = "Enable Data Sync")
    public static final String SYNC_ENABLED = "sync.enabled"
    private boolean syncEnabled

    @Reference(bind='setSlingRepository')
    SlingRepository slingRepository

    @Activate
    void activate(ComponentContext componentContext) {
        log.info "Activate\n\n"
        syncServerHostname = componentContext.properties[SYNC_SERVER_HOSTNAME] as String
        syncServerPort = componentContext.properties[SYNC_SERVER_PORT] as String
        syncServerUsername = componentContext.properties[SYNC_SERVER_USERNAME] as String
        syncServerPassword = componentContext.properties[SYNC_SERVER_PASSWORD] as String
        syncPaths = componentContext.properties[SYNC_PATHS] as String[]
        syncEnabled = componentContext.properties[SYNC_ENABLED] as boolean

        if(syncEnabled) {
            doSync(syncPaths as Collection<String>)
        }
    }

    @Override
    void doSync(Collection<String> whiteList) {
        for(String path: whiteList) {
            withStopWatch(path) {
                sync(path)
            }
        }
    }

    private void sync(String path) {
        final String syncPath = "http://${syncServerHostname}:${syncServerPort}/bin/twc/server/grab?path=${path}"
        //create the get request
        HttpGet get = new HttpGet(syncPath)

        try {
                HttpResponse status = httpClient.execute(get)
                HttpEntity responseEntity = status.entity
                ProtobufUnmarshaller protobufUnmarshaller = new ProtobufUnmarshaller()
                long count = 0
                JcrUtil.withSession(slingRepository, "admin") { Session session ->

                    //Preprocessor step
                    //Receive and register all unknown namespaces first
                    PreProcessorProtos.Preprocessors preprocessors = PreProcessorProtos.Preprocessors.parseDelimitedFrom(responseEntity.content)
                    protobufUnmarshaller.fromPreprocessorsProto(preprocessors, session)

                    while (true) {
                        try{
                            if(count == BATCH_SIZE) {
                                //Intermediate save()
                                log.info "Current path: ${path}. session.save() for batch size : ${BATCH_SIZE}"
                                session.save()
                                count = 0
                            }
                            NodeProtos.Node nodeProto = NodeProtos.Node.parseDelimitedFrom(responseEntity.content)
                            if(!nodeProto) {
                                session.save()
                                log.info "Received all data from Server for Path: ${path}. Completed unmarshalling. Total nodes : ${protobufUnmarshaller.nodeCount}"
                                break
                            }
                            protobufUnmarshaller.fromNodeProto(nodeProto, session)
                            count++
                        }
                        catch (final Exception e) {
                            log.warn "Exception while unmarshalling received Protobuf", e
                            break
                        }
                    }
                }
        }
        catch(Exception e) {
            log.error "Error while requesting a content sync for current Path: ${[path]}", e
        }

    }

    private DefaultHttpClient getHttpClient() {
        DefaultHttpClient client = new DefaultHttpClient()

        CredentialsProvider credentialsProvider = new BasicCredentialsProvider()
        credentialsProvider.setCredentials(
                new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                new UsernamePasswordCredentials(syncServerUsername, syncServerPassword)
        )
        client.setCredentialsProvider(credentialsProvider)
        client
    }

    private <T> T withStopWatch(String currentPath, Closure<T> closure) {
        StopWatch stopWatch = new StopWatch("Content sync from ${syncServerHostname} for Current Path ${currentPath}")
        stopWatch.start()

        T retVal = closure.call()

        stopWatch.stop()
        log.info stopWatch.shortSummary()

        return retVal
    }

}
