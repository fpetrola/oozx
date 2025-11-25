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

package com.fpetrola.oozx.api;

import java.util.List;
import java.util.Map;

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
    public List<String> additionalDownloads;
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