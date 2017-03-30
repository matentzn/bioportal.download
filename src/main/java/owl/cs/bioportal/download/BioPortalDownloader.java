package owl.cs.bioportal.download;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import owl.cs.man.ac.uk.experiment.csv.CSVUtilities;


public class BioPortalDownloader {

	private static String API_KEY = "0798b515-1ad4-4585-a9b4-2b4ad0f44f5e";
	private static Set<Future<Map<String, String>>> futures = new HashSet<Future<Map<String, String>>>();

	/**
	 * @param args
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws IOException, SAXException,
			ParserConfigurationException, InterruptedException {

		if (args.length != 4) {
			throw new RuntimeException(
					"6 parameters needed: (1) path to the download directory, (2) the bpfilelist, (3) an overall java timeout per download, (4) last parameter string  'last' for only latest snapshot");
		}

		File bioportaldir = new File(args[0]);
		File bpindex = new File(args[1]);
		File orig = new File(bioportaldir, "original");
		File owlxml = new File(bioportaldir, "owlxml");
		File temp = new File(bioportaldir, "tmp");
		File metadata = new File(bioportaldir, "metadata");
		File owlxmlmetadata = new File(metadata, "bp_owlxml_metadata.csv");
		File origmetadata = new File(metadata, "bp_orig_metadata.csv");

		if (!orig.exists()) {
			orig.mkdirs();
		}
		if (!owlxml.exists()) {
			owlxml.mkdirs();
		}
		if (!temp.exists()) {
			temp.mkdirs();
		}
		if (!metadata.exists()) {
			metadata.mkdirs();
		}

		int timeout = Integer.parseInt(args[2]);
		boolean lastavailable = args[3].equals("last");

		Map<String, Map<String, String>> currentbp = CSVUtilities
				.getRecordsIndexedBy(origmetadata,
						BioPortalMetrics.ORIGFILENAME.getName());
		System.out.println("Last repo size: " + currentbp.size());

		List<Map<String, String>> newVersion = CSVUtilities.getAllRecords(bpindex);
		System.out.println("Current : "+newVersion.size()+ " last: "+lastavailable);
		
		if(lastavailable) {
			newVersion = lastAvailable(newVersion);
		}
		
		
		System.out.println("Last: "+newVersion.size());
		
		/* OLD: BioPortalFileList.extractBioPortalMetaData();
		
		CSVUtilities.writeCSVData(
				new File(metadata,"bp_download_metadata" + System.currentTimeMillis()
						+ ".csv"), newVersion, false); */

		ExecutorService pool = Executors.newFixedThreadPool(4);

		for (Map<String, String> rec : newVersion) {
			String origfilename = rec.get(BioPortalMetrics.ORIGFILENAME
					.getName());
			System.out.println(origfilename+"?");
			if (currentbp.containsKey(origfilename)) {
				System.out.print("-in current corp-");
				String downloadedsuccess = currentbp.get(origfilename).get(
						BioPortalMetrics.DOWNLOADEDSUCCESS.getName());
				System.out.print("-sucess:"+downloadedsuccess+"-");
				if (downloadedsuccess != null && !downloadedsuccess.isEmpty()) {
					if (downloadedsuccess.trim().equals("1")) {
						System.out.println(origfilename
								+ " has already been downloaded, omitting");
						continue;
					}
				}
			}
			String download = rec.get(BioPortalMetrics.DOWNLOAD.getName());

			Map<String, String> config = new HashMap<String, String>();
			config.put("request", download);
			config.put("apikey", API_KEY);
			config.put("tmpdir", temp.toString());
			config.put("origdir", orig.toString());
			config.put("owlxmldir", owlxml.toString());
			config.put("owlxmlmetadata", owlxmlmetadata.toString());
			config.put("origmetadata", origmetadata.toString());
			config.putAll(rec);

			Future<Map<String, String>> future = pool
					.submit(new BioPortalDownloadOntology(config));
			futures.add(future);
		}

		for (Future<Map<String, String>> future : futures) {
			try {
				Map<String, String> data = future.get(timeout,
						TimeUnit.MILLISECONDS);
			} catch (ExecutionException e) {
				e.printStackTrace();
			} catch (TimeoutException e) {
				e.printStackTrace();
			}
		}

		pool.shutdown();
		pool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

		System.out.println("Done..");
	}

	private static List<Map<String, String>> lastAvailable(List<Map<String, String>> newVersion) {
		Map<String, Map<String, Map<String, String>>> ind = BioPortalServices.getRecordsIndexedByOntologyId(newVersion);
		System.out.println("IND: "+ind.size());
		return BioPortalServices.sampleSnapshot(ind);
	}

	public static void writeData(File csv, Map<String, String> data) {
		CSVUtilities.writeCSVData(csv, data, true);
	}

}
