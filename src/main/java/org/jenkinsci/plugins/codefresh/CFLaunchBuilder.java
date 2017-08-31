package org.jenkinsci.plugins.codefresh;

import com.google.gson.JsonObject;
import hudson.AbortException;
import hudson.Launcher;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.model.BuildBadgeAction;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.plugins.git.BranchSpec;
import hudson.plugins.git.GitSCM;
import hudson.scm.SCM;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.ListBoxModel.Option;
import hudson.util.Secret;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import java.io.IOException;
import static java.lang.Thread.sleep;
import java.net.MalformedURLException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.lazy.LazyBuildMixIn;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.kohsuke.stapler.QueryParameter;
import java.util.Properties;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;

public class CFLaunchBuilder extends Builder {

    private final String cfComposition;
    private final List<CFVariable> cfVars;
    private final boolean setCFVars;
     
    @DataBoundConstructor
    public CFLaunchBuilder(String cfComposition, SetCFVars setCFVars) {
       
        this.cfComposition = cfComposition;
        if (setCFVars != null) {
            this.setCFVars = true;
            this.cfVars = setCFVars.vars;
        } else {
            this.setCFVars = false;
            this.cfVars = null;
        }
    }

    public static class SetCFVars {

        private final List<CFVariable> vars;

        @DataBoundConstructor
        public SetCFVars(List<CFVariable> vars) {
            this.vars = vars;
        }
    }
    public List<CFVariable> getCfVars() {
        return cfVars;
    }
    
    public boolean isSetCFVars(){
        return setCFVars;
    }
    
    public String getCfComposition() {
        return cfComposition;
    }
    
    @Override
    public Descriptor getDescriptor() {
         return (Descriptor) super.getDescriptor();
    }
    
    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {

        CFProfile profile = null;
        CFGlobalConfig config = CFGlobalConfig.get();
        try{
            profile= new CFProfile(config.getCfUser(), config.getCfToken(), config.getCfUrl(), false);
        }
        catch (NullPointerException ne)
        {
            listener.getLogger().println("Couldn't get Codefresh profile details. Please check your system configuration.");
            return false;
        }

    
        CFApi api = new CFApi(config.getCfToken(), config.getCfUrl(), config.isSelfSignedCert());
        
        try {
                listener.getLogger().println("*******\n");
                String compositionId = profile.getCompositionIdByName(cfComposition);
                String launchId = api.launchComposition(compositionId, cfVars);
                JsonObject process = api.getProcess(launchId);
                String processUrl = api.getBuildUrl(launchId);
                String status = process.get("status").getAsString();
                while (status.equals("pending") || status.equals("running") || status.equals("elected")) {
                    listener.getLogger().println("Launching Codefresh composition environment: "+cfComposition+".\n Waiting 5 seconds...");
                    Thread.sleep(5 * 1000);
                    status = api.getProcess(launchId).get("status").getAsString();
                }
                listener.getLogger().print(api.getFinalLogs(api.getProcess(launchId).get("progress").getAsString()  + "\n"));
                switch (status) {
                    case "success":
                        String envUrl = api.getEnvUrl(api.getProcess(launchId));
                        build.addAction(new CodefreshBuildBadgeAction(envUrl, status, "Environment"));
                        build.addAction(new CodefreshEnvVarAction("CODEFRESH_ENV_URL", envUrl));
                        listener.getLogger().println("\nCodefresh environment launched successfully - " + envUrl);
                        return true;
                    case "error":
                        build.addAction(new CodefreshBuildBadgeAction(processUrl, status, "Environment"));
                        listener.getLogger().println("\nCodefresh enironment launch failed!");
                        return false;
                    default:
                        build.addAction(new CodefreshBuildBadgeAction(processUrl, status, "Environment"));
                        listener.getLogger().println("\n Codefresh environment launch exited with status " + status + ".");
                        return false;
                }
            } catch (Exception ex) {

                Logger.getLogger(CFLaunchBuilder.class.getName()).log(Level.SEVERE, null, ex);
                listener.getLogger().println("\nCodefresh environment launch failed with exception: " + ex.getMessage() + ".");
                build.addAction(new CodefreshBuildBadgeAction("", "error", "Environment"));
                return false;
            }
    }


