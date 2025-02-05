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

package com.fpetrola.z80.minizx.emulation.finders;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;
import com.google.gson.*;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;


public class MultimapAdapter implements JsonDeserializer<Multimap<String, ?>>, JsonSerializer<Multimap<String, ?>> {
  public Multimap<String, ?> deserialize(JsonElement json, Type type,
                                         JsonDeserializationContext context) throws JsonParseException {
    final HashMultimap<String, Object> result = HashMultimap.create();
    final Map<String, Collection<Object>> map = context.deserialize(json, multimapTypeToMapType(type));
    for (final Map.Entry<String, Collection<Object>> e : map.entrySet()) {
      result.putAll(e.getKey(), e.getValue());
    }
    return result;
  }


  @Override
  public JsonElement serialize(Multimap<String, ?> src, Type type, JsonSerializationContext context) {
    final Map<?, ?> map = src.asMap();
    return context.serialize(map);
  }


  private <V> Type multimapTypeToMapType(Type type) {
    final Type[] typeArguments = ((ParameterizedType) type).getActualTypeArguments();
    assert typeArguments.length == 2;
    @SuppressWarnings("unchecked")

    final TypeToken<Map<String, Collection<V>>> mapTypeToken = new TypeToken<Map<String, Collection<V>>>() {
    }.where(new TypeParameter<V>() {
    }, (TypeToken<V>) TypeToken.of(typeArguments[1]));
    return mapTypeToken.getType();
  }

  public static Gson getGson() {
    final MultimapAdapter multimapAdapter = new MultimapAdapter();
    final Type type = new TypeToken<SetMultimap<Integer, Integer>>() {
    }.getType();
    //		final Type type2 = new TypeToken<Multimap<String, Obj>>() {}.getType();
    final Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(SetMultimap.class, multimapAdapter)
        .create();
    return gson;
  }
}
