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

package com.twcable.grabbit.server.services

import groovy.transform.CompileStatic
import javax.jcr.Node as JcrNode

/**
 * Custom Wrapper for Node Iterator that will iterate through a list of Nodes containing of the root node and its children
 * Accounts for cases where certain paths(i.e nodes) needs to be excluded.
 */
@CompileStatic
final class ExcludePathNodeIterator implements Iterator<JcrNode> {

    private Iterator<JcrNode> nodeIterator
    private Collection<String> excludePathList

    public ExcludePathNodeIterator(Iterator<JcrNode> nodeIterator, Collection<String> excludePaths) {
        this.nodeIterator = nodeIterator
        this.excludePathList = (excludePaths == null) ? (Collection<String>)Collections.EMPTY_LIST : excludePaths
    }

    @Override
    boolean hasNext() {
        nodeIterator.hasNext()
    }

    @Override
    JcrNode next() {
        if(!excludePathList.isEmpty())
            (JcrNode)nodeIterator.find { JcrNode node -> !isPathInExcludedList(node.path) }
        else
            nodeIterator.next()
    }

    @Override
    void remove() {
        nodeIterator.remove()
    }

    private boolean isPathInExcludedList(String path) {
        return excludePathList.any { path.equals(it) || path.startsWith("${it}/") }
    }
}
