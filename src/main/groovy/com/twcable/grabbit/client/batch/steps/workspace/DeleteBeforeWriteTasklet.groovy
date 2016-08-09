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
package com.twcable.grabbit.client.batch.steps.workspace

import com.twcable.grabbit.client.batch.ClientBatchJobContext
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus

import javax.annotation.Nonnull
import javax.annotation.Nullable
import javax.jcr.Node
import javax.jcr.PathNotFoundException
import javax.jcr.Session

/**
 * A client preSyncFlow tasklet that deletes nodes under the job path
 * <p>
 *     This tasklet is used to clear the workspace that will be written to by the job path.
 *     If exclude paths are provided, it does not delete those paths, rather it deletes the nodes
 *     around the exclude paths.  If no exclude paths are provided, the entire tree under the
 *     job path is deleted.
 *
 *     This tasklet is activated by the "deleteBeforeWrite" path configuration
 * </p>
 */
@Slf4j
@CompileStatic
class DeleteBeforeWriteTasklet implements Tasklet {

    private String jobPath

    private Collection<String> relativeExcludePaths


    /**
     * @param jobPath the job path to evaluate.  Comes from the job parameters
     * @param excludePaths a '*' delimited string containing the paths to exclude.  Comes from the job parameters
     */
    DeleteBeforeWriteTasklet(@Nonnull final String jobPath, @Nullable String excludePaths) {
        //Ensures the job path leads with a '/'
        this.jobPath = cleanJobPath(jobPath)
        //Takes the * delimited paths string, creates a collection of relative paths, and normalizes
        this.relativeExcludePaths = createExcludePaths(this.jobPath, excludePaths)
    }

    private Session theSession() {
        ClientBatchJobContext.session
    }

    /**
     * @param paths '*' delimited, expected to be driven from job parameters e.g /foo/bar/blah{@literal *}/foo/bar/meh
     * @param jobPath the job path, used to make sure we are building well-formed relative paths e.g /foo/bar
     * @return A collection of the exclude paths that are guaranteed to lead with a '/', e.g [blah, meh].  If null or empty, returns an empty collection
     * @throws IllegalStateException if any paths are not well-formed to the jobPath
     */
    private static Collection<String> createExcludePaths(@Nonnull final String jobPath, @Nullable String paths) {
        final thisPathsString = paths?.trim()
        if(!thisPathsString) {
            return []
        }
        final theExcludePaths = thisPathsString.split("\\*") as Collection<String>
        final jobPathWithSlash = jobPath + '/'

        if (theExcludePaths.any {!it.startsWith(jobPathWithSlash)}) throw new IllegalStateException("Not all exclude paths start with \"${jobPath}\": ${theExcludePaths}")

        return theExcludePaths.collect { it.substring(jobPathWithSlash.length()) }
    }

    /**
     * @param jobPath expected to be driven from job parameters
     * @return a "cleaned" job path, i.e leads with a '/', no trailing '/', and trimmed, e.g "/foo/bar"
     */
    private static String cleanJobPath(String jobPath) {
        String thisJobPath = jobPath.trim()
        //Add the leading '/' if not present
        thisJobPath = (thisJobPath[0] != '/') ? "/${thisJobPath}" : thisJobPath
        //Remove any trailing '/'
        thisJobPath = (thisJobPath[-1] != '/') ? thisJobPath : thisJobPath[0..-2] as String

        return thisJobPath
    }


    /**
     * Given the current context in the form of a step contribution, do whatever
     * is necessary to process this unit inside a transaction. Implementations
     * return {@link RepeatStatus#FINISHED} if finished. If not they return
     * {@link RepeatStatus#CONTINUABLE}. On failure throws an exception.
     *
     * @param contribution mutable state to be passed back to update the current
     * step execution
     * @param chunkContext attributes shared between invocations but not between
     * restarts
     * @return an {@link RepeatStatus} indicating whether processing is
     * continuable.
     */
    @Override
    RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        final Session session = theSession()

