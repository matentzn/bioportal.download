package owl.cs.bioportal.download;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.tika.Tika;

public class AppendFileExtension {

	/**
	 * @param args
	 * @throws IOException 
	*/
	
	private static final Map<String,String> mimetypeExtensionMap;
	static {
		Map<String,String> mimetypeem = new HashMap<String, String>();
		mimetypeem.put("application/xml", ".xml");
		mimetypeem.put("text/plain", ".txt");
		mimetypeem.put("application/rdf+xml", ".rdf");
		mimetypeem.put("application/xhtml+xml", ".xml");
		mimetypeem.put("application/zip", ".zip");
		mimetypeExtensionMap = Collections.unmodifiableMap(mimetypeem);
    }
	
	
	public static void main(String[] args) throws IOException {
		if(args.length!=2) {
			throw new RuntimeException("You need the path to the (flat) directory containing the files.");
		}
		File dir = new File(args[0]);
		File targetDir = new File(args[1]);
		
		for(File file:dir.listFiles()) {
			if(file.isFile()) {
				appendFileExtension(file,targetDir);
			}
		}
	}
	
	public static File appendFileExtension(File file, File targetDir) throws IOException {
		String mimetype = getMimeType(file);
		String extension = mimetypeExtensionMap.get(mimetype);
		File newFile = new File(targetDir,file.getName()+extension);
		FileUtils.copyFile(file, newFile);
		System.out.println("Copied: "+newFile+"("+mimetype+")");
		return newFile;
	}
	
	 public static String getMimeType(File file) {
	        String mimeType = null;
	        try {

	            Tika tika = null;
	            tika = new Tika();
	            mimeType = tika.detect(file);

	        } catch (FileNotFoundException e) {
	           e.printStackTrace();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }

	        return mimeType;
	    }
}
