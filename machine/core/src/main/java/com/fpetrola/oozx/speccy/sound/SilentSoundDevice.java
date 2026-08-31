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

package com.fpetrola.oozx.speccy.sound;

/**
 * A sound device that opens nothing.
 * <p>
 * Tests used to get the real one, which asks the operating system for an audio line. On Linux that
 * reaches PipeWire through JNI, and a crash there is a crash of the whole JVM: the run stops where
 * it stands and every test after it is simply never reached. Nothing under test needs a speaker.
 */
public class SilentSoundDevice extends JavaSoundDevice {

  public int sound_lowlevel_init(String device, int[] freqPtr, int[] stereoPtr) {
    return 0;
  }

  public void sound_lowlevel_frame(int[] data, int len) {
  }

  public void sound_lowlevel_end() {
  }
}