        final Node rootNode
        try {
            rootNode = session.getNode(jobPath)
        }
        catch(PathNotFoundException ex) {
            log.warn "deleteBeforeWrite was enabled for jobPath ${jobPath} but job path does not exist on client!"
            return RepeatStatus.FINISHED
        }

        //If we don't have any exclude paths, we don't have to worry about only clearing particular subtrees
        if(!relativeExcludePaths || relativeExcludePaths.empty) {
            rootNode.remove()
        }
        else {
            deletePartialTree(rootNode, relativeExcludePaths)
        }

        session.save()
        return RepeatStatus.FINISHED
    }


    /**
     * Called recursively deleting any nodes under rootNode, that aren't apart of any relativeExcludePaths
     */
    private void deletePartialTree(@Nonnull final Node rootNode, @Nullable Collection<String> relativeExcludePaths) {
        //Base case.  If a root node does not have any exclude paths under it, it must have been excluded
        if(!relativeExcludePaths) {
            return
        }
        //Compute the current tree level excluded nodes, and compute the remaining paths to traverse
        Collection<NodeAndExcludePaths> nodeAndExcludePaths = relativeExcludePaths.inject([] as Collection<NodeAndExcludePaths>) { def acc, def thisPath ->
            final thisNodeAndExcludePath = NodeAndExcludePaths.fromPath(thisPath)
            //If we have already created a nodeAndExcludePath for this node name, just add to its exclude paths
            final matchingNodeName = acc.find { it.nodeName == thisNodeAndExcludePath.nodeName }
            if(matchingNodeName) {
                matchingNodeName.excludePaths.addAll(thisNodeAndExcludePath.excludePaths)
            }
            else {
                acc.add(thisNodeAndExcludePath)
            }
            return acc
        } as Collection
        //Delete nodes allowed under this tree
        final rootNodeChildren = rootNode.getNodes()
        while(rootNodeChildren.hasNext()) {
            final currentNode = rootNodeChildren.nextNode()
            //If this node is in our exclusion list, don't delete it
            if(!(nodeAndExcludePaths.find { currentNode.name == it.nodeName })) {
                currentNode.remove()
            }
        }
        //Recurse on each
        nodeAndExcludePaths.each {
            try {
                deletePartialTree(rootNode.getNode(it.nodeName), it.excludePaths)
            }
            catch(PathNotFoundException ex) {
                log.warn "Exclude node ${it.nodeName} of parent ${rootNode.path} does not exist on client, so we won't traverse it for adjacent node deletion"
            }
        }
    }

    /**
     * If we have a path for a node such as 'foo/bar/doo'
     * then node is 'foo', and the relativeExcludePaths is 'bar/doo'.  As other relativeExcludePaths are inspected, we may find that
     * there are NodeAndExcludePaths with the same nodeName, in which case they will be merged by adding to an instance's excludePaths
     */
    static class NodeAndExcludePaths {

        String nodeName

        //Can mutate in the case described above
        Collection<String> excludePaths

        /**
         * @param path Expects a relative path e.g 'foo/bar/doo'
         * @return NodeExcludePath(nodeName: "foo", excludePaths: "bar/doo")
         */
        static NodeAndExcludePaths fromPath(final String path) {
            final String thisPath = path
            //We need to find the current node, and the remaining path e.g 'foo/bar/doo', node: foo, remaining path: /bar/doo
            final slashIndex = thisPath.indexOf('/')
            //There is no remaining path
            if(slashIndex == -1) {
                return new NodeAndExcludePaths(nodeName: thisPath, excludePaths: [])
            }
            final remainingPath = thisPath.substring(slashIndex+1)
            final node = thisPath.substring(0, slashIndex)
            return new NodeAndExcludePaths(nodeName: node, excludePaths: [remainingPath])
        }
    }

}
