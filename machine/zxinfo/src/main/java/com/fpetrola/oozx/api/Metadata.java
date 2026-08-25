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
