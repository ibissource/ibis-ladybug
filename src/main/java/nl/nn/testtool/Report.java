/*
   Copyright 2018-2021 WeAreFrank!

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

import lombok.Data;
import lombok.SneakyThrows;
import nl.nn.testtool.MessageEncoder.ToStringResult;
import nl.nn.testtool.run.ReportRunner;
import nl.nn.testtool.storage.Storage;
import nl.nn.testtool.transform.MessageTransformer;
import nl.nn.testtool.transform.ReportXmlTransformer;
import nl.nn.testtool.util.CsvUtil;
import nl.nn.testtool.util.EscapeUtil;
import nl.nn.testtool.util.XmlUtil;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.Transient;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.*;

/**
 * @author Jaco de Groot
 */
public class Report implements Serializable {
	// The serialVersionUID only needs to be changed when throwing an exception
	// on reading an older version needs to be forced. As long as Java is able
	// to read the older version there's probably no reason to force an
	// exception. In the case Java isn't able to read the older version it will
	// throw an exception, hence no reason either to force throwing an
	// exception. The serialVersionUID also only effects reading objects through
	// ObjectInputStream, it doesn't effect reading objects through XMLDecoder.
	private transient static final long serialVersionUID = 5;
	private transient static Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private transient TestTool testTool;
	// Please note that the set method should return void for XmlEncoder to
	// store the property (hence the setVariableCsvWithoutException method)
	private Long startTime;
	private Long endTime;
	private String correlationId;
	private String name;
	private String description;
	private String path;
	private String stubStrategy;
	private List<Checkpoint> checkpoints = new ArrayList<Checkpoint>();
	private String transformation;
	private String variableCsv;
	// Please note that the get and set methods need @Transient annotation for
	// XmlEncoder to not store the property.
	private transient Report originalReport;
	private transient List<String> threads = new ArrayList<String>();
	private transient Map<String, Integer> threadCheckpointIndex = new HashMap<String, Integer>();
	private transient Map<String, Integer> threadFirstLevel = new HashMap<String, Integer>();
	private transient Map<String, Integer> threadLevel = new HashMap<String, Integer>();
	private transient Map<String, String> threadParent = new HashMap<String, String>();
	private transient int threadsActiveCount = 0;
	private transient boolean closed;
	private transient Storage storage;
	private transient Integer storageId;
	private transient long storageSize;
	private transient ReportXmlTransformer reportXmlTransformer;
	private transient ReportXmlTransformer globalReportXmlTransformer;
	private transient String xml;
	private transient boolean differenceChecked = false;
	private transient boolean differenceFound = false;
	private transient Map<String, String> truncatedMessageMap = new RefCompareMap<String, String>();
	private transient boolean reportFilterMatching = true;
	private transient boolean logReportFilterMatching = true;
	private transient boolean logMaxCheckpoints = true;
	private transient boolean logMaxMemoryUsage = true;
	private transient Map<Object, Set<Checkpoint>> streamingMessageListeners = new HashMap<Object, Set<Checkpoint>>();
	private transient Map<Object, StreamingMessageResult> streamingMessageResults = new HashMap<Object, StreamingMessageResult>();

	public Report() {
		String threadName = Thread.currentThread().getName();
		threads.add(threadName);
		threadCheckpointIndex.put(threadName, new Integer(0));
		threadFirstLevel.put(threadName, new Integer(0));
		threadLevel.put(threadName, new Integer(0));
		threadsActiveCount++;
	}

	@Transient
	@JsonIgnore
	public void setTestTool(TestTool testTool) {
		this.testTool = testTool;
	}

	@Transient
	@JsonIgnore
	public TestTool getTestTool() {
		return testTool;
	}

	@Transient
	@JsonIgnore
	public void setClosed(boolean closed) {
		this.closed = closed;
	}

	@Transient
	@JsonIgnore
	public boolean isClosed() {
		return closed;
	}

	@Transient
	@JsonIgnore
	public void setStorage(Storage storage) {
		this.storage = storage;
	}

	@Transient
	@JsonIgnore
	public Storage getStorage() {
		return storage;
	}

