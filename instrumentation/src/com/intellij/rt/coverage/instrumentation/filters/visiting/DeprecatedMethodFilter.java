/*
 * Copyright 2000-2022 JetBrains s.r.o.
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

package com.intellij.rt.coverage.instrumentation.filters.visiting;

import com.intellij.rt.coverage.instrumentation.Instrumenter;
import com.intellij.rt.coverage.instrumentation.filters.enumerating.KotlinDefaultArgsBranchFilter;
import com.intellij.rt.coverage.instrumentation.kotlin.KotlinUtils;
import org.jetbrains.coverage.gnu.trove.TIntHashSet;
import org.jetbrains.coverage.gnu.trove.TIntProcedure;
import org.jetbrains.coverage.org.objectweb.asm.AnnotationVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

import java.util.HashSet;

public class DeprecatedMethodFilter extends MethodVisitingFilter {
  private static final String DEPRECATED_METHODS = "DEPRECATED_METHODS_SET";
  private boolean myShouldIgnoreMethod = false;
  private TIntHashSet myMethodLines;
  private String myName;

  public boolean isApplicable(Instrumenter context, int access, String name, String desc, String signature, String[] exceptions) {
    return KotlinUtils.isKotlinClass(context);
  }

  @Override
  public void initFilter(MethodVisitor methodVisitor, Instrumenter context, String name, String desc) {
    super.initFilter(methodVisitor, context, name, desc);
    myName = name;
    if (name.endsWith(KotlinDefaultArgsBranchFilter.DEFAULT_ARGS_SUFFIX)) {
      final Object property = myContext.getProperty(DEPRECATED_METHODS);
      if (property != null) {
        //noinspection unchecked
        final HashSet<String> deprecatedMethods = (HashSet<String>) property;
        final String originalName = name.substring(0, name.length() - KotlinDefaultArgsBranchFilter.DEFAULT_ARGS_SUFFIX.length());
        myShouldIgnoreMethod |= deprecatedMethods.contains(originalName);
      }
    }
  }

  @Override
  public void visitLineNumber(int line, Label start) {
    if (myShouldIgnoreMethod && myContext.getLineData(line) == null) {
      if (myMethodLines == null) myMethodLines = new TIntHashSet();
      myMethodLines.add(line);
    }
    super.visitLineNumber(line, start);
  }

  @Override
  public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
    final AnnotationVisitor av = super.visitAnnotation(descriptor, visible);
    if (!"Lkotlin/Deprecated;".equals(descriptor)) return av;
    return new AnnotationVisitor(Opcodes.API_VERSION, av) {
      @Override
      public void visitEnum(String name, String descriptor, String value) {
        super.visitEnum(name, descriptor, value);
        if (!"Lkotlin/DeprecationLevel;".equals(descriptor)) return;
        myShouldIgnoreMethod |= "ERROR".equals(value) || "HIDDEN".equals(value);
      }
    };
  }

  @Override
  public void visitEnd() {
    super.visitEnd();
    if (myShouldIgnoreMethod) {
      Object property = myContext.getProperty(DEPRECATED_METHODS);
      if (property == null) {
        property = new HashSet<String>();
        myContext.addProperty(DEPRECATED_METHODS, property);
      }
      //noinspection unchecked
      final HashSet<String> deprecatedMethods = (HashSet<String>) property;
      deprecatedMethods.add(myName);
    }
    if (!myShouldIgnoreMethod || myMethodLines == null) return;
    myMethodLines.forEach(new TIntProcedure() {
      public boolean execute(int line) {
        myContext.removeLine(line);
        return true;
      }
    });
  }
}