package owl.cs.bioportal.download;

public enum BioPortalMetrics {
	
	ID("bp_id"),ONTOLOGYID("bp_ontologyid"),LATESTVERSION("bp_latestversion"), DISPLAYNAME("bp_displayname"), 
	DATERELEASE("bp_released"),DATECREATE("bp_created"),ABBREVIATION("bp_abbrev"),FORMAT("bp_format"),FILENAME("filename"), HOMEPAGE("bp_homepage"),DESCRIPTION("bp_description"), DOWNLOAD("bp_download"), 
	OWLXMLFILENAME("bp_owlxmlfilename"), ORIGFILENAME("bp_origfilename"), VERSION("bp_version"), DOWNLOADEDSUCCESS("downloadedsuccess"), SUBMISSIONID("submissionid");
	
	private String name;

	private BioPortalMetrics(String name) {
		this.name = name;
	}
	
	public String getName() {
		return this.name;
	}

}
