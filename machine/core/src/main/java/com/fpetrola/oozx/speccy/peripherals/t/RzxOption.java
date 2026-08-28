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
package com.fpetrola.oozx.speccy.peripherals.t;

/**
 * A recording of a game, offered for playing, and where it was found.
 * <p>
 * Two catalogues list them and neither contains the other: over 329 games from three searches,
 * 26 were in both, 15 only in what ZXDB hosts and 108 only in the RZX Archive. So both are asked
 * and the answers put together, with each one saying where it came from - the Archive knows who
 * recorded it, which ZXDB does not.
 *
 * @param label what to show in the menu
 * @param url   where to fetch it, a .rzx or a .zip holding one
 */
public record RzxOption(String label, String url) {
}
