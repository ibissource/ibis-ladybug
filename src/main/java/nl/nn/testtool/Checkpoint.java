/*
   Copyright 2018 Nationale-Nederlanden

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
package nl.nn.testtool;

import net.sf.saxon.trans.XPathException;
import nl.nn.testtool.run.ReportRunner;
import nl.nn.testtool.run.RunResult;
import nl.nn.testtool.storage.StorageException;
import nl.nn.testtool.util.ImportResult;
import nl.nn.testtool.util.LogUtil;
import nl.nn.testtool.util.XmlUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.ws.security.util.DOM2Writer;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.w3c.dom.Node;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Jaco de Groot
 */
public class Checkpoint implements Serializable, Cloneable {
	// See comment above field serialVersionUID on class Report
	private transient static final long serialVersionUID = 4;
	private transient static Logger log = LogUtil.getLogger(Checkpoint.class);
	private Report report;
	private String threadName;
	private String sourceClassName;
	private String name;
	private String message;
	private int type;
	private int level = 0;
	private boolean messageHasBeenStubbed = false;
	private int stub = STUB_FOLLOW_REPORT_STRATEGY;
	private int preTruncatedMessageLength = -1;
	private long estimatedMemoryUsage = -1;
	private transient static Pattern genericVariablePattern;
	private transient static Pattern externalVariablePattern;
	private transient Map<String, Pattern> variablePatternMap;
	static {
		genericVariablePattern = Pattern.compile("\\$\\{.*?\\}");
		externalVariablePattern = Pattern.compile("\\$\\{checkpoint\\(([0-9]+#[0-9]+)\\)(\\.xpath\\((.*?)\\)|)\\}");
	}

	public transient static final int TYPE_NONE = 0;
	public transient static final int TYPE_STARTPOINT = 1;
	public transient static final int TYPE_ENDPOINT = 2;
	public transient static final int TYPE_ABORTPOINT = 3;
	public transient static final int TYPE_INPUTPOINT = 4;
	public transient static final int TYPE_OUTPUTPOINT = 5;
	public transient static final int TYPE_INFOPOINT = 6;
	public transient static final int TYPE_THREADCREATEPOINT = 7;
	public transient static final int TYPE_THREADSTARTPOINT = 8;
	public transient static final int TYPE_THREADENDPOINT = 9;
	public transient static final int STUB_FOLLOW_REPORT_STRATEGY = -1;
	public transient static final int STUB_NO = 0;
	public transient static final int STUB_YES = 1;
	
	public Checkpoint() {
		// Only for Java XML encoding/decoding! Use other constructor instead.
	}

	public Checkpoint(Report report, String threadName, String sourceClassName,
			String name, Object message, int type, int level) {
		this.report = report;
		this.threadName = threadName;
		this.sourceClassName = sourceClassName;
		this.name = name;
		setMessage(message);
		this.type = type;
		this.level = level;
	}

	// JsonIgnore is used so that Jackson will not get into an infinite loop trying to reference report,
	// which already contains checkpoint.
	@JsonIgnore
	public void setReport(Report report) {
		this.report = report;
	}

	@JsonIgnore
	public Report getReport() {
		return report;
	}

	public void setThreadName(String threadName) {
		this.threadName = threadName;
	}

	public String getThreadName() {
		return threadName;
	}

	public void setSourceClassName(String sourceClassName) {
		this.sourceClassName = sourceClassName;
	}

