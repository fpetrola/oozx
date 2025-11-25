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
