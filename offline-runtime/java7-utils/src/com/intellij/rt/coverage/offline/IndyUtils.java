/*
 * Copyright 2000-2024 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.rt.coverage.offline;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

@SuppressWarnings("unused")
public class IndyUtils {
  public static CallSite getOrCreateHits(MethodHandles.Lookup lookup, String name, MethodType type, String className, int length) {
    return constant(RawProjectInit.getOrCreateHits(className, length));
  }

  public static CallSite getOrCreateHitsMask(MethodHandles.Lookup lookup, String name, MethodType type, String className, int length) {
    return constant(RawProjectInit.getOrCreateHitsMask(className, length));
  }

  private static CallSite constant(Object cst) {
    return new ConstantCallSite(MethodHandles.constant(Object.class, cst));
  }
}
