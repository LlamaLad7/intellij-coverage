/*
 * Copyright 2000-2018 JetBrains s.r.o.
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

package com.intellij.rt.coverage.instrumentation;

import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.instrumentation.filters.FilterUtils;
import com.intellij.rt.coverage.instrumentation.filters.visiting.MethodVisitingFilter;
import com.intellij.rt.coverage.util.ClassNameUtil;
import com.intellij.rt.coverage.util.StringsPool;
import org.jetbrains.coverage.gnu.trove.TIntHashSet;
import org.jetbrains.coverage.gnu.trove.TIntObjectHashMap;
import org.jetbrains.coverage.org.objectweb.asm.ClassVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;

public abstract class Instrumenter extends MethodFilteringVisitor {
  protected final ProjectData myProjectData;
  private final boolean myShouldCalculateSource;

  protected TIntObjectHashMap<LineData> myLines = new TIntObjectHashMap<LineData>(4, 0.99f);
  private TIntHashSet myIgnoredLines;
  protected int myMaxLineNumber = -1;

  protected ClassData myClassData;
  protected boolean myProcess;
  private boolean myIgnoreSection;

  public Instrumenter(final ProjectData projectData, ClassVisitor classVisitor, String className, boolean shouldCalculateSource) {
    super(classVisitor, className);
    myProjectData = projectData;
    myShouldCalculateSource = shouldCalculateSource;
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    myProcess = (access & Opcodes.ACC_INTERFACE) == 0;
    myClassData = myProjectData.getOrCreateClassData(StringsPool.getFromPool(getClassName()));
    super.visit(version, access, name, signature, superName, interfaces);
  }


  public MethodVisitor visitMethod(final int access,
                                   final String name,
                                   final String desc,
                                   final String signature,
                                   final String[] exceptions) {
    final MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
    if (mv == null) return null;
    if (!shouldInstrumentMethod(access, name, desc, signature, exceptions)) return mv;
    myProcess = true;
    final MethodVisitor filters = chainFilters(mv, access, name, desc, signature, exceptions);
    return new MethodVisitor(Opcodes.API_VERSION, filters) {
      @Override
      public void visitLineNumber(int line, Label start) {
        if (isIgnoreSection()) onIgnoredLine(line);
        super.visitLineNumber(line, start);
      }
    };

  }

  private MethodVisitor chainFilters(MethodVisitor root, int access, String name,
                                     String desc, String signature, String[] exceptions) {
    root = createMethodLineEnumerator(root, name, desc, access, signature, exceptions);
    for (MethodVisitingFilter filter : FilterUtils.createVisitingFilters()) {
      if (filter.isApplicable(this, access, name, desc, signature, exceptions)) {
        filter.initFilter(root, this, name, desc);
        root = filter;
      }
    }
    return root;
  }

  protected abstract MethodVisitor createMethodLineEnumerator(MethodVisitor mv, String name, String desc, int access, String signature, String[] exceptions);

  public void visitEnd() {
    if (myProcess) {
      initLineData();
      myLines = null;
      myClassData.setIgnoredLines(myIgnoredLines);
      myIgnoredLines = null;
    }
    super.visitEnd();
  }

  protected abstract void initLineData();

  protected LineData getOrCreateLineData(int line, String name, String desc) {
    //create lines again if class was loaded again by another class loader; may be myLinesArray should be cleared
    if (myLines == null) myLines = new TIntObjectHashMap<LineData>();
    LineData lineData = myLines.get(line);
    if (lineData == null) {
      lineData = new LineData(line, StringsPool.getFromPool(name + desc));
      myLines.put(line, lineData);
      if (myIgnoredLines != null) myIgnoredLines.remove(line);
    }
    if (line > myMaxLineNumber) myMaxLineNumber = line;
    return lineData;
  }

  public void visitSource(String source, String debug) {
    super.visitSource(source, debug);
    if (myShouldCalculateSource) {
      myProjectData.getOrCreateClassData(StringsPool.getFromPool(getClassName())).setSource(source);
    }
    if (debug != null) {
      myProjectData.addLineMaps(getClassName(), JSR45Util.extractLineMapping(debug, getClassName()));
    }
  }

  public void visitOuterClass(String outerClassName, String methodName, String methodSig) {
    if (myShouldCalculateSource) {
      final String fqnName = ClassNameUtil.convertToFQName(outerClassName);
      final ClassData outerClass = myProjectData.getOrCreateClassData(StringsPool.getFromPool(fqnName));
      if (outerClass.getSource() == null) {
        outerClass.setSource(myClassData.getSource());
      }
    }
    super.visitOuterClass(outerClassName, methodName, methodSig);
  }

  public boolean isSampling() {
    return myProjectData.isSampling();
  }

  public LineData getLineData(int line) {
    return myLines.get(line);
  }

  public ProjectData getProjectData() {
    return myProjectData;
  }

  public void removeLine(final int line) {
    myLines.remove(line);
    onIgnoredLine(line);
  }

  private void onIgnoredLine(final int line) {
    if (myIgnoredLines == null) {
      myIgnoredLines = new TIntHashSet();
    }
    myIgnoredLines.add(line);
  }

  public int linesCount() {
    return myLines.size();
  }

  public boolean isIgnoreSection() {
    return myIgnoreSection;
  }

  /**
   * Set ignore flag. All the lines are ignored when this flag is enabled.
   */
  public void setIgnoreSection(boolean ignore) {
    myIgnoreSection = ignore;
  }
}