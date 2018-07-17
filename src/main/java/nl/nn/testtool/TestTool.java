/*
 * Created on 01-Nov-07
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package nl.nn.testtool;

import java.io.IOException;
import java.io.InputStream;
import java.rmi.server.UID;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import nl.nn.testtool.storage.LogStorage;
import nl.nn.testtool.transform.MessageTransformer;
import nl.nn.testtool.util.LogUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * @author m00f069
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class TestTool {
	private static Logger log = LogUtil.getLogger(TestTool.class);
	public final static String LOGGING_STORAGE_NAME = "Logging";
	private String configName;
	private String configVersion;
	private int maxCheckpoints = 2500;
	private Debugger debugger;
	private boolean reportGeneratorEnabled = true;
	private List reportsInProgress = new ArrayList();
	private Map reportsInProgressByCorrelationId = new HashMap();
	private long numberOfReportsInProgress = 0;
	private long reportsInProgressEstimatedMemoryUsage = 0;
	private Map originalReports = new HashMap();
	private List startpointProviders = new ArrayList();
	private List startpointProviderNames = new ArrayList();
	private LogStorage logStorage;
	private MessageTransformer messageTransformer;
	private List reportsToSkip = new ArrayList();
	private String regexFilter;

	public void setConfigName(String configName) {
		this.configName = configName;
	}

	public String getConfigName() {
		return configName;
	}

	public void setConfigVersion(String configVersion) {
		this.configVersion = configVersion;
	}

	public String getConfigVersion() {
		return configVersion;
	}

	public void setMaxCheckpoints(int maxCheckpoints) {
		this.maxCheckpoints = maxCheckpoints;
	}

	public int getMaxCheckpoints() {
		return maxCheckpoints;
	}

	public void setDebugger(Debugger debugger) {
		this.debugger = debugger;
	}

	public void setLogStorage(LogStorage logStorage) {
		this.logStorage = logStorage;
	}

	public LogStorage getLogStorage() {
		return logStorage;
	}

	public void setMessageTransformer(MessageTransformer messageTransformer) {
		this.messageTransformer = messageTransformer;
	}

	public MessageTransformer getMessageTransformer() {
		return messageTransformer;
	}

	public void setReportGeneratorEnabled(boolean reportGeneratorEnabled) {
		this.reportGeneratorEnabled = reportGeneratorEnabled;
	}

	public boolean getReportGeneratorEnabled() {
		return reportGeneratorEnabled;
	}
	
	public void setRegexFilter(String regexFilter) {
		this.regexFilter = regexFilter;
	}

	public String getRegexFilter() {
		return regexFilter;
	}

	private Object checkpoint(String correlationId, String threadId,
			String sourceClassName, String name, Object message, int checkpointType,
			int levelChangeNextCheckpoint) {
		synchronized(reportsInProgress) {
			if (checkpointType == Checkpoint.TYPE_STARTPOINT
					&& StringUtils.isNotEmpty(regexFilter)
					&& name != null && !name.matches(regexFilter)) {
				reportsToSkip.add(correlationId);
			}
			if (reportsToSkip.contains(correlationId)) {
				//skip
			} else {
				Report report = (Report)reportsInProgressByCorrelationId.get(correlationId);
				if (report == null && reportGeneratorEnabled) {
					if (checkpointType == Checkpoint.TYPE_STARTPOINT) {
						log.debug("Create new report for '" + correlationId + "'");
						report = new Report();
						report.setStartTime(System.currentTimeMillis());
						report.setTestTool(this);
						report.setCorrelationId(correlationId);
						report.setName(name);
						Report originalReport;
						synchronized(originalReports) {
							originalReport = (Report)originalReports.remove(correlationId);
						}
						if (originalReport == null) {
							report.setStubStrategy(debugger.getDefaultStubStrategy());
						} else {
							report.setStubStrategy(originalReport.getStubStrategy());
							report.setOriginalReport(originalReport);
						}
						reportsInProgress.add(0, report);
						reportsInProgressByCorrelationId.put(correlationId, report);
						numberOfReportsInProgress++;
					} else {
						log.warn("Report for '" + correlationId + "' is null, could not add checkpoint '" + name + "'");
					}
				}
				if (report != null) {
					reportsInProgressEstimatedMemoryUsage = reportsInProgressEstimatedMemoryUsage - report.getEstimatedMemoryUsage();
					message = report.checkpoint(threadId, sourceClassName, name, message, checkpointType, levelChangeNextCheckpoint);
					reportsInProgressEstimatedMemoryUsage = reportsInProgressEstimatedMemoryUsage + report.getEstimatedMemoryUsage();
					if (report.finished()) {
						report.setEndTime(System.currentTimeMillis());
						log.debug("Report is finished for '" + correlationId + "'");
						reportsInProgress.remove(report);
						reportsInProgressByCorrelationId.remove(correlationId);
						numberOfReportsInProgress--;
						reportsInProgressEstimatedMemoryUsage = reportsInProgressEstimatedMemoryUsage - report.getEstimatedMemoryUsage();
						logStorage.storeWithoutException(report);
					}
				}
			}
			if (checkpointType == Checkpoint.TYPE_ENDPOINT
					&& StringUtils.isNotEmpty(regexFilter)
					&& name != null && !name.matches(regexFilter)) {
				reportsToSkip.remove(correlationId);
			}
		}
		return message;
	}
	
	public Object startpoint(String correlationId, String sourceClassName, String name, Object message) {
		return checkpoint(correlationId, null, sourceClassName, name, message, Checkpoint.TYPE_STARTPOINT, 1);
	}

	public Object endpoint(String correlationId, String sourceClassName, String name, Object message) {
		return checkpoint(correlationId, null, sourceClassName, name, message, Checkpoint.TYPE_ENDPOINT, -1);
	}

	public Object inputpoint(String correlationId, String sourceClassName, String name, Object message) {
		return checkpoint(correlationId, null, sourceClassName, name, message, Checkpoint.TYPE_INPUTPOINT, 0);
	}

	public Object outputpoint(String correlationId, String sourceClassName, String name, Object message) {
		return checkpoint(correlationId, null, sourceClassName, name, message, Checkpoint.TYPE_OUTPUTPOINT, 0);
	}

	public Object infopoint(String correlationId, String sourceClassName, String name, Object message) {
		return checkpoint(correlationId, null, sourceClassName, name, message, Checkpoint.TYPE_INFOPOINT, 0);
	}

	public Object checkpoint(String correlationId, String sourceClassName, String name, Object message) {
		return checkpoint(correlationId, null, sourceClassName, name, message, Checkpoint.TYPE_NONE, 0);
	}

	/**
	 * Specify the name of a previous startpoint to abort to or a unique name
	 * to finish the report or thread.
	 */
	public Object abortpoint(String correlationId, String sourceClassName, String name, Object message) {
		return checkpoint(correlationId, null, sourceClassName, name, message, Checkpoint.TYPE_ABORTPOINT, -1);
	}

	/**
	 * Set a marker in the report for a child thread to appear. This method
	 * should be called by the parent thread. Specify a threadId that will also
	 * be used by the child thread when calling threadStartpoint. The name of the
	 * child thread can be used as threadId (when known at this point).
	 */
	public void threadCreatepoint(String correlationId, String threadId) {
		checkpoint(correlationId, threadId, null, null, null, Checkpoint.TYPE_THREADCREATEPOINT, 0);
	}

	/**
	 * Startpoint for a child thread. Specify a threadId that was also used on
	 * calling threadStartpoint.
	 */
	public Object threadStartpoint(String correlationId, String threadId, String sourceClassName, String name, Object message) {
		return checkpoint(correlationId, threadId, sourceClassName, name, message, Checkpoint.TYPE_THREADSTARTPOINT, 1);
	}

	/**
	 * Startpoint for a child thread. This method can be used when the name of
	 * the child thread was used as threadId on calling threadCreatepoint.
	 */
	public Object threadStartpoint(String correlationId, String sourceClassName, String name, Object message) {
		return threadStartpoint(correlationId, Thread.currentThread().getName(), sourceClassName, name, message);
	}

	public Object threadEndpoint(String correlationId, String sourceClassName, String name, Object message) {
		return checkpoint(correlationId, null, sourceClassName, name, message, Checkpoint.TYPE_THREADENDPOINT, -1);
	}

	public static String getCorrelationId() {
		return getName().replaceAll(" ", "_")
				+ "-" + getVersion().replaceAll(" ", "_")
				+ "-" + new UID().toString();
	}

	public String rerun(Report report, SecurityContext securityContext) {
		return rerun(null, report, securityContext);
	}

	public String rerun(String correlationId, Report report, SecurityContext securityContext) {
		String errorMessage = null;
		if (correlationId == null) {
			correlationId = getCorrelationId();
		}
		boolean reportGeneratorEnabled = getReportGeneratorEnabled();
		if (reportGeneratorEnabled) {
			synchronized(originalReports) {
				originalReports.put(correlationId, report);
			}
		}
		try {
			errorMessage = debugger.rerun(correlationId, report, securityContext);
		} finally {
			if (reportGeneratorEnabled) {
				Report originalReport;
				synchronized(originalReports) {
					originalReport = (Report)originalReports.remove(correlationId);
				}
				if (errorMessage == null && originalReport != null) {
					errorMessage = "Rerun didn't trigger any checkpoint";
				}
			}
		}
		return errorMessage;
	}

    /**
     * Get the endpoint for the current level from the original report. The
     * optional found and returned checkpoint can be used to check whether the
     * next endpoint will be stubbed, hence code until the next endpoint can
     * be skipped. This method will always return null when report is not in
     * rerun.
     */
	public Checkpoint getOriginalEndpointOrAbortpointForCurrentLevel(String correlationId) {
		Checkpoint result = null;
		synchronized(reportsInProgress) {
			Report report = (Report)reportsInProgressByCorrelationId.get(correlationId);
			if (report != null) {
				result = report.getOriginalEndpointOrAbortpointForCurrentLevel();
			}
		}
		return result;
	}
	// TODO vorige methode niet meer nodig?! hier nog documentern dat je met geturnde report voorzicht moet zijn omdat het nog in progress is? 
	public Report getReportInProgress(String correlationId) {
		synchronized(reportsInProgress) {
			return (Report)reportsInProgressByCorrelationId.get(correlationId);
		}
	}

	public List getStubStrategies() {
		return debugger.getStubStrategies();
	}

	public String getDefaultStubStrategy() {
		return debugger.getDefaultStubStrategy();
	}

	/**
	 * Check whether the checkpoint should be stubbed for the given stub
	 * strategy.
	 * 
	 * @param checkpoint
	 * @param strategy
	 * @return
	 */	
	public boolean stub(Checkpoint checkpoint, String strategy) {
		return debugger.stub(checkpoint, strategy);
	}

	public Report getReportInProgress(int index) {
		Report reportClone = null;
		synchronized(reportsInProgress) {
			if (index > -1 && index < reportsInProgress.size()) {
				Report report = (Report)reportsInProgress.get(index);
				try {
					reportClone = (Report)report.clone();
				} catch (CloneNotSupportedException e) {
					log.error("Unable to clone report in progress", e);
				}
			}
		}
		return reportClone;
	}

	public long getNumberOfReportsInProgress() {
		return numberOfReportsInProgress;
	}

	public long getReportsInProgressEstimatedMemoryUsage() {
		return reportsInProgressEstimatedMemoryUsage;
	}
	
	public void register(StartpointProvider startpointProvider) {
		synchronized(startpointProviders) {
			startpointProviders.add(startpointProvider);
			startpointProviderNames.add(startpointProvider.getName());
		}
	}
	
	public List getStartpointProviderNames() {
		synchronized(startpointProviders) {
			return startpointProviderNames;
		}
	}
	
	public StartpointProvider getStartpointProvider(String name) {
		synchronized(startpointProviders) {
			int i = startpointProviderNames.indexOf(name);
			if (i == -1) {
				return null;
			} else {
				return (StartpointProvider)startpointProviders.get(i);
			}
		}
	}

	public static String getName() {
		return Package.getPackage("nl.nn.testtool").getSpecificationTitle();
	}
	
	public static String getVersion() {
		String version;
		String state = getProperty("version.state");
		if ("final".equals(state)) {
			version = getReleaseVersion();
		} else {
			version = getImplementationVersion() + " " + state + " " + getProperty("version.timestamp");
		}
		return version;
	}

	public static String getSpecificationVersion() {
		return Package.getPackage("nl.nn.testtool").getSpecificationVersion();
	}

	public static String getReleaseVersion() {
		return Package.getPackage("nl.nn.testtool").getSpecificationVersion() + "." + getProperty("version.release");
	}

	public static String getImplementationVersion() {
		return Package.getPackage("nl.nn.testtool").getImplementationVersion();
	}

	public static String getProperty(String property) {
		Properties properties = new Properties();
		InputStream inputStream = TestTool.class.getResourceAsStream("/nl/nn/testtool/TestTool.properties");
		try {
			properties.load(inputStream);
		} catch(IOException e) {
		}
		return properties.getProperty(property);
	}
	
}