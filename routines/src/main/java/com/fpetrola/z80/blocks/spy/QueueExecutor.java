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

package com.fpetrola.z80.blocks.spy;

import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class QueueExecutor {
  public Queue<Runnable> threadSafeQueue;
  BlockingQueue<String> blockingQueue = new LinkedBlockingDeque<>(20);

  public QueueExecutor() {
    initQueue();
  }

  public void initQueue() {
    threadSafeQueue = new ConcurrentLinkedQueue<>();

    Thread consumerThread = new Thread(() -> {
      while (true) {
        if (!threadSafeQueue.isEmpty()) {
          Runnable item = threadSafeQueue.poll();
          item.run();
        }
      }
    });

    consumerThread.start();
  }
}
