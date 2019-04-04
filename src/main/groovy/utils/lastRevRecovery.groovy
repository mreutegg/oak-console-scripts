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
import org.apache.jackrabbit.oak.plugins.document.ClusterNodeInfoDocument
import org.apache.jackrabbit.oak.plugins.document.DocumentNodeStore
import org.apache.jackrabbit.oak.plugins.document.DocumentStore
import org.apache.jackrabbit.oak.plugins.document.util.Utils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static org.apache.jackrabbit.oak.plugins.document.Collection.NODES

@CompileStatic
class RecoveryUtil {
    DocumentNodeStore nodeStore
    DocumentStore docStore
    Logger log = LoggerFactory.getLogger("org.apache.jackrabbit.oak.plugins.document.LastRevRecoveryAgent")

    RecoveryUtil(DocumentNodeStore ns) {
        this.nodeStore = ns
        this.docStore = ns.documentStore
    }

    List getChildDocs(String path) {
        final String to = Utils.getKeyUpperLimit(path)
        final String from = Utils.getKeyLowerLimit(path)
        return docStore.query(NODES, from, to, 10000)
    }

    def recover() {
        String path = System.getProperty("path")
        Integer clusterId = Integer.getInteger("recoverClusterId")
        boolean dryRun = nodeStore.getClusterId() == 0
        if (path == null) {
            println("Path not specified via 'path' system property")
            return
        }
        if (clusterId == null) {
            println("Recover clusterId not specified via 'recoverClusterId' system property")
            return
        }

        boolean isActive = false
        ClusterNodeInfoDocument.all(docStore).each {
            if (it.getClusterId() == clusterId && it.isActive()) {
                isActive = true
            }
        }
        if (isActive) {
            println("Cannot run recover on $clusterId as it's currently active")
            return
        }
        String p = path
        for (;;) {
            log.info("Running recovery on $p")
            List childDocs = getChildDocs(p)
            nodeStore.lastRevRecoveryAgent.recover(childDocs, clusterId, dryRun)
            if (PathUtils.denotesRoot(p)) {
                break
            }
            p = PathUtils.getParentPath(p)
        }
    }
}

new RecoveryUtil(session.store).recover()


