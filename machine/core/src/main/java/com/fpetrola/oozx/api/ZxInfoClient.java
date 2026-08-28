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

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.annotations.jaxrs.PathParam;

import java.util.List;

@Path("/v3")
@Produces(MediaType.APPLICATION_JSON)
public interface ZxInfoClient {

    /** JSON detail level accepted by /search and /games/{id}. The API defaults to "compact". */
    String MODE_TINY = "tiny";
    String MODE_COMPACT = "compact";
    String MODE_FULL = "full";

    /**
     * General search. Only "query" is required; every other parameter narrows the result down.
     * Pass null to leave a filter out - RESTEasy omits null query params.
     */
    @GET
    @Path("/search")
    SearchResponse searchGames(
        @QueryParam("query") String query,
        @QueryParam("size") @DefaultValue("10") int size,
        @QueryParam("offset") @DefaultValue("0") String offset,
        @QueryParam("mode") @DefaultValue(MODE_COMPACT) String mode,
        @QueryParam("titlesonly") Boolean titlesOnly,
        @QueryParam("sort") String sort,
        @QueryParam("contenttype") String contentType,
        @QueryParam("year") Integer year,
        @QueryParam("language") String language,
        @QueryParam("genretype") String genreType,
        @QueryParam("genresubtype") String genreSubType,
        @QueryParam("machinetype") String machineType,
        @QueryParam("controls") String controls,
        @QueryParam("multiplayermode") String multiplayerMode,
        @QueryParam("multiplayertype") String multiplayerType,
        @QueryParam("originalpublication") String originalPublication,
        @QueryParam("availability") String availability,
        @QueryParam("tosectype") String tosecType,
        @QueryParam("group") String group,
        @QueryParam("groupname") String groupName
    );

    @GET
    @Path("/games/{id}")
    GameResponse getGameDetails(@PathParam("id") String id, @QueryParam("mode") String mode);

    /** The values the search filters accept, with how many entries each one has. */
    @GET
    @Path("/metadata/")
    Metadata getMetadata();

    /** Suggestions across titles, publishers and authors, for type-as-you-go fields. */
    @GET
    @Path("/suggest/{term}")
    List<Suggestion> getSuggestions(@PathParam("term") String term);

    @GET
    @Path("/suggest/author/{term}")
    List<Suggestion> getSuggestionsAuthor(@PathParam("term") String term);

    @GET
    @Path("/suggest/publisher/{term}")
    List<Suggestion> getSuggestionsPublisher(@PathParam("term") String term);

    /**
     * Looks up the entry a file belongs to, by MD5 (32 chars) or SHA512 (128 chars)
     * of the tape/disk image itself. Answers 404 when nothing matches.
     */
    @GET
    @Path("/filecheck/{hash}")
    FileCheckResult getFileByHash(@PathParam("hash") String hash);
}
