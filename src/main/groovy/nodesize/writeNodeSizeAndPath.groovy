/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


import com.google.common.base.Function
import com.google.common.base.Stopwatch
import com.google.common.collect.FluentIterable
import com.google.common.collect.Iterables
import com.google.common.collect.TreeTraverser
import groovy.transform.CompileStatic
import org.apache.jackrabbit.oak.api.PropertyState
import org.apache.jackrabbit.oak.commons.PathUtils
import org.apache.jackrabbit.oak.spi.state.ChildNodeEntry
import org.apache.jackrabbit.oak.spi.state.NodeState
import org.apache.jackrabbit.oak.spi.state.NodeStore

import javax.jcr.PropertyType

@CompileStatic
class NodeSizeWriter {

    long sizeThreshold = Integer.getInteger("biggerThan", 100_000)
    String fileName = "node-sizes.txt"
    NodeStore nodeStore
    long bigNodeCount
    long nodeCount


    void dump() {
        Stopwatch w = Stopwatch.createStarted()
        File file = new File(fileName)
        def itr = getTreeTraversor(nodeStore.root)

        println "Writing nodes sizes to $file"

        file.withPrintWriter { pw ->
            for (SimpleTree t in itr) {
                dumpNodeSize(pw, t)
            }
        }

        println "Total $bigNodeCount big nodes found in $nodeCount nodes in $w"
    }

    private void dumpNodeSize(PrintWriter pw, SimpleTree tree) {
        nodeCount++
        long size = 0;
        tree.state.properties.each { PropertyState ps ->
            if (ps.getType().tag() == PropertyType.STRING){
                if (ps.isArray()){
                    for (int i = 0; i < ps.count(); i++) {
                        size += ps.size(i)
                    }
                } else {
                    size += ps.size()
                }
            }
        }
        if (size > sizeThreshold) {
            writeNodeSize(pw, tree, size)
        }

        if (nodeCount % 10000 == 0) {
            println "Traversed $nodeCount nodes so far ..."
        }
    }

    private void writeNodeSize(PrintWriter pw, SimpleTree tree, long size) {
        pw.printf("%s|%s%n", size, tree.path)
        bigNodeCount++

        if (bigNodeCount % 100 == 0) {
            println "=> Found $bigNodeCount big nodes so far ..."
        }
    }

    private static FluentIterable<SimpleTree> getTreeTraversor(NodeState state){
        def traversor = new TreeTraverser<SimpleTree>(){
            Iterable<SimpleTree> children(SimpleTree root) {
                return root.children()
            }
        }
        return traversor.preOrderTraversal(new SimpleTree(state, "/"))
    }

    private static class SimpleTree {
        final NodeState state
        final String path

        SimpleTree(NodeState state, String path){
            this.state = state
            this.path = path
        }

        Iterable<SimpleTree> children(){
            return Iterables.transform(state.childNodeEntries, {ChildNodeEntry cne ->
                new SimpleTree(cne.nodeState, PathUtils.concat(path, cne.name))} as Function)
        }
    }
}

new NodeSizeWriter(nodeStore: session.store).dump()

