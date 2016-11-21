package org.jenkinsci.plugins.codefresh;

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
import java.util.logging.Level;
import java.util.logging.Logger;
import jenkins.model.lazy.LazyBuildMixIn;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.kohsuke.stapler.QueryParameter;

public class CodefreshBuilder extends Builder {

    private final boolean launchCf;
    private boolean buildCf;
    private final String cfService;
    private final String cfComposition;
    private final boolean selectService;
    private final String cfBranch;

    @DataBoundConstructor
    public CodefreshBuilder(LaunchComposition launchCf, Boolean build, SelectService selectService) {
        this.buildCf = build;

        if (selectService != null) {
            this.cfService = selectService.cfService;
            this.cfBranch = selectService.cfBranch;
            this.selectService = true;
        } else {
            this.selectService = false;
            this.cfService = null;
            this.cfBranch = "";
        }

        if (launchCf != null) {
            this.cfComposition = launchCf.cfComposition;
            this.launchCf = true;
        } else {
            this.launchCf = false;
            this.cfComposition = null;
        }

    }

    public static class LaunchComposition {

        private final String cfComposition;

        @DataBoundConstructor
        public LaunchComposition(String cfComposition) {
            this.cfComposition = cfComposition;
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
    public boolean isLaunchCf() {
        return launchCf;
    }

    public boolean isBuild() {
        return buildCf;
    }

    public String getCfService() {
        return cfService;
    }

    public boolean isSelectService() {
        return selectService;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {

        CFProfile profile = null;
        try{
            profile= new CFProfile(getDescriptor().getCfUser(), getDescriptor().getCfToken());
        }
        catch (NullPointerException ne)
        {
            listener.getLogger().println("Couldn't get Codefresh profile details. Please check your system configuration.");
            return false;
        }

        String serviceId = "";
        String gitPath = "";
        String branch = "";

        CFApi api = new CFApi(getDescriptor().getCfToken());
        if (!buildCf && !launchCf){
            listener.getLogger().println("Codefresh - neither build nor composition launch was selected.\n Are you sure that's what you meant?" );
            return true;
        }

        if (buildCf) {
                String serviceName = this.getCfService();

                if (serviceName == null) {
                    SCM scm = build.getProject().getScm();
                    if (!(scm instanceof GitSCM)) {
                        listener.getLogger().println("Codefresh: you've specified you want to run a Codefresh build,\n but we didn't find"
                                + " any git repository defined or service name specified for the build.\n"
                                + "Are you sure that's what you meant?" );
                        return false;
                    }

                    final GitSCM gitSCM = (GitSCM) scm;
                    branch = gitSCM.getBranches().get(0).getName().replaceFirst("\\*\\/", "");
                    RemoteConfig remote = gitSCM.getRepositories().get(0);
                    URIish uri = remote.getURIs().get(0);
                    gitPath = uri.getPath();
                    serviceName = gitPath.split("/")[2].split("\\.")[0];
                    serviceId = profile.getServiceIdByPath(gitPath);
                    if (serviceId == null) {
                        listener.getLogger().println("\nUser " + getDescriptor().getCfUser() + " has no Codefresh service defined for url " + gitPath + ".\n Exiting.");
                        return false;
                    }
                } else {

                    serviceId = profile.getServiceIdByName(cfService);
                    branch = cfBranch;
                    if (serviceId == null) {
                        listener.getLogger().println("\nService Id not found for " + cfService + ".\n Exiting.");
                        return false;
                    }

                }

            listener.getLogger().println("\nTriggering Codefresh build. Service: " + serviceName + ".\n");

            String buildId = api.startBuild(serviceId, branch);
            String progressId = api.getBuildProgress(buildId);
            String status = api.getProgressStatus(progressId);
            String progressUrl = api.getBuildUrl(progressId);
            while (status.equals("running") || status.equals("pending")) {
                listener.getLogger().println("Codefresh build " + status + " - " + progressUrl + "\n Waiting 5 seconds...");
                Thread.sleep(5 * 1000);
                status = api.getProgressStatus(progressId);
            }

            switch (status) {
                case "success":
                    if (!launchCf) {
                        build.addAction(new CodefreshBuildBadgeAction(progressUrl, status));
                        build.addAction(new CodefreshEnvVarAction("CODEFRESH_BUILD_URL", progressUrl));
                    }
                    listener.getLogger().println("Codefresh build successfull!");
                    break;
                case "error":
                    build.addAction(new CodefreshBuildBadgeAction(progressUrl, status));
                    build.addAction(new CodefreshEnvVarAction("CODEFRESH_BUILD_URL", progressUrl));
                    listener.getLogger().println("Codefresh build failed!");
                    return false;
                default:
                    build.addAction(new CodefreshBuildBadgeAction(progressUrl, status));
                    build.addAction(new CodefreshEnvVarAction("CODEFRESH_BUILD_URL", progressUrl));
                    listener.getLogger().println("Codefresh build exited with status " + status + ".");
                    return false;
            }
        }

        if (launchCf) {
            try {
                listener.getLogger().println("*******\n");
                String compositionId = profile.getCompositionIdByName(cfComposition);
                String launchId = api.launchComposition(compositionId);
                String status = api.getProgressStatus(launchId);
                String processUrl = api.getBuildUrl(launchId);
                while (status.equals("running") || status.equals("pending")) {
                    listener.getLogger().println("Launching Codefresh composition environment: "+cfComposition+".\n Waiting 5 seconds...");
                    Thread.sleep(5 * 1000);
                    status = api.getProgressStatus(launchId);
                }

                switch (status) {
                    case "success":
                        String envUrl = api.getEnvUrl(launchId);
                        build.addAction(new CodefreshBuildBadgeAction(envUrl, status));
                        build.addAction(new CodefreshEnvVarAction("CODEFRESH_ENV_URL", envUrl));
                        listener.getLogger().println("Codefresh environment launched successfully - " + envUrl);
                        return true;
                    case "error":
                        build.addAction(new CodefreshBuildBadgeAction(processUrl, status));
                        listener.getLogger().println("Codefresh enironment launch failed!");
                        return false;
                    default:
                        build.addAction(new CodefreshBuildBadgeAction(processUrl, status));
                        listener.getLogger().println("Codefresh environment launch exited with status " + status + ".");
                        return false;
                }
            } catch (Exception ex) {

                Logger.getLogger(CodefreshBuilder.class.getName()).log(Level.SEVERE, null, ex);
                listener.getLogger().println("Codefresh environment launch failed with exception: " + ex.getMessage() + ".");
                build.addAction(new CodefreshBuildBadgeAction("", "error"));
                return false;
            }

        }
        return true;

    }


    public boolean performStep(Run run, TaskListener listener) throws IOException, InterruptedException {

        CFProfile profile = new CFProfile(getDescriptor().getCfUser(), getDescriptor().getCfToken());
        String serviceId = "";
        String gitPath = "";
        String branch = "";

         if (!buildCf && !launchCf){
            listener.getLogger().println("Codefresh - neither build nor composition launch was selected.\n Are you sure that's what you meant?" );
            return true;
        }
        CFApi api = new CFApi(getDescriptor().getCfToken());
        if (buildCf) {
                String serviceName = this.getCfService();

                if (serviceName == null) {
                    listener.getLogger().println("\nUser " + getDescriptor().getCfUser() + "has no Codefresh service defined for url " + gitPath + ".\n Exiting.");
                    return false;
                } else {

                    serviceId = profile.getServiceIdByName(cfService);
                    branch = cfBranch;
                    if (serviceId == null) {
                        listener.getLogger().println("\nService Id not found for " + cfService + ".\n Exiting.");
                        return false;
                    }

               }


            listener.getLogger().println("\nTriggering Codefresh build. Service: " + serviceName + ".\n");

            String buildId = api.startBuild(serviceId, branch);
            String progressId = api.getBuildProgress(buildId);
            String status = api.getProgressStatus(progressId);
            String progressUrl = api.getBuildUrl(progressId);
            while (status.equals("running") || status.equals("pending")) {
                listener.getLogger().println("Codefresh build " + status + " - " + progressUrl + "\n Waiting 5 seconds...");
                Thread.sleep(5 * 1000);
                status = api.getProgressStatus(progressId);
            }

            switch (status) {
                case "success":
                    if (!launchCf) {
                        run.addAction(new CodefreshBuildBadgeAction(progressUrl, status));
                        run.addAction(new CodefreshEnvVarAction("CODEFRESH_BUILD_URL", progressUrl));
                    }
                    listener.getLogger().println("Codefresh build successfull!");
                    break;
                case "error":
                    run.addAction(new CodefreshBuildBadgeAction(progressUrl, status));
                    run.addAction(new CodefreshEnvVarAction("CODEFRESH_BUILD_URL", progressUrl));
                    listener.getLogger().println("Codefresh build failed!");
                    return false;
                default:
                    run.addAction(new CodefreshBuildBadgeAction(progressUrl, status));
                    run.addAction(new CodefreshEnvVarAction("CODEFRESH_BUILD_URL", progressUrl));
                    listener.getLogger().println("Codefresh build exited with status " + status + ".");
                    return false;
            }
        }

        if (launchCf) {
            try {
                listener.getLogger().println("*******\n");
                String compositionId = profile.getCompositionIdByName(cfComposition);
                String launchId = api.launchComposition(compositionId);
                String status = api.getProgressStatus(launchId);
                String processUrl = api.getBuildUrl(launchId);
                while (status.equals("running") || status.equals("pending")) {
                    listener.getLogger().println("Launching Codefresh composition environment: "+cfComposition+".\n Waiting 5 seconds...");
                    Thread.sleep(5 * 1000);
                    status = api.getProgressStatus(launchId);
                }

                switch (status) {
                    case "success":
                        String envUrl = api.getEnvUrl(launchId);
                        run.addAction(new CodefreshBuildBadgeAction(envUrl, status));
                        run.addAction(new CodefreshEnvVarAction("CODEFRESH_ENV_URL", envUrl));
                        listener.getLogger().println("Codefresh environment launched successfully - " + envUrl);
                        return true;
                    case "error":
                        run.addAction(new CodefreshBuildBadgeAction(processUrl, status));
                        listener.getLogger().println("Codefresh enironment launch failed!");
                        return false;
                    default:
                        run.addAction(new CodefreshBuildBadgeAction(processUrl, status));
                        listener.getLogger().println("Codefresh environment launch exited with status " + status + ".");
                        return false;
                }
            } catch (Exception ex) {

                Logger.getLogger(CodefreshBuilder.class.getName()).log(Level.SEVERE, null, ex);
                listener.getLogger().println("Codefresh environment launch failed with exception: " + ex.getMessage() + ".");
                run.addAction(new CodefreshBuildBadgeAction("", "error"));
                return false;
            }

        }
        return true;
    }


    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        private String cfUser;
        private Secret cfToken;
        private CFApi api;

        public DescriptorImpl()
        {
            load();
        }

        public FormValidation doTestConnection(@QueryParameter("cfUser") final String cfUser, @QueryParameter("cfToken") final String cfToken) throws IOException {
            String userName = null;
            try {
                api = new CFApi(Secret.fromString(cfToken));
                userName = api.getUser();
            } catch (IOException e) {
                return FormValidation.error("Couldn't connect. Please check your token and internet connection.\n" + e.getMessage());
            }

            if (userName != null) {
                if (userName.equals(cfUser)) {
                    return FormValidation.ok("Success");
                } else {
                    return FormValidation.error("Username and token don't match");
                }
            }
            return FormValidation.error("Couldn't connect. Please check your token and internet connection.");
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
            return "Define Codefresh Integration";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            cfUser = formData.getString("cfUser");
            cfToken = Secret.fromString(formData.getString("cfToken"));
            save();
            return super.configure(req, formData);
        }

        public String getCfUser() {
            return cfUser;
        }

        public Secret getCfToken() {
            return cfToken;
        }

        public ListBoxModel doFillCfServiceItems(@QueryParameter("cfService") String cfService) throws IOException, MalformedURLException {
            ListBoxModel items = new ListBoxModel();
            if (cfToken == null) {
                throw new IOException("No Codefresh Integration Defined!!! Please configure in System Settings.");
            }
            try {
                api = new CFApi(cfToken);
                for (CFService srv : api.getServices()) {
                    String name = srv.getName();
                    items.add(new Option(name, name, cfService.equals(name)));

                }
            } catch (IOException e) {
                throw e;
            }
            return items;
        }

        public ListBoxModel doFillCfCompositionItems(@QueryParameter("cfComposition") String cfComposition) throws IOException, MalformedURLException {
            ListBoxModel items = new ListBoxModel();
            if (cfToken == null) {
                throw new IOException("No Codefresh Integration Defined!!! Please configure in System Settings.");
            }
            try {
                api = new CFApi(cfToken);
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

    public static class CodefreshBuildBadgeAction implements BuildBadgeAction {

        private final String buildUrl;
        private final String buildStatus;
        private final String iconFile;

        public CodefreshBuildBadgeAction(String buildUrl, String buildStatus) {
            super();
            this.buildUrl = buildUrl;
            this.buildStatus = buildStatus;
            switch (buildStatus) {
                case "success":
                    this.iconFile = "/plugin/codefresh/images/16x16/leaves_green.png";
                    break;
                case "unstable":
                    this.iconFile = "/plugin/codefresh/images/16x16/leaves_yellow.png";
                    break;
                case "error":
                    this.iconFile = "/plugin/codefresh/images/16x16/leaves_red.png";
                    break;
                default:
                    this.iconFile = "/plugin/codefresh/images/16x16/leaves_red.png";
            }
        }

        @Override
        public String getDisplayName() {
            return "Codefresh Build Page";
        }

        @Override
        public String getIconFileName() {
            return iconFile;
        }

        @Override
        public String getUrlName() {
            return buildUrl;
        }

    }

}
