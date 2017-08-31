![Codefresh Logo](/src/main/webapp/images/24x24/leaves_green.png?raw=true "Codefresh")

[![Codefresh build status]( https://g.codefresh.io/api/badges/build?repoOwner=jenkinsci&repoName=codefresh-plugin&branch=master&pipelineName=codefresh-plugin&accountName=antweiss_github&type=cf-1)]( https://g.codefresh.io/repositories/jenkinsci/codefresh-plugin/builds?filter=trigger:build;branch:master;service:5979c46d587b160001541892~codefresh-plugin)

[Codefresh](http://g.codefresh.io) Plugin for Jenkins

The purpose of this plugin is to allow integrating [Codefresh](http://g.codefresh.io)  docker flow pipelines into your existing Jenkins flows.

Getting Started:

1. Define the connection to Codefresh in Jenkins system config (Manage Jenkins->Confiure system-> scroll down to find '*Define Codefesh Integration*').
This requires filling out your user name and Codefresh auth token.
You can verify the authentication by using the '_Test Connection_' button.

   To find your auth token:
    - log in to Codefresh and then open https://g.codefresh.io/api/ in another tab of the same browser.
    - Copy your token from the right-hand text field on the Swagger header.

   Screenshot:

![System Configuration](http://otomato.link/wp-content/uploads/2016/07/Screen-Shot-2016-07-21-at-11.15.44-AM.png)


2. Trigger Codefresh pipeline execution from your freestyle Jenkins jobs:

    - 'Add Build Step' -> 'Run Codefresh Pipeline'
    - If you don't do anything else - this will trigger the pipeline that corresponds to the git repo defined in the job's SCM configuration.
    - If there's no SCM defined for current job or you would like to trigger a pipeline for a different repository - check the 'Choose service to build' option. This will present you with drop-down selectable list of all your defined Codefresh pipelines.
    - Optionally define the git branch you want the pipeline to run for. If branch field is left empty - default branch will be built (usually 'master')
    - Check the 'Set Codefresh Pipeline Variables' checkbox to define environment variables to pass to the build

    Screenshot:

![Run Codefresh Pipeline](http://otomato.link/wp-content/uploads/2017/08/Screen-Shot-2017-08-08-at-6.05.22-PM.png)


Once configured - the plugin will trigger Codefresh, run your tests and report the results.
On job completion a Codefresh badge will appear next to the build execution. Clicking on it will take you to the build results page on Codefresh:


![Codefresh Badge](http://otomato.link/wp-content/uploads/2016/07/Screen-Shot-2016-07-21-at-11.39.04-AM.png)

3. Launch Codefresh composition environment from your freestyle Jenkins jobs:

    - This requires you to define at least one docker-compose compositions on your Codefresh account.
    - 'Add Build Step' -> 'Launch Codefresh Composition'
    - Choose composition name from the drop-down selectable list of all your defined Codefresh compositions.
    - Check the 'Set Codefresh Composition Variables' checkbox to define environment variables to pass to the execution environment.

    Screenshot:

![Launch Codefresh Composition](http://otomato.link/wp-content/uploads/2017/08/Screen-Shot-2017-08-08-at-6.11.59-PM.png)

