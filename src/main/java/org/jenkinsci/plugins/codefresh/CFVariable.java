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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 *
 * @author antweiss
 */
public class CFVariable extends AbstractDescribableImpl<CFVariable> 
{ 
    
    public String variable; 
    public String value; 
    
    @DataBoundConstructor 
    public CFVariable(String Variable, String Value) { 
        this.variable = Variable;  
        this.value = Value;             
    }
    
    public CFVariable(JSONObject var) { 
        this.variable = var.getString("Variable");  
        this.value = var.getString("Value");               
    } 
    
    public CFVariable(String jsonString) { 
        JsonObject var  = new JsonParser().parse(jsonString).getAsJsonObject();
        this.variable = var.get("Variable").getAsString();  
        this.value = var.get("Value").getAsString();               
    } 
    public String getVariable() { 
        return variable; 
    } 
    
    public String getValue() { 
        return value; 
    } 
    

      @Extension 
      public static class DescriptorImpl extends Descriptor<CFVariable> { 
          public String getDisplayName() { return "Codefresh Variables"; } 
      } 
} 