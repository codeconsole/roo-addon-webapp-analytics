package org.codeconsole.roo.addon.webappanalytics;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.springframework.roo.addon.security.SecurityOperations;
import org.springframework.roo.addon.web.mvc.controller.WebMvcOperations;
import org.springframework.roo.metadata.MetadataService;
import org.springframework.roo.process.manager.FileManager;
import org.springframework.roo.process.manager.MutableFile;
import org.springframework.roo.project.Dependency;
import org.springframework.roo.project.Path;
import org.springframework.roo.project.PathResolver;
import org.springframework.roo.project.ProjectMetadata;
import org.springframework.roo.project.ProjectOperations;
import org.springframework.roo.support.util.FileCopyUtils;
import org.springframework.roo.support.util.TemplateUtils;
import org.springframework.roo.support.util.WebXmlUtils;
import org.springframework.roo.support.util.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Implementation of commands that are available via the Roo shell.
 *
 */
@Component
@Service
public class AnalyticsOperationsImpl implements AnalyticsOperations {

	private static Logger logger = Logger.getLogger(AnalyticsOperations.class.getName());

	@Reference private FileManager fileManager;
	@Reference private MetadataService metadataService;
	@Reference private PathResolver pathResolver;
	@Reference private ProjectOperations projectOperations;

	public boolean isInstallAnalyticsAvailable(boolean debug) {
		ProjectMetadata project = (ProjectMetadata) metadataService.get(ProjectMetadata.getProjectIdentifier());
		if (project == null) {
			if (debug) {
				logger.info("Please configure a project first. Run 'project'.");
			}
			return false;
		}

		// Do not permit installation unless they have a web project
		if (!fileManager.exists(pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "/WEB-INF/web.xml"))) {
			if (debug) {
				logger.info("Please set up a web project. No web.xml has been found. The 'controller' command will do this for you.");
			}			
			return false;
		}
		
		// Only permit if email is configured.
		if (!isEmailConfigured()) {
			if (debug) {
				logger.info("Please configure email first.  Run 'email sender setup'.");
			}			
			return false;
		}

		// Only permit installation if they don't already have some version of Webapp Analytics installed
		if (!(project.getDependenciesExcludingVersion(new Dependency("org.codeconsole", "webapp-analytics", "0.5.9")).size() == 0)) {
			if (debug) {
				logger.info("Webapp Analytics has already been installed.");
			}
			return false;
		}
		return true;
	}

	private boolean isEmailConfigured() {
		String contextPath = pathResolver.getIdentifier(Path.SPRING_CONFIG_ROOT, "applicationContext.xml");
		try {
			if (fileManager.exists(contextPath)) {
				Document appCtx = XmlUtils.getDocumentBuilder().parse(fileManager.updateFile(contextPath).getInputStream());
				return XmlUtils.findFirstElement("/beans/bean[@class='org.springframework.mail.javamail.JavaMailSenderImpl']", (Element) appCtx.getFirstChild()) != null;
			} else {
				new IllegalStateException("Could not aquire the Spring applicationContext.xml file");
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}					
		return false;
	}
	
	
	public void installAnalytics() {
		// Parse the configuration.xml file
		Element configuration = XmlUtils.getConfiguration(getClass());

		// Add dependencies to POM
		updateDependencies(configuration);		
		
		// Copy the template across
		String destination = pathResolver.getIdentifier(Path.SPRING_CONFIG_ROOT, "applicationContext-analytics.xml");
		if (!fileManager.exists(destination)) {
			try {
				FileCopyUtils.copy(TemplateUtils.getTemplate(getClass(), "applicationContext-analytics-template.xml"), fileManager.createFile(destination).getOutputStream());
			} catch (IOException ioe) {
				throw new IllegalStateException(ioe);
			}
		}		

		String webXml = pathResolver.getIdentifier(Path.SRC_MAIN_WEBAPP, "WEB-INF/web.xml");

		try {
			if (fileManager.exists(webXml)) {
				MutableFile mutableWebXml = fileManager.updateFile(webXml);
				Document webXmlDoc = XmlUtils.getDocumentBuilder().parse(mutableWebXml.getInputStream());
				String afterFilter = null;
				// If Spring Security is installed
				if (XmlUtils.findFirstElement("/web-app/filter[filter-name = '" + SecurityOperations.SECURITY_FILTER_NAME + "']", webXmlDoc.getDocumentElement()) != null) {
					afterFilter = SecurityOperations.SECURITY_FILTER_NAME;
				} else {
					afterFilter = WebMvcOperations.HTTP_METHOD_FILTER_NAME;
				}
				WebXmlUtils.addFilterAtPosition(WebXmlUtils.FilterPosition.BETWEEN, afterFilter, WebMvcOperations.OPEN_ENTITYMANAGER_IN_VIEW_FILTER_NAME, AnalyticsOperations.ANALYTICS_FILTER_NAME, "org.codeconsole.web.analytics.AnalyticsFilter", "/*", webXmlDoc, null, new WebXmlUtils.WebXmlParam("history-size", "50"), new WebXmlUtils.WebXmlParam("exclude-urls", "/example/resource.*"), new WebXmlUtils.WebXmlParam("exclude-params", ".*password.*"));				
				XmlUtils.writeXml(mutableWebXml.getOutputStream(), webXmlDoc);
			} else {
				throw new IllegalStateException("Could not acquire " + webXml);
			}
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}		
	}
	
	private void updateDependencies(Element configuration) {
		List<Element> analyticsDependencies = XmlUtils.findElements("/configuration/webapp-analytics/dependencies/dependency", configuration);
		for (Element dependencyElement : analyticsDependencies) {
			projectOperations.addDependency(new Dependency(dependencyElement));
		}
	}	
}