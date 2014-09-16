package com.twc.webcms.sync.utils.unmarshaller

import com.twc.webcms.sync.proto.NodeProtos.Property
import com.twc.webcms.sync.proto.NodeProtos.Node
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.jackrabbit.commons.JcrUtils

import javax.jcr.Node as JcrNode
import javax.jcr.Session
import javax.jcr.nodetype.NodeType

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
    int count

    public ProtobufUnmarshaller() {
        count = 0;
    }

    void unmarshall(Node nodeProto, Session session) {
        log.debug "Received NodeProto: ${nodeProto}"
        List<Property> properties = nodeProto.properties.propertyList
        final String primaryType = properties.find { Property protoProperty -> protoProperty.name == JCR_PRIMARYTYPE }.value
        log.debug "Primary Type: ${primaryType}"

        if(primaryType == "nt:file") {
            def temp = nodeProto.name.split("/")
            final String fileName = temp.last()
            final String parentName = nodeProto.name.replaceAll("/${fileName}", "")
            final JcrNode parentNode = JcrUtils.getOrCreateByPath(parentName, null, session)
            JcrNode fileNode = JcrUtils.getOrAddNode(parentNode, fileName, NodeType.NT_FILE)
            JcrUtils.getOrAddNode(fileNode, JcrNode.JCR_CONTENT, NodeType.NT_RESOURCE)
            count++
        }
        else {
            JcrNode currentNode = JcrUtils.getOrCreateByPath(nodeProto.name, primaryType, session)
            properties.each { Property protoProperty ->
                if( ![
                        JCR_PRIMARYTYPE,
                        "jcr:createdBy",
                        "jcr:created",
                        "jcr:uuid",
                        "cq:lastReplicatedBy",
                        "cq:lastReplicated",
                        "cq:lastReplicationAction",
                        "cq:lastReplicationStatus",
                        "cq:lastPublishedBy",
                        "cq:lastPublished"
                    ].contains(protoProperty.name) && protoProperty.hasValue() ) {
                    log.debug "Current Property: ${protoProperty}"
                    currentNode.setProperty(protoProperty.name, protoProperty.value)
                }
            }
            count++
        }
    }
}
