/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
 package org.jenkinsci.plugins.codefresh;

import hudson.util.Secret;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author antweiss
 */
public class CFProfile {
    private final String cfUser;
    private final Secret cfToken;
    private final String cfUrl;
    private List<CFPipeline> services;
    private List<CFComposition> compositions;
    private CFApi api;
    private boolean selfSignedCert;
    //private List<CFService> services;

    public CFProfile(String cfUser, Secret cfToken, String cfUrl, boolean selfSignedCert) throws IOException {
        this.cfUser = cfUser;
        this.cfToken = cfToken;
        this.cfUrl = cfUrl;
        this.selfSignedCert = selfSignedCert;
        this.api = new CFApi();
        this.services = api.getPipelines();
        this.compositions = api.getCompositions();
    }

    public String getUser(){
        return this.cfUser;
    }

    public Secret getToken(){
        return this.cfToken;
    }

    public int testConnection() throws IOException{
       api = new CFApi(cfToken, cfUrl, selfSignedCert );
       
       try{
           api.getConnection("");
       }
       catch (Exception e)
       {
            throw e;
       }
       return 0;
    }

    String getServiceIdByName(String serviceName) {
        for (CFPipeline service: services){
            if (service.getName().equals(serviceName))
            {
                return service.getId();
            }
        }
        return null;
    }

    String getServiceIdByPath(String gitPath) {
        String serviceName = gitPath.split("/")[2].split("\\.")[0];      
        for (CFPipeline service: services){
            if (service.getName().equals(serviceName))
            {
                    return service.getId();
            }
        }
        return null;
    }
    
    String getServiceRepoOwner(String serviceId) {
        for (CFPipeline service: services){
            if (service.getId().equals(serviceId))
            {
                return service.getRepoOwner();
            }
        }
        return null;
    }

    String getServiceRepoName(String serviceId) {
        for (CFPipeline service: services){
            if (service.getId().equals(serviceId))
            {
                return service.getRepoName();
            }
        }
        return null;
    }

    String getCompositionIdByName(String compositionName) {
        for (CFComposition composition: compositions){
            if (composition.getName().equals(compositionName))
            {
                return composition.getId();
            }
        }
        return null;
    }
}
