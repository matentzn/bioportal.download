package owl.cs.bioportal.download;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import owl.cs.man.ac.uk.experiment.csv.CSVUtilities;


public class BioPortalFileList {

	private static String API_KEY = "0798b515-1ad4-4585-a9b4-2b4ad0f44f5e";
	private static String BASEURL = "http://data.bioontology.org";

	// static final String REST_URL = "http://data.bioontology.org";
	static final ObjectMapper mapper = new ObjectMapper();

	/**
	 * @param args
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws IOException, SAXException,
			ParserConfigurationException, InterruptedException {
		List<Map<String, String>> ids = extractBioPortalMetaData();
		CSVUtilities.writeCSVData(
				new File("D:\\000\\bp2017\\meta" + System.currentTimeMillis()
						+ ".csv"), ids, false);
	}

	public static List<Map<String, String>> extractBioPortalMetaData()
			throws IOException, SAXException, ParserConfigurationException {
		List<Map<String, String>> ids = new ArrayList<Map<String,String>>();
		try {
		JsonNode ontologies = getOntologyList();
		ids.addAll(getDownloadListIds(ontologies));
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return ids;
	}

	public static JsonNode getOntologyList() {
		String resourcesString = get(BASEURL + "/");
		JsonNode resources = jsonToNode(resourcesString);
		
		String link = resources.get("links").findValue("ontologies").asText();
		
		JsonNode ontologies = jsonToNode(get(link));
		return ontologies;
	}

	public static String createName(String displayname, String abbreviation,
			String subid, String extension) {
		String filename = displayname.trim().replaceAll(" ", "-")
				.replaceAll("[^a-zA-Z0-9_-]", "");
		filename = filename.toLowerCase();
		filename = abbreviation + "." + filename + "." + subid;
		if (!extension.isEmpty()) {
			filename = filename + "." + extension;
		}
		filename = filename.toLowerCase();
		return filename;
	}

	private static String get(String urlToGet) {
		URL url;
		HttpURLConnection conn;
		BufferedReader rd;
		String line;
		String result = "";
		try {
			url = new URL(urlToGet);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Authorization", "apikey token=" + API_KEY);
			conn.setRequestProperty("Accept", "application/json");
			rd = new BufferedReader(
					new InputStreamReader(conn.getInputStream()));
			while ((line = rd.readLine()) != null) {
				result += line;
			}
			rd.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	private static JsonNode jsonToNode(String json) {
		JsonNode root = null;
		try {
			root = mapper.readTree(json);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return root;
	}

	private static List<Map<String, String>> getDownloadListIds(
			JsonNode ontologies) throws IOException, SAXException,
			ParserConfigurationException {

		List<Map<String, String>> data = new ArrayList<Map<String, String>>();

		int ct = 0;
		int all = 0;

		for (JsonNode ontology : ontologies) {
			all++;

			String id = extractDataFromNode(ontology, "@id");

			if (id.isEmpty()) {
				continue;
			}
			String label = extractDataFromNode(ontology, "name");
			System.out.println("Ontology: " + label);
			String abbreviation = extractDataFromNode(ontology, "acronym");
			

			JsonNode submissions = jsonToNode(get(ontology.get("links")
					.findValue("submissions").asText()));
			
			if(submissions==null) {
				System.out.println("Submissions are null!");
				continue;
			}

			boolean validFormat = true;
			for (JsonNode submission : submissions) {
				if (submission != null) {
					Map<String, String> rec = new HashMap<String, String>();
					//System.out.println(submission);
					String dateCreate = extractDataFromNode(submission,
							"creationDate");
					String dateRelease = extractDataFromNode(submission,
							"released");
					String format = extractDataFromNode(submission,
							"hasOntologyLanguage");
					String version = extractDataFromNode(submission, "version");
					String submissionid = extractDataFromNode(submission,
							"submissionId");
					String download = id+"/submissions/"+submissionid+"/download";

					validFormat = ontologyHasValidFormat(format);
					if (validFormat) {
						String filename = createName(label, abbreviation, submissionid, "");
						rec.put(BioPortalMetrics.OWLXMLFILENAME.getName(), filename
								+ ".owl.xml");
						rec.put(BioPortalMetrics.ORIGFILENAME.getName(), filename + ".orig");
						rec.put(BioPortalMetrics.DISPLAYNAME.getName(), label);
						rec.put(BioPortalMetrics.ID.getName(), id);
						rec.put(BioPortalMetrics.DATECREATE.getName(),
								dateCreate);
						rec.put(BioPortalMetrics.DATERELEASE.getName(),
								dateRelease);
						rec.put(BioPortalMetrics.ABBREVIATION.getName(),
								abbreviation);
						rec.put(BioPortalMetrics.VERSION.getName(), version);
						rec.put(BioPortalMetrics.FORMAT.getName(), format);
						rec.put(BioPortalMetrics.DOWNLOAD.getName(), download);
						rec.put(BioPortalMetrics.SUBMISSIONID.getName(),
								submissionid);
						data.add(rec);
						ct++;
					}
				} else {
					System.out.println("No latest submission?");
				}
			}

		}
		System.out.println("ALL: " + all);
		System.out.println("CT: " + ct);
		return data;
	}

	private static String extractDataFromNode(JsonNode node, String data) {
		JsonNode jn = node.get(data);
		if (jn != null) {
			return jn.asText();
		} else {
			return "";
		}
	}

	private static boolean ontologyHasValidFormat(String format) {
		boolean validFormat = false;
		System.out.println("Format: " + format);
		if (format.contains("OWL")) {
			validFormat = true;
		} else if (format.contains("OBO")) {
			validFormat = true;
		} else if (format.contains("PROTEGE")) {
			validFormat = true;
		}
		return validFormat;
	}
}
