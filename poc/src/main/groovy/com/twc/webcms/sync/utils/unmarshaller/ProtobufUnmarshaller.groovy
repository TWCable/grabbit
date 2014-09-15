package com.twc.webcms.sync.utils.unmarshaller

import com.twc.webcms.sync.proto.NodeProtos.Property
import com.twc.webcms.sync.proto.NodeProtos.Node
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.jackrabbit.commons.JcrUtils

import javax.jcr.Node as JcrNode
import javax.jcr.Session

import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE

@Slf4j
@CompileStatic
class ProtobufUnmarshaller {
    /*
        TODO:
        1. Doesn't work for all properties
        2. Fails for syncs like /checkout-mocks which store "jcr:data" properties
        3. Doesn't work for multiple value properties
    */
    static void unmarshall(Node nodeProto, Session session) {
        log.info "Received NodeProto: ${nodeProto}"
        List<Property> properties = nodeProto.properties.propertyList
        final String primaryType = properties.find { Property protoProperty -> protoProperty.name == JCR_PRIMARYTYPE }.value
        log.info "Primary Type: ${primaryType}"
        JcrNode currentNode = JcrUtils.getOrCreateByPath(nodeProto.name, primaryType, session)
        properties.each { Property protoProperty ->
            if( ![JCR_PRIMARYTYPE, "jcr:createdBy", "jcr:created"].contains(protoProperty.name) && protoProperty.hasValue() ) {
                log.info "Current Property: ${protoProperty}"
                currentNode.setProperty(protoProperty.name, protoProperty.value)
            }
        }
    }
}
