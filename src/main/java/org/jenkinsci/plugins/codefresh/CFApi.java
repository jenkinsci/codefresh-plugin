/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jenkinsci.plugins.codefresh;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import hudson.util.Secret;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.commons.io.IOUtils;

import org.jsoup.*;

/**
 *
 * @author antweiss
 */
public class CFApi {

    private SSLSocketFactory sf = null;
    private String cfUrl = "https://g.codefresh.io/api";
    private Secret cfToken;
    private boolean https = false;
    private TrustManager[] trustAllCerts;
    private static final Logger LOGGER = Logger.getLogger(CFApi.class.getName());

    private static final Integer PIPELINE_BATCH_SIZE = 50;


    public CFApi(Secret cfToken, String cfUrl, boolean selfSignedCert) throws MalformedURLException, IOException {

        this.cfToken = cfToken;
        this.cfUrl = cfUrl + "/api";
        if (cfUrl.contains("https")){
             secureContext(selfSignedCert);
        }

    }
    
    public CFApi() throws MalformedURLException, IOException {
        try {
            CFGlobalConfig config = CFGlobalConfig.get();
            if ( config == null  )
            {
                LOGGER.log(Level.SEVERE,"Couldn't get Codefresh configuration. Did you define one?");
                throw new IOException();
            }
            this.cfToken = config.getCfToken();
            this.cfUrl = config.getCfUrl() + "/api";
            if (cfUrl.contains("https")){
                secureContext(config.isSelfSignedCert());
            }
        }
        catch (Exception e)
        {
            throw e;
        } 
            

    }

    public List<CFPipeline> getPipelines() throws IOException {
        List<CFPipeline> pipelines = new ArrayList<>();
        List<CFPipeline> batch;
        int offset = 0;
        int limit = PIPELINE_BATCH_SIZE;
        while (!(batch = getPipelines(offset, limit)).isEmpty()) {
            pipelines.addAll(batch);
            offset += PIPELINE_BATCH_SIZE;
            limit += PIPELINE_BATCH_SIZE;
            LOGGER.info("Load pipelines batch from " + offset + " to " + limit);
        }
        return pipelines;
    }

    public List<CFPipeline> getPipelines(int offset, int limit) throws IOException {
        HttpURLConnection conn = getConnection(String.format(cfUrl + "/pipelines?offset=%d&limit=%d", offset, limit));
        List<CFPipeline> services = new ArrayList<CFPipeline>();
        InputStream is = conn.getInputStream();
        String jsonString = IOUtils.toString(is);
        JsonParser parser = new JsonParser();
        JsonArray serviceList;
        if (parser.parse(jsonString).getAsJsonObject().get("docs") != null) {
            serviceList = new JsonParser().parse(jsonString).getAsJsonObject().get("docs").getAsJsonArray();
            for (JsonElement service : serviceList) {
                String name = service.getAsJsonObject().getAsJsonObject("metadata").get("name").getAsString();
                String id = URLEncoder.encode(name, "UTF-8");
                services.add(new CFPipeline(cfToken, name, id));
            }
        }
        return services;
    }

    public String getUser() throws MalformedURLException, IOException
    {
        String userUrl = cfUrl + "/user";
        HttpURLConnection conn = getConnection(userUrl);
        conn.setRequestMethod("GET");
        InputStream is = conn.getInputStream();
        String jsonString = IOUtils.toString(is);
        JsonObject user = new JsonParser().parse(jsonString).getAsJsonObject();
        String userName = user.get("userName").getAsString();
        return userName;
    }

    public String startBuild(String serviceId, String branch, List<CFVariable> vars) throws MalformedURLException, IOException
    {
        String buildUrl = cfUrl + "/builds/" + serviceId ;
        String buildOptions = "";
        HttpURLConnection conn = getConnection(buildUrl);
        conn.setRequestMethod("POST");
        //branch can not be empty - use master if no value provided
        if (branch.isEmpty())
        { 
            branch = "master";
        }
        
        conn.setRequestProperty("Content-Type","application/json");
        conn.setRequestProperty("User-Agent","jenkins-plugin1.7");
        conn.setRequestProperty("Codefresh-Agent","jenkins-plugin");
        JsonObject options = new JsonObject();
        options.addProperty("branch", branch);
        if (vars != null){
            JsonObject var2json = new JsonObject();
            for (CFVariable var: vars){
                var2json.addProperty(var.variable, var.value);
            }
            options.add("variables", var2json);
        }
        buildOptions = options.toString();

        try (OutputStreamWriter outs = new OutputStreamWriter(conn.getOutputStream(),"UTF-8")) {
            outs.write(buildOptions);
            outs.flush();
        }
        catch (Exception e)
        {
            throw e;
        }


        InputStream is = conn.getInputStream();
        return IOUtils.toString(is).replace("\"", "");
    }

