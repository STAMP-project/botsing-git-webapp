# botsing-git-webapp
Gitlab Webhook for STAMP Botsing automation + test/demo webapp.
Relies on [STAMP Botsing](https://github.com/STAMP-project/botsing) crash reproduction java framework.

## What is provided ?

- A test/demo webapp: Run Botsing manually on your project from your web browser, by just dropping an exception stack in a text area, or uploading it from a log file,
or extracting it by browsing Gitlab issues (for Gitlab-hosted projects).

- A Gitlab WebHook: Fot gitlab-hosted projects, automate Botsing by integrating it in the Gitlab issue workflow
(auto-detect exception stacks in new or updated issues, run Botsing, and publish Botsing results as an issue comment if relevant).

## Quick start

Build: mvn clean install

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
