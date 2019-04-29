package eu.stamp_project.botsing.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Properties;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.gitlab4j.api.models.Issue;

import eu.stamp_project.cicd.utils.botsing.ExceptionExtractor;
import eu.stamp_project.cicd.utils.git.GitlabIssueManager;

/**
 * Gitlab issue servlet: browse gitlab issues, preprocess for Botsing invocation.
 * @author Pierre-Yves Gibello - OW2
 *
 */
public class IssueServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public IssueServlet() {
    	super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		// Check session configuration is complete for processing
		String error = ServletUtils.configCompletion(request);
		if(error != null) {
			ServletUtils.forwardError(request, response, error);
			return;
		}

		// Retrieve Gitlab configuration (eg. gitlab.url ...)
		// Required to invoke Gitlab APIs.
		Properties gitlabConfig = ServletUtils.getGitlabConfig(getServletContext());
		if(gitlabConfig == null) {
			ServletUtils.forwardError(request, response, "gitlab.properties not found");
			return;
		}

		response.getWriter().append("<html><head><title>Botsing web application</title></head>");
		response.getWriter().append("<body>");

		// Retrieve issue ID: if absent, list issues
		String issue = request.getParameter("issue");
		int iid = -1;
		if(issue == null || issue.length() < 1) { // list issues assumed
			String closed = request.getParameter("withclosed");

			List<Issue> issues = GitlabIssueManager.listIssues(gitlabConfig, GitlabIssueManager.ISSUE_OPENED);
			response.getWriter().append("<h2>Open issues</h2>");
			response.getWriter().append("<table border>");
			for (Issue iss : issues) {
				boolean exceptionLikely = GitlabIssueManager.isExceptionLikely(iss);
				response.getWriter().append("<tr>" + (exceptionLikely ? "<td bgcolor=\"#00ff00\">" : "<td>") + iss.getIid() + "</td>"
						+ "<td><a href=\"IssueServlet?issue=" + iss.getIid() + "\">"
						+ iss.getTitle() + "</a></td></tr>\n");
			}
			response.getWriter().append("</table>\n");
			
			if(closed != null) {
				issues = GitlabIssueManager.listIssues(gitlabConfig, GitlabIssueManager.ISSUE_CLOSED);
				response.getWriter().append("<h2>Closed issues</h2>");
				response.getWriter().append("<table border>");
				for (Issue iss : issues) {
					boolean exceptionLikely = GitlabIssueManager.isExceptionLikely(iss);
					response.getWriter().append("<tr>" + (exceptionLikely ? "<td bgcolor=\"#00ff00\">" : "<td>") + iss.getIid() + "</td>"
							+ "<td><a href=\"IssueServlet?issue=" + iss.getIid() + "\">"
							+ iss.getTitle() + "</a></td></tr>\n");
				}
				response.getWriter().append("</table>\n");
			}
		} else { // Issue ID provided: parse issue to retrieve exceptions stacks for Botsing.

			try {
				iid = Integer.parseInt(issue);
			} catch(Exception e) {
				ServletUtils.forwardError(request, response, "issue ID should be an integer");
				return;
			}

			try {
				
				// Retrieve issue from Gitlab
				Issue gitlabIssue = GitlabIssueManager.getIssue(gitlabConfig, iid);

				// Preprocess issue contents, looking for exception stacks
				List<String> exceptions = ExceptionExtractor.explodeExceptions(new BufferedReader(new StringReader(gitlabIssue.getDescription())));
				response.getWriter().append("<h1>Exception stacks in issue #" + iid + "</h1>");
				if(exceptions == null) {
					response.getWriter().append("<p>No exception stack found</p>");
				} else { // Display each exception stack in a form, so the user can trigger Botsing
					for(String exception : exceptions) {
						response.getWriter().append("<p><form method=\"post\" action=\"BotsingServlet\">");
						response.getWriter().append("<textarea name=\"exception\" cols=\"80\" rows=\"16\">" + exception + "</textarea>");
						response.getWriter().append("<input type=\"submit\" value=\"Run Botsing\">");
						response.getWriter().append("</form></p>");
					}
				}

			} catch(Exception e) {
				e.printStackTrace(System.err);
				ServletUtils.forwardError(request, response, e);
				return;
			}

		}

		response.getWriter().append("<p><a href=\"index.jsp\">Home</a></p>");
		response.getWriter().append("</body></html>");
	}

}
