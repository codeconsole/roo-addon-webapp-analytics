package org.codeconsole.roo.addon.webappanalytics;

/**
 * Interface of commands that are available via the Roo shell.
 *
 */
public interface AnalyticsOperations {
	String ANALYTICS_FILTER_NAME = "analyticsFilter";
	boolean isInstallAnalyticsAvailable(boolean debug);
	void installAnalytics();	
}