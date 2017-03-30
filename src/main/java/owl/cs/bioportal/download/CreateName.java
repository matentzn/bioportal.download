package owl.cs.bioportal.download;

import java.io.File;
import java.util.List;
import java.util.Map;

import owl.cs.man.ac.uk.experiment.csv.CSVUtilities;

public class CreateName {
	public static void main(String[] args) {
		List<Map<String,String>> data = CSVUtilities.getAllRecords(new File("D:\\00bp2014\\bp_metadata1399473416571.csv"));
		for(Map<String,String> rec:data) {
			String displayname = rec.get(BioPortalMetrics.DISPLAYNAME.getName());
			String abbreviation = rec.get(BioPortalMetrics.ABBREVIATION.getName());
			String subid = rec.get(BioPortalMetrics.SUBMISSIONID.getName());
			System.out.println(createName(displayname, abbreviation, subid,"orig"));
		}
	}

	public static String createName(String displayname, String abbreviation,
			String subid, String extension) {
		String filename = displayname.trim().replaceAll(" ", "-")
				.replaceAll("[^a-zA-Z0-9_-]", "");
		filename = filename.toLowerCase();
		filename = abbreviation+"."+filename+"."+ subid+"."+extension;
		filename = filename.toLowerCase();
		return filename;
	}
}
