/*
 * Copyright 2000-2021 JetBrains s.r.o.
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

package com.intellij.rt.coverage.report;

import com.intellij.rt.coverage.util.ProcessUtil;
import org.junit.Test;

import java.io.File;

public class IntegrationTest {

  @Test
  public void testXML() throws Throwable {
    final Reporter reporter = TestUtils.runTest(".*", "testData.simple.Main");
    final File xmlFile = XMLCoverageReportTest.createXMLFile();
    final String quotes = getQuotes();

    final String[] commandLine = {
        "-classpath", System.getProperty("java.class.path"),
        "com.intellij.rt.coverage.report.Main",
        "reports=" + quotes + reporter.getReport().getDataFile().getAbsolutePath() + quotes
            + ":" + quotes + reporter.getReport().getSourceMapFile().getAbsolutePath() + quotes,
        "xml=" + quotes + xmlFile.getAbsolutePath() + quotes,
        "output=" + quotes + outputPath() + quotes};
    ProcessUtil.execJavaProcess(commandLine);
    XMLCoverageReportTest.verifyProjectXML(xmlFile, "xmlIntegrationTest.xml");
  }

  @Test
  public void testHTML() throws Throwable {
    final Reporter reporter = TestUtils.runTest(".*", "testData.simple.Main");
    final File htmlDir = HTMLCoverageReportTest.createHtmlDir(reporter.getReport().getDataFile());
    final String quotes = getQuotes();

    final String[] commandLine = {
        "-classpath", System.getProperty("java.class.path"),
        "com.intellij.rt.coverage.report.Main",
        "reports=" + quotes + reporter.getReport().getDataFile().getAbsolutePath() + quotes
            + ":" + quotes + reporter.getReport().getSourceMapFile().getAbsolutePath() + quotes,
        "html=" + quotes + htmlDir.getAbsolutePath() + quotes,
        "sources=" + quotes + new File("test").getAbsolutePath() + quotes,
        "output=" + quotes + outputPath() + quotes};
    ProcessUtil.execJavaProcess(commandLine);
    HTMLCoverageReportTest.verifyHTMLDir(htmlDir);
  }

  private String getQuotes() {
    return System.getProperty("os.name").startsWith("Windows") ? "\\\"" : "\"";
  }

  private static String outputPath() {
    return "build" + File.separator + "classes" + File.separator + "java" + File.separator + "test";
  }
}