	public void setStorageId(Integer storageId) {
		this.storageId = storageId;
	}

	public Integer getStorageId() {
		return storageId;
	}

	@Transient
	@JsonIgnore
	public void setStorageSize(long storageSize) {
		this.storageSize = storageSize;
	}

	@Transient
	@JsonIgnore
	public Long getStorageSize() {
		return storageSize;
	}
	
	public void setStartTime(Long startTime) {
		this.startTime = startTime;
	}

	public Long getStartTime() {
		return startTime;
	}

	public void setEndTime(Long endTime) {
		this.endTime = endTime;
	}

	public Long getEndTime() {
		return endTime;
	}

	public void setCorrelationId(String correlationId) {
		this.correlationId = correlationId;
	}
	
	public String getCorrelationId() {
		return correlationId;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getPath() {
		return path;
	}

	public String getFullPath() {
		return (StringUtils.isNotEmpty(getPath()) ? getPath() : "/") + getName();
	}
	
	public void setStubStrategy(String stubStrategy) {
		this.stubStrategy = stubStrategy;
	}

	public String getStubStrategy() {
		return stubStrategy;
	}

	public void setTransformation(String transformation) {
		this.transformation = transformation;
	}

	public String getTransformation() {
		return transformation;
	}

	@Transient
	@JsonIgnore
	public void setReportXmlTransformer(ReportXmlTransformer reportXmlTransformer) {
		this.reportXmlTransformer = reportXmlTransformer;
	}

	@Transient
	@JsonIgnore
	public ReportXmlTransformer getReportXmlTransformer() {
		return reportXmlTransformer;
	}

	@Transient
	@JsonIgnore
	public void setGlobalReportXmlTransformer(ReportXmlTransformer globalReportXmlTransformer) {
		this.globalReportXmlTransformer = globalReportXmlTransformer;
	}

	@Transient
	@JsonIgnore
	public ReportXmlTransformer getGlobalReportXmlTransformer() {
		return globalReportXmlTransformer;
	}

	@Transient
	@JsonIgnore
	public void setOriginalReport(Report originalReport) {
		this.originalReport = originalReport;
	}

	@Transient
	public Report getOriginalReport() {
		return originalReport;
	}

	@Transient
	public void setReportFilterMatching(boolean reportFilterMatching) {
		this.reportFilterMatching = reportFilterMatching;
	}

	@Transient
	public boolean isReportFilterMatching() {
		return reportFilterMatching;
	}

	protected <T> T checkpoint(String childThreadId, String sourceClassName, String name, T message,
			StubableCode stubableCode, StubableCodeThrowsException stubableCodeThrowsException,
			Set<String> matchingStubStrategies, int checkpointType, int levelChangeNextCheckpoint) {
		String parentThreadName = Thread.currentThread().getName();
		if (checkpointType == Checkpoint.TYPE_THREADCREATEPOINT) {
			if (!threads.contains(parentThreadName)) {
				log.warn("Unknown parent thread '" + parentThreadName + "' for child thread '" + childThreadId
						+ "' , ignored checkpoint " + getCheckpointLogDescription(name, checkpointType, null));
			} else {
				threadCreatepoint(parentThreadName, childThreadId);
			}
		} else {
			if (checkpointType == Checkpoint.TYPE_THREADSTARTPOINT && !threads.contains(childThreadId)) {
				parentThreadName = threads.get(threads.size() - 1);
				threadCreatepoint(parentThreadName, childThreadId);
				log.warn("New child thread '" + childThreadId + "' for parent thread '" + parentThreadName
						+ "' detected, use threadCreatepoint() before threadStartpoint() for checkpoint "
						+ getCheckpointLogDescription(name, checkpointType, null));
			} else if (checkpointType == Checkpoint.TYPE_STARTPOINT && !threads.contains(parentThreadName)) {
				checkpointType = Checkpoint.TYPE_THREADSTARTPOINT;
				childThreadId = parentThreadName;
				parentThreadName = threads.get(threads.size() - 1);
				threadCreatepoint(parentThreadName, childThreadId);
				log.warn("New child thread '" + childThreadId + "' for parent thread '" + parentThreadName
						+ "' detected, use threadCreatepoint() and threadStartpoint() instead of startpoint() for checkpoint "
						+ getCheckpointLogDescription(name, checkpointType, null));
			}
			message = addCheckpoint(childThreadId, sourceClassName, name, message, stubableCode, stubableCodeThrowsException,
					matchingStubStrategies, checkpointType, levelChangeNextCheckpoint);
		}
		return message;
	}

	private void threadCreatepoint(String parentThreadName, String childThreadId) {
		threads.add(threads.indexOf(parentThreadName), childThreadId);
		threadCheckpointIndex.put(childThreadId, threadCheckpointIndex.get(parentThreadName));
		threadFirstLevel.put(childThreadId, (Integer)threadLevel.get(parentThreadName));
		threadLevel.put(childThreadId, (Integer)threadLevel.get(parentThreadName));
		threadParent.put(childThreadId, parentThreadName);
		threadsActiveCount++;
	}

	private  <T> T addCheckpoint(String childThreadId, String sourceClassName, String name, T message,
			StubableCode stubableCode, StubableCodeThrowsException stubableCodeThrowsException,
			Set<String> matchingStubStrategies, int checkpointType, int levelChangeNextCheckpoint) {
		String threadName = Thread.currentThread().getName();
		Integer index = (Integer)threadCheckpointIndex.get(threadName);
		Integer level = (Integer)threadLevel.get(threadName);
		if (name == null) {
			log.warn("Ignored checkpoint with null name " + getCheckpointLogDescription(name, checkpointType, level));
		} else {
			// At this point index will already be != null when name of the child thread was used as childThreadId when
			// calling threadCreatepoint()
			if (index == null && checkpointType == Checkpoint.TYPE_THREADSTARTPOINT) {
				index = (Integer)threadCheckpointIndex.remove(childThreadId);
				if (index != null) {
					// Rename child thread id in the relevant maps to the actual thread name of the child thread (which
					// at this point is the current thread (calling it's first checkpoint (threadStartpoint())) for as
					// far as they are not already the same (in which case index will be initialized with a non null
					// value at the beginning of this method
					threads.add(threads.indexOf(childThreadId), threadName);
					threads.remove(childThreadId);
					threadCheckpointIndex.put(threadName, index);
					level = (Integer)threadFirstLevel.remove(childThreadId);
					threadFirstLevel.put(threadName, level);
					level = (Integer)threadLevel.remove(childThreadId);
					threadLevel.put(threadName, level);
					String parent = (String)threadParent.remove(childThreadId);
					threadParent.put(threadName, parent);
				} else {
					log.warn("Unknown childThreadId '" + childThreadId
							+ "', use the same childThreadId when calling threadCreatepoint() and threadStartpoint()");
				}
			}
			if (index == null) {
				log.warn("Unknown thread, ignored checkpoint " + getCheckpointLogDescription(name, checkpointType,
						level));
			} else {
				if (!isReportFilterMatching()) {
					message = TestTool.execute(stubableCode, stubableCodeThrowsException, message);
					if (logReportFilterMatching) {
						log.debug("Report name doesn't match report filter regex, ignored checkpoint "
								+ getCheckpointLogDescription(name, checkpointType, level) + " "
								+ getOtherCheckpointsLogDescription());
						logReportFilterMatching = false;
					}
				} else if (checkpoints.size() >= testTool.getMaxCheckpoints()) {
					message = TestTool.execute(stubableCode, stubableCodeThrowsException, message);
					if (logMaxCheckpoints) {
						log.warn("Maximum number of checkpoints exceeded, ignored checkpoint "
								+ getCheckpointLogDescription(name, checkpointType, level) + " "
								+ getOtherCheckpointsLogDescription());
						logMaxCheckpoints = false;
					}
				} else if (getEstimatedMemoryUsage() >= testTool.getMaxMemoryUsage()) {
					message = TestTool.execute(stubableCode, stubableCodeThrowsException, message);
					if (logMaxMemoryUsage) {
						log.warn("Maximum memory usage reached for this report, ignored checkpoint "
								+ getCheckpointLogDescription(name, checkpointType, level) + " "
								+ getOtherCheckpointsLogDescription());
						logMaxMemoryUsage = false;
					}
				} else {
					message = addCheckpoint(threadName, sourceClassName, name, message, stubableCode,
							stubableCodeThrowsException, matchingStubStrategies, checkpointType, index, level
							);
				}
				Integer newLevel = new Integer(level.intValue() + levelChangeNextCheckpoint);
				threadLevel.put(threadName, newLevel);
				if (newLevel.equals(threadFirstLevel.get(threadName))) {
					close(threadName);
				}
			}
		}
		return message;
	}

	@SneakyThrows
	private  <T> T addCheckpoint(String threadName, String sourceClassName, String name, T message,
			StubableCode stubableCode, StubableCodeThrowsException stubableCodeThrowsException,
			Set<String> matchingStubStrategies, int checkpointType, Integer index, Integer level) {
		Checkpoint checkpoint = new Checkpoint(this, threadName, sourceClassName, name, checkpointType, level.intValue());
		boolean stub = false;
		if (originalReport != null) {
			Path path = checkpoint.getPath(true);
			Checkpoint originalCheckpoint = (Checkpoint)originalReport.getCheckpoint(path);
			if (originalCheckpoint == null) {
				if (matchingStubStrategies != null) {
					if (matchingStubStrategies.contains(originalReport.getStubStrategy())) {
						stub = true;
					}
				} else if (testTool.getDebugger() != null) {
					stub = testTool.stub(checkpoint, originalReport.getStubStrategy());
				}
			} else {
				checkpoint.setStub(originalCheckpoint.getStub());
				if (originalCheckpoint.getStub() == Checkpoint.STUB_FOLLOW_REPORT_STRATEGY) {
					if (matchingStubStrategies != null) {
						if (matchingStubStrategies.contains(originalReport.getStubStrategy())) {
							stub = true;
						}
					} else if (testTool.getDebugger() != null) {
						stub = testTool.stub(originalCheckpoint, originalReport.getStubStrategy());
					}
				} else if (originalCheckpoint.getStub() == Checkpoint.STUB_NO) {
					stub = false;
				} else if (originalCheckpoint.getStub() == Checkpoint.STUB_YES) {
					stub = true;
				}
			}
			if (stub) {
				checkpoint.setStubbed(true);
				if (originalCheckpoint == null) {
					checkpoint.setStubNotFound(path.toString());
				}
				message = getMessageEncoder().toObject(originalCheckpoint, message);
				message = checkpoint.setMessage(message);
			}
		}
		if (!stub) {
			try {
				message = TestTool.execute(stubableCode, stubableCodeThrowsException, message);
			} catch(Throwable t) {
				testTool.abortpoint(correlationId, sourceClassName, name, t.getMessage());
				throw t;
			}
			message = checkpoint.setMessage(message);
		}
		for (int i = threads.indexOf(threadName); i < threads.size(); i++) {
			String key = threads.get(i);
			Integer value = (Integer)threadCheckpointIndex.get(key);
			threadCheckpointIndex.put(key, new Integer(value.intValue() + 1));
		}
		// Add checkpoint to the list after stubable code has been executed. Otherwise when a report in progress is
		// opened is might give the impression that the stubable code is already executed
		checkpoints.add(index.intValue(), checkpoint);
		if (log.isDebugEnabled()) {
			log.debug("Added checkpoint " + getCheckpointLogDescription(name, checkpointType, level));
		}
		return message;
	}

	protected String truncateMessage(Checkpoint checkpoint, String message) {
		if (testTool.getMaxMessageLength() > -1 && message != null
				&& message.toString().length() > testTool.getMaxMessageLength()) {
			// For a message that is referenced by multiple checkpoints, have one truncated message that is
			// referenced by those checkpoints, to prevent creating multiple String objects representing the
			// same string and occupying unnecessary memory.
			checkpoint.setPreTruncatedMessageLength(message.length());
			if(truncatedMessageMap.containsKey(message)) {
				return truncatedMessageMap.get(message);
			} else {
				String truncatedMessage = message.substring(0, testTool.getMaxMessageLength());
				truncatedMessageMap.put(message, truncatedMessage);
				return truncatedMessage;
			}
		}
		return message;
	}

	public Checkpoint getOriginalEndpointOrAbortpointForCurrentLevel() {
		Checkpoint result = null;
		if (originalReport != null) {
			Checkpoint lastCheckpoint = (Checkpoint)checkpoints.get(checkpoints.size() - 1);
			Checkpoint checkpoint = originalReport.getCheckpoint(lastCheckpoint.getPath());
			if (checkpoint != null) {
				int i = originalReport.checkpoints.indexOf(checkpoint) + 1;
				while (checkpoint.getType() != Checkpoint.TYPE_ENDPOINT
						&& checkpoint.getType() != Checkpoint.TYPE_ABORTPOINT
						&& i < originalReport.checkpoints.size()) {
					checkpoint = (Checkpoint)originalReport.checkpoints.get(i);
					i++;
				}
				if (checkpoint.getType() == Checkpoint.TYPE_ENDPOINT
						|| checkpoint.getType() == Checkpoint.TYPE_ABORTPOINT) {
					result = checkpoint;
				}
			}
		}
		return result;
	}

	protected void close() {
		while (threads.size() > 0) {
			close(threads.get(0));
		}
	}

	protected void close(String threadName) {
		if (threads.remove(threadName)) {
			threadCheckpointIndex.remove(threadName);
			threadFirstLevel.remove(threadName);
			threadLevel.remove(threadName);
			threadParent.remove(threadName);
			threadsActiveCount--; 
		}
	}

	protected boolean threadsFinished() {
		return threadsActiveCount < 1;
	}

	protected boolean streamingMessageListenersFinished() {
		return streamingMessageListeners.size() < 1;
	}

	protected void addStreamingMessageListener(Object streamingMessage, Checkpoint checkpoint) {
		synchronized(streamingMessageListeners) {
			Set<Checkpoint> checkpoints = streamingMessageListeners.get(streamingMessage);
			if (checkpoints == null) {
				checkpoints = new HashSet<Checkpoint>();
			}
			checkpoints.add(checkpoint);
			streamingMessageListeners.put(streamingMessage, checkpoints);
		}
	}

	protected void removeStreamingMessageListener(Object streamingMessage, Checkpoint checkpoint) {
		synchronized(streamingMessageListeners) {
			Set<Checkpoint> checkpoints = streamingMessageListeners.remove(streamingMessage);
			if (checkpoints != null) {
				checkpoints.remove(checkpoint);
				if (checkpoints.size() > 0) {
					streamingMessageListeners.put(streamingMessage, checkpoints);
				}
			}
		}
	}

	protected boolean isKnownStreamingMessage(Object streamingMessage) {
		synchronized(streamingMessageListeners) {
			return streamingMessageListeners.get(streamingMessage) != null;
		}
	}

	protected void closeStreamingMessage(String messageClassName, Object streamingMessage, String streamingType,
			String charset, Object message, int preTruncatedMessageLength, Throwable exception) {
		synchronized(streamingMessageListeners) {
			StreamingMessageResult streamingMessageResult = new StreamingMessageResult();
			streamingMessageResult.setMessageClassName(messageClassName);
			streamingMessageResult.setStreamingType(streamingType);
			streamingMessageResult.setCharset(charset);
			streamingMessageResult.setMessage(message);
			streamingMessageResult.setPreTruncatedMessageLength(preTruncatedMessageLength);
			streamingMessageResult.setException(exception);
			streamingMessageResults.put(streamingMessage, streamingMessageResult);
			closeStreamingMessageListeners();
		}
		getTestTool().closeReport(this);
	}

	/**
	 * Close all streaming message listeners for which the streaming message has been closed
	 */
	private void closeStreamingMessageListeners() {
		synchronized(streamingMessageListeners) {
			Set<Object> finishedStreamingMessage = new HashSet<Object>();
			for (Object streamingMessage : streamingMessageListeners.keySet()) {
				StreamingMessageResult streamingMessageResult = streamingMessageResults.remove(streamingMessage);
				if (streamingMessageResult != null) {
					finishedStreamingMessage.add(streamingMessage);
					for (Checkpoint checkpoint : streamingMessageListeners.get(streamingMessage)) {
						checkpoint.setStreaming(streamingMessageResult.getStreamingType());
						if (streamingMessageResult.getException() != null) {
							checkpoint.setMessage(streamingMessageResult.getException());
						} else {
							Object message = streamingMessageResult.getMessage();
							String charset = streamingMessageResult.getCharset();
							ToStringResult toStringResult = getMessageEncoder().toString(message, charset);
							checkpoint.setMessage(toStringResult.getString());
							checkpoint.setEncoding(toStringResult.getEncoding());
							checkpoint.setMessageClassName(streamingMessageResult.getMessageClassName());
							checkpoint.setPreTruncatedMessageLength(streamingMessageResult.getPreTruncatedMessageLength());
						}
					}
				}
			}
			for (Object streamingMessage: finishedStreamingMessage) {
				streamingMessageListeners.remove(streamingMessage);
			}
		}
	}

	public Checkpoint getCheckpoint(Path path) {
		Checkpoint result = null;
		Iterator<Checkpoint> iterator = checkpoints.iterator();
		while (result == null && iterator.hasNext()) {
			Checkpoint checkpoint = (Checkpoint)iterator.next();
			if (path.equals(checkpoint.getPath())) {
				result = checkpoint;
			}
		}
		return result;
	}

	public void setCheckpoints(List<Checkpoint> checkpoints) {
		this.checkpoints = checkpoints;
	}

	public List<Checkpoint> getCheckpoints() {
		return checkpoints;
	}
	
	public Checkpoint getInputCheckpoint() {
		return checkpoints.get(0);
	}

	public int getNumberOfCheckpoints() {
		return checkpoints.size();
	}

	public long getEstimatedMemoryUsage() {
		long estimatedMemoryUsage = 0L;
		for (Checkpoint checkpoint : checkpoints) {
			estimatedMemoryUsage += checkpoint.getEstimatedMemoryUsage();
		}
		return estimatedMemoryUsage;
	}

	@Transient
	@JsonIgnore
	public void setDifferenceChecked(boolean differenceChecked) {
		this.differenceChecked = differenceChecked;
	}

	@Transient
	@JsonIgnore
	public boolean isDifferenceChecked() {
		return differenceChecked;
	}

	@Transient
	@JsonIgnore
	public void setDifferenceFound(boolean differenceFound) {
		this.differenceFound = differenceFound;
	}

	@Transient
	@JsonIgnore
	public boolean isDifferenceFound() {
		return differenceFound;
	}

	@Transient
	@JsonIgnore
	public MessageTransformer getMessageTransformer() {
		return testTool.getMessageTransformer();
	}

	@Transient
	@JsonIgnore
	public MessageEncoder getMessageEncoder() {
		return testTool.getMessageEncoder();
	}

	@Transient
	@JsonIgnore
	public MessageCapturer getMessageCapturer() {
		return testTool.getMessageCapturer();
	}

	public Object clone() throws CloneNotSupportedException {
		Report report = new Report();
		report.setTestTool(testTool);
		report.setStartTime(startTime);
		report.setEndTime(endTime);
		report.setCorrelationId(correlationId);
		report.setName(name);
		report.setDescription(description);
		report.setPath(path);
		report.setStubStrategy(stubStrategy);
		report.setTransformation(transformation);
		report.setVariableCsv(variableCsv);
		List<Checkpoint> checkpoints = new ArrayList<Checkpoint>();
		for (Checkpoint checkpoint : this.checkpoints) {
			checkpoint = (Checkpoint)checkpoint.clone();
			checkpoint.setReport(report);
			checkpoints.add(checkpoint);
		}
		report.setCheckpoints(checkpoints);
		return report;
	}

	public String toString() {
		return name;
	}

	public String toXml() {
		return toXml(null);
	}

	public String toXml(ReportRunner reportRunner) {
		if (xml == null) {
			StringBuffer stringBuffer = new StringBuffer();
			stringBuffer.append("<Report");
			stringBuffer.append(" Name=\"" + EscapeUtil.escapeXml(name) + "\"");
			stringBuffer.append(" Description=\"" + EscapeUtil.escapeXml(description) + "\"");
			stringBuffer.append(" Path=\"" + EscapeUtil.escapeXml(path) + "\"");
			stringBuffer.append(" CorrelationId=\"" + EscapeUtil.escapeXml(correlationId) + "\"");
			stringBuffer.append(" StartTime=\"" + startTime + "\"");
			stringBuffer.append(" EndTime=\"" + endTime + "\"");
			stringBuffer.append(" NumberOfCheckpoints=\"" + getNumberOfCheckpoints() + "\"");
			stringBuffer.append(" EstimatedMemoryUsage=\"" + getEstimatedMemoryUsage() + "\"");
			stringBuffer.append(">");
			for (Checkpoint checkpoint : checkpoints) {
				String message;
				if(reportRunner != null && checkpoint.containsVariables()) {
					message = checkpoint.getMessageWithResolvedVariables(reportRunner);
				} else {
					message = checkpoint.getMessage();
				}
				stringBuffer.append("<Checkpoint");
				stringBuffer.append(" Name=\"" + EscapeUtil.escapeXml(checkpoint.getName()) + "\"");
				stringBuffer.append(" Type=\"" + EscapeUtil.escapeXml(checkpoint.getTypeAsString()) + "\"");
				stringBuffer.append(" Level=\"" + checkpoint.getLevel() + "\"");
				if (checkpoint.getSourceClassName() != null) {
					stringBuffer.append(" SourceClassName=\"" + EscapeUtil.escapeXml(checkpoint.getSourceClassName()) + "\"");
				}
				if (checkpoint.getMessageClassName() != null) {
					stringBuffer.append(" MessageClassName=\"" + EscapeUtil.escapeXml(checkpoint.getMessageClassName()) + "\"");
				}
				if (checkpoint.getPreTruncatedMessageLength() != -1) {
					stringBuffer.append(" PreTruncatedMessageLength=\"" + checkpoint.getPreTruncatedMessageLength() + "\"");
				}
				if (checkpoint.getEncoding() != null) {
					stringBuffer.append(" Encoding=\"" + EscapeUtil.escapeXml(checkpoint.getEncoding()) + "\"");
				}
				if (checkpoint.getStreaming() != null) {
					stringBuffer.append(" Streaming=\"" + EscapeUtil.escapeXml(checkpoint.getStreaming()) + "\"");
				}
				if (checkpoint.getStub() != Checkpoint.STUB_FOLLOW_REPORT_STRATEGY) {
					stringBuffer.append(" Stub=\"" + checkpoint.getStub() + "\"");
				}
				if (checkpoint.isStubbed()) {
					stringBuffer.append(" Stubbed=\"" + checkpoint.isStubbed() + "\"");
				}
				if (checkpoint.getStubNotFound() != null) {
					stringBuffer.append(" StubNotFound=\"" + checkpoint.getStubNotFound() + "\"");
				}
				if (message == null) {
					stringBuffer.append(" Null=\"true\"/>");
				} else {
					if (XmlUtil.isXml(message)) {
						String textDecl = null;
						if (message.startsWith("<?")) {
							int i = message.indexOf("?>") + 2;
							textDecl = message.substring(0, i);
							stringBuffer.append(" TextDecl=\"");
							stringBuffer.append(EscapeUtil.escapeXml(textDecl));
							stringBuffer.append("\">");
							message = message.substring(i);
						} else {
							stringBuffer.append(">");
						}
						stringBuffer.append(message);
					} else {
						stringBuffer.append(">");
						stringBuffer.append(EscapeUtil.escapeXml(message));
					}
					stringBuffer.append("</Checkpoint>");
				}
			}
			stringBuffer.append("</Report>");
			xml = stringBuffer.toString();
			if (reportXmlTransformer != null || (transformation != null && transformation.trim().length() > 0)) {
				if (reportXmlTransformer == null) {
					reportXmlTransformer = new ReportXmlTransformer();
					reportXmlTransformer.setXslt(transformation);
				}
				xml = reportXmlTransformer.transform(xml);
			} else if (globalReportXmlTransformer != null) {
				xml = globalReportXmlTransformer.transform(xml);
			}
		}
		return xml;
	}

	public void flushCachedXml() {
		reportXmlTransformer = null;
		xml = null;
	}

	private String getCheckpointLogDescription(String name, int type, Integer level) {
		return "(name: " + name + ", type: " + Checkpoint.getTypeAsString(type) + ", level: " + level
				+ ", correlationId: " + correlationId + ")";
	}

	private String getOtherCheckpointsLogDescription() {
		return "(next checkpoints for this report will be ignored without any logging)";
	}

	public String getVariableCsv() {
		return variableCsv;
	}

	// XMLEncoder requires a void setter function.
	public void setVariableCsv(String variableCsv) {
		if (StringUtils.isEmpty(variableCsv)) {
			this.variableCsv = null;
			return;
		}
		String errorMessage = CsvUtil.validateCsv(variableCsv, ";", 2);
		if (errorMessage != null) {
			throw new IllegalArgumentException(errorMessage);
		}
		this.variableCsv = variableCsv;
	}

	public String setVariableCsvWithoutException(String variableCsv) {
		try {
			setVariableCsv(variableCsv);
			return null;
		} catch (IllegalArgumentException e) {
			return e.getMessage();
		}
	}

	public Map<String, String> getVariablesAsMap() {
		if(StringUtils.isEmpty(variableCsv)) {
			return null;
		}
		Map<String, String> variableMap = new LinkedHashMap<String, String>();
		Scanner scanner = new Scanner(variableCsv);
		List<String> lines = new ArrayList<String>();
		while(scanner.hasNextLine()) {
			String nextLine = scanner.nextLine();
			if(StringUtils.isNotEmpty(nextLine) && !nextLine.startsWith("#")) {
				lines.add(nextLine);
			}
		}
		scanner.close();
		
		List<String> params = Arrays.asList(lines.get(0).split(";"));
		for(String key : params) {
			String value = lines.get(1).split(";")[params.indexOf(key)];
			variableMap.put(key, value);
		}
		return variableMap;
	}
}

/**
 * A custom implementation of the map interface that compares keys based on their object reference, i.e. comparing with 'o1 == o2' rather than 'o1.equals(o2)'.
 * This greatly enhances the performance of maps with large objects as keys.
 * <p>
 * Note: Since this implementation was written with a specific use case in mind, most methods are not implemented and will throw an exception when called. 
 */
class RefCompareMap<K, V> implements Map<K, V> {
	
