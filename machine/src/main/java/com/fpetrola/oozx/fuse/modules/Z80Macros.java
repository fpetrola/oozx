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

package com.fpetrola.oozx.fuse.modules;

public class Z80Macros {
  public static int BC = 1;
  public static int DE = 1;
  public static int HL = 1;
  public static int AF = 1;
  public static int IX = 1;
  public static int PC = 1;
  public static int FLAG_C = 1;
  public static int AF_ = 4;
  public static int A = 3;
  public static int F = 3;
  public static int B = 2;
  public static int C = 5;
  public static int D = 2;
  public static int E = 5;
  public static int H = 2;
  public static int L = 5;
  public int A_() {
    return 0;
  }

  public int F_() {
    return 0;
  }

  public int DE() {
    return 0;
  }

  public void CP(int i) {

  }

  public void INC(int b) {

  }
}