    public HttpURLConnection getConnection(String urlString) throws MalformedURLException, IOException {
        if ( urlString.isEmpty())
        {
            urlString = cfUrl;
        }
        URL connUrl = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) connUrl.openConnection();
        conn.setRequestProperty("x-access-token", cfToken.getPlainText());
        conn.setUseCaches(false);
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setInstanceFollowRedirects(true);
        if (https){
            HttpsURLConnection.setFollowRedirects(true);
            return (HttpsURLConnection) conn;
        }  
        return conn;
    }

    String getBuildProgress(String buildId) throws IOException {
        String buildUrl = cfUrl + "/builds/" + buildId;
        HttpURLConnection conn = getConnection(buildUrl);
        conn.setRequestMethod("GET");
        InputStream is = conn.getInputStream();
        String jsonString = IOUtils.toString(is);
        JsonObject build = new JsonParser().parse(jsonString).getAsJsonObject();
        String progress = build.get("progress_id").getAsString();
        return progress;
    }

    JsonObject getProcess(String processId) throws IOException {
        String progressUrl = cfUrl + "/builds/" + processId;
        HttpURLConnection conn = getConnection(progressUrl);
        conn.setRequestMethod("GET");
        InputStream is = conn.getInputStream();
        String jsonString = IOUtils.toString(is);
        JsonObject progress = new JsonParser().parse(jsonString).getAsJsonObject();
        //String status = progress.get("status").getAsString();
        return progress;
    }

    String getBuildUrl(String progressId) throws IOException {
        String buildUrl = cfUrl.substring(0, cfUrl.lastIndexOf('/')) + "/process/" + progressId;
        return buildUrl;
    }

    String launchService(String serviceId, String repoOwner, String repoName, String branch) throws Exception {
        String launchUrl = cfUrl + "/runtime/testit";
        String launchOptions = "";
       // URL launchEP = new URL(launchUrl);
        HttpURLConnection conn = getConnection(launchUrl);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type","application/json");
        conn.setRequestProperty("User-Agent","jenkins-plugin1.7");
        conn.setRequestProperty("Codefresh-Agent","jenkins-plugin");
        JsonObject options = new JsonObject();

        options.addProperty("repoOwner", repoOwner);
        options.addProperty("repoName", repoName);
        options.addProperty("branch", branch);

        launchOptions = options.toString();

        try (OutputStreamWriter outs = new OutputStreamWriter(conn.getOutputStream(),"UTF-8")) {
            outs.write(launchOptions);
            outs.flush();
        }
        catch (Exception e)
        {
            throw e;
        }


        InputStream is = conn.getInputStream();
        String jsonString = IOUtils.toString(is);
        JsonObject process = new JsonParser().parse(jsonString).getAsJsonObject();
        String processId = process.get("id").getAsString();
        return processId;
    }

    String launchComposition(String compositionId, List<CFVariable> vars) throws Exception {
        String launchUrl = cfUrl + "/compositions/"+compositionId+"/run";
        String launchOptions = "";
        HttpURLConnection conn = getConnection(launchUrl);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type","application/json");
        conn.setRequestProperty("User-Agent","jenkins-plugin1.7");
        conn.setRequestProperty("Codefresh-Agent","jenkins-plugin");

        JsonObject options = new JsonObject();
        if (vars != null){
            JsonObject var2json = new JsonObject();
            for (CFVariable var: vars){
                var2json.addProperty(var.variable, var.value);
            }
            options.add("compositionVariables", var2json);
        }
        launchOptions = options.toString();

        try (OutputStreamWriter outs = new OutputStreamWriter(conn.getOutputStream(),"UTF-8")) {
            outs.write(launchOptions);
            outs.flush();
        }
        catch (Exception e)
        {
            throw e;
        }

        String processId = null;
                
        try (InputStream is = conn.getInputStream()){
            String jsonString = IOUtils.toString(is);
            JsonObject process = new JsonParser().parse(jsonString).getAsJsonObject();
            processId = process.get("id").getAsString();
        }
        catch (IOException e)
        {
            try (InputStream es = conn.getErrorStream()){
                String jsonString = IOUtils.toString(es);
                JsonObject out = new JsonParser().parse(jsonString).getAsJsonObject();
                String message = out.get("message").getAsString();
                throw new java.io.IOException(message);
            }
            catch (Exception i)
            {
                throw i;
            }
        }
        return processId;
    }

    String getEnvUrl(JsonObject process) throws IOException {
        
        
        String progressId = process.get("progress_id").getAsString();
        
        JsonArray environment = getEnvByProgressID(progressId);
        JsonArray instances = environment.get(0).getAsJsonObject().get("instances").getAsJsonArray();
        JsonObject Urls = instances.get(0).getAsJsonObject().get("urls").getAsJsonObject();
        JsonObject UrlObj = Urls.get("run").getAsJsonArray().get(0).getAsJsonObject();
        JsonObject http = UrlObj.getAsJsonObject("http");
        String envUrl = http.get("public").getAsString();
        
        return envUrl;
    }

    JsonArray getEnvByProgressID(String progressId) throws IOException {
        String progressUrl = cfUrl + "/environments?progress=" + progressId;
        HttpURLConnection conn = getConnection(progressUrl);
        conn.setRequestMethod("GET");
        InputStream is = conn.getInputStream();
        String jsonString = IOUtils.toString(is);
        JsonArray environment = new JsonParser().parse(jsonString).getAsJsonArray();
        
        return environment;
    }
    
    String getEnvIdByProgressID(String progressId) throws IOException {
       
        JsonArray environment = getEnvByProgressID(progressId);
        String envId = environment.get(0).getAsJsonObject().get("_id").getAsString();
        return envId;
    }
    
    String getEnvIdByURL(String envURL) throws IOException {
        String getEnvsUrl = cfUrl + "/environments";
        HttpURLConnection conn = getConnection(getEnvsUrl);
        conn.setRequestMethod("GET");
        InputStream is = conn.getInputStream();
        String jsonString = IOUtils.toString(is);
        String envId = "";
        JsonArray envList = new JsonParser().parse(jsonString).getAsJsonArray();
        for (int i = 0; i < envList.size(); i++) {
            JsonObject environment = (JsonObject)envList.get(i);
            envId = environment.get("_id").getAsString();
            JsonArray instances = environment.get("instances").getAsJsonArray();
            for (int k = 0; k < instances.size(); k++)
            {
                JsonArray urls = ((JsonObject)instances.get(k)).get("urls").getAsJsonObject().get("run").getAsJsonArray();
                String publicURL = urls.get(0).getAsJsonObject().get("http").getAsJsonObject().get("public").getAsString();
                if ( envURL.equals(publicURL))
                {
                    return envId;
                }

            }
        }
        return envId;
    }
    
    String getFinalLogs(String progressId) throws IOException {
        String getLogsUrl = cfUrl + "/progress/download/" + progressId ;
        HttpURLConnection conn = getConnection(getLogsUrl);
        conn.setRequestMethod("GET");
        InputStream is = conn.getInputStream();
        String logsHtml = IOUtils.toString(is);
        return Jsoup.parse(logsHtml).text();
    }

    public List<CFComposition> getCompositions() throws MalformedURLException, IOException
    {
        String compositionUrl = cfUrl + "/compositions";
        HttpURLConnection conn = getConnection(compositionUrl);
        List<CFComposition> compositions = new ArrayList<CFComposition>();
        conn.setRequestMethod("GET");
        InputStream is = conn.getInputStream();
        String jsonString = IOUtils.toString(is);
        JsonArray compositionList = new JsonParser().parse(jsonString).getAsJsonArray();
        for (int i = 0; i < compositionList.size(); i++) {
            JsonObject obj = (JsonObject)compositionList.get(i);
            compositions.add(new CFComposition(obj.get("name").getAsString(),
                                                obj.get("_id").getAsString()));
        }
        return compositions;
    }

    boolean terminateEnv(String envId) throws Exception {
        String terminateUrl = cfUrl + "/environments/"+envId+"/terminate";
        String launchOptions = "";
        HttpURLConnection conn = getConnection(terminateUrl);
        conn.setRequestMethod("GET");
        
        InputStream is = conn.getInputStream();
        if (IOUtils.toString(is).equals("terminated"))
        {
                    return true;
        }
        return false;
    }

    private void secureContext(boolean selfSignedCert) {
        this.https = true;
        trustAllCerts = new TrustManager[]{new X509TrustManager(){
            public X509Certificate[] getAcceptedIssuers(){return null;}
            public void checkClientTrusted(X509Certificate[] certs, String authType){}
            public void checkServerTrusted(X509Certificate[] certs, String authType){}
        }};

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            this.sf = sc.getSocketFactory();
            HttpsURLConnection.setDefaultSSLSocketFactory(this.sf);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        if( selfSignedCert ) {
            HttpsURLConnection.setDefaultHostnameVerifier(
                new HostnameVerifier(){
                    @Override
                    public boolean verify(String hostname,
                            SSLSession sslSession) {
                        return true;
                    }
            });
        }
    }

}
