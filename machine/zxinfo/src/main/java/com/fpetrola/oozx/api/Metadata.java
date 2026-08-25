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

import java.util.ArrayList;
import java.util.List;

/**
 * What /metadata/ offers as filter values, with how many entries each one has.
 * <p>
 * The values are the ones the search endpoint accepts for its machinetype and genretype
 * parameters, so a filter built from this cannot offer something the server will reject, and
 * the counts say which ones are worth offering at all.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Metadata {
    public Facet machinetypes;
    public Facet genretypes;
    public Facet features;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Facet {
        /** Name of the search parameter these values go to, e.g. "machinetype". */
        public String parameter;
        public String type;
        public List<Value> values;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Value {
        /** Set for machinetypes and genretypes. */
        public String value;
        /** Set for features, which are named differently. */
        public String groupname;
        public int doc_count;

        public String name() {
            return value != null ? value : groupname;
        }

        @Override
        public String toString() {
            return name();
        }
    }

    /** Names of a facet's values, worth offering, commonest first. Empty when absent. */
    public static List<String> namesOf(Facet facet, int minimumEntries) {
        List<String> names = new ArrayList<>();
        if (facet == null || facet.values == null) {
            return names;
        }
        facet.values.stream()
            .filter(value -> value.doc_count >= minimumEntries && value.name() != null)
            .sorted((a, b) -> b.doc_count - a.doc_count)
            .forEach(value -> names.add(value.name()));
        return names;
    }
}
