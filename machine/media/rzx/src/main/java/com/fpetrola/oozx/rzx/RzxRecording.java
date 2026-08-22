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
