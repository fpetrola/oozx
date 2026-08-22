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

package com.fpetrola.oozx.rzx;

import java.util.List;

/**
 * Somebody's recorded playthrough, as the RZX Archive lists it.
 *
 * @param spectrumComputingId the game this is a recording of, in the same id space the ZXInfo
 *                            API uses, which is what lets a search result be matched to it
 * @param note                what the archive says about the recording, such as needing TR-DOS
 *                            or having been made with Rollback
 * @param distributionDenied  the archive lists it but cannot hand it over, so there is
 *                            something to show and nothing to play
 */
public record RzxRecording(String id,
                           String title,
                           String submitter,
                           String note,
                           Integer spectrumComputingId,
                           String sourceUrl,
                           String worldOfSpectrumUrl,
                           String videoUrl,
                           List<RzxDownload> downloads,
                           boolean distributionDenied) {

  public boolean isPlayable() {
    return !downloads.isEmpty();
  }

  /** The file to fetch. Recordings come as a bare .rzx or as a zip holding one. */
  public RzxDownload download() {
    return downloads.isEmpty() ? null : downloads.get(0);
  }

  public record RzxDownload(String url, int sizeKb) {

    public boolean isZipped() {
      return url.toLowerCase().endsWith(".zip");
    }
  }
}
