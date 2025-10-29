// src/main/java/com/example/Main.java
package com.fpetrola.oozx.api;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

public class Main {
  private static final String BASE_URL = "https://api.zxinfo.dk";

  public static void main(String[] args) {
    Client client = ClientBuilder.newClient();
    ResteasyWebTarget target = (ResteasyWebTarget) client.target(BASE_URL);
    ZxInfoClient zxClient = target.proxy(ZxInfoClient.class);

    System.out.println("=== Búsqueda: 'equinox' ===");
    SearchResponse response = zxClient.searchGames("everyone wally", 5, 0);

    System.out.println("Tiempo: " + response.took + "ms");
    System.out.println("Total encontrados: " + response.hits.total.value);
    System.out.println("Mostrando " + response.hits.hits.size() + " resultados:\n");

    for (Hit hit : response.hits.hits) {
      GameEntry game = hit._source;
//            game.id = hit._id; // asignamos el ID real
//            System.out.println("[" + game.id + "] " + game.title +
//                    " (" + game.originalYearOfRelease + ") - " +
//                    (game.publishers != null && !game.publishers.isEmpty() ? game.publishers.get(0).name : "Unknown"));
//            System.out.println("   Género: " + game.genre + " | Máquina: " + game.machineType);
//            if (game.score != null && game.score.score != null) {
//                System.out.println("   Puntuación: " + game.score.score + "/10 (" + game.score.votes + " votos)");
//            }
      game.screens.forEach(s -> {
        String filename = s.filename;
        System.out.println(filename);
      });
      System.out.println();
    }

    // Detalles del primer juego
    if (!response.hits.hits.isEmpty()) {
      String firstId = response.hits.hits.get(0)._id;
      System.out.println("=== Detalles del juego ID: " + firstId + " ===");
//            GameEntry detail = zxClient.getGameDetails(firstId);
//            detail.id = firstId;
//            System.out.println("Título: " + detail.title);
//            System.out.println("Año: " + detail.originalYearOfRelease);
//            System.out.println("Editor: " + (detail.publishers != null ? detail.publishers.get(0).name : "N/A"));
//            System.out.println("Género: " + detail.genre);
//            System.out.println("Descargas adicionales: " + detail.additionalDownloads.size());
//            System.out.println("Pantallas: " + detail.screens.size());
    }

    client.close();
  }
}