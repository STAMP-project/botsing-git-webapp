package eu.stamp_project.botsing.gitlab.webhook;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Gitlab web hook implementation servlet for issue events.
 * @author Pierre-Yves Gibello - OW2
 *
 */
@WebServlet(urlPatterns = "/IssueWebHookServlet", asyncSupported = true)
public class IssueWebHookServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	ThreadPoolExecutor executor;

	/**
     * @see HttpServlet#HttpServlet()
     */
    public IssueWebHookServlet() {
        super();
    }

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		this.executor = new ThreadPoolExecutor(100, 200, 50000L,
				TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(100));
		config.getServletContext().setAttribute("executor", this.executor);
	}
	
	@Override
	public void destroy() {
		this.executor.shutdown();
		super.destroy();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// Delegate asynchronous request processing to Botsing async processor
		request.startAsync();
		ThreadPoolExecutor executor = (ThreadPoolExecutor) request.getServletContext().getAttribute("executor");
		executor.execute(new BotsingAsyncProcessor(request));
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

}