	public String getSourceClassName() {
		return sourceClassName;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setMessage(String message) {
		// report is null when called by XMLDecoder
		if (report != null && report.getTestTool() != null && report.getMessageTransformer() != null) {
			message = report.getMessageTransformer().transform(message);
		}
		this.message = message;
	}

	public void setMessage(Object message) {
		if (message != null) {
			if (message instanceof Node) {
				Node node = (Node)message;
				setMessage(DOM2Writer.nodeToString(node));
			} else {
				setMessage(message.toString());
			}
		}
	}

	public String getMessage() {
		return message;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getType() {
		return type;
	}

	public String getTypeAsString() {
		return getTypeAsString(getType());
	}

	public static String getTypeAsString(int type) {
		String typeAsString = null;
		switch (type) {
			case TYPE_NONE : typeAsString = "None"; break;
			case TYPE_STARTPOINT : typeAsString = "Startpoint"; break;
			case TYPE_ENDPOINT : typeAsString = "Endpoint"; break;
			case TYPE_ABORTPOINT : typeAsString = "Abortpoint"; break;
			case TYPE_INPUTPOINT : typeAsString = "Inputpoint"; break;
			case TYPE_OUTPUTPOINT : typeAsString = "Outputpoint"; break;
			case TYPE_INFOPOINT : typeAsString = "Infopoint"; break;
			case TYPE_THREADSTARTPOINT : typeAsString = "ThreadStartpoint"; break;
			case TYPE_THREADENDPOINT : typeAsString = "ThreadEndpoint"; break;
		}
		return typeAsString;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public int getLevel() {
		return level;
	}
	
	public void setMessageHasBeenStubbed(boolean messageHasBeenStubbed) {
		this.messageHasBeenStubbed = messageHasBeenStubbed;
	}

	public boolean getMessageHasBeenStubbed() {
		return messageHasBeenStubbed;
	}

	public void setStub(int stub) {
		this.stub = stub;
	}
	
	public int getStub() {
		return stub;
	}

	@JsonIgnore
	public Path getPath() {
		Path path = new Path(level + 1);
		path.setName(level, name);
		int currentLevel = level;
		for (int i = report.getCheckpoints().indexOf(this) - 1; i >= 0; i--) {
			Checkpoint currentCheckpoint = (Checkpoint)report.getCheckpoints().get(i);
			if (currentCheckpoint.getLevel() == currentLevel && currentCheckpoint.getName().equals(name)) {
				path.incrementCount(currentLevel);
			} else if (currentCheckpoint.getLevel() < currentLevel) {
				currentLevel = currentCheckpoint.getLevel();
				path.setName(currentLevel, currentCheckpoint.getName());
			}
		}
		return path;
	}

	/**
	 * Estimated memory usage in bytes.
	 * 
	 * @return estimated memory usage in bytes
	 */
	public long getEstimatedMemoryUsage() {
		if (estimatedMemoryUsage > -1) {
			return estimatedMemoryUsage;
		} else if (message == null) {
			return 0L;
		} else {
			return message.length() * 2;
		}
	}

	public Object clone() throws CloneNotSupportedException {
		Checkpoint checkpoint = (Checkpoint)super.clone();
		checkpoint.setReport(null);
		return checkpoint;
	}

	public String toString() {
		return name;
	}

	/**
	 * Sets the length of the message before it was truncated, so that the Ladybug's UI
	 * can display the amount of characters that were removed from the original message.
	 * 
	 * @param length The length of the message before it was truncated.
	 */
	public void setPreTruncatedMessageLength(int length) {
		preTruncatedMessageLength = length;
	}

	public int getPreTruncatedMessageLength() {
		return preTruncatedMessageLength;
	}
	
	public void setEstimatedMemoryUsage(long estimatedMemoryUsage) {
		this.estimatedMemoryUsage = estimatedMemoryUsage;
	}

	public String getMessageWithResolvedVariables(ReportRunner reportRunner) {
		String result = getMessage();
		if(getMessage() != null && containsVariables()) {
			// 1. Parse external report variables
			if(reportRunner != null) {
				List<MatchResult> matchResults = new ArrayList<MatchResult>();
				Matcher m = externalVariablePattern.matcher(getMessage());
				while(m.find()) {
					matchResults.add(m.toMatchResult());
				}
				for(MatchResult matchResult : matchResults) {
					int reportStorageId = Integer.parseInt(matchResult.group(1).split("#")[0]);
					int checkpointIndex = Integer.parseInt(matchResult.group(1).split("#")[1]);
					String xpathExpression = null;
					if(StringUtils.isNotEmpty(matchResult.group(2))) {
						xpathExpression = matchResult.group(3);
					}
					
					// Determine the target report
					Report targetReport = null;
					try {
						for(Entry<Integer, RunResult> entry : reportRunner.getResults().entrySet()) {
							if(entry.getKey() == reportStorageId) {
								targetReport = reportRunner.getRunResultReport(entry.getValue().correlationId);
							}
						}
					} catch (StorageException e) {
						log.error(e);
					}
					// Attempt to fetch data from xpath in target checkpoint's XML message
					if(targetReport != null) {
						try {
							String targetCheckpointMessage = targetReport.getCheckpoints().get(checkpointIndex).getMessage();
							if(StringUtils.isNotEmpty(targetCheckpointMessage)) {
								if(StringUtils.isNotEmpty(xpathExpression)) {
									try {
										String xpathResult = XmlUtil.createXPathEvaluator(xpathExpression).evaluate(targetCheckpointMessage);
										if(xpathResult != null) {
											try {
												result = result.replace(matchResult.group(), xpathResult);
											} catch (IllegalArgumentException e) {
												if(genericVariablePattern.matcher(xpathResult).find()) {
													log.warn(warningMessageHeader(matchResult.group())
															+"Specified xpath expression points to incorrectly parsed parameter "+xpathResult+"; "
															+ "see other recent log warnings for a possible cause");
												}
											}
										}
									} catch (XPathException e) {
										log.warn(warningMessageHeader(matchResult.group())+"Invalid xpath expression or XML message in target checkpoint");
									}
								} else {
									result = result.replaceAll(Pattern.quote(matchResult.group()), targetCheckpointMessage);
								}
							} else {
								log.warn(warningMessageHeader(matchResult.group())+"Target checkpoint ["+targetReport.getCheckpoints().get(checkpointIndex)+"] contains no message");
							}
						} catch (IndexOutOfBoundsException e) {
							log.warn(warningMessageHeader(matchResult.group())+"Index out of bounds: checkpoint with index ["+checkpointIndex+"] does not exist in report with storageId ["+reportStorageId+"]");
						}
					} else {
						log.warn(warningMessageHeader(matchResult.group())+"Run result not found for storageId ["+reportStorageId+"] - please make sure it runs before this report");
					}
				}
			}
			// 2. Parse local variables
			if(StringUtils.isNotEmpty(report.getVariableCsv())) {
				Map<String, String> variableMap = report.getVariablesAsMap();
				Map<String, Pattern> variablePatternMap = getVariablePatternMap(variableMap);
				for(Entry<String, String> entry : variableMap.entrySet()) {
					Matcher m = variablePatternMap.get(entry.getKey()).matcher(getMessage());
					while(m.find()) {
						result = result.replaceAll(Pattern.quote(m.group()), entry.getValue());
					}
				}
			}
		}
		return result;
	}

	private String warningMessageHeader(String parameter) {
		return "Could not parse parameter "+parameter+" found in the input of report ["+report.getFullPath()+"] with storageId ["+report.getStorageId()+"]\n"; 
	}
	
	public boolean containsVariables() {
		if(StringUtils.isEmpty(getMessage())) return false;
		return genericVariablePattern.matcher(getMessage()).find();
	}

	protected Map<String, Pattern> getVariablePatternMap(Map<String, String> variableMap) {
		if(variablePatternMap == null) {
			variablePatternMap = new HashMap<String, Pattern>();
			for(Entry<String, String> entry : variableMap.entrySet()) {
				variablePatternMap.put(entry.getKey(), Pattern.compile("\\$\\{"+entry.getKey()+"\\}"));
			}
		}
		return variablePatternMap;
	}

	public int getIndex() {
		return report.getCheckpoints().indexOf(this);
	}

	public String getUID() {
		return report.getStorageId()+"#"+report.getCheckpoints().indexOf(this);
	}

	/**
	 * To be called when reports are uploaded to the Ladybug. Updates variables referring to 
	 * a report that had its storageId changed.
	 */
	public boolean updateVariables(List<ImportResult> importResults) {
		boolean isVariablesUpdated = false;
		Matcher m = externalVariablePattern.matcher(getMessage());
		List<MatchResult> matchResults = new ArrayList<MatchResult>();
		while(m.find()) {
			matchResults.add(m.toMatchResult());
		}
		for(MatchResult matchResult : matchResults) {
			int matchResultStorageId = Integer.valueOf(matchResult.group(1).split("#")[0]);
			for(ImportResult importResult : importResults) {
				if(matchResultStorageId == importResult.getOldStorageId()) {
					int newStorageId = importResult.getNewStorageId();
					String newVar = matchResult.group().replaceAll(String.valueOf(matchResultStorageId), String.valueOf(newStorageId));
					setMessage(getMessage().replace(matchResult.group(), newVar));
					isVariablesUpdated = true;
				}
			}
		}
		return isVariablesUpdated;
	}
}
