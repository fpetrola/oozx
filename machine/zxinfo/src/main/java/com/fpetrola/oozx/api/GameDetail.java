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
}