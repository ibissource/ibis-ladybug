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

import nl.nn.testtool.MetadataExtractor;
import nl.nn.testtool.Report;
import nl.nn.testtool.echo2.test.TestComponent;
import nl.nn.testtool.echo2.util.Upload;
import nl.nn.testtool.storage.CrudStorage;
import nl.nn.testtool.storage.Storage;
import nl.nn.testtool.storage.StorageException;
import nl.nn.testtool.transform.ReportXmlTransformer;
import nl.nn.testtool.util.Export;
import nl.nn.testtool.util.ExportResult;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

@Path("/")
public class ReportApi extends ApiBase {
	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	/**
	 * Returns the report details for the given storage and id.
	 *
	 * @param storageParam Name of the storage.
	 * @param storageId Storage id of the report.
	 * @param xml True if Xml of the report needs to be returned.
	 * @param globalTransformer True if reportXmlTransformer should be set for the report.
	 * @return A response containing serialized Report object.
	 * @throws ApiException If exception is thrown while reading report.
	 */
	@GET
	@Path("/report/{storage}/{storageId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getReport(@PathParam("storage") String storageParam,
							  @PathParam("storageId") int storageId,
							  @QueryParam("xml") @DefaultValue("false") boolean xml,
							  @QueryParam("globalTransformer") @DefaultValue("false") boolean globalTransformer) throws ApiException {
		try {
			Storage storage = getBean(storageParam);
			Report report = getReport(storage, storageId);
			if (report == null)
				return Response.status(Response.Status.NOT_FOUND).build();
			if (xml) {
				if (globalTransformer) {
					ReportXmlTransformer reportXmlTransformer = getBean("reportXmlTransformer");
					if (reportXmlTransformer != null)
						report.setGlobalReportXmlTransformer(reportXmlTransformer);
				}
				HashMap<String, String> map = new HashMap<>(1);
				map.put("xml", report.toXml());
				return Response.ok(map).build();
			} else {
				return Response.ok().entity(report).build();
			}
		} catch (StorageException e) {
			throw new ApiException("Exception while getting report [" + storageId + "] from storage [" + storageParam + "]", e);
		}
	}

	/**
	 * Deletes the report.
	 *
	 * @param storageParam Name of the storage.
	 * @param storageId Storage id of the report to be deleted.
	 * @return "Ok" if deleted properly, "Not implemented" if storage does not support deletion, "Not found" if report does not exist.
	 */
	@DELETE
	@Path("/report/{storage}/{storageId}")
	public Response deleteReport(@PathParam("storage") String storageParam, @PathParam("storageId") int storageId) {
		Storage storage = getBean(storageParam);
		if (!(storage instanceof CrudStorage)) {
			String msg = "Given storage [" + storageParam + "] does not implement delete function.";
			logger.warn(msg);
			return Response.status(Response.Status.NOT_IMPLEMENTED).entity(msg).build();
		}
		try {
			Report report = getReport(storage, storageId);
			if (report == null)
				return Response.status(Response.Status.NOT_FOUND).build();
			((CrudStorage) storage).delete(report);
		} catch (StorageException e) {
			throw new ApiException("Exception while deleting a report.", e);
		}
		return Response.ok().build();
	}

	@GET
	@Path("/report/latest/{storage}/{numberOfReports}")
	public Response getLatestReports(@PathParam("storage") String storageParam, @PathParam("numberOfReports") int number) {
		try {
			Storage storage = getBean(storageParam);
			List<List<Object>> metadata = storage.getMetadata(-1, Arrays.asList("storageId", "endTime"),
					Arrays.asList(null, null), MetadataExtractor.VALUE_TYPE_OBJECT);
			number = Math.min(metadata.size(), number);
			if (number < 1)
				return Response.noContent().build();
			metadata.sort(new Comparator<List<Object>>() {
				@Override
				public int compare(List<Object> o1, List<Object> o2) {
					return (int) ((Long) o1.get(1) - (Long) o2.get(1));
				}
			});
			ArrayList<Report> reports = new ArrayList<>(number);
			for (int i = 1; i <= number; i++) {
				reports.add(getReport(storage, (Integer) metadata.get(metadata.size() - i).get(0)));
			}
			return Response.ok(reports).build();
		} catch (StorageException e) {
			throw new ApiException("Exception while getting the latest " + number + " reports.", e);
		}
	}

