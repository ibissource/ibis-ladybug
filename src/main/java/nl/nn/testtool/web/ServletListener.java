/*
   Copyright 2021 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.testtool.web;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.WebListener;
import java.lang.invoke.MethodHandles;

@WebListener
public class ServletListener implements ServletContextListener {
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Override
	public void contextInitialized(ServletContextEvent servletContextEvent) {
		logger.info("Initialized servlet context is found.");
		ServletContext context = servletContextEvent.getServletContext();

		// Add ladybug backend
		String name = "ladybug";
		String mapping = "/ladybug/*";
		logger.info("Registering servlet with name [" + name + "] with mapping [" + mapping + "].");
		ServletRegistration.Dynamic serv = context.addServlet(name, ApiServlet.class);
		serv.setLoadOnStartup(0);
		serv.addMapping(mapping);
		serv.setInitParameters(ApiServlet.getInitParameters());
		context.log("Finished registering servlet with name [" + name + "] with mapping [" + mapping + "].");

		// Add ladybug frontend server
		name = "ladybug-frontend";
		mapping = "/ladybug/frontend/*";
		logger.info("Registering servlet with name [" + name + "] with mapping [" + mapping + "].");
		serv = context.addServlet(name, FrontendServlet.class);
		serv.setLoadOnStartup(0);
		serv.addMapping(mapping);
		context.log("Finished registering servlet with name [" + name + "] with mapping [" + mapping + "].");
	}

	@Override
	public void contextDestroyed(ServletContextEvent servletContextEvent) {
	}
}
