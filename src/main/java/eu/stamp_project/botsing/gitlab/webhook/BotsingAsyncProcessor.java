package eu.stamp_project.botsing.gitlab.webhook;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;

import org.gitlab4j.api.GitLabApiException;

/**
 * Asynchronous processing of issue webhook requests to trigger Botsing.
 * Intended to run in a specific thread.
 * @author Pierre-Yves Gibello - OW2
 *
 */
public class BotsingAsyncProcessor implements Runnable {

	HttpServletRequest request;
	AsyncContext asyncCtx;
	
	/**
	 * Prepare request processor.
	 * @param request The HTTP request that triggers the event.
	 */
	public BotsingAsyncProcessor(HttpServletRequest request) {
		this.request = request;
		this.asyncCtx = request.getAsyncContext();
		this.asyncCtx.setTimeout(0);
	}

	/**
	 * Process request in specific thread.
	 */
	public void run() {
		try {
			IssueWebHookListener.getInstance().triggerEvent(this.request);
		} catch (GitLabApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.asyncCtx.complete();
	}

}
