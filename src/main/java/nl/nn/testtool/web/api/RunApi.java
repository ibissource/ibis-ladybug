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
package nl.nn.testtool.web.api;

import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.Report;
import nl.nn.testtool.run.ReportRunner;
import nl.nn.testtool.run.RunResult;
import nl.nn.testtool.storage.CrudStorage;
import nl.nn.testtool.storage.Storage;
import nl.nn.testtool.storage.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/runner")
public class RunApi extends ApiBase {
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	/**
	 * Rerun the given reports, and save the output the target storage.
	 *
	 * @param debugStorageParam Target storage to use as debug storage for re-runner.
	 * @param sources Map containing storage names and storage ids for reports that will re-run.
	 */
	@POST
	@Path("/run/{debugStorage}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response runReport(@PathParam("debugStorage") String debugStorageParam, Map<String, List<Integer>> sources) {
		Storage debugStorage = getBean(debugStorageParam);
		List<Report> reports = new ArrayList<>();
		List<String> exceptions = new ArrayList<>();

		// Reran reports will allow us to keep track of old reran reports.
		// This will later be used in replace and result.
		HashMap<Integer, Report> reranReports = getSessionAttr("reranReports", false);
		if (reranReports == null) {
			reranReports = new HashMap<>();
			setSessionAttr("reranReports", reranReports);
		}

		for (String storageParam : sources.keySet()) {
			Storage storage = getBean(storageParam);
			for (int storageId : sources.get(storageParam)) {
				try {
					Report report = storage.getReport(storageId);
					if (report != null)  report.setTestTool(getBean("testTool"));
					reports.add(report);
					reranReports.put(storageId, report);
				} catch (StorageException e) {
					String message = "Exception for report in [" + storageParam + "] with storage id [" + storageId + "]: " + e.getMessage();
					exceptions.add(message);
					logger.error(message, e);
					e.printStackTrace();
				}
			}
		}

		ReportRunner runner = getRunner(debugStorage);
		String exception = runner.run(reports, true, true);

		if (exceptions.size() > 0) {
			String message = "Following exceptions were thrown, causing the related reports not to run. " + String.join(". ", exceptions);
			return Response.serverError().entity(message).build();
		} else if (exception != null) {
			return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(exception).build();
		}
		return Response.ok().build();
	}

	/**
	 * Get the results from the re-runner for the given debug storage.
	 * @param debugStorageParam Name of the debug storage that runner uses.
	 */
	@GET
	@Path("/result/{debugStorage}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getResults(@PathParam("debugStorage") String debugStorageParam) {
		try {
			HashMap<Integer, Report> reranReports = getSessionAttr("reranReports", true);
			Storage debugStorage = getBean(debugStorageParam);
			ReportRunner runner = getRunner(debugStorage);

			Map<Integer, RunResult> results = runner.getResults();
			Map<String, Object> data = new HashMap<>(3);
			Map<Integer, Object> returningResults = new HashMap<>(results.size());

			for (Map.Entry<Integer, RunResult> entry : results.entrySet()) {
				RunResult runResult = entry.getValue();
				HashMap<String, Object> res = new HashMap<>(2);
				try {
					Report runResultReport = runner.getRunResultReport(runResult.correlationId);

					// Calculate number of stubbed checkpoints.
					int stubbed = 0;
					boolean first = true;
					for (Checkpoint checkpoint : runResultReport.getCheckpoints()) {
						if (first) {
							first = false;
						} else if (checkpoint.isStubbed()) {
							stubbed++;
						}
					}

					res.put("report", runResultReport);
					res.put("stubbed", stubbed);
					res.put("total", runResultReport.getCheckpoints().size() - 1);

					Report report = reranReports.get(entry.getKey());
					res.put("previous-time", report.getEndTime() - report.getStartTime());
					res.put("current-time", runResultReport.getEndTime() - runResultReport.getStartTime());
				} catch (StorageException exception) {
					res.put("exception", exception);
					exception.printStackTrace();
				}
				returningResults.put(entry.getKey(), res);
			}
			data.put("results", returningResults);
			data.put("progress", runner.getProgressValue());
			data.put("max-progress", runner.getMaximum());
			return Response.ok(data).build();
		} catch (Throwable t) {
			t.printStackTrace();
			throw t;
		}
	}

