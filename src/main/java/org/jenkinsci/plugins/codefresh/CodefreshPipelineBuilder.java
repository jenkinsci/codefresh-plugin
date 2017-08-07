package org.jenkinsci.plugins.codefresh;

import com.google.gson.JsonObject;
import hudson.AbortException;
import hudson.Launcher;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.model.BuildBadgeAction;
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
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import static javafx.scene.input.KeyCode.K;
import static javafx.scene.input.KeyCode.V;
import jenkins.model.lazy.LazyBuildMixIn;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.kohsuke.stapler.QueryParameter;

public class CodefreshPipelineBuilder extends Builder {

    private final String cfService;
    private final boolean selectService;
    private final boolean setCFVars;
    private final String cfBranch;
    private List<CFVariable> cfVars;
    

    @DataBoundConstructor
    public CodefreshPipelineBuilder(SelectService selectService, SetCFVars setCFVars){
 
        if (selectService != null) {
            this.cfService = selectService.cfService;
            this.cfBranch = selectService.cfBranch;
            this.selectService = true;
        } else {
            this.selectService = false;
            this.cfService = null;
            this.cfBranch = "";
        }
        if (setCFVars != null) {
            this.setCFVars = true;
            this.cfVars = setCFVars.vars;
        } else {
            this.setCFVars = false;
            this.cfVars = null;
        }

    }

    public static class SelectService {

        private final String cfService;
        private final String cfBranch;

        @DataBoundConstructor
        public SelectService(String cfService, String cfBranch) {
            this.cfService = cfService;
            this.cfBranch = cfBranch;
        }
    }
  
    public static class SetCFVars {

        private List<CFVariable> vars;

        @DataBoundConstructor
        public SetCFVars(List<CFVariable> vars) {
            this.vars = vars;
        }
    }
    public String getCfService() {
        return cfService;
    }

    public String getCfBranch() {
        return cfBranch;
    }

    public List<CFVariable> getCfVars() {
        return cfVars;
    }
    public boolean isSelectService() {
        return selectService;
    }
    
