package owl.cs.bioportal.download;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDocumentFormat;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.RemoveImport;

import owl.cs.man.ac.uk.experiment.dataset.OntologySerialiser;


public class BioPortalOWLAPISerialiserRunner {

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
		
		File notParseableDir = new File(targetDir,"not_parseable");
		notParseableDir.mkdir();
		File importProblem = new File(targetDir,"importProblem");
		importProblem.mkdir();
		File serialisationProblem = new File(targetDir,"serialisationProblem");
		serialisationProblem.mkdir();
		File unkownProblem = new File(targetDir,"unkownProblem");
		unkownProblem.mkdir();
		File importsDir = new File(targetDir,"imports");
		importsDir.mkdir();
		File owlxmlDir = new File(targetDir,"owlxml");
		owlxmlDir.mkdir();
		File rdfxmlDir = new File(targetDir,"rdfxml");
		rdfxmlDir.mkdir();
		File functionalDir = new File(targetDir, "functional");
		functionalDir.mkdir();
		
		//File mergedImports = new File(targetDir,"mergedImports");

		for (File file : dir.listFiles()) {
			System.out.println("Processing: "+file.getName());
			OWLOntology ontology = null;
			OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
			try {
			System.out.println("Loading with OWLAPI");
			ontology = loadDocument(notParseableDir, file, ontology, manager);
			if(ontology==null) {
				continue;
			}
			System.out.println("Redirecting Imports..");
			redirectImports(importsDir, importProblem, file, ontology, manager); 
			
			System.out.println("Serialising..");
			serialiseToVariousFormats(owlxmlDir, rdfxmlDir,
					functionalDir, file, ontology, manager, serialisationProblem);
			}
			catch (Throwable e) {
				try {
					FileUtils.copyFileToDirectory(file, unkownProblem, false);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				e.printStackTrace();
			}
		}

	}

	private static void serialiseToVariousFormats(File owlxmlDir, File rdfxmlDir, File functionalDir, File file,
			OWLOntology ontology, OWLOntologyManager manager, File serialisationProblemDir) {
		try {
			OntologySerialiser.saveOWLXML(owlxmlDir, ontology, file.getName(), manager);
		} catch (Throwable e) {
			try {
				FileUtils.copyFileToDirectory(file, serialisationProblemDir, false);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
		try {
			OntologySerialiser.saveRDFXML(rdfxmlDir, ontology, file.getName(), manager);
		} catch (Throwable e) {
			try {
				FileUtils.copyFileToDirectory(file, serialisationProblemDir, false);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
		try {
			OntologySerialiser.saveFunctional(functionalDir, ontology, file.getName(), manager);
			//saveMergedImports(importsDir, ontology, file.getName(), manager);
		} catch (Throwable e) {
			try {
				FileUtils.copyFileToDirectory(file, serialisationProblemDir, false);
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
		
	}

	private static void redirectImports(File importsDir, File importFailed, File file, OWLOntology ontology,
			OWLOntologyManager manager) throws ImportSerialiseException {
		try {
			redirectImportClosure(ontology, manager, importsDir);
		} catch (Throwable e1) {
			try {
				FileUtils.copyFileToDirectory(file, importFailed, false);
			} catch (IOException e) {
				e.printStackTrace();
				throw new ImportSerialiseException();
			}
			e1.printStackTrace();
		}
	}

	private static OWLOntology loadDocument(File notParseableDir, File file,
			OWLOntology ontology, OWLOntologyManager manager) {
		try {
			ontology = manager.loadOntologyFromOntologyDocument(file);
		} catch (Throwable e) {
			try {
				FileUtils.copyFileToDirectory(file, notParseableDir, true);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
		return ontology;
	}

	private static IRI redirectImportClosure(OWLOntology ontology,
			OWLOntologyManager manager, File importsDir) throws ImportSerialiseException {
		Set<OWLOntology> imports = manager.getDirectImports(ontology); 
		IRI newIRI = null;
		if(imports.isEmpty()) {
			String name = UUID.randomUUID().toString();
			if(ontology.getOntologyID()!=null) {
				if(ontology.getOntologyID().getOntologyIRI()!=null) {
					name = "import"+ontology.getOntologyID().getOntologyIRI().toString().replaceAll("[^a-zA-Z0-9_]", "");
				}
			}
			File output = new File(importsDir,name);
			try {
				OutputStream os = new FileOutputStream(output);
				OWLDocumentFormat format = new OWLXMLDocumentFormat();
				manager.saveOntology(ontology,format,os);
			} catch (OWLOntologyStorageException e) {
				e.printStackTrace();
				throw new ImportSerialiseException();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				throw new ImportSerialiseException();
			}
			newIRI = IRI.create(output);
		} else {
			Set<IRI> iris = new HashSet<IRI>();
			for(OWLOntology imported:imports) {
				try {
					//manager.loadOntology(imported.getOntologyID().getOntologyIRI());
					redirectImportClosure(imported, manager, importsDir);
					iris.add(imported.getOntologyID().getOntologyIRI().or(IRI.create("http://unkowniri.owl")));
				} catch (Exception e) {
					e.printStackTrace();
					throw new ImportSerialiseException();
				}
			}
			for(OWLImportsDeclaration axiom:ontology.getImportsDeclarations()) {
				RemoveImport remove = new RemoveImport(ontology, axiom);
				manager.applyChange(remove);
			}
			for(IRI iri:iris) {
				AddImport import1 = new AddImport(ontology, manager.getOWLDataFactory().getOWLImportsDeclaration(iri));
				manager.applyChange(import1);
			}
		}
		return newIRI;
	}

	/*private static void saveMergedImports(File targetDir,
			OWLOntology ontology, String name, OWLOntologyManager manager) throws OWLOntologyCreationException, FileNotFoundException, OWLOntologyStorageException {
		// TODO Auto-generated method stub
		OWLOntologyFormat format = new OWLXMLOntologyFormat();
		save(targetDir, ontology, manager, format, name+".xml.owl");
	}*/

	

}