	/**
	 * Update the report with the given values..
	 *
	 * @param storageParam Name of the storage.
	 * @param storageId Storage id of the report.
	 * @param map Map containing ["name" or "path" or "variables" or "description" or "transformation"]
	 */
	@POST
	@Path("/report/{storage}/{storageId}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateReport(@PathParam("storage") String storageParam, @PathParam("storageId") int storageId, Map<String, String> map) {
		String[] fields = new String[]{"name", "path", "variables", "description", "transformation"};
		if (map.isEmpty() || !mapContainsOnly(map, null, fields))
			return Response.status(Response.Status.BAD_REQUEST).build();

		try {
			Storage storage = getBean(storageParam);
			Report report = getReport(storage, storageId);
			if (report == null)
				return Response.status(Response.Status.NOT_FOUND).build();

			if (StringUtils.isNotEmpty(map.get("name")))
				report.setName(map.get("name"));

			if (StringUtils.isNotEmpty(map.get("path")))
				report.setPath(TestComponent.normalizePath(map.get("path")));

			if (StringUtils.isNotEmpty(map.get("description")))
				report.setDescription(map.get("description"));

			if (StringUtils.isNotEmpty(map.get("transformation")))
				report.setTransformation(map.get("transformation"));

			String variables = map.get("variables");
			if (variables != null && !variables.equals(report.getVariableCsv())) {
				report.setVariableCsv(variables);
			}

			HashMap<String, Serializable> result = new HashMap<>(3);
			report.flushCachedXml();
			boolean storageUpdated = false;
			if (storage instanceof CrudStorage) {
				CrudStorage crudStorage = (CrudStorage) storage;
				crudStorage.update(report);
				storageUpdated = true;
			} else {
				ReportXmlTransformer reportXmlTransformer = getBean("reportXmlTransformer");
				if (reportXmlTransformer != null)
					report.setGlobalReportXmlTransformer(reportXmlTransformer);
				result.put("xml", report.toXml());
			}

			result.put("storageUpdated", storageUpdated);
			result.put("report", report);
			return Response.ok(result).build();
		} catch (StorageException e) {
			throw new ApiException("Exception while setting transformation for a report.", e);
		}
	}

	/**
	 * Return transformation of a report.
	 *
	 * @param storageParam Name of the storage.
	 * @param storageId Storage id of the report.
	 * @return Response containing a map containing transformation.
	 */
	@GET
	@Path("/report/transformation/{storage}/{storageId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getReportTransformation(@PathParam("storage") String storageParam, @PathParam("storageId") int storageId) {
		try {
			Storage storage = getBean(storageParam);
			String transformation = getReport(storage, storageId).getTransformation();
			Map<String, String> map = new HashMap<>(1);
			map.put("transformation", transformation);
			return Response.ok(map).build();
		} catch (StorageException e) {
			throw new ApiException("Exception while setting transformation for a report.", e);
		}
	}

	/**
	 * Copy the reports from the given storages and ids to the given target storage.
	 *
	 * @param storageParam Name of the target storage.
	 * @param sources Map [String, Integer] where keys are storage names and integers are storage ids for the reports to be copied.
	 */
	@PUT
	@Path("/report/store/{storage}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response copyReport(@PathParam("storage") String storageParam, Map<String, List<Integer>> sources) {
		Storage target = getBean(storageParam);
		Map<String, String> exceptions = new HashMap<>();
		for (String src : sources.keySet()) {
			try {
				Storage srcStorage = getBean(src);

				for (int storageId : sources.get(src)) {
					try {
						((CrudStorage) target).store(getReport(srcStorage, storageId));
					} catch (StorageException storageException) {
						exceptions.put(src + "_" + storageId, storageException.getMessage());
						logger.error("Could not copy the report. #Exceptions for request: " + exceptions, storageException);
					}
				}
			} catch (ApiException e) {
				exceptions.put(src, e.getMessage());
			}
		}
		// TODO: Find a better error response code.
		if (exceptions.size() > 0)
			return Response.status(Response.Status.BAD_REQUEST).entity(exceptions).build();
		return Response.ok().build();
	}

