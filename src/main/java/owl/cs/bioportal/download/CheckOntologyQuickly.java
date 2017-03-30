package owl.cs.bioportal.download;

import java.io.File;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public class CheckOntologyQuickly {


	/**
	 * @param args
	 */
	public static void main(String[] args) {

		File dir = new File("D:\\rdfxml");

		for (File file : dir.listFiles()) {
			System.out.println("Processing: " + file.getName());
			OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
			try {
				OWLOntology module = manager.loadOntologyFromOntologyDocument(file);
				System.out.println("Loading with OWLAPI");
				System.out.println("Axioms: "+module.getAxiomCount());
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}

	}


}
