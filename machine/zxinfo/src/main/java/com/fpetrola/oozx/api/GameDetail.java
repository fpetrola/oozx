/*
 * Copyright (c) 2023-2026 Fernando Damian Petrola
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.fpetrola.oozx.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GameDetail {
    public String id;
    public String title;
    public String yearOfRelease;
    public Integer originalMonthOfRelease;
    public Integer originalDayOfRelease;
    public String publisher;
    public List<String> publishers;
    public String genre;
    public String genreType;
    public String genreSubType;
    public String machineType;
    public List<String> machines;
    public String memoryRequired;
    public List<String> screenshots;
    public String description;
    public String availability;
    public Double score;
    public Integer xrated;
    public List<String> authors;
    public List<AdditionalDownload> additionalDownloads;
    /** Subset of additionalDownloads whose type is "Game map" (scanned/drawn maps, JPG or PNG). */
    public List<AdditionalDownload> gameMaps;
    public List<Map<String, String>> releases;
    public String coverImageUrl;
    public String rating;
    
    // Fields from GameEntry for compatibility
    public String contentType;
    public String zxinfoVersion;
    public String isbn;

    @Override
    public String toString() {
        return "GameDetail{\n" +
                "  id='" + id + "',\n" +
                "  title='" + title + "',\n" +
                "  year='" + yearOfRelease + "',\n" +
                "  publisher='" + publisher + "',\n" +
                "  genre='" + genre + "',\n" +
                "  machine='" + machineType + "',\n" +
                "  memory='" + memoryRequired + "',\n" +
                "  screenshots=" + screenshots + "\n" +
                "}";
    }

    /** One of these out of a whole entry, which is the shape every window here reads. */
    public static GameDetail of(GameEntry entry, String gameId) {

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
      for (Object screenMap : entry.screens) {
        Screen screen = Screen.from(screenMap);
        if (screen != null) {
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
    }

    // Handle additional downloads
    if (entry.additionalDownloads != null && !entry.additionalDownloads.isEmpty()) {
      detail.additionalDownloads = new java.util.ArrayList<>(entry.additionalDownloads);
      detail.gameMaps = ZxInfoApiHandler.extractGameMaps(entry.additionalDownloads);
    }

    // Handle releases. LinkedHashMap keeps the column order stable across rows,
    // which is what the details table relies on when it derives its columns from row 0.
    if (entry.releases != null && !entry.releases.isEmpty()) {
      detail.releases = new java.util.ArrayList<>();
      for (Release release : entry.releases) {
        java.util.Map<String, String> releaseMap = new java.util.LinkedHashMap<>();
        releaseMap.put("Title", ZxInfoApiHandler.joinTitles(release.releaseTitles, entry.title));
        releaseMap.put("Year", release.yearOfRelease != null ? release.yearOfRelease.toString() : "N/A");
        releaseMap.put("Publisher", ZxInfoApiHandler.firstPublisherName(release.publishers));
        releaseMap.put("Price", ZxInfoApiHandler.formatPrice(release.releasePrice));
        releaseMap.put("Code", release.code != null ? release.code : "");
        releaseMap.put("Barcode", release.barcode != null ? release.barcode : "");
        releaseMap.put("Files", String.valueOf(release.files != null ? release.files.size() : 0));
        detail.releases.add(releaseMap);
      }
    }
    return detail;
    }
}
