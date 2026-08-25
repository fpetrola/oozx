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

/**
 * Wrapper for Elasticsearch response containing metadata and the actual game data
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameResponse {
    public String _index;
    public String _id;
    public Integer _version;
    public Integer _seq_no;
    public Integer _primary_term;
    public Boolean found;
    public GameEntry _source;
    
    public GameEntry getGameEntry() {
        if (_source != null) {
            _source._id = _id;
            _source._index = _index;
            _source._version = _version;
            _source._seq_no = _seq_no;
            _source._primary_term = _primary_term;
            _source.found = found;
        }
        return _source;
    }
}
