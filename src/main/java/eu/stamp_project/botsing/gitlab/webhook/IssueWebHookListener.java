package eu.stamp_project.botsing.gitlab.webhook;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.webhook.IssueEvent;
import org.gitlab4j.api.webhook.WebHookListener;
import org.gitlab4j.api.webhook.WebHookManager;

import eu.stamp_project.botsing.servlet.ServletUtils;
import eu.stamp_project.cicd.utils.botsing.BotsingInvoker;
import eu.stamp_project.cicd.utils.botsing.ExceptionExtractor;
import eu.stamp_project.cicd.utils.git.GitlabIssueManager;
import eu.stamp_project.cicd.utils.misc.FileUtils;

/**
 * Gitlab4j web hook listener for issue events (singleton class).
 * @author Pierre-Yves Gibello - OW2
 *
 */
public class IssueWebHookListener implements WebHookListener {
	
	static IssueWebHookListener theListener = null;

	WebHookManager manager = new WebHookManager();
	ServletContext context = null;
	
	/**
	 * Retrieve IssueWebHookListener singleton instance.
	 * @return The IssueWebHookListener singleton instance
	 */
	public static synchronized IssueWebHookListener getInstance() {
		if(theListener == null) theListener = new IssueWebHookListener();
		return theListener;
	}

	/**
	 * Private constructor, to prevent instantiation (getInstance() should be called instead).
	 */
	private IssueWebHookListener() {
		// Register this as Gitlab4j issue listener (see onIssueEvent() method)
		manager.addListener(this);
	}

	/**
	 * Fire event if needed (delegate decision to Gitlab4j according to HTTP request)
	 * @param request
	 * @throws GitLabApiException
	 */
	public void triggerEvent(HttpServletRequest request) throws GitLabApiException {
		if(this.context == null) this.context = request.getServletContext();
		this.manager.handleEvent(request);
	}

	/**
	 * Handle Gitlab4j issue event (listener callback).
	 * @param event The Gitlab4j issue event that triggers this method
	 */
	public void onIssueEvent(IssueEvent event) {
		Properties config = ServletUtils.getGitlabConfig(this.context);
		int projectId = Integer.parseInt(config.getProperty("gitlab.project"));

		// Handle issue if relevant (in current project)
		System.out.println("Got issue event on Issue " + event.getObjectAttributes().getTitle() + " with action=" + event.getObjectAttributes().getAction());
		if(event.getObjectAttributes().getProjectId() == projectId && isActionSupported(event.getObjectAttributes().getAction())) {
	
			try {
				//TODO Botsing should do the filtering, to find the most relevant exception (scheduled in future releases...)
				//TODO The algorithm below, run botsing starting from the exception closest to the crash and stop at first test, is too simplistic...
				
				List<String> exceptions = ExceptionExtractor.explodeExceptions(new BufferedReader(new StringReader(event.getObjectAttributes().getDescription())));
				// Reverse exception list: the last one is the closest to the crash !
				Collections.reverse(exceptions);

				for(String exception : exceptions) {

					File tempDir = Files.createTempDirectory("botsing").toFile();
					
					int retcode = BotsingInvoker.runBotsing(config.getProperty("local.defaultpom"),
							config.getProperty("botsing.version"),
							FileUtils.tempFile(exception),
							1, tempDir.getAbsolutePath(),
							"-Dglobal_timeout=1800", //TODO temporary due to Botsing bug (NPE if no global timeout !) - 1800 is default
							null);
					File test = null;
					if(retcode == 0) {
						test = BotsingInvoker.findGeneratedTest(tempDir);
					}
					if(test != null) { // Successfully generated test: comment issue and break !
				        // Markdown syntax requires 2 or more spaces at EOL for line break...
						GitlabIssueManager.commentIssue(config, event.getObjectAttributes().getIid(),
								"** This is an auto-generated comment, from STAMP Botsing stack trace analysis tool **  \n"
								+ "The following code snippet should generate an exception trace reported in this issue:  \n"
								+ "```\n" + FileUtils.fileToString(test) + "\n```\n");
					}
					FileUtils.deleteIfExists(tempDir);
					
					if(test != null) break; // Successful generation of test: stop with Botsing !
				}
			} catch (IOException ignore) { System.out.println(ignore);}
		}
	}

	/**
	 * Tells whether an action on a specific issue should trigger Botsing, or not.
	 * For example, Botsing should run when an issue is created or updated,
	 * not when it is closed.
	 * @param action The action, as specified by the Gitlab API
	 * @return true if Botsing should be run, false otherwise
	 */
	private boolean isActionSupported(String action) {
		return ("open".equalsIgnoreCase(action)
				|| "update".equalsIgnoreCase(action)
				|| "reopen".equalsIgnoreCase(action));
	}
}
