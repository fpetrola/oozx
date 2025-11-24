/*
 *
 *  * Copyright (c) 2023-2025 Fernando Damian Petrola
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

// src/main/java/com/example/Main.java
package com.fpetrola.oozx.api;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import java.util.List;

public class ZxInfoApiHandler {
  private final String BASE_URL = "https://api.zxinfo.dk";

  public static void main(String[] args) {
    new ZxInfoApiHandler().search("everyone wally");
  }

  public List<Hit> search(String everyoneWally) {
    Client client = null;
    client = ClientBuilder.newClient();
    ResteasyWebTarget target = (ResteasyWebTarget) client.target(BASE_URL);
    ZxInfoClient zxClient = target.proxy(ZxInfoClient.class);
    SearchResponse response = zxClient.searchGames(everyoneWally, 150, 0);

//    for (Hit hit : response.hits.hits) {
//      GameEntry game = hit._source;
////            game.id = hit._id; // asignamos el ID real
////            System.out.println("[" + game.id + "] " + game.title +
////                    " (" + game.originalYearOfRelease + ") - " +
////                    (game.publishers != null && !game.publishers.isEmpty() ? game.publishers.get(0).name : "Unknown"));
////            System.out.println("   Género: " + game.genre + " | Máquina: " + game.machineType);
////            if (game.score != null && game.score.score != null) {
////                System.out.println("   Puntuación: " + game.score.score + "/10 (" + game.score.votes + " votos)");
////            }
//      game.screens.forEach(s -> {
//        String filename = s.filename;
//        System.out.println(filename);
//      });
//      System.out.println();
//    }

//    // Detalles del primer juego
//    if (!response.hits.hits.isEmpty()) {
//      String firstId = response.hits.hits.get(0)._id;
//      System.out.println("=== Detalles del juego ID: " + firstId + " ===");
////            GameEntry detail = zxClient.getGameDetails(firstId);
////            detail.id = firstId;
////            System.out.println("Título: " + detail.title);
////            System.out.println("Año: " + detail.originalYearOfRelease);
////            System.out.println("Editor: " + (detail.publishers != null ? detail.publishers.get(0).name : "N/A"));
////            System.out.println("Género: " + detail.genre);
////            System.out.println("Descargas adicionales: " + detail.additionalDownloads.size());
////            System.out.println("Pantallas: " + detail.screens.size());
//    }

    client.close();

    return response.hits.hits;
  }
}