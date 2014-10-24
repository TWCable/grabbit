package com.twc.grabbit.server.services.impl

import com.twc.grabbit.jcr.JcrUtil
import com.twc.grabbit.server.batch.ServerBatchJob
import com.twc.grabbit.server.services.JcrContentRecursiveIterator
import com.twc.grabbit.server.services.ServerService
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.apache.felix.scr.annotations.Component
import org.apache.felix.scr.annotations.Reference
import org.apache.felix.scr.annotations.Service
import org.apache.jackrabbit.commons.NamespaceHelper
import org.apache.jackrabbit.commons.flat.TreeTraverser
import org.apache.sling.jcr.api.SlingRepository
import org.springframework.context.ConfigurableApplicationContext

import javax.annotation.Nonnull
import javax.jcr.Node as JcrNode
import javax.jcr.Session
import javax.servlet.ServletOutputStream

@Slf4j
@CompileStatic
@Component(label = "Grabbit Server Service", description = "Grabbit Server Service", immediate = true, metatype = true, enabled = true)
@Service(ServerService)
@SuppressWarnings('GroovyUnusedDeclaration')
class DefaultServerService implements ServerService{

    @Reference(bind='setSlingRepository')
    SlingRepository slingRepository

    @Reference(bind='setConfigurableApplicationContext')
    ConfigurableApplicationContext configurableApplicationContext


    void getContentForRootPath(@Nonnull String path, @Nonnull ServletOutputStream servletOutputStream) {

        if(path == null) throw new IllegalStateException("path == null")
        if(servletOutputStream == null) throw new IllegalStateException("servletOutputStream == null")

        JcrUtil.withSession(slingRepository, "admin") { Session session ->
            Iterator<JcrNode> nodeIterator

            //If the path is of type "/a/b/.", that means we should not do a recursive search of b's children
            //We should stop after getting all the children of b
            if(path.split("/").last() == "." ) {
                final String actualPath = path.substring(0, path.length() - 2)
                final JcrNode rootNode = session.getNode(actualPath)
                nodeIterator = new JcrContentRecursiveIterator(rootNode)
            }
            else {
                final JcrNode rootNode = session.getNode(path)
                nodeIterator = TreeTraverser.nodeIterator(rootNode)
            }

            ServerBatchJob batchJob = configuredServerBatchJob(session, nodeIterator, servletOutputStream, path)
            batchJob.run()
        }
    }

    private ServerBatchJob configuredServerBatchJob(Session session, Iterator<JcrNode> nodeIterator,
                                                ServletOutputStream servletOutputStream, String path) {
        ServerBatchJob batchJob = new ServerBatchJob.ConfigurationBuilder(configurableApplicationContext)
                                    .andConfiguration(new NamespaceHelper(session).namespaces.iterator(),nodeIterator,servletOutputStream)
                                    .andPath(path)
                                    .build()
        batchJob
    }
}
