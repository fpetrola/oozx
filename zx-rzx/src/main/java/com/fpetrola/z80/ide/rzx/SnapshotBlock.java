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

package com.fpetrola.z80.ide.rzx;

public class SnapshotBlock {
  private boolean externalData;
  private boolean compressed;
  private String snapshotExtension;
  private int uncompressedLength;
  private byte[] snapshotData;

  public String getSnapshotExtension() {
    return snapshotExtension;
  }

  public int getUncompressedLength() {
    return uncompressedLength;
  }

  public byte[] getSnapshotData() {
    return snapshotData;
  }

  public boolean isExternalData() {
    return externalData;
  }

  public void setExternalData(boolean externalData) {
    this.externalData = externalData;
  }

  public boolean isCompressed() {
    return compressed;
  }

  public void setCompressed(boolean compressed) {
    this.compressed = compressed;
  }

  public void setSnapshotExtension(String snapshotExtension) {
    this.snapshotExtension = snapshotExtension;
  }

  public void setUncompressedLength(int uncompressedLength) {
    this.uncompressedLength = uncompressedLength;
  }

  public void setSnapshotData(byte[] snapshotData) {
    this.snapshotData = snapshotData;
  }
}
