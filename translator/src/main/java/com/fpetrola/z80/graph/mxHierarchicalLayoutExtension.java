/*
 *
 *  * Copyright (c) 2023-2024 Fernando Damian Petrola
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.fpetrola.z80.graph;

import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.view.mxGraph;

import java.util.*;

final class mxHierarchicalLayoutExtension extends mxHierarchicalLayout {
  mxHierarchicalLayoutExtension(mxGraph graph) {
    super(graph);
  }

  public void run(Object parent) {
    // Separate out unconnected hierarchies
    List<Set<Object>> hierarchyVertices = new ArrayList<Set<Object>>();
    Set<Object> allVertexSet = new LinkedHashSet<Object>();

    if (this.roots == null && parent != null) {
      Set<Object> filledVertexSet = filterDescendants(parent);

      this.roots = new ArrayList<Object>();

      while (!filledVertexSet.isEmpty()) {
        List<Object> candidateRoots = findRoots(parent, filledVertexSet);

        for (Object root : candidateRoots) {
          Set<Object> vertexSet = new LinkedHashSet<Object>();
          hierarchyVertices.add(vertexSet);

          traverse(root, true, null, allVertexSet, vertexSet, hierarchyVertices, filledVertexSet);
        }

        this.roots.addAll(candidateRoots);
      }
    } else {
      // Find vertex set as directed traversal from roots

      for (int i = 0; i < roots.size(); i++) {
        Set<Object> vertexSet = new LinkedHashSet<Object>();
        hierarchyVertices.add(vertexSet);

        traverse(roots.get(i), true, null, allVertexSet, vertexSet, hierarchyVertices, null);
      }
    }

    // Iterate through the value removing parents who have children in this layout

    // Perform a layout for each separate hierarchy
    // Track initial coordinate x-positioning
    double initialX = 0;
    Iterator<Set<Object>> iter = hierarchyVertices.iterator();

    while (iter.hasNext()) {
      Set<Object> vertexSet = iter.next();

      this.model = new mxGraphHierarchyModel2(this, vertexSet.toArray(), roots, parent);

      cycleStage(parent);
      layeringStage();
      crossingStage(parent);
      initialX = placementStage(initialX, parent);
    }
  }
}