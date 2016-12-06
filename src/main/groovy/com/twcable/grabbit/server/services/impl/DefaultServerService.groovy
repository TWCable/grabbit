/*
 * Copyright 2015 Time Warner Cable, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.twcable.grabbit.server.services.impl

import com.twcable.grabbit.jcr.JCRUtil
import com.twcable.grabbit.server.batch.ServerBatchJob
import com.twcable.grabbit.server.services.ExcludePathNodeIterator
import com.twcable.grabbit.server.services.RootNodeWithMandatoryIterator
import com.twcable.grabbit.server.services.ServerService
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
import javax.annotation.Nullable
import javax.jcr.Node as JcrNode
import javax.jcr.Session
import javax.servlet.ServletOutputStream

@Slf4j
@CompileStatic
@Component(label = "Grabbit Server Service", description = "Grabbit Server Service", immediate = true, metatype = true, enabled = true)
@Service(ServerService)
@SuppressWarnings('GroovyUnusedDeclaration')
class DefaultServerService implements ServerService {

    @Reference(bind = 'setSlingRepository')
    SlingRepository slingRepository

    @Reference(bind = 'setConfigurableApplicationContext')
    ConfigurableApplicationContext configurableApplicationContext


    @Override
    void getContentForRootPath(
            @Nullable String serverUsername,
            @Nonnull String path,
            @Nullable Collection<String> excludePaths,
            @Nullable String afterDateString,
            @Nonnull ServletOutputStream servletOutputStream) {

        if (path == null) throw new IllegalStateException("path == null")
        if (excludePaths == null) excludePaths = (Collection<String>) Collections.EMPTY_LIST
        if (servletOutputStream == null) throw new IllegalStateException("servletOutputStream == null")

        JCRUtil.withSession(slingRepository, serverUsername) { Session session ->
            Iterator<JcrNode> nodeIterator

            //If the path is of type "/a/b/.", that means we should not do a recursive search of b's children
            //We should stop after getting all the children of b
            if (path.split("/").last() == ".") {
                final String actualPath = path.substring(0, path.length() - 2)
                final JcrNode rootNode = session.getNode(actualPath)
                nodeIterator = new RootNodeWithMandatoryIterator(rootNode)
            }
            else {
                final JcrNode rootNode = session.getNode(path)
                nodeIterator = TreeTraverser.nodeIterator(rootNode)
            }

            //Iterator wrapper for excludePaths exclusions
            nodeIterator = new ExcludePathNodeIterator(nodeIterator, excludePaths)

            ServerBatchJob batchJob = new ServerBatchJob.ConfigurationBuilder(configurableApplicationContext)
                .andConfiguration(new NamespaceHelper(session).namespaces.iterator(), nodeIterator, servletOutputStream)
                .andPath(path, excludePaths)
                .andContentAfterDate(afterDateString)
                .build()
            batchJob.run()
        }
    }
}