	private List<K> keys = new ArrayList<K>();
	private List<V> values = new ArrayList<V>();
	
	@Override
	public V get(Object key) {
		int i = 0;
		for(Object o : keys) {
			if(o == key) {
				return values.get(i);
			}
			i++;
		}
		return null;
	}
	
	@Override
	public V put(K key, V value) {
		keys.add(key);
		values.add(value);
		
		return value;
	}
	
	@Override
	public boolean containsKey(Object key) {
		for(Object o : keys) {
			if(o == key) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void clear() {
		throw new NotImplementedException();
	}

	@Override
	public boolean containsValue(Object value) {
		throw new NotImplementedException();
	}

	@Override
	public Set<Map.Entry<K,V>> entrySet() {
		throw new NotImplementedException();
	}

	@Override
	public boolean isEmpty() {
		throw new NotImplementedException();
	}

	@Override
	public Set<K> keySet() {
		throw new NotImplementedException();
	}

	@Override
	public void putAll(Map<? extends K,? extends V> m) {
		throw new NotImplementedException();
	}

	@Override
	public V remove(Object key) {
		throw new NotImplementedException();
	}

	@Override
	public int size() {
		throw new NotImplementedException();
	}

	@Override
	public Collection<V> values() {
		throw new NotImplementedException();
	}
}

@Data
class StreamingMessageResult {
	String messageClassName;
	String streamingType;
	String charset;
	Object message;
	int preTruncatedMessageLength;
	Throwable exception;
}
