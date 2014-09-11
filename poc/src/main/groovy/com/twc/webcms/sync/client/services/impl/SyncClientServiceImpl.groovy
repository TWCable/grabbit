package com.twc.webcms.sync.client.services.impl

import com.twc.webcms.sync.client.services.SyncClientService
import com.twc.webcms.sync.jcr.JcrUtil
import com.twc.webcms.sync.proto.NodeProtos
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.felix.scr.annotations.Component
import org.apache.felix.scr.annotations.Reference
import org.apache.felix.scr.annotations.Service
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.jackrabbit.commons.JcrUtils
import org.apache.sling.jcr.api.SlingRepository

import javax.jcr.Node
import javax.jcr.Session

import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE

@Slf4j
@CompileStatic
@Component(label = "Content Sync Client Service", description = "Content Sync Client Service", immediate = true, metatype = true, enabled = true)
@Service(SyncClientService)
class SyncClientServiceImpl implements SyncClientService {

    @Reference(bind='setSlingRepository')
    SlingRepository slingRepository

    public void doSync(String rootPath) {

        final String serverPath = "http://sagar.twcable.com:4503/bin/server/sync"

        DefaultHttpClient client = new DefaultHttpClient()

        //create the get request
        HttpGet get = new HttpGet("${serverPath}?rootPath=${rootPath}")

        try {
            HttpResponse status = client.execute(get)
            HttpEntity responseEntity = status.entity
            JcrUtil.withSession(slingRepository, "admin") { Session session ->
                while (true) {
                    try{
                        NodeProtos.Node nodeProto = NodeProtos.Node.parseDelimitedFrom(responseEntity.content)
                        log.info "Received NodeProto: ${nodeProto}"
                        List<NodeProtos.Property> properties = nodeProto.properties.propertyList
                        final String primaryType = properties.find { NodeProtos.Property protoProperty -> protoProperty.name == JCR_PRIMARYTYPE }.value
                        Node currentNode = JcrUtils.getOrCreateByPath(nodeProto.name, primaryType , session)
                        properties.each { NodeProtos.Property protoProperty ->
                            if(protoProperty.name != JCR_PRIMARYTYPE ) {
                                currentNode.setProperty(protoProperty.name, protoProperty.value)
                            }
                        }
                        session.save()
                        log.info "Primary Type: ${primaryType}"

                    }
                    catch (final Exception e) {
                        log.warn "Reached END : ", e
                        session.save()
                        break
                    }
                }
            }
        }
        catch(Exception e) {
            log.error "Error while requesting a content sync for RootPath: ${rootPath}", e
        }
    }
}
