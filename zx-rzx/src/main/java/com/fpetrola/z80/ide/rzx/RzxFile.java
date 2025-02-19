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

public class RzxFile {
  private final RzxHeader header;
  private final CreatorInfo creatorInfo;
  private final SnapshotBlock snapshotBlock;
  private final InputRecordingBlock inputRecordingBlock;
  /**
   * File bytes before the input block (0x80) — header, creator, snapshot block — kept exactly
   * as read from disk, so extending a recording never has to re-serialize the initial state
   * (which this code can't write anyway and doesn't need to change): the extended file starts
   * from the same snapshot and only the frame list changes. Works for both embedded and
   * external-descriptor snapshots since it never interprets them. {@code null} if the file had
   * no 0x80 block.
   */
  private byte[] prefix;
  /** Input blocks exactly as read from disk, from the first 0x80 to end of file; {@code prefix
   *  + tail} reconstructs the whole original file for the append-new-block mode. */
  private byte[] tail;

  public RzxFile(RzxHeader header, CreatorInfo creatorInfo, SnapshotBlock snapshotBlock, InputRecordingBlock inputRecordingBlock) {
    this.header = header;
    this.creatorInfo = creatorInfo;
    this.snapshotBlock = snapshotBlock;
    this.inputRecordingBlock = inputRecordingBlock;
  }

  public RzxHeader getHeader() {
    return header;
  }

  public CreatorInfo getCreatorInfo() {
    return creatorInfo;
  }

  public SnapshotBlock getSnapshotBlock() {
    return snapshotBlock;
  }

  public InputRecordingBlock getInputRecordingBlock() {
    return inputRecordingBlock;
  }

  public byte[] getPrefix() {
    return prefix;
  }

  public void setPrefix(byte[] prefix) {
    this.prefix = prefix;
  }

  public byte[] getTail() {
    return tail;
  }

  public void setTail(byte[] tail) {
    this.tail = tail;
  }
}
