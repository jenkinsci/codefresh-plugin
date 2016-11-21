package org.jenkinsci.plugins.codefresh;


import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.EnvironmentContributingAction;
import hudson.model.InvisibleAction;


public class CodefreshEnvVarAction extends InvisibleAction implements EnvironmentContributingAction {
    private String key;
    private String value;

    public CodefreshEnvVarAction(String key, String value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public void buildEnvVars(AbstractBuild<?, ?> build, EnvVars env) {
        env.put(key, value);
    }
}
