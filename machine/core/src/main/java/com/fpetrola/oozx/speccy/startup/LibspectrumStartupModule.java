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

package com.fpetrola.oozx.speccy.startup;

public class LibspectrumStartupModule extends AbstractStartupModule {
  public LibspectrumStartupModule() {
    super(DisplayStartupModule.class);
  }


  public void init() {
    //        if (Libspectrum.checkVersion(LIBSPECTRUM_MIN_VERSION)) {
//            if (Libspectrum.init() != 0) return 1;
//        } else {
//            userInterface.error(UiError.ERROR, "libspectrum version %s found, but %s required",
//                    Libspectrum.version(), LIBSPECTRUM_MIN_VERSION);
//            return 1;
//        }
  }

  public void endFn() {
  }

}