	/**
	 * Upload the given report to storage.
	 *
	 * @param storageParam Name of the target storage.
	 * @param attachment Attachment containing report.
	 */
	@POST
	@Path("/report/upload/{storage}")
	@Produces(MediaType.TEXT_HTML)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadFile(@PathParam("storage") String storageParam, @Multipart("file") Attachment attachment) {
		Storage storage = getBean(storageParam);
		if (!(storage instanceof CrudStorage)) {
			throw new ApiException("Given storage is not a Crud Storage. Therefore no reports can be added externally.");
		}
		CrudStorage crudStorage = (CrudStorage) storage;

		String filename = attachment.getContentDisposition().getParameter("filename");
		InputStream in = attachment.getObject(InputStream.class);
		String errorMessage = Upload.upload(filename, in, crudStorage, logger);
		if (StringUtils.isEmpty(errorMessage)) {
			return Response.ok().build();
		}
		return Response.status(Response.Status.BAD_REQUEST).entity(errorMessage).build();
	}

	/**
	 * Uploads the given report to in-memory storage it, parses it and then returns the report in json format.
	 *
	 * @param attachment Attachment containing report.
	 * @return List of serialized report objects.
	 */
	@POST
	@Path("/report/upload")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response getFileReport(@Multipart("file") Attachment attachment) {
		CrudStorage storage = new nl.nn.testtool.storage.memory.Storage();
		String filename = attachment.getContentDisposition().getParameter("filename");
		InputStream in = attachment.getObject(InputStream.class);
		String errorMessage = Upload.upload(filename, in, storage, logger);
		if (StringUtils.isNotEmpty(errorMessage))
			return Response.status(Response.Status.BAD_REQUEST).entity(errorMessage).build();
		try {
			Iterator storageIdsIterator = storage.getStorageIds().iterator();
			ArrayList<Report> reports = new ArrayList<>(storage.getStorageIds().size());
			while (storageIdsIterator.hasNext()) {
				Report report = getReport(storage, ((Integer) storageIdsIterator.next()));
				reports.add(report);
			}
			return Response.ok(reports).build();
		} catch (StorageException e) {
			throw new ApiException("Exception while getting the parsed reports from in-memory storage.", e);
		}
	}

	/**
	 * Download the given reports.
	 *
	 * @param storageParam Name of the storage.
	 * @param exportReportParam "true" or "1" to save the serialized version of report.
	 * @param exportReportXmlParam "true" or "1" to save Xml version of report.
	 * @param storageIds List of storage ids to download.
	 */
	@GET
	@Path("/report/download/{storage}/{exportReport}/{exportReportXml}")
	@Produces("application/octet-stream")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response downloadFile(@PathParam("storage") String storageParam, @PathParam("exportReport") String exportReportParam,
								 @PathParam("exportReportXml") String exportReportXmlParam, @QueryParam("id") List<Integer> storageIds) {
		Storage storage = getBean(storageParam);
		if (storageIds == null || storageIds.isEmpty())
			return Response.status(Response.Status.BAD_REQUEST).build();
		boolean exportReport = exportReportParam.equalsIgnoreCase("true") || exportReportParam.equals("1");
		boolean exportReportXml = exportReportXmlParam.equalsIgnoreCase("true") || exportReportXmlParam.equals("1");
		try {
			ExportResult export;
			if (storageIds.size() == 1) {
				Report report = getReport(storage, storageIds.get(0));
				export = Export.export(report, exportReport, exportReportXml);
			} else {
				export = Export.export(storage, storageIds, exportReport, exportReportXml);
			}
			Response.ResponseBuilder response = Response.ok(export.getTempFile(), MediaType.APPLICATION_OCTET_STREAM);
			response.header("Content-Disposition", "attachment; filename=" + export.getSuggestedFilename());
			return response.build();
		} catch (StorageException e) {
			throw new ApiException("Exception while requesting report from the storage.", e);
		}
	}

