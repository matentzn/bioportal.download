package owl.cs.bioportal.download;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import owl.cs.man.ac.uk.experiment.analysis.AnalysisUtils;
import owl.cs.man.ac.uk.experiment.csv.CSVUtilities;
import owl.cs.man.ac.uk.experiment.file.FileUtilities;
import owl.cs.man.ac.uk.experiment.util.ExperimentUtilities;

public class BioPortalServices {

	public static void createLatestSubmissionSnapshot(File bioportaldir) throws IOException {

		File metadata = new File(bioportaldir, "metadata");
		File owlxmlmetadata = new File(metadata, "bp_owlxml_metadata.csv");
		File origmetadata = new File(metadata, "bp_orig_metadata.csv");

		List<Map<String, String>> originalmetadata = CSVUtilities.getAllRecords(origmetadata);
		Map<String, Map<String, String>> owlxml_metadata = CSVUtilities.getRecordsIndexedBy(owlxmlmetadata,
				BioPortalMetrics.OWLXMLFILENAME.getName());
		Map<String, Map<String, Map<String, String>>> all = getRecordsIndexedByOntologyId(originalmetadata);
		List<Map<String, String>> snapshotRecords = sampleSnapshot(all);

		exportSnapshot(bioportaldir, owlxml_metadata, snapshotRecords);
		// FileUtilities.zip(snapshotfiles_owlxml, zipowlxml);

	}

	static List<Map<String, String>> sampleSnapshot(Map<String, Map<String, Map<String, String>>> all) {
		List<Map<String, String>> snapshotRecords = new ArrayList<Map<String, String>>();

		for (String id : all.keySet()) {
			Set<String> keyset = all.get(id).keySet();
			Set<Integer> keys = new HashSet<Integer>();
			for (String k : keyset) {
				//System.out.println(k); if(true) continue;
				try {
					Integer i = Integer.valueOf(k);
					keys.add(i);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			int max = ExperimentUtilities.getMax(keys);
			String maxstr = max + "";
			Map<String, String> rec = all.get(id).get(maxstr);
			snapshotRecords.add(rec);
		}
		return snapshotRecords;
	}

	private static void exportSnapshot(File bioportaldir, Map<String, Map<String, String>> owlxml_metadata,
			List<Map<String, String>> snapshotRecords) {
		File snapshots = new File(bioportaldir, "snapshots");
		DateFormat df = new SimpleDateFormat("yyyy.MM.dd");
		String formattedDate = df.format(new Date());
		File snapshot = new File(snapshots, formattedDate);

		if (!snapshot.exists()) {
			snapshots.mkdirs();
		}

		File orig = new File(bioportaldir, "original");
		File owlxml = new File(bioportaldir, "owlxml");
		File csv_orig = new File(snapshot, "bioportalmetadata_original_" + formattedDate + ".csv");
		File csv_owlxml = new File(snapshot, "bioportal_owlxmlmergedimports_" + formattedDate + ".csv");
		File zipowlxml = new File(snapshot, "bioportal_owlxmlmergedimports_" + formattedDate + ".zip");
		File ziporig = new File(snapshot, "bioportal_original_" + formattedDate + ".zip");
		List<File> snapshotfiles_orig = new ArrayList<File>();
		List<File> snapshotfiles_owlxml = new ArrayList<File>();
		List<Map<String, String>> metadata_orig = new ArrayList<Map<String, String>>();
		List<Map<String, String>> metadata_owlxml = new ArrayList<Map<String, String>>();
		for (Map<String, String> rec : snapshotRecords) {
			String filename_owlxml = rec.get(BioPortalMetrics.OWLXMLFILENAME.getName());
			System.out.println(filename_owlxml);
			String filename_orig = rec.get(BioPortalMetrics.ORIGFILENAME.getName());
			snapshotfiles_orig.add(new File(orig, filename_orig));
			snapshotfiles_owlxml.add(new File(owlxml, filename_owlxml));
			metadata_orig.add(cleanRecord(rec));
			if (owlxml_metadata.containsKey(filename_owlxml)) {
				metadata_owlxml.add(cleanRecord(owlxml_metadata.get(filename_owlxml)));
			}
		}
		CSVUtilities.writeCSVData(csv_owlxml, metadata_owlxml, false);
		CSVUtilities.writeCSVData(csv_orig, metadata_orig, false);
		FileUtilities.zip(snapshotfiles_orig, ziporig);
	}

	static Map<String, Map<String, Map<String, String>>> getRecordsIndexedByOntologyId(
			List<Map<String, String>> originalmetadata) {
		Map<String, Map<String, Map<String, String>>> all = new HashMap<String, Map<String, Map<String, String>>>();

		for (Map<String, String> rec : originalmetadata) {

			if (!rec.containsKey(BioPortalMetrics.SUBMISSIONID.getName())) {
				System.out.println(rec + " does not contain submission!");
				continue;
			}
			// System.out.println(rec);
			String id = rec.get(BioPortalMetrics.ID.getName());
			String submission = rec.get(BioPortalMetrics.SUBMISSIONID.getName());
			
			if (!all.containsKey(id)) {
				all.put(id, new HashMap<String, Map<String, String>>());
			}
			all.get(id).put(submission, rec);
		}
		return all;
	}

	private static Map<String, String> cleanRecord(Map<String, String> rec) {
		rec.remove("filepath");
		rec.remove("apikey");
		rec.remove("owlxmldir");
		rec.remove("origmetadata");
		rec.remove("owlxmlmetadata");
		rec.remove("origdir");
		rec.remove("tmpdir");
		return rec;
	}

	public static void main(String[] args)
			throws IOException, SAXException, ParserConfigurationException, InterruptedException {

		if (args.length != 1) {
			throw new RuntimeException("1 parameters needed: path to the bioportal directory");
		}

		File bioportaldir = new File(args[0]);
		if (!bioportaldir.isDirectory()) {
			throw new RuntimeException("Not a valid directory!");
		}
		createLatestSubmissionSnapshot(bioportaldir);
	}

}
