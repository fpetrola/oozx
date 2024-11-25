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

import com.fpetrola.z80.spy.ExecutionStep;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.AttributeType;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.dot.DOTExporter;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CustomGraph {

  protected Map<String, Map<String, Attribute>> edgeAttributes = new HashMap<>();
  protected Map<String, Map<String, Attribute>> vertexAttributes = new HashMap<>();
  public DefaultDirectedGraph<String, String> g2 = new DefaultDirectedGraph<>(String.class);
  private final Map<String, String> vertexes = new HashMap<>();
  public int edges;

  public void  exportGraph() {
    try {
      DOTExporter<String, String> export = new DOTExporter<>();
      export.setEdgeAttributeProvider(t -> edgeAttributes.get(t));
      export.setVertexAttributeProvider(t -> vertexAttributes.get(t));
      export.exportGraph(g2, new FileWriter("graph.dot"));
    } catch (IOException e) {
    }
  }

  public void addVertex(String vertextId, String label) {
    if (label == null || label.equals("null"))
      System.out.println("null?");
    g2.addVertex(vertextId);
    Map<String, Attribute> attributes = new HashMap<>();
    attributes.put("label", new DefaultAttribute<String>(label, AttributeType.STRING));
    vertexAttributes.put(vertextId, attributes);
  }

  protected String getVertexLabel(Object currentStep) {
    String string = currentStep.toString();

    if (string == null || string.equals("null"))
      System.out.println("null?");

//    System.out.println(string);
    return string;
  }

  protected String getVertexId(Object currentStep) {
    return currentStep.toString();
  }

  private String addOrCreateVertex(Object currentStep) {
    String id = getVertexId(currentStep);
    String a = vertexes.get(id);
    String label = getVertexLabel(currentStep);
    if (a == null) {
      addVertex(id, label);
      vertexes.put(id, label);
    } else {
      vertexAttributes.get(id).put("label", DefaultAttribute.createAttribute(label));
    }
    return id;
  }

  public void addEdge(Object sourceVertex, Object targetVertex, String label) {
    String edgeName = edges++ + "";
    String sourceVertexId = addOrCreateVertex(sourceVertex);
    String targetVertexId = addOrCreateVertex(targetVertex);

    Map<String, Attribute> attributes = new HashMap<>();
    attributes.put("label", new DefaultAttribute<String>(label, AttributeType.STRING));
    edgeAttributes.put(edgeName, attributes);
    g2.addEdge(sourceVertexId, targetVertexId, edgeName);

//    List<String> pre = Graphs.predecessorListOf(g2, "");
  }

  public void mergeVertexWith(ExecutionStep targetVertex, ExecutionStep sourceVertex) {
    String targetVertexId = addOrCreateVertex(targetVertex);
    String sourceVertexId = addOrCreateVertex(sourceVertex);

    Set<String> sourceEdges = new HashSet<String>(g2.edgesOf(sourceVertexId));
    for (String edge : sourceEdges) {
      String edgeSource = g2.getEdgeSource(edge);
      String edgeTarget = g2.getEdgeTarget(edge);
      Map<String, Attribute> map = edgeAttributes.get(edge);
      Attribute attribute = map.get("label");
      String edgeName = edges++ + "";
      edgeAttributes.put(edgeName, map);
      g2.addEdge(targetVertexId, edgeTarget, edgeName);

      g2.removeEdge(edge);
    }

    g2.removeVertex(sourceVertexId);
  }

}