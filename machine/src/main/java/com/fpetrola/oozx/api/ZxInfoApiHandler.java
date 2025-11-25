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

    client.close();

    return response.hits.hits;
  }

  /**
   * Fetches game details from the API by game ID and converts to GameDetail
   * @param gameId the game ID to fetch
   * @return GameDetail with all available information from the API
   */
  public GameDetail fetchGameDetails(String gameId) {
    Client client = null;
    try {
      client = ClientBuilder.newClient();
      ResteasyWebTarget target = (ResteasyWebTarget) client.target(BASE_URL);
      ZxInfoClient zxClient = target.proxy(ZxInfoClient.class);
      GameEntry gameEntry = zxClient.getGameDetails(gameId);
      
      return convertGameEntryToDetail(gameEntry, gameId);
    } catch (Exception e) {
      System.err.println("Error fetching game details: " + e.getMessage());
      e.printStackTrace();
      return null;
    } finally {
      if (client != null) {
        client.close();
      }
    }
  }

  /**
   * Converts GameEntry from API to GameDetail for UI display
   */
  private GameDetail convertGameEntryToDetail(GameEntry entry, String gameId) {
    GameDetail detail = new GameDetail();
    
    detail.id = gameId;
    detail.title = entry.title;
    detail.yearOfRelease = entry.originalYearOfRelease != null ? entry.originalYearOfRelease.toString() : null;
    detail.originalMonthOfRelease = entry.originalMonthOfRelease;
    detail.originalDayOfRelease = entry.originalDayOfRelease;
    detail.machineType = entry.machineType;
    detail.genre = entry.genre;
    detail.genreType = entry.genreType;
    detail.genreSubType = entry.genreSubType;
    detail.availability = entry.availability;
    detail.isbn = entry.isbn;
    detail.xrated = entry.xrated;
    detail.contentType = entry.contentType;
    detail.zxinfoVersion = entry.zxinfoVersion;
    
    // Handle score
    if (entry.score != null) {
      detail.score = entry.score.score != null ? entry.score.score.doubleValue() : null;
    }
    
    // Handle publishers
    if (entry.publishers != null && !entry.publishers.isEmpty()) {
      detail.publisher = entry.publishers.get(0).name;
      detail.publishers = new java.util.ArrayList<>();
      for (Publisher pub : entry.publishers) {
        detail.publishers.add(pub.name);
      }
    }
    
    // Handle authors
    if (entry.authors != null && !entry.authors.isEmpty()) {
      detail.authors = new java.util.ArrayList<>();
      for (Author author : entry.authors) {
        if (author.name != null) {
          detail.authors.add(author.name);
        }
      }
    }
    
    // Handle screenshots
    if (entry.screens != null && !entry.screens.isEmpty()) {
      detail.screenshots = new java.util.ArrayList<>();
      for (Screen screen : entry.screens) {
        String screenshotUrl = null;
        // Try to use URL if available
        if (screen.url != null && !screen.url.isEmpty()) {
          screenshotUrl = screen.url;
        } else if (screen.scrUrl != null && !screen.scrUrl.isEmpty()) {
          screenshotUrl = screen.scrUrl;
        } else if (screen.filename != null && !screen.filename.isEmpty()) {
          // Construct full URL from filename
          screenshotUrl = "https://media.zxinfo.dk/media/" + screen.filename;
        }
        if (screenshotUrl != null) {
          detail.screenshots.add(screenshotUrl);
        }
      }
    }
    
    // Handle additional downloads
    if (entry.additionalDownloads != null && !entry.additionalDownloads.isEmpty()) {
      detail.additionalDownloads = new java.util.ArrayList<>();
      for (AdditionalDownload download : entry.additionalDownloads) {
        if (download.path != null) {
          StringBuilder downloadInfo = new StringBuilder(download.path);
          if (download.type != null) {
            downloadInfo.append(" (").append(download.type).append(")");
          }
          detail.additionalDownloads.add(downloadInfo.toString());
        }
      }
    }
    
    // Handle releases
    if (entry.releases != null && !entry.releases.isEmpty()) {
      detail.releases = new java.util.ArrayList<>();
      for (Release release : entry.releases) {
        if (release.publishers != null && !release.publishers.isEmpty()) {
          for (Publisher publisher : release.publishers) {
            java.util.Map<String, String> releaseMap = new java.util.HashMap<>();
            releaseMap.put("Publisher", publisher.name != null ? publisher.name : "N/A");
            detail.releases.add(releaseMap);
          }
        }
      }
    }
    
    return detail;
  }
}