/*
 * The MIT License
 *
 * Copyright 2017 antweiss.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.jenkinsci.plugins.codefresh;

import org.jvnet.hudson.test.JenkinsRule;
import org.apache.commons.io.FileUtils;
import hudson.model.*;
import hudson.tasks.Shell;
import net.sf.json.JSONObject;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import org.junit.Rule;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
/**
 *
 * @author antweiss
 */
public class CFPipelineBuilderTest {

  @Rule public JenkinsRule j = new JenkinsRule();
  @Test public void first() throws Exception {
//    CFGlobalConfig config = new CFGlobalConfig();
//    String creds = "{\"cfUser\" : \"antweiss\","
//            + "\"cfUrl\" : \"https://app-staging.codefresh.io\","
//            + "\"cfToken\":\"eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJfaWQiOiI1OTkyYjVhYmE4MzYyNzAwMDE5OTdkYzIiLCJhY2NvdW50SWQiOiI1OTkyYjVhYmE4MzYyNzAwMDE5OTdkYzMiLCJpYXQiOjE1MDI3ODY5ODcsImV4cCI6MTUwNTM3ODk4N30.VjWstakdABpAitIJYlIgRNqTJxhSIxNCJvk7CKw0DeQ\"}";
//    JSONObject formData =  JSONObject.fromObject(creds);
//    config.configure(Stapler.getCurrentRequest(), formData);
//    FreeStyleProject project = j.createFreeStyleProject();
//    project.getBuildersList().add(new CodefreshPipelineBuilder(new CodefreshPipelineBuilder.SelectPipeline("oto-orders", ""), null));
//    FreeStyleBuild build = project.scheduleBuild2(0).get();
//    System.out.println(build.getDisplayName() + " completed");
//
//    String s = FileUtils.readFileToString(build.getLogFile());
//    assertThat(s, containsString("+ running"));
  }
}