    public boolean isSetCFVars(){
        return setCFVars;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {

        CFProfile profile = null;
        CFGlobalConfig config = CFGlobalConfig.get();
        try{
            profile= new CFProfile(config.getCfUser(), config.getCfToken());
        }
        catch (NullPointerException ne)
        {
            listener.getLogger().println("Couldn't get Codefresh profile details. Please check your system configuration.");
            return false;
        }

        String serviceId = "";
        String gitPath = "";
        String branch = "";

        CFApi api = new CFApi(config.getCfToken());
    
        String serviceName = this.getCfService();

        if (serviceName == null) {
            SCM scm = build.getProject().getScm();
            if (!(scm instanceof GitSCM)) {
                listener.getLogger().println("Codefresh: you've specified you want to run a Codefresh pipeline,\n but we didn't find"
                        + " any git repository defined or service name specified for the build.\n"
                        + "Are you sure that's what you meant?" );
                return false;
            }

            final GitSCM gitSCM = (GitSCM) scm;
            branch = gitSCM.getBranches().get(0).getName().replaceFirst("\\*\\/", "");
            listener.getLogger().println("\nBranch " + branch + " detected.");
            RemoteConfig remote = gitSCM.getRepositories().get(0);
            URIish uri = remote.getURIs().get(0);
            gitPath = uri.getPath();
            //serviceName = gitPath.split("/")[2].split("\\.")[0];
            serviceId = profile.getServiceIdByPath(gitPath);
            if (serviceId == null) {
                listener.getLogger().println("\nUser " + config.getCfUser() + " has no Codefresh service defined for url " + gitPath + ".\n Exiting.");
                return false;
            }
        } else {

            serviceId = profile.getServiceIdByName(cfService);
            branch = cfBranch;
            if (serviceId == null) {
                listener.getLogger().println("\nService Id not found for " + cfService + ".\n Exiting.");
                return false;
        }

        listener.getLogger().println("\nTriggering Codefresh pipeline. Service: " + serviceName + ".\n");

        String buildId = api.startBuild(serviceId, branch, cfVars);
        JsonObject process = api.getProcess(buildId);
        String status = process.get("status").getAsString();
        String progressUrl = api.getBuildUrl(buildId);
            while (status.equals("pending") || status.equals("running") || status.equals("elected")) {
            listener.getLogger().println("Codefresh pipeline " + status + " - " + progressUrl + "\n Waiting 5 seconds...");
            Thread.sleep(5 * 1000);
            status = api.getProcess(buildId).get("status").getAsString();
        }

        listener.getLogger().print(api.getFinalLogs(api.getProcess(buildId).get("progress").getAsString() + "\n"));

        switch (status) {
            case "success":
                build.addAction(new CodefreshBuildBadgeAction(progressUrl, status, "Pipeline"));
                build.addAction(new CodefreshEnvVarAction("CODEFRESH_BUILD_URL", progressUrl));
                listener.getLogger().println("\n\nCodefresh build successfull!");
                break;
            case "error":
                build.addAction(new CodefreshBuildBadgeAction(progressUrl, status, "Pipeline"));
                listener.getLogger().println("Codefresh build failed!");
                return false;
            default:
                build.addAction(new CodefreshBuildBadgeAction(progressUrl, status, "Pipeline"));
                listener.getLogger().println("Codefresh build exited with status " + status + ".");
                return false;
        }

        
        }
        return true;
    }


    public boolean performStep(Run run, TaskListener listener) throws IOException, InterruptedException {

        CFGlobalConfig config = CFGlobalConfig.get();
        CFProfile profile = new CFProfile(config.getCfUser(), config.getCfToken());
        String serviceId = "";
        String gitPath = "";
        String branch = "";

       
        CFApi api = new CFApi(config.getCfToken());
            String serviceName = this.getCfService();

           if (serviceName == null) {
               listener.getLogger().println("\nUser " + config.getCfUser() + "has no Codefresh pipeline defined for url " + gitPath + ".\n Exiting.");
               return false;
           } else {

               serviceId = profile.getServiceIdByName(cfService);
               branch = cfBranch;
               if (serviceId == null) {
                   listener.getLogger().println("\nPipeline Id not found for " + cfService + ".\n Exiting.");
                   return false;
               }

          }


       listener.getLogger().println("\nTriggering Codefresh build. Pipeline: " + serviceName + ".\n");

       String buildId = api.startBuild(serviceId, branch, cfVars);
       JsonObject process = api.getProcess(buildId);
       String status = process.get("status").getAsString();
       String progressUrl = api.getBuildUrl(buildId);
       Integer timer = 0;
       while (status.equals("pending") || status.equals("running") || status.equals("elected")) {
           listener.getLogger().println("Codefresh build " + status + " - " + progressUrl + "\n");
           Thread.sleep(5 * 1000);
           timer += 5000;
           status = api.getProcess(buildId).get("status").getAsString();
       }
       String time = String.format("%02d min, %02d sec",TimeUnit.MILLISECONDS.toMinutes(timer),TimeUnit.MILLISECONDS.toSeconds(timer));
       listener.getLogger().println("Codefresh build finished. Duration: "+ time);
       listener.getLogger().print(api.getFinalLogs(api.getProcess(buildId).get("progress").getAsString() + "\n"));

       switch (status) {
           case "success":

               run.addAction(new CodefreshBuildBadgeAction(progressUrl, status, "Build"));
               listener.getLogger().println("\n\nCodefresh build successfull!");
               break;
           case "error":
               run.addAction(new CodefreshBuildBadgeAction(progressUrl, status, "Build"));
               listener.getLogger().println("Codefresh build failed!");
               return false;
           default:
               run.addAction(new CodefreshBuildBadgeAction(progressUrl, status, "Build"));
               listener.getLogger().println("Codefresh build exited with status " + status + ".");
               return false;
       }

        return true;
    }


    @Override
    public Descriptor getDescriptor() {
        return (Descriptor) super.getDescriptor();
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
        public String getDisplayName() {
            return "Run Codefresh Pipeline";
        }


        public ListBoxModel doFillCfServiceItems(@QueryParameter("cfService") String cfService) throws IOException, MalformedURLException {
            ListBoxModel items = new ListBoxModel();
            String cfToken = null;
            try {               
                CFGlobalConfig config = CFGlobalConfig.get();
                cfToken = config.getCfToken().getPlainText();
            } catch (NullPointerException ne) {
                Logger.getLogger(CodefreshPipelineStep.class.getName()).log(Level.SEVERE, null, ne);
                return null;
            }
            try {
                api = new CFApi(Secret.fromString(cfToken));
                for (CFService srv : api.getServices()) {
                    String name = srv.getName();
                    items.add(new Option(name, name, cfService.equals(name)));

                }
            } catch (IOException e) {
                throw e;
            }
            return items;
        }

    }

//    public static class CodefreshBuildBadgeAction implements BuildBadgeAction {
//
//        private String buildUrl;
//        private final String buildStatus;
//        private final String iconFile;
//        private final String type;
//        private String displayName;
//
//        public CodefreshBuildBadgeAction(String buildUrl, String buildStatus, String type) {
//            super();
//            this.buildUrl = buildUrl;
//            this.buildStatus = buildStatus;
//            this.type = type;
//            this.displayName = "Codefresh " + type + " Url";
//            switch (buildStatus) {
//                case "success":
//                    this.iconFile = "/plugin/codefresh/images/16x16/leaves_green.png";
//                    break;
//                case "unstable":
//                    this.iconFile = "/plugin/codefresh/images/16x16/leaves_yellow.png";
//                    break;
//                case "error":
//                    this.iconFile = "/plugin/codefresh/images/16x16/leaves_red.png";
//                    break;
//                default:
//                    this.iconFile = "/plugin/codefresh/images/16x16/leaves_red.png";
//            }
//        }
//
//        @Override
//        public String getDisplayName() {
//            return displayName;
//        }
//
//        @Override
//        public String getIconFileName() {
//            return iconFile;
//        }
//
//        @Override
//        public String getUrlName() {
//            return buildUrl;
//        }
//        
//        public String getType() {
//            return type;
//        }
//
//        public void setUrl(String Url) {
//            this.buildUrl = Url;
//        }
//        
//        public void setDisplayName(String name) {
//            this.displayName = name;
//        }
//    }

}
