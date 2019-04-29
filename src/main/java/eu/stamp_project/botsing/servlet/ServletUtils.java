package eu.stamp_project.botsing.servlet;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Utilitary methods for Botsing webapp
 * @author Pierre-Yves Gibello - OW2
 *
 */
public class ServletUtils {

	/**
	 * Prepare session for error handling, then forward request to error.jsp
	 * @param request The HTTP resquest
	 * @param response The HTTP response
	 * @param message Error message
	 * @throws ServletException
	 * @throws IOException
	 */
	public static void forwardError(HttpServletRequest request, HttpServletResponse response, String message) throws ServletException, IOException {
		request.getSession().setAttribute("errormessage", message);
		request.getRequestDispatcher("error.jsp").forward(request, response);
	}

	/**
	 * Prepare session for error handling, then forward request to error.jsp
	 * @param request The HTTP resquest
	 * @param response The HTTP response
	 * @param e Throwable that caused the error
	 * @throws ServletException
	 * @throws IOException
	 */
	public static void forwardError(HttpServletRequest request, HttpServletResponse response, Throwable e) throws ServletException, IOException {
		forwardError(request, response, e.getMessage());
	}

	/**
	 * Retrieve gitlab configuration (stored in gitlab.properties resource file)
	 * @param context The servlet context
	 * @return
	 */
	public static Properties getGitlabConfig(ServletContext context) {
		Properties gitlabConfig = new Properties();
		try {
			gitlabConfig.load(context.getResourceAsStream("/WEB-INF/classes/botsing-gitlab.properties"));
			return gitlabConfig;
		} catch(IOException ioe) {
			return null;
		}
	}

	/**
	 * Check that request-level config is complete for Botsing servlets
	 * @param request The HTTP servlet request
	 * @return An error message, null if OK
	 */
	public static String configCompletion(HttpServletRequest request) {
		String error = null;
		if(request.getSession().getAttribute("pompath") == null) {
			error = "Missing path to project pom file";
		}
		return error;
	}

}
