/*
 *
 *  * Copyright (c) 2023-2024 Fernando Damian Petrola
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

@Path("/v3")
@Produces(MediaType.APPLICATION_JSON)
public interface ZxInfoClient {

    @GET
    @Path("/search")
    SearchResponse searchGames(
        @QueryParam("query") String query,
        @QueryParam("size") @DefaultValue("10") int size,
        @QueryParam("offset") @DefaultValue("0") int offset
    );

    @GET
    @Path("/games/{id}")
    GameEntry getGameDetails(@PathParam("id") String id);
}