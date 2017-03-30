package owl.cs.bioportal.download;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public class OWLAPISerialiser {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		if (args.length != 2) {
			throw new RuntimeException(
					"You need the path to the (flat) directory containing the files, the targetDir and the trashbin.");
		}
		File dir = new File(args[0]);
		File targetDir = new File(args[1]);
		
		targetDir.mkdir();

		for (File file : dir.listFiles()) {
			
			//OWLOntology ontology = null;
			OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
			try {
				String filename = file.getName().substring(0,file.getName().indexOf(".owl.xml"))+".xml.rdf";
				File newfile = new File(targetDir, filename);
				if(newfile.exists()) {
					continue;
				}
				System.out.println("Processing: " + file.getName());
				System.out.println("Loading with OWLAPI");
				OWLOntology module = manager.loadOntologyFromOntologyDocument(file);
				OWLDocumentFormat format = new  OWLXMLDocumentFormat();
				OutputStream os = new FileOutputStream(newfile);
				manager.saveOntology(module,format, os);
				os.close();
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}

	}

}