    public boolean performStep(Run run, TaskListener listener) throws IOException, InterruptedException {

        CFGlobalConfig config = CFGlobalConfig.get();
        CFProfile profile = new CFProfile(config.getCfUser(), config.getCfToken(), config.getCfUrl(), false);
       
                
        CFApi api = new CFApi(config.getCfToken(), config.getCfUrl(),config.isSelfSignedCert());
        try {
            listener.getLogger().println("*******\n");
            String compositionId = profile.getCompositionIdByName(cfComposition);
            if (compositionId == null){
                //listener.getLogger().println("Composition " + cfComposition + " not found. Exiting");
                throw new AbortException("Composition " + cfComposition + " not found. Exiting");
            }
            String launchId = api.launchComposition(compositionId, cfVars);
            JsonObject process = api.getProcess(launchId);
            String status = process.get("status").getAsString();
            String processUrl = api.getBuildUrl(launchId);
            while (status.equals("pending") || status.equals("running") || status.equals("elected")) {
                listener.getLogger().println("Launching Codefresh composition environment: "+cfComposition+".\n Waiting 5 seconds...");
                Thread.sleep(5 * 1000);
                status = api.getProcess(launchId).get("status").getAsString();
            }

            listener.getLogger().print(api.getFinalLogs(api.getProcess(launchId).get("progress").getAsString()  + "\n"));

            switch (status) {
                case "success":
                    String envUrl = api.getEnvUrl(api.getProcess(launchId));
                    run.addAction(new CodefreshBuildBadgeAction(envUrl, status, "Environment" ));
                    listener.getLogger().println("Codefresh environment launched successfully - " + envUrl);
                    return true;
                case "error":
                    run.addAction(new CodefreshBuildBadgeAction(processUrl, status, "Environment" ));
                    listener.getLogger().println("Codefresh enironment launch failed!");
                    return false;
                default:
                    run.addAction(new CodefreshBuildBadgeAction(processUrl, status, "Environment" ));
                    listener.getLogger().println("Codefresh environment launch exited with status " + status + ".");
                    return false;
            }
        } catch (Exception ex) {

            Logger.getLogger(CFLaunchBuilder.class.getName()).log(Level.SEVERE, null, ex);
            listener.getLogger().println("Codefresh environment launch failed with exception: " + ex.getMessage() + ".");
            run.addAction(new CodefreshBuildBadgeAction("", "error", "Environment" ));
            throw new AbortException(ex.getMessage());
        }

    }

    @Extension
    public static final class Descriptor extends BuildStepDescriptor<Builder> {

        private CFApi api;
        public Descriptor()
        {
            load();
        }
        
        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        @Override
        public String getDisplayName() {
            return "Launch Codefresh Composition";
        }
        
        public ListBoxModel doFillCfCompositionItems(@QueryParameter("cfComposition") String cfComposition) throws IOException, MalformedURLException {
            ListBoxModel items = new ListBoxModel();
            String cfToken, cfUrl = null;
            boolean selfSignedCert = false;
            try {               
                CFGlobalConfig config = CFGlobalConfig.get();
                cfToken = config.getCfToken().getPlainText();
                cfUrl = config.getCfUrl();
                selfSignedCert = config.isSelfSignedCert();
                
            } catch (NullPointerException ne) {
                Logger.getLogger(CodefreshPipelineStep.class.getName()).log(Level.SEVERE, null, ne);
                return null;
            }
            try {
                api = new CFApi(Secret.fromString(cfToken), cfUrl, selfSignedCert);
                for (CFComposition comp : api.getCompositions()) {
                    String name = comp.getName();
                    items.add(new Option(name, name, cfComposition.equals(name)));

                }
            } catch (IOException e) {
                throw e;
            }
            return items;
        }
        
    }
   
 
}
