package mockup.servlet;

import mockup.Replaces;
import ourlib.nonapp.TaintAPI;

import javax.servlet.http.HttpServletRequest;

@Replaces("com.oreilly.servlet.MultipartRequest")
public class MultipartRequest {
	public MultipartRequest(HttpServletRequest req, String saveDir) {		
	}
	
	public String getParameter(String name) {
		return TaintAPI.getTaintedString();
	}
}
