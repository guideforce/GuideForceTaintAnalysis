/*******************************************************************************
 * Copyright (c) 2012 Secure Software Engineering Group at EC SPRIDE.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors: Christian Fritz, Steven Arzt, Siegfried Rasthofer, Eric
 * Bodden, and others.
 ******************************************************************************/
package mockup.servlet;

import mockup.Replaces;
import ourlib.nonapp.TaintAPI;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import java.util.Enumeration;
import java.util.StringTokenizer;

@Replaces({"javax.servlet.ServletConfig","javax.servlet.GenericServlet"})
public class DummyServletConfig implements ServletConfig {

	@Override
	public String getInitParameter(String arg0) {
		return TaintAPI.getTaintedString();
	}

	@Override
	public Enumeration<?> getInitParameterNames() {
		String s = TaintAPI.getTaintedString();
		return new StringTokenizer(s);
	}

	@Override
	public ServletContext getServletContext() {
		return new DummyServletContext();
	}

	@Override
	public String getServletName() {
		return "dummyServlet";
	}
}
