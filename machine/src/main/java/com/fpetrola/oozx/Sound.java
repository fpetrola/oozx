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

package com.fpetrola.oozx;

public class Sound {
  public static boolean enabled = true;

  public static void frame() {
//    Z80.audio.endFrame();
//    Z80.audio.sendAudioFrame();
  }

  public static void beeper(long tstates, int i) {
//    Z80.audio.updateAudio((int) tstates, i);
  }

  public static void end() {
  }

  public static void init(String soundDevice) {

  }

  public static void pause() {

  }

  public static void unpause() {

  }

}
