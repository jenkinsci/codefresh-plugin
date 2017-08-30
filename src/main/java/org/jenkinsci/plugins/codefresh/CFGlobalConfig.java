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

import hudson.Extension;
import hudson.util.FormValidation;
import hudson.util.Secret;
import java.io.IOException;
import java.net.URI;
import static jenkins.YesNoMaybe.YES;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 *
 * @author antweiss
 */

@Extension(dynamicLoadable=YES)
public class CFGlobalConfig extends GlobalConfiguration {
    
    private String cfUser;
    private Secret cfToken;
    private CFApi api;
    private String cfUrl;
    private boolean selfSignedCert;
    
    public static CFGlobalConfig get() {
        Jenkins jenkins = Jenkins.getInstance();
        if (jenkins != null) {
          CFGlobalConfig config = jenkins.getDescriptorByType(CFGlobalConfig.class);
          if (config != null) {
            return config;
          }
        }
        return null;
    }   
    
    public CFGlobalConfig() {
        load();
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
        // To persist global configuration information,
        // set that to properties and call save().
        cfUser = formData.getString("cfUser");
        cfToken = Secret.fromString(formData.getString("cfToken"));
        cfUrl = formData.getString("cfUrl");
        selfSignedCert = formData.getBoolean("selfSignedCert");
        
        save();
        return super.configure(req, formData);
    }
    
    public String getCfUser() {
          return cfUser;
      }

    public String getCfUrl() {
          return cfUrl;
      }
    
    public Secret getCfToken() {
          return cfToken;
    }
    
    public boolean isSelfSignedCert() {
        return selfSignedCert;
    }
    
    public FormValidation doTestConnection(@QueryParameter("cfUser") final String cfUser, @QueryParameter("cfToken") final String cfToken, 
                                                @QueryParameter("cfUrl") final String cfUrl, 
                                                @QueryParameter("selfSignedCert") final boolean selfSignedCert) throws IOException {
            String userName = null;
            try {
                api = new CFApi(Secret.fromString(cfToken), cfUrl, selfSignedCert);
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

}

