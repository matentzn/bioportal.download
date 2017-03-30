package owl.cs.bioportal.download;

import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.io.FileUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import owl.cs.man.ac.uk.experiment.csv.CSVUtilities;
import owl.cs.man.ac.uk.experiment.file.FileUtilities;
import owl.cs.man.ac.uk.experiment.metrics.StaticMetrics;
import owl.cs.man.ac.uk.experiment.ontology.OntologyUtilities;
import owl.cs.man.ac.uk.experiment.util.ExperimentUtilities;


public class BioPortalDownloadOntology implements Callable<Map<String,String>>{

	String restRequest;
	File targetFile;
	boolean validconfig = true;
	private String filename;
	private String API_KEY;
	private Map<String,String> config;

	
	public BioPortalDownloadOntology(Map<String,String> config) {
		if(!config.containsKey("tmpdir")) {
			validconfig = false;
		}
		if(!config.containsKey("origdir")) {
			validconfig = false;
		}
		if(!config.containsKey("owlxmldir")) {
			validconfig = false;
		}
		if(!config.containsKey(BioPortalMetrics.ORIGFILENAME.getName())) {
			validconfig = false;
		}
		if(!config.containsKey(BioPortalMetrics.OWLXMLFILENAME.getName())) {
			validconfig = false;
		}
		if(!config.containsKey("owlxmlmetadata")) {
			validconfig = false;
		}
		if(!config.containsKey("origmetadata")) {
			validconfig = false;
		}
		if(!config.containsKey("request")) {
			validconfig = false;
		}
		
		this.restRequest = config.containsKey("request") ? config.get("request") : "";
		this.filename = config.containsKey(BioPortalMetrics.ORIGFILENAME.getName()) ? config.get(BioPortalMetrics.ORIGFILENAME.getName()) : "";
		this.targetFile = config.containsKey("tmpdir") ? new File(config.get("tmpdir"),filename) : new File("");
		this.config = config;
		this.API_KEY = config.containsKey("apikey") ? config.get("apikey") : "";
	}

	public Map<String,String> call() throws Exception {
		Map<String,String> data = new HashMap<String,String>();
		if(!validconfig) {
			data.put("error", "invalidconfig");
			writeData(data);
			return data;
		}
		
        try {
        	File temp = new File(config.get("tmpdir"),config.get(BioPortalMetrics.ORIGFILENAME.getName()));
        	File owlxml = new File(config.get("owlxmldir"),config.get(BioPortalMetrics.OWLXMLFILENAME.getName()));
        	File orig = new File(config.get("origdir"),config.get(BioPortalMetrics.ORIGFILENAME.getName()));
        	
        	OWLOntologyManager man = OWLManager.createOWLOntologyManager();
        	OWLOntologyManager man2 = OWLManager.createOWLOntologyManager();
        	InputStream is = download();
        	FileUtils.copyInputStreamToFile(is, temp);
        	File owlfile = FileUtilities.unzipOwlFile(temp,orig.getName(),temp.getParentFile());
        	/*if (entryName.contains(".owl") || entryName.contains(".xml") || entryName.contains(".obo")) {
                if (!entryName.contains("MACOSX")) {
                    outfile = new File(out + entryName);
                }
            }*/
        	if(owlfile.isDirectory()) {
        		owlfile = ExperimentUtilities.getReferenceOntologyInDir(owlfile);
        	}
        	if(owlfile==null) {
        		data.put("error", "could not determine ontology file in unzipped folder");
        		writeData(data);
        		return data;
        	}
        	String abbrev = config.get(BioPortalMetrics.ABBREVIATION.getName());
        	OWLOntology o = man.loadOntologyFromOntologyDocument(owlfile);
        	OntologyUtilities.saveOntologyMergedOWLXML(owlxml,o,abbrev);
        	OWLOntology owlxmlo = man2.loadOntologyFromOntologyDocument(owlxml);
        	StaticMetrics sm = new StaticMetrics(o);
        	StaticMetrics sm2 = new StaticMetrics(owlxmlo);
        	
        	Map<String,String> owlxmlmetadata = new HashMap<String,String>();
        	owlxmlmetadata.putAll(sm2.getEssentialMetrics());
        	owlxmlmetadata.put(BioPortalMetrics.DOWNLOADEDSUCCESS.getName(), "true");
        	owlxmlmetadata.putAll(ExperimentUtilities.getDefaultExperimentData("BioPortalDownloader", owlxml));
        	writeData(owlxmlmetadata,new File(config.get("owlxmlmetadata")));
        	
        	FileUtils.copyFile(temp, orig);
        	
        	data.putAll(sm.getEssentialMetrics());
        	data.put(BioPortalMetrics.DOWNLOADEDSUCCESS.getName(), "true");
        	data.putAll(ExperimentUtilities.getDefaultExperimentData("BioPortalDownloader", owlfile));
        	writeData(data,new File(config.get("origmetadata")));
        }
        catch(Exception e) {
        	e.printStackTrace();
        	data.put("bpdown_exception", e.getClass().toString());
        	data.put("bpdown_exception_cause", e.getCause().getClass().toString()+"");
        	data.put("bpdown_exception_message", e.getMessage().substring(0,40).replace("\n", ""));
			data.put("error", "download_or_merge");
			writeData(data);
			return data;
        }
        
		return data;
	}
	
	private void writeData(Map<String, String> data) {
		writeData(data,new File(config.get("origmetadata")));		
	}

	public void writeData(Map<String, String> data, File file) {
		data.putAll(config);
		CSVUtilities.writeCSVData(file, data, true);
	}
	
	private InputStream download() {
		URL url;
		HttpURLConnection conn;
		try {
			url = new URL(restRequest);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Authorization", "apikey token=" + API_KEY);
			conn.setRequestProperty("Accept", "application/json");
			InputStream is = conn.getInputStream();
			return is;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
