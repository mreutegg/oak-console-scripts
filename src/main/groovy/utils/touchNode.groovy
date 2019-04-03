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

import groovy.transform.CompileStatic
import org.apache.jackrabbit.oak.commons.PathUtils
import org.apache.jackrabbit.oak.spi.commit.CommitInfo
import org.apache.jackrabbit.oak.spi.commit.EmptyHook
import org.apache.jackrabbit.oak.spi.state.NodeBuilder
import org.apache.jackrabbit.oak.spi.state.NodeState
import org.apache.jackrabbit.oak.spi.state.NodeStateUtils
import org.apache.jackrabbit.oak.spi.state.NodeStore

@CompileStatic
class NodeUtil {
    NodeStore nodeStore

    def touch() {
        String path = System.getProperty("path")
        assert path : "Path not specified via 'path' system property"
        NodeState root = nodeStore.root
        NodeState state = NodeStateUtils.getNode(root, path)
        if (!state.exists()) {
            println "Node does not exist at $path"
            return
        }
        NodeBuilder builder = root.builder()
        builderAt(builder, path).setProperty(":dummy", "dummy")
        merge(builder)

        builder = nodeStore.root.builder();
        builderAt(builder, path).removeProperty(":dummy")
        merge(builder)
        println "Touched node at $path"
    }

    def merge(NodeBuilder builder) {
        nodeStore.merge(builder, EmptyHook.INSTANCE, CommitInfo.EMPTY)
    }

    NodeBuilder builderAt(NodeBuilder nb, String path) {
        for (String name : PathUtils.elements(path)) {
            nb = nb.child(name)
        }
        return nb
    }
}

new NodeUtil(nodeStore: session.store).touch()


