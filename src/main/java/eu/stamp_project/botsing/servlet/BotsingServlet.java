package eu.stamp_project.botsing.servlet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import eu.stamp_project.cicd.utils.botsing.BotsingInvoker;
import eu.stamp_project.cicd.utils.botsing.ExceptionExtractor;
import eu.stamp_project.cicd.utils.misc.FileUtils;

/**
 * Servlet to invoke Botsing synchronously and display output in browser (may be slow).
 * Intended for demo and testing.
 * @author Pierre-Yves Gibello - OW2
 *
 */
public class BotsingServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public BotsingServlet() {
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

		// Retrieve exception stack log for Botsing (mandatory)
		// Can be a log with exceptions scattered inside, if "preprocess" parameter is present.
		String exception = request.getParameter("exception");
		if(exception == null) {
			ServletUtils.forwardError(request, response, "exception parameter not specified");
			return;
		}
		
		// Need to all response.getOutputStream later on...
		// so can't call getWriter(), to avoid IllegalState
		PrintWriter writer = new PrintWriter(response.getOutputStream());
		writer.append("<html><head><title>Botsing web application</title></head>");
		writer.append("<body>\n");
		writer.flush();

		// Preprocess mode: parse exception log (not necessarily a single exception stack).
		// Each single exception stack will be displayed and proposed for Botsing processing.
		if(request.getParameter("preprocess") != null) {
			List<String> exceptions = ExceptionExtractor.explodeExceptions(new BufferedReader(new StringReader(exception)));
			if(exceptions == null) {
				writer.append("<p>No exception stack found</p>");
			} else {
				for(String ex : exceptions) {
					writer.append("<p><form method=\"post\" action=\"BotsingServlet\">");
					writer.append("<textarea name=\"exception\" cols=\"80\" rows=\"16\">" + ex + "</textarea>");
					writer.append("<input type=\"submit\" value=\"Run Botsing\">");
					writer.append("</form></p>");
				}
			}
		} else { // Run Botsing ! Parameter "exception" supposed to be a single exception stack here.
			String pom = (String)request.getSession().getAttribute("pompath");
			writer.append("<h1>Run botsing using pom " + pom + "</h1>\n");
			writer.append("<pre>\n");
			writer.flush();
			
			File tempDir = Files.createTempDirectory("botsing-webapp").toFile();
			// Check for additional "-D" options
			String options = ServletUtils.getGitlabConfig(getServletContext()).getProperty("botsing.options");
			int retcode = BotsingInvoker.runBotsing(pom,
					ServletUtils.getGitlabConfig(getServletContext()).getProperty("botsing.version"),
					FileUtils.tempFile(exception), 1, tempDir.getAbsolutePath(),
					options,
					new PrintStream(response.getOutputStream()));
			writer.append("</pre>\n");
			File test = null;
			if(retcode == 0) {
				test = BotsingInvoker.findGeneratedTest(tempDir);
			}
			
			// If Botsing generated a test, display the code.
			if(test != null) {
				writer.append("<h2>SUCCESS: Crash tests successfully generated !</h2><br/>\n");
				BufferedReader in = null;
				try {
					in = new BufferedReader(new FileReader(test));
					String line;
					writer.append("<code><pre>\n");
					while((line = in.readLine()) != null) {
						writer.append(line + "\n");
					}
					writer.append("</pre></code>\n");
				} finally {
					if(in != null) try { in.close(); } catch(Exception ignore) { }
				}
				FileUtils.deleteIfExists(tempDir);
				
			} else {
				writer.append("<h2>FAILURE: Crash tests generation failed.</h2><br/>");
			}
		}

		writer.append("<p><a href=\"index.jsp\">Home</a></p>");
		writer.append("</body></html>");
		writer.flush();
	}

}
