package eu.stamp_project.botsing.servlet;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import eu.stamp_project.cicd.utils.botsing.ExceptionExtractor;

/**
 * Log file upload servlet: parse file looking for exception stacks, make them available to call Botsing.
 * Multipart servlet to handle file upload (maximum size 5 Mo).
 * @author Pierre-Yves Gibello - OW2
 *
 */
@MultipartConfig(fileSizeThreshold = 1024 * 1024,
	maxFileSize = 1024 * 1024 * 5, 
	maxRequestSize = 1024 * 1024 * 5 * 5)
public class LogFileServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public LogFileServlet() {
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
		
		boolean done = false;
		
		response.getWriter().append("<html><head><title>Botsing web application</title></head>");
		response.getWriter().append("<body>");

		// Retrieve "logfile" part (file upload), then preprocess for Botsing invocation.
		for (Part part : request.getParts()) {
			String control = part.getName();
			if("logfile".equals(control)) {
				try {
					String logfile = "/tmp" + File.separator + "_" + part.getSubmittedFileName();
					part.write(logfile);
					List<String> exceptions = ExceptionExtractor.explodeExceptions(new File(logfile));
					if(exceptions == null) {
						response.getWriter().append("<p>No exception stack found</p>");
					} else {
						for(String exception : exceptions) {
							response.getWriter().append("<p><form method=\"post\" action=\"BotsingServlet\">");
							response.getWriter().append("<textarea name=\"exception\" cols=\"80\" rows=\"16\">" + exception + "</textarea>");
							response.getWriter().append("<input type=\"submit\" value=\"Run Botsing\">");
							response.getWriter().append("</form></p>");
						}
					}
					done = true;
				} catch(Exception e) {
					e.printStackTrace(System.err);
					ServletUtils.forwardError(request, response, e);
					return;
				}
			}
		}

		if(! done) {
			ServletUtils.forwardError(request, response, "Unexpected error during log file upload or preprocessing");
			return;
		}

		response.getWriter().append("<p><a href=\"index.jsp\">Home</a></p>");
		response.getWriter().append("</body></html>");
	}

}
