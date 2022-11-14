/*
 * Copyright 2000-2020 JetBrains s.r.o.
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

package com.intellij.rt.coverage.instrumentation.filters.methods;

import com.intellij.rt.coverage.instrumentation.InstrumentationUtils;
import com.intellij.rt.coverage.instrumentation.MethodFilteringVisitor;
import com.intellij.rt.coverage.instrumentation.filters.KotlinUtils;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

public class KotlinSyntheticConstructorOfSealedClassFilter implements MethodFilter {
  public boolean shouldFilter(int access, String name, String desc, String signature, String[] exceptions, MethodFilteringVisitor context) {
    if (isSealedConstructor(access, name, desc, context)) {
      context.addProperty(KotlinUtils.SEALED_CLASS_LABEL, true);
      return true;
    }
    return false;
  }

  private static boolean isSealedConstructor(int access, String name, String desc, MethodFilteringVisitor context) {
    return (access & Opcodes.ACC_SYNTHETIC) != 0
        && InstrumentationUtils.CONSTRUCTOR.equals(name)
        && desc.endsWith(KotlinUtils.KOTLIN_DEFAULT_CONSTRUCTOR_MARKER + ")V")
        && context.isAbstract();
  }
}
