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
