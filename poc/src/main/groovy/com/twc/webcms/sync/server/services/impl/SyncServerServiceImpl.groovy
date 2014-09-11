package com.twc.webcms.sync.server.services.impl

import com.twc.webcms.sync.jcr.JcrUtil
import com.twc.webcms.sync.proto.NodeProtos
import com.twc.webcms.sync.server.services.SyncServerService
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.felix.scr.annotations.Activate
import org.apache.felix.scr.annotations.Component
import org.apache.felix.scr.annotations.Service
import org.apache.jackrabbit.commons.flat.TreeTraverser
import org.apache.sling.api.resource.Resource
import org.apache.sling.api.resource.ResourceResolver
import org.apache.sling.api.resource.ResourceResolverFactory
import org.apache.sling.jcr.api.SlingRepository
import org.osgi.service.component.ComponentContext
import org.apache.felix.scr.annotations.Reference

import javax.jcr.Property
import javax.jcr.PropertyType
import javax.jcr.Session
import javax.servlet.ServletOutputStream


@Slf4j
@CompileStatic
@Component(label = "Content Sync Server Service", description = "Content Sync Server Service", immediate = true, metatype = true, enabled = true)
@Service(SyncServerService)
class SyncServerServiceImpl implements SyncServerService{

    @Reference(bind = 'setResourceResolverFactory')
    ResourceResolverFactory resourceResolverFactory

    @Reference
    SlingRepository slingRepository



    @Activate
    protected void activate(ComponentContext ctx){

    }

    public void getProtosForRootPath(String rootPath, ServletOutputStream servletOutputStream) {

        JcrUtil.withSession(slingRepository, "admin") { Session session ->
            javax.jcr.Node rootNode = session.getNode(rootPath)
            Iterator<javax.jcr.Node> nodeIterator = TreeTraverser.nodeIterator(rootNode)
            while(nodeIterator.hasNext()) {
                NodeProtos.Node nodeProto = getNodeProto(nodeIterator.next())
                nodeProto.writeDelimitedTo(servletOutputStream)
                servletOutputStream.flush()
            }
        }
    }

    private static NodeProtos.Node getNodeProto(javax.jcr.Node node) {
        final List<Property> properties = node.properties.toList()
        NodeProtos.Node.Builder nodeBuilder = NodeProtos.Node.newBuilder()
        nodeBuilder.setName(node.path)

        NodeProtos.Properties.Builder propertiesBuilder = NodeProtos.Properties.newBuilder()
        properties.each { Property property ->
            NodeProtos.Property.Builder propertyBuilder = NodeProtos.Property.newBuilder()
            propertyBuilder.setName(property.name)

            //TODO: This does NOT yet account for various types of Property Types and Values
            propertyBuilder.setType(property.type)
            if(!property.multiple) {
                propertyBuilder.setValue(property.value.string)
            }
            propertiesBuilder.addProperty(propertyBuilder.build())
        }
        nodeBuilder.setProperties(propertiesBuilder.build())

        nodeBuilder.build()
    }

}