	@PUT
	@Path("/replace/{debugStorage}/{storageId}")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response runReport(@PathParam("debugStorage") String debugStorageStorageParam, @PathParam("storageId") int storageId) {
		try {
			// Get run result report.
			Storage debugStorage = getBean(debugStorageStorageParam);
			ReportRunner reportRunner = getRunner(debugStorage);
			Report runResultReport = reportRunner.getRunResultReport(reportRunner.getResults().get(storageId).correlationId);

			// Get original report.
			HashMap<Integer, Report> reranReports = getSessionAttr("reranReports", true);
			Report report = reranReports.get(storageId);

			// Apply transformations, etc
			logger.debug("Replacing report [" + report.getStorage().getName() + ":" + report.getStorageId() + "] " +
					"with [" + debugStorage.getName() + ":" + runResultReport.getStorageId() + "]");
			runResultReport.setTestTool(report.getTestTool());
			runResultReport.setName(report.getName());
			runResultReport.setDescription(report.getDescription());
			if (report.getCheckpoints().get(0).containsVariables()) {
				runResultReport.getCheckpoints().get(0).setMessage(report.getCheckpoints().get(0).getMessage());
			}
			runResultReport.setPath(report.getPath());
			runResultReport.setTransformation(report.getTransformation());
			runResultReport.setReportXmlTransformer(report.getReportXmlTransformer());
			runResultReport.setVariableCsvWithoutException(report.getVariableCsv());
			runResultReport.setStorageId(report.getStorageId());

			((CrudStorage) report.getStorage()).update(runResultReport);
			reportRunner.getResults().remove(storageId);
			reranReports.remove(storageId);
			return Response.ok(runResultReport).build();
		} catch (StorageException storageException) {
			storageException.printStackTrace();
			throw new ApiException("Exception while replacing report with storage id [" + storageId + "]", storageException);
		}
	}
	/**
	 * Resets all the report runners.
	 */
	@POST
	@Path("/reset")
	public Response resetAll() {
		Object sessionAttr = getSessionAttr("reportRunner", false);
		if (!(sessionAttr instanceof Map)) {
			setSessionAttr("reportRunner", new HashMap<Object, Object>());
			return Response.ok().build();
		}

		Map<Object, Object> map = (Map) sessionAttr;
		for (Object key : map.keySet()) {
			Object val = map.get(key);
			if (val instanceof ReportRunner) {
				ReportRunner runner = (ReportRunner) val;
				runner.reset();
			} else {
				map.remove(key);
			}
		}
		return Response.ok().build();
	}

	/**
	 * Resets the re-runner with the given debug storage.
	 * @param storageParam Name of the debug storage.
	 */
	@POST
	@Path("/reset/{debugStorage}")
	public Response resetRunner(@PathParam("debugStorage") String storageParam) {
		Storage storage = getBean(storageParam);
		ReportRunner runner = getRunner(storage);
		runner.reset();
		return Response.ok().build();
	}

	/**
	 * Return from the map, or generate report re-runner with the given debug storage.
	 * With the API, each session can create multiple re-runners for different debug storages.
	 *
	 * @param debugStorage Debug storage.
	 * @return ReportRunner that uses the given debug storage.
	 */
	private ReportRunner getRunner(Storage debugStorage) {
		Map<Object, Object> runners;

		Object sessionAttr = getSessionAttr("reportRunner", false);
		if (sessionAttr instanceof Map) {
			runners = (Map) sessionAttr;
		} else {
			runners = new HashMap<>();
			setSessionAttr("reportRunner", runners);
		}

		ReportRunner runner = (ReportRunner) runners.get(debugStorage);
		if (runner == null) {
			runner = new ReportRunner();
			runner.setTestTool(getBean("testTool"));
			runner.setDebugStorage(debugStorage);
			runner.setSecurityContext(this);
			runners.put(debugStorage, runner);
		}

		return runner;
	}
}
