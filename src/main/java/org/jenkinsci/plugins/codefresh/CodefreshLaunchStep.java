/*
 * The MIT License
 *
 * Copyright 2016 antweiss.
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

import com.google.inject.Inject;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

/**
 *
 * @author antweiss
 */
public class CodefreshLaunchStep extends AbstractStepImpl {
    private String cfComposition = "";
    private List<CFVariable> cfVars = null;  
    
    
    @DataBoundConstructor
    public CodefreshLaunchStep() {
         this.cfComposition = "";
                         
    }
    
    public List<CFVariable> getCfVars(){
        return cfVars;
    }

    
    public String getCfComposition() {
        return cfComposition;
    }
    
    @DataBoundSetter
    public void setCfComposition(String cfComposition) {
        this.cfComposition = cfComposition;
    }
    
    @DataBoundSetter
    public void setCfVars(List<CFVariable> cfVars) {
        this.cfVars = cfVars;
    }
    
  
    @Extension
    public static final class DescriptorImpl extends AbstractStepDescriptorImpl {
        
        public DescriptorImpl() {
            super(Execution.class);
        }
        @Override
        public String getFunctionName() {
            return "codefreshLaunch";
        }
        
        @Override
        public String getDisplayName() {
            return "Launch a Codefresh Composition";
        }
        
        
        public ListBoxModel doFillCfCompositionItems(@QueryParameter("cfComposition") String cfComposition) throws  IOException, MalformedURLException {
            ListBoxModel items = new ListBoxModel();
            //default to global config values if not set in step, but allow step to override all global settings
            Jenkins jenkins;
            String cfToken, cfUrl = null;
            boolean selfSignedCert = false;
            
            try {
                CFApi api = new CFApi();
                for (CFComposition composition: api.getCompositions())
                {
                    String name = composition.getName();
                    items.add(new ListBoxModel.Option(name, name, cfComposition.equals(name)));

                }
            }
            catch (IOException e)
            {
                    throw e;
            }
            return items;
        }
        
      }

    
    public static final class Execution extends AbstractSynchronousNonBlockingStepExecution<Boolean> {

        @Inject
        private transient CodefreshLaunchStep step;

        @StepContextParameter
        private transient Run run;

        
        @StepContextParameter
        private transient Launcher launcher;


        @StepContextParameter
        private transient TaskListener listener;
        
   
        @Override
        protected Boolean run() throws Exception {
            
        CFLaunchBuilder.SetCFVars setVars = new CFLaunchBuilder.SetCFVars(step.cfVars);
        CFLaunchBuilder builder = new CFLaunchBuilder(step.cfComposition,setVars);
            return builder.performStep(run,listener);
        }
        
        private static final long serialVersionUID = 1L;
    }
}
