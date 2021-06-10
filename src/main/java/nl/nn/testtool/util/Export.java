/*
   Copyright 2018 Nationale-Nederlanden, 2020-2021 WeAreFrank!

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
package nl.nn.testtool.util;

import nl.nn.testtool.Checkpoint;
import nl.nn.testtool.Report;
import nl.nn.testtool.TestTool;
import nl.nn.testtool.storage.Storage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.XMLEncoder;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandles;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Export {
	private static Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public static ExportResult export(Storage storage) {
		return export(storage, null);
	}

	public static ExportResult export(Storage storage, List storageIds, boolean exportReport,
											boolean exportReportXml) {
		return export(storage, storageIds, null, exportReport, exportReportXml, null, null);
	}

	public static ExportResult export(Storage storage,
			String suggestedFilenameWithoutExtension) {
		return export(storage, suggestedFilenameWithoutExtension, true, false);
	}

	public static ExportResult export(Storage storage,
			String suggestedFilenameWithoutExtension, boolean exportReport,
			boolean exportReportXml) {
		return export(storage, null, exportReport, exportReportXml, null,
				suggestedFilenameWithoutExtension);
	}

	public static ExportResult export(Report report) {
		return export(report, true, false);
	}

	public static ExportResult export(Report report, boolean exportReport,
			boolean exportReportXml) {
		return export(null, report, exportReport, exportReportXml, null,
				null);
	}

	public static ExportResult export(Report report, Checkpoint checkpoint) {
		return export(null, report, true, false, checkpoint, null);
	}

	public static ExportResult export(Checkpoint checkpoint) {
		return export(null, null, false, false, checkpoint, null);
	}

	private static ExportResult export(Storage storage, Report report,
									   boolean exportReport, boolean exportReportXml,
									   Checkpoint checkpoint, String suggestedFilenameWithoutExtension) {
		return export(storage, null, report, exportReport, exportReportXml, checkpoint, suggestedFilenameWithoutExtension);
	}
	private static ExportResult export(Storage storage, List storageIds, Report report,
			boolean exportReport, boolean exportReportXml,
			Checkpoint checkpoint, String suggestedFilenameWithoutExtension) {
		ExportResult exportResult = new ExportResult();
		FileOutputStream fileOutputStream = null;
		ZipOutputStream zipOutputStream = null;
		try {
			if (storage != null) {
				if (storageIds == null || storageIds.isEmpty())
					storageIds = storage.getStorageIds();
				if (suggestedFilenameWithoutExtension == null) {
					suggestedFilenameWithoutExtension = "Ladybug "+storage.getName();
					suggestedFilenameWithoutExtension += " "+new SimpleDateFormat("yyyyMMdd-HHmm").format(new Date());
					int size = storageIds.size();
					suggestedFilenameWithoutExtension += " (" + size;
					if (size == 1) {
						suggestedFilenameWithoutExtension += " report)";
					} else {
						suggestedFilenameWithoutExtension += " reports)";
					}
				}
				fileOutputStream = createTempFile(
						suggestedFilenameWithoutExtension, ".zip",
						exportResult);
				zipOutputStream = new ZipOutputStream(fileOutputStream);
				Set duplicateCheck = new HashSet();
				Iterator iterator = storageIds.iterator();
				while (iterator.hasNext()) {
					try {
						report = storage.getReport((Integer) iterator.next());
						if (report == null)
							continue;
					} catch (Exception e) {
						exportResult.setErrorMessage(e.toString());
						continue;
					}
					// TODO bij storage al afvangen dat er geen dubbele namen voor kunnen komen?
					int duplicateNumber = 1;
					String zipEntryName = getZipEntryName(report, duplicateNumber);
					while (duplicateCheck.contains(zipEntryName)) {
						duplicateNumber++;
						zipEntryName = getZipEntryName(report, duplicateNumber);
					}
					duplicateCheck.add(zipEntryName);
					if (exportReport) {
						writeReport(report, zipEntryName, zipOutputStream);
					}
					if (exportReportXml) {
						String reportXml = report.toXml();
						zipEntryName = zipEntryName.substring(0, zipEntryName.length() - 4) + getFileExtension(reportXml);
						writeReportXml(reportXml, zipEntryName, zipOutputStream);
					}
				}
			} else if (report != null && !exportReport && exportReportXml) {
				String reportXml = report.toXml();
				fileOutputStream = createTempFile(
						replaceSpecialCharsInFilename(report.getName()),
						getFileExtension(reportXml), exportResult);
				fileOutputStream.write(getMessageBytes(reportXml));
			} else if (report != null && (exportReportXml || checkpoint != null)) {
				fileOutputStream = createTempFile(
						replaceSpecialCharsInFilename(report.getName()), ".zip",
						exportResult);
				zipOutputStream = new ZipOutputStream(fileOutputStream);
				String zipEntryName;
				// Report
				zipEntryName = getZipEntryName(report, 1);
				writeReport(report, zipEntryName, zipOutputStream);
				// Report xml
				if (exportReportXml) {
					String reportXml = report.toXml();
					zipEntryName = zipEntryName.substring(0, zipEntryName.length() - 4) + getFileExtension(reportXml);
					writeReportXml(reportXml, zipEntryName, zipOutputStream);
				}
				// Checkpoint
				if (checkpoint != null) {
					zipEntryName = replaceSpecialCharsInFilename(checkpoint.getName()) + getFileExtension(checkpoint.getMessage());
					writeCheckpoint(checkpoint, zipEntryName, zipOutputStream);
				}
			} else if (report != null) {
				fileOutputStream = createTempFile(
						replaceSpecialCharsInFilename(report.getName()), ".ttr",
						exportResult);
				fileOutputStream.write(getReportBytes(report));
			} else if (checkpoint != null) {
				fileOutputStream = createTempFile(
						replaceSpecialCharsInFilename(checkpoint.getName()),
						getFileExtension(checkpoint.getMessage()), exportResult);
				fileOutputStream.write(getMessageBytes(checkpoint.getMessage()));
			}
		} catch(Throwable throwable) {
			log.error("Caught throwable creating file", throwable);
			exportResult.setErrorMessage("Unable to create export: " + throwable.getMessage());
			if (exportResult.getTempFile() != null) {
				exportResult.getTempFile().delete();
			}
		} finally {
			closeOutputStream(zipOutputStream, "closing zipOutputStream", log);
			closeOutputStream(fileOutputStream, "closing fileOutputStream", log);
		}
		return exportResult;
	}

	private static FileOutputStream createTempFile(
			String suggestedFilenameWithoutExtension, String extension,
			ExportResult exportResult) throws IOException {
		File file = File.createTempFile("testtool-", extension);
		file.deleteOnExit();
		log.debug("Temporary file name: " + file.getAbsolutePath());
		exportResult.setTempFile(file);
		exportResult.setSuggestedFilename(replaceSpecialCharsInFilename(suggestedFilenameWithoutExtension) + extension);
		return new FileOutputStream(file);

	}
	private static String getFileExtension(String message) {
		// TODO use a more intelligent method to determine xml type (parse the message with an xml parser or apply an xpath on the message like it is done in XpathMetadataFieldExtractor?)
		if (message != null && message.startsWith("<")) {
			return ".xml";
		} else {
			return ".txt";
		}
	}

	private static String getZipEntryName(Report report, int duplicateNumber) {
		String name = replaceSpecialCharsInFilename(report.getName());
		if (report.getPath() != null) {
			name = replaceSpecialCharsInPath(report.getPath()) + name;
		}
		if (duplicateNumber > 1) {
			name = name + " (" + duplicateNumber + ")";
		}
		name = name + ".ttr";
		// TODO ook \ aan het begin verwijderen? Niet toestaan in storage?
		if (name.startsWith("/")) {
			name = name.substring(1);
		}
		return name;
	}

	private static String replaceSpecialCharsInFilename(String filename) {
		// Char ':' not allowed in filenames on Windows. Prevent them
		// from being used on Unix. TODO check for other chars too?
		return replaceSpecialCharsInPath(filename).replaceAll("/", "_").replaceAll("\\\\", "_");
	}

	private static String replaceSpecialCharsInPath(String path) {
		// Char ':' not allowed in filenames on Windows. Prevent them
		// from being used on Unix.
		// TODO check for other chars too
		// TODO backslashes hier ook vervangen? en/of zorgen dat report paden niet met speciale chars aangemaakt kunnen worden?
		return path.replaceAll(":", "_");
	}

	private static void writeReport(Report report, String zipEntryName,
			ZipOutputStream zipOutputStream) throws IOException {
		writeZipEntry(zipEntryName, getReportBytes(report), zipOutputStream);
	}

	private static void writeReportXml(String reportXml, String zipEntryName,
			ZipOutputStream zipOutputStream) throws IOException {
		writeZipEntry(zipEntryName, getMessageBytes(reportXml), zipOutputStream);
	}

	private static void writeCheckpoint(Checkpoint checkpoint, String zipEntryName,
			ZipOutputStream zipOutputStream) throws IOException {
		writeZipEntry(zipEntryName, getMessageBytes(checkpoint.getMessage()), zipOutputStream);
	}

	private static void writeZipEntry(String zipEntryName, byte[] bytes,
			ZipOutputStream zipOutputStream) throws IOException {
		zipOutputStream.putNextEntry(new ZipEntry(zipEntryName));
		zipOutputStream.write(bytes);
		zipOutputStream.closeEntry();
	}

	public static byte[] getReportBytes(Report report) throws IOException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		GZIPOutputStream gzipOutputStream = null;
		// Use XMLEncoder as it is more compatible over different Java
		// versions than ObjectOutputStream although ObjectOutputStream
		// seems a lot faster with large messages.
		// Older versions of the Test Tool used to write more than one
		// report to a ttr file. As it turned out that downloading more than
		// about 300 reports with a total compressed size of about 20 MB
		// began to blow up the JVM (Java 1.6 with 1 GB of memory) the Test
		// Tool was redesigned to use a zipfile to download more than one
		// report. A little debugging showed that calling the close method
		// on the XMLEncoder consumed a lot of time. More evidence about bad
		// performance of XMLEncoder can be found on the internet.
		XMLEncoder xmlEncoder = null;
		try {
			gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream);
			xmlEncoder = new XMLEncoder(gzipOutputStream);
			xmlEncoder.writeObject(TestTool.getVersion());
			xmlEncoder.writeObject(report);
		} finally {
			closeXMLEncoder(xmlEncoder);
			closeOutputStream(gzipOutputStream, "closing gzipOutputStream", log);
		}
		return byteArrayOutputStream.toByteArray();
	}

	private static byte[] getMessageBytes(String message) throws UnsupportedEncodingException {
		// TODO use a different encoding when specified in the xml message?
		if (message == null) {
			return new byte[0];
		} else {
			return message.getBytes("UTF-8");
		}
	}

	public static void closeXMLEncoder(XMLEncoder xmlEncoder) {
		if (xmlEncoder != null) {
			xmlEncoder.close();
		}
	}

	public static void closeOutputStream(OutputStream outputStream, String action, Logger log) {
		if (outputStream != null) {
			try {
				outputStream.close();
			} catch(IOException e) {
				log.warn("IOException " + action, e);
			}
		}
	}

}
