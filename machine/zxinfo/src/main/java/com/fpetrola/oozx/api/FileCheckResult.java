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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Result of /filecheck/{hash}: the entry a tape/disk image belongs to.
 * The API answers 404 when no entry matches the hash.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileCheckResult {
    public String entry_id;
    public String title;
    public String zxinfoVersion;
    public String contentType;
    public Integer originalYearOfRelease;
    public String machineType;
    public String genre;
    public String genreType;
    public String genreSubType;
    public List<Publisher> publishers;
    /** Every known file with this hash, across archives and TOSEC sets. */
    public List<GameEntry.MD5Hash> file;

    @Override
    public String toString() {
        return title + " (" + originalYearOfRelease + ") | ID: " + entry_id;
    }
}
