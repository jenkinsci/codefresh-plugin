/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkinsci.plugins.codefresh;

import hudson.util.Secret;

/**
 *
 * @author antweiss
 */
public class CFPipeline {
    private final String name;
    private final String id;
    private final Secret cfToken;
    private final String repoOwner;
    private final String repoName;

    public CFPipeline(Secret cfToken, String gitRepo, String id, String repoOwner, String repoName ) {
        this.name = gitRepo;
        this.cfToken = cfToken;
        this.id = id;
        this.repoOwner = repoOwner;
        this.repoName = repoName;
    }

    public CFPipeline(Secret cfToken, String gitRepo, String id) {
        this.name = gitRepo;
        this.cfToken = cfToken;
        this.id = id;
        this.repoOwner = null;
        this.repoName = null;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }
    
    public String getRepoOwner() {
        return repoOwner;
    }
    
    public String getRepoName() {
        return repoName;
    }

}
