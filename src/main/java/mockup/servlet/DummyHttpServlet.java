package mockup.servlet;

import mockup.Replaces;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@SuppressWarnings("serial")
@Replaces({"javax.servlet.http.HttpServlet"})
public class DummyHttpServlet extends GenericServlet {
	
	static {
		// javax.servlet.http.HttpServlet implements <clinit>. So we need to implement it as well.
		int z = 1;
	}

	@Override
	public ServletConfig getServletConfig(){
		return new DummyServletConfig();
	}

	@Override
	public void service(ServletRequest arg0, ServletResponse arg1)
			throws ServletException, IOException {
	}
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp) {
	}

}