	/**
	 * Copy or move report files in the same storage to different paths.
	 *
	 * @param storageParam Name of the storage.
	 * @param storageId Storage id of the report to be moved.
	 * @param map Map containing "path" and "action". Actions could be "copy" or "move".
	 */
	@PUT
	@Path("/report/move/{storage}/{storageId}")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response updatePath(@PathParam("storage") String storageParam, @PathParam("storageId") int storageId, Map<String, String> map) {
		CrudStorage storage = getBean(storageParam);
		String path = map.get("path");
		String action = map.get("action");
		if (StringUtils.isEmpty(action) || StringUtils.isEmpty(path))
			return Response.status(Response.Status.BAD_REQUEST).entity("[action] and [path] is required as request body.").build();

		try {
			Report original = getReport(storage, storageId);
			if ("copy".equalsIgnoreCase(action)) {
				Report clone = (Report) original.clone();
				clone.setPath(path);
				storage.store(clone);
			} else if ("move".equalsIgnoreCase(action)) {
				original.setPath(path);
				storage.update(original);
			} else {
				return Response.status(Response.Status.BAD_REQUEST)
						.entity("Action parameter can only be either [copy] or [move]").build();
			}

			return Response.ok().build();
		} catch (StorageException e) {
			throw new ApiException("Storage exception with storage id [" + storageId + "] in storage [" + storageParam + "]", e);
		} catch (CloneNotSupportedException e) {
			throw new ApiException("Cloning exception for report with storage id [" + storageId + "] in storage [" + storageParam + "]", e);
		}
	}

	/**
	 * Cloning the reports with the given parameters.
	 *
	 * @param storageParam Name of the storage.
	 * @param storageId Storage id of the report to be cloned.
	 * @param map Map containing csv for cloning.
	 */
	@POST
	@Path("/report/move/{storage}/{storageId}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response cloneReport(@QueryParam("storage") String storageParam, @QueryParam("storageId") int storageId, Map<String, String> map) {
		CrudStorage storage = getBean(storageParam);
		Report original = null;
		try {
			original = getReport(storage, storageId);
			String previousMessage = original.getInputCheckpoint().getMessage();
			boolean force = "true".equalsIgnoreCase(map.getOrDefault("force", "false"))
					|| "1".equalsIgnoreCase(map.getOrDefault("force", "false"));
			original.getInputCheckpoint().setMessage(map.get("message"));
			if (!original.getInputCheckpoint().containsVariables() && !force) {
				original.getInputCheckpoint().setMessage(previousMessage);
				return Response.status(Response.Status.BAD_REQUEST).entity("No variables found in input message; press again to confirm").build();
			}
		} catch (StorageException e) {
			logger.error("Exception while cloning the report", e);
			return Response.status(Response.Status.BAD_REQUEST).entity("Report could not be found.").build();
		}

		Scanner scanner = new Scanner(map.get("csv"));
		String firstLine = null;
		boolean originalSet = false;
		ArrayList<String> exceptions = new ArrayList<>();
		while (scanner.hasNextLine()) {
			String nextLine = scanner.nextLine();
			if (StringUtils.isNotEmpty(nextLine) && !nextLine.startsWith("#"))
				continue;
			if (firstLine == null) {
				firstLine = nextLine;
			} else {
				try {
					if (originalSet) {
						Report clone = (Report) original.clone();
						clone.setVariableCsvWithoutException(firstLine + "\n" + nextLine);
						storage.store(clone);
					} else {
						originalSet = true;
						original.setVariableCsvWithoutException(firstLine + "\n" + nextLine);
						storage.update(original);
					}
				} catch (CloneNotSupportedException | StorageException e) {
					exceptions.add(e.getMessage() + " Caused by: " + e.getCause() + " For line: [" + nextLine + "]");
				}
			}
		}
		scanner.close();
		return Response.ok().entity(exceptions).build();
	}

	/**
	 * Returns the report and sets the testtool with the bean named "testTool".
	 * @param storage Storage to get the report from.
	 * @param storageId Storage id of the report.
	 * @return Report.
	 * @throws StorageException ...
	 */
	public Report getReport(Storage storage, Integer storageId) throws StorageException {
		Report report = storage.getReport(storageId);
		if (report != null)  report.setTestTool(getBean("testTool"));
		return report;
	}
}
