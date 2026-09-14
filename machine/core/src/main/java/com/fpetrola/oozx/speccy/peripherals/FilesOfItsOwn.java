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


package com.fpetrola.oozx.speccy.peripherals;

/**
 * A device that has files of its own beside the one a snapshot came from.
 * <p>
 * Here because a snapshot is a file somewhere, and not all of them come alone: the colours of a
 * game in 256 colours are in a file with the same name, and so would be a list of pokes. Whoever
 * loads a snapshot knows the path and has nothing to do with it, and what it should hand the path
 * to is not something it can be made to name.
 */
public interface FilesOfItsOwn {
  /** Where the snapshot came from, so that whoever keeps something beside it can go and look. */
  void beside(String url);
}
