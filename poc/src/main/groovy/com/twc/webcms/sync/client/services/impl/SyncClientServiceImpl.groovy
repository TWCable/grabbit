package com.twc.webcms.sync.client.services.impl

import com.twc.webcms.sync.client.services.SyncClientService
import com.twc.webcms.sync.jcr.JcrUtil
import com.twc.webcms.sync.proto.NodeProtos
import com.twc.webcms.sync.utils.unmarshaller.ProtobufUnmarshaller
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.commons.httpclient.Credentials
import org.apache.commons.httpclient.UsernamePasswordCredentials
import org.apache.felix.scr.annotations.Activate
import org.apache.felix.scr.annotations.Component
import org.apache.felix.scr.annotations.Property as ScrProperty
import org.apache.felix.scr.annotations.Reference
import org.apache.felix.scr.annotations.Service
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.sling.jcr.api.SlingRepository
import org.osgi.service.component.ComponentContext

import javax.jcr.Session

@Slf4j
@CompileStatic
@Component(label = "Content Sync Client Service", description = "Content Sync Client Service", immediate = true, metatype = true, enabled = true)
@Service(SyncClientService)
@SuppressWarnings('GroovyUnusedDeclaration')
class SyncClientServiceImpl implements SyncClientService {

    @ScrProperty(label = "Sync Server Hostname", description = "Sync Server Hostname")
    public static final String SYNC_SERVER_HOSTNAME = "sync.server.hostname"
    private String syncServerHostname

    @ScrProperty(label = "Sync Server Port", description = "Sync Server Port")
    public static final String SYNC_SERVER_PORT = "sync.server.port"
    private String syncServerPort

    @ScrProperty(label = "Sync Root Path", description = "Sync Root Path")
    public static final String SYNC_ROOT_PATH = "sync.root.path"
    private String syncRootPath

    @ScrProperty(label = "Sync Root Path", description = "Sync Root Path")
    public static final String SYNC_SERVER_URI = "sync.server.uri"
    private String syncServerUri

    @ScrProperty(boolValue = false, label = "Enable Data Sync")
    public static final String SYNC_ENABLED = "sync.enabled"
    private boolean syncEnabled


    @Activate
    public void activate(ComponentContext componentContext) {
        syncServerHostname = componentContext.properties[SYNC_SERVER_HOSTNAME] as String
        syncServerPort = componentContext.properties[SYNC_SERVER_PORT] as String
        syncRootPath = componentContext.properties[SYNC_ROOT_PATH] as String
        syncServerUri = componentContext.properties[SYNC_SERVER_URI] as String
        syncEnabled = componentContext.properties[SYNC_ENABLED] as boolean

        if(syncEnabled) {
            doSync()
        }
    }

    @Reference(bind='setSlingRepository')
    SlingRepository slingRepository

    public void doSync() {
        final String syncPath = "http://admin:admin@${syncServerHostname}:${syncServerPort}${syncServerUri}?rootPath=${syncRootPath}"
        DefaultHttpClient client = new DefaultHttpClient()

        //create the get request
        HttpGet get = new HttpGet(syncPath)

        try {
            HttpResponse status = client.execute(get)
            HttpEntity responseEntity = status.entity
            JcrUtil.withSession(slingRepository, "admin") { Session session ->
                while (true) {
                    try{
                        NodeProtos.Node nodeProto = NodeProtos.Node.parseDelimitedFrom(responseEntity.content)
                        if(!nodeProto) {
                            log.info "Received all data from Server for Sync. Completed unmarshalling."
                            session.save()
                            break;
                        }
                        ProtobufUnmarshaller.unmarshall(nodeProto, session)
                    }
                    catch (final Exception e) {
                        log.warn "Exception while unmarshalling received Protobuf", e
                        session.save()
                        break
                    }
                }
            }
        }
        catch(Exception e) {
            log.error "Error while requesting a content sync for syncPath: ${syncPath}", e
        }
    }
}
