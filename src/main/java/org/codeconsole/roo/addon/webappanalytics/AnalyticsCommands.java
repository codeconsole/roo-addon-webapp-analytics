package org.codeconsole.roo.addon.webappanalytics;

import java.util.logging.Logger;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.component.ComponentContext;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.shell.converters.StaticFieldConverter;
import org.springframework.roo.support.util.Assert;

/**
 * Sample of a command class. The command class is registered by the Roo shell following an
 * automatic classpath scan. You can provide simple user presentation-related logic in this
 * class. You can return any objects from each method, or use the logger directly if you'd
 * like to emit messages of different severity (and therefore different colours on 
 * non-Windows systems).
 * 
 */
@Component
@Service
public class AnalyticsCommands implements CommandMarker {
	
	private static Logger logger = Logger.getLogger(AnalyticsCommands.class.getName());

	@Reference private AnalyticsOperations analytics;

	@CliAvailabilityIndicator("analytics setup") public boolean isSetupAnalyticsAvailable() {
		return analytics.isInstallAnalyticsAvailable(false);
	}

	@CliCommand(value = "analytics setup", help = "Install Webapp Analytics into your project for tracking page performance.") 
	public void setupAnayltics() {
		analytics.installAnalytics();
	}	
	
	@CliAvailabilityIndicator("analytics info") public boolean isAnalyticsInfoAvailable() {
		return true;
	}

	@CliCommand(value = "analytics info", help = "Determines if Webapp Analytics can be installed into your project.") 
	public void analyticsInfo() {
		logger.info("Determining Webapp Analytics Installation Requriements.");
		if (analytics.isInstallAnalyticsAvailable(true)) {
			logger.info("You are now able to install Webapp Analytics, run 'analytics setup' to install.");
		}
	}	
}