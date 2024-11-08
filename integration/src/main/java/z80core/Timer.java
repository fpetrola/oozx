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

package z80core;

public class Timer {

  protected long averageTime;
  private long startTime;
  private long endTime;
  protected long elapsedTime;
  private long sumTime;
  private long times;
  private long lower = Long.MAX_VALUE;
  private String name;

  public Timer(String name) {
    this.name = name;
  }

  public long end() {
    endTime = System.nanoTime();
    elapsedTime = endTime - startTime;
    if (times > 1000 && elapsedTime > averageTime)
      elapsedTime= averageTime;

    sumTime += elapsedTime;
    averageTime = sumTime / ++times;
    
    if (elapsedTime < lower) {
      lower = elapsedTime;
      System.out.println(name + ": lower -> " + lower);
    }

    
    if (times % 1000000 == 0) {
      System.out.println(name + ": average -> " + averageTime);
    }

    return elapsedTime;
  }

  public void start() {
    startTime = System.nanoTime();
  }

  public long average() {
    return averageTime;
  }

  public void reset() {
    lower = Long.MAX_VALUE;
    sumTime= 0;
    times= 0;
  }

}