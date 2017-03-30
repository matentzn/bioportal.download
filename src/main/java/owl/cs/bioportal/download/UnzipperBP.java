package owl.cs.bioportal.download;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import owl.cs.man.ac.uk.experiment.file.CompressedFilenameFilter;
import owl.cs.man.ac.uk.experiment.file.Unzipper;
import owl.cs.man.ac.uk.experiment.ontology.OntologyFiletypePattern;

public class UnzipperBP {

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		if (args.length != 2) {
			throw new RuntimeException(
					"You need the path to the (flat) directory containing the files.");
		}
		File dir = new File(args[0]);
		File tmpDir = new File(args[1]);

		File tmpDirOldArchives = new File(tmpDir, "oldArchives");
		File tmpDirUNZIP = new File(tmpDir, "UNZIP");
		File tmpDirManual = new File(tmpDir, "MULTIPLE");
		File tmpDirDiscarded = new File(tmpDir, "DISCARDEDARCHIVE");

		for (File file : dir.listFiles(new CompressedFilenameFilter())) {
			File zipDir;
			File ontology = null;
			try {
				zipDir = decompress(file, tmpDirUNZIP);
				List<File> resultingFilehandles = listFilesAndFilesSubDirectories(zipDir);
				List<File> candidates = getPotentialOntologyFiles(resultingFilehandles);

				boolean multipleCandidate = false;
				if (candidates.size() == 1) {
					ontology = candidates.get(0);
				} else if (candidates.size() > 1) {
					multipleCandidate = true;
				}

				if (ontology == null) {
					System.out
							.println("There was no unique ontology file to be found in directory "
									+ zipDir);
					if (multipleCandidate) {
						FileUtils.moveFileToDirectory(file, tmpDirManual, true);
					} else {
						FileUtils.moveFileToDirectory(file, tmpDirDiscarded,
								true);
					}

				} else {
					File destination = new File(dir, ontology.getName());
					if (destination.exists()) {
						destination = new File(dir, ontology.getName()
								+ "_copy");
					}
					FileUtils.copyFile(ontology, destination);
					FileUtils
					.moveFileToDirectory(file, tmpDirOldArchives, true);
				}
			} catch (Throwable e) {
				e.printStackTrace();
				FileUtils.moveFileToDirectory(file, tmpDirManual, true);
			}
		}
	}

	private static File decompress(File file, File tmpDirP) throws IOException {
		File tmpDir = new File(tmpDirP, System.currentTimeMillis() + "_tmp");
		tmpDir.mkdirs();
		if (Unzipper.isZippedFile(file)) {
			File unzipDir = Unzipper.unZip(file, tmpDir);
			for(File recentlyUnzipped:unzipDir.listFiles(new CompressedFilenameFilter())) {
				decompress(recentlyUnzipped, unzipDir);
			}
		} else {
			Unzipper.unGzip(file, tmpDir);
		}
		return tmpDir;
	}

	public static List<File> listFilesAndFilesSubDirectories(File dir) {

		List<File> files = new ArrayList<File>();

		for (File file : dir.listFiles()) {
			if (file.isFile()) {
				files.add(file);
			} else if (file.isDirectory()) {
				if (!(file.getName().equals("__MACOSX"))
						&& !(file.getName().startsWith("."))) {
					List<File> filesInSub = listFilesAndFilesSubDirectories(file);
					files.addAll(filesInSub);
				}
			}
		}
		return files;
	}

	private static List<File> getPotentialOntologyFiles(
			List<File> resultingFilehandles) {
		List<File> potentialOntologies = new ArrayList<File>();

		for (File file : resultingFilehandles) {
			if (OntologyFiletypePattern
					.potentialFileType(file.getName(), false)) {
				potentialOntologies.add(file);
			}
		}

		if (potentialOntologies.size() == 1) {
			return potentialOntologies;
		}

		return potentialOntologies;
	}

	public static File createTempDirectory() throws IOException {
		final File temp;

		temp = File.createTempFile("temp", Long.toString(System.nanoTime()));

		if (!(temp.delete())) {
			throw new IOException("Could not delete temp file: "
					+ temp.getAbsolutePath());
		}

		if (!(temp.mkdir())) {
			throw new IOException("Could not create temp directory: "
					+ temp.getAbsolutePath());
		}

		return (temp);
	}
}
