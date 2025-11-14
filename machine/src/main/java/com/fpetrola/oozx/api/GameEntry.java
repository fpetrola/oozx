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

// src/main/java/com/example/GameEntry.java
package com.fpetrola.oozx.api;

import java.util.List;
import java.util.Map;

public class GameEntry {
    public String contentType;
    public String zxinfoVersion;
    public String title;
    public Integer originalYearOfRelease;
    public Integer originalMonthOfRelease;
    public Integer originalDayOfRelease;
    public String machineType;
    public int xrated;
    public String genre;
    public String genreType;
    public String genreSubType;
    public String isbn;
    public String availability;
    public Score score;
    public List<Publisher> publishers;
    public List<Author> authors;
    public List<Release> releases;
    public List<AdditionalDownload> additionalDownloads;
    public List<Screen> screens;
    public String _index;
    public String _id;
    public String _version;
    public String _seq_no;
    public String _primary_term;
    public String found;
    public Map<String, String> _source;

    @Override
    public String toString() {
        return title + " (" + originalYearOfRelease + ") - " + 
               (publishers != null && !publishers.isEmpty() ? publishers.get(0).name : "Unknown") +
               " | ID: " ;
    }

    // Helper para usar en Main (se asigna después)
    public String id;
}