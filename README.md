# botsing-git-webapp
Gitlab Webhook for STAMP Botsing automation + test/demo webapp.
Relies on [STAMP Botsing](https://github.com/STAMP-project/botsing) crash reproduction java framework.

## What is provided ?

- A test/demo webapp: Run Botsing manually on your project from your web browser, by just dropping an exception stack in a text area, or uploading it from a log file,
or extracting it by browsing Gitlab issues (for Gitlab-hosted projects).

- A Gitlab WebHook: Fot gitlab-hosted projects, automate Botsing by integrating it in the Gitlab issue workflow
(auto-detect exception stacks in new or updated issues, run Botsing, and publish Botsing results as an issue comment if relevant).

The test/demo webapp can be used to set up your environment: configuration, link with your gitlab project...
When it works (you can run Botsing manually through the webapp), then you can easily set up the Gitlab Web hook, as it is just one step further.

## Quick start

Build: mvn clean install  
Warning: build temporarily requires another (not yet released) project: [stamp-cicd-utils](https://github.com/STAMP-project/stamp-cicd-utils). Clone and build it prior to building botsing-git-webapp.

Deploy: deploy botsing-git-webapp.war file (from target/ directory) in your favorite servlet container.
For example, copy it in [Apache Tomcat](http://tomcat.apache.org)'s webapps/ directory.

Test: Reach the botsing-git-webapp web application from your web browser.
Generally deployed at [http://localhost:8080/botsing-git-webapp](http://localhost:8080/botsing-git-webapp).

## Prerequisites for Botsing to run

Edit botsing-gitlab.properties, located in the webapp's WEB-INF/classes directory.

There, you can specify:

- botsing.version (MANDATORY): The version of Botsing to be used. If using a SNAPSHOT, you have to clone Botsing then build/install it (not necessary if using a release).
- local.defaultpom (OPTIONAL): The path to the pom.xml in a local clone of the project you will run Botsing against.
Botsing requires a fully built project (so CLASSPATH is fully resolved), and is run from the project's pom.xml (See Botsing documentation if required).

Note: if local.defaultpom is not specified, the test/demo webapp will work, as the user will be prompted for the POM path.
This is not applicable if using the gitlab WebHook (local.defaultpom is mandatory for the WebHook servlet).

Restart the webapp or the application server if required to reload the configuration.

Note: at this step, exception stacks can be provided to Botsing as full text (paste in a text area or file upload). To retrieve exception stacks from Gitlab issues, configure gitlab link (see next section: it is just 3 more fields in the properties file).

## Setup Gitlab Webhook to integrate Botsing in issues workflow

### Webapp setup

Edit botsing-gitlab.properties, located in the webapp's WEB-INF/classes directory.

There, in addition to the Botsing-related setup, you should specify (all fields mandatory):

- gitlab.url: the URL of the Gitlab server where your project is hosted
- gitlab.token: the access token for the Gitlab API on your server (to generate one, go in your Gitlab user settings, choose "Access tokens", and create a personal access token with scope "api" - the token is a string that looks like "i_rjH-MM3YuFsvAxCTsH").
- gitlab.project: your Gitlab project Id (appears under the project title in the project page on Gitlab).
- Make sure the path to the local POM is specified (see above - the field is now mandatory).

Restart the webapp or the application server if required to reload the configuration.

### Webhook setup

In your Gitlab project page: go to Settings / Integrations.

In the URL field, insert the URL to the WebHook servlet:  
`http://<your-server-name-or-IP>/botsing-git-webapp/IssueWebHookServlet`

Check the "Issues events" box (and uncheck all other ones), then click "Add webhook".

To test the webhook: create a new issue, with an exception stack inside (hint: the exception stack should be inserted as raw text in the markdown, no formatting: paste it between 2 lines, each of them containing only 3 backquotes).

The webhook should be triggered (scan your app server logs), and if Botsing is successful, a comment will be added to the issue with corresponding Botsing output (generally, the source code of a test that reproduces the stack trace in the issue). The botsing process itself can take a few minutes, so the comment may not appear immediately.
To perform multiple tests, simply update the issue content (or create another one).

### Note concerning localhost testing

If testing with a local installation of Gitlab (on localhost), you have to allow localhost Webhooks in Gitlab: log in as gitlab administrator, go to the admin area (wrench logo in the top menu), Settings/Network section, and check the "Allow requests to the local network from hooks and services" in the "Outboud requests" panel.
