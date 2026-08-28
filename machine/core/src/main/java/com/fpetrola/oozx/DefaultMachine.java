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

package com.fpetrola.oozx;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The model a machine falls back to: the 48K.
 * <p>
 * Switching models passes through it first, so the new machine starts from a known state. It
 * used to be whichever model happened to be registered first, which was true only because Speccy
 * listed the 48K at the head of a varargs call — reordering that list would have changed the
 * default with nothing failing to say so. Naming it makes the order mean nothing.
 */
@BindingAnnotation
@Retention(RetentionPolicy.RUNTIME)
public @interface DefaultMachine {
}
