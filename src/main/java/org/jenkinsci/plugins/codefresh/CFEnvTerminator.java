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

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.BuildBadgeAction;
import hudson.model.BuildListener;
import hudson.model.Project;
import hudson.model.Result;
import hudson.model.Run;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import java.io.IOException;
import static java.lang.System.in;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.codefresh.CodefreshBuildBadgeAction;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 *
 * @author antweiss
 */
public class CFEnvTerminator extends Recorder {

    
    private boolean terminateOnFailure;
    
    @DataBoundConstructor
    public CFEnvTerminator(boolean terminateOnFailure){
        this.terminateOnFailure = terminateOnFailure;
    }
    
    public boolean isTerminateOnFailure() {
        return terminateOnFailure;
    }
    
    @Override
    public BuildStepMonitor getRequiredMonitorService() {
         return BuildStepMonitor.BUILD;
    }
    
    @Override
    public boolean perform(AbstractBuild<?, ?> build,
                           Launcher launcher, final BuildListener listener)
            throws InterruptedException, IOException {

        if ( build.getResult() == Result.FAILURE && (!terminateOnFailure)){
            listener.getLogger().println("Not terminating the Codefresh environment as build result was FAILURE");
            return true;
        }
        
        listener.getLogger().print("\nCodefresh environment termination: ");
        try {
             
            String envUrl = build.getEnvironment(listener).get("CODEFRESH_ENV_URL");
            //listener.getLogger().println("ENV url is " + envUrl);
            if ( envUrl == null || envUrl.isEmpty() )
            {
                listener.getLogger().println("Couldn't get Codefresh environment url. Did you launch one?");
                return true;
            }
            
            CFApi api = new CFApi();

            try {
                    String envId = api.getEnvIdByURL(envUrl);
                    if (! api.terminateEnv(envId))
                    {
                        listener.getLogger().println("Couldn't terminate Codefresh environment. Did you launch one?");
                        return true;
                    }
                    else
                    {
                        listener.getLogger().println("Successfully terminated Codefresh environment " + envId + " at " + envUrl);
                        // remove environment url badge
                        List<BuildBadgeAction> actions = build.getBadgeActions();
                        for(Iterator badgeIterator = actions.iterator();
                                    badgeIterator.hasNext();) {
                            BuildBadgeAction b = (BuildBadgeAction) badgeIterator.next();
                            if(b instanceof CodefreshBuildBadgeAction) {
                                    if (((CodefreshBuildBadgeAction) b).getType().equals("Environment")){
                                        ((CodefreshBuildBadgeAction) b).setDisplayName("Codefresh Environment Terminated");
                                        ((CodefreshBuildBadgeAction) b).setUrl(null);

                                    }
                            }
                          }
                        return true;
                    }

                } catch (Exception ex) {
                    Logger.getLogger(CFEnvTerminator.class.getName()).log(Level.SEVERE, null, ex);
                    return true;
            }
        } catch (IOException e){
            listener.getLogger().println("Couldn't get Codefresh configuration. Did you define one?");
            return true;
        }

    }
      
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher>{

            
            @Override
            public boolean isApplicable(Class<? extends AbstractProject> type) {
                return true;
            }
            
             public DescriptorImpl()
            {
                load();
            }
           
            @Override
            public String getDisplayName() {
                return "Terminate Codefresh Environment";
            }
    }
        

   
}
