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

package com.fpetrola.z80.transform.a;

import com.github.javaparser.ast.expr.Expression;

import java.util.ArrayList;
import java.util.List;

class FieldUsageData {
  public Class<?> fieldType;
  public String initializer;
  boolean isReadBeforeWrite;
    boolean isModified;
    List<Expression> readUsages = new ArrayList<>();
    List<Expression> writeUsages = new ArrayList<>();
  }