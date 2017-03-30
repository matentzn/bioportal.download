package owl.cs.bioportal.download;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by
 * User: Samantha Bail
 * Date: 22/02/2012
 * Time: 17:15
 * The University of Manchester
 */


public class FileHandler {


    /**
     * @param startDir directory which containsSubTree the files and subdirectories to search
     * @param filter   a string (e.g. file extension) which is contained in the files to be listed
     * @return a sorted list of files in startDir that contain the filter in their file name
     */
    public static List<String> getFilteredFileList(File startDir, String filter) {

        List<String> fileList = new ArrayList<String>();
        try {
            validateDirectory(startDir);
            for (File f : getUnsortedFileList(startDir)) {
                if (f.getName().contains(filter) && !f.getName().contains("svn-base")) {
                    fileList.add(f.getAbsolutePath());
                }
            }
            Collections.sort(fileList);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        } catch (Error e) {
            System.err.println(e.getMessage());
        }
        return fileList;
    }

    /**
     * @param startDir directory which containsSubTree the files and subdirectories to search
     * @return a sorted list of files in startDir
     */
    public static List<String> getFileList(File startDir) {
        List<String> fileList = new ArrayList<String>();
        try {
            validateDirectory(startDir);
            for (File f : getUnsortedFileList(startDir)) {
                if (!f.getName().contains("svn-base") && !f.getName().contains(".DS_STORE")) {
                    fileList.add(f.getAbsolutePath());
                }
            }
            Collections.sort(fileList);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        } catch (Error e) {
            System.err.println(e.getMessage());
        }
        return fileList;
    }

    /**
     * @param startDir directory which containsSubTree the files and subdirectories to search
     * @return unsorted, unfiltered list of files in startDir
     */
    private static List<File> getUnsortedFileList(File startDir) {
        List<File> result = new ArrayList<File>();
        File[] filesAndDirs = startDir.listFiles();
        List<File> filesDirs = Arrays.asList(filesAndDirs);
        for (File file : filesDirs) {
            result.add(file); //always add, even if it is a directory
            if (!file.isFile()) {
                //if it is a directory -> recursive call!
                List<File> deeperList = getUnsortedFileList(file);
                result.addAll(deeperList);
            }
        }
        return result;
    }

    /**
     * Directory is valid if it exists, does not represent a file, and can be read.
     * @param aDirectory directory to validate
     * @throws java.io.FileNotFoundException exception
     */
    private static void validateDirectory(File aDirectory) throws FileNotFoundException {
        if (aDirectory == null) {
            throw new IllegalArgumentException("Directory should not be null.");
        }
        if (!aDirectory.exists()) {
            throw new FileNotFoundException("Directory does not exist: " + aDirectory);
        }
        if (!aDirectory.isDirectory()) {
            throw new IllegalArgumentException("Is not a directory: " + aDirectory);
        }
        if (!aDirectory.canRead()) {
            throw new IllegalArgumentException("Directory cannot be read: " + aDirectory);
        }
    }

    /**
     * deepCopy a file from one location to another
     * @param sourceFile source file
     * @param targetFile destination file
     */
    public void copyFile(File sourceFile, File targetFile) {
        try {

            InputStream in = new FileInputStream(sourceFile);
            //to append the file.
            // OutputStream out = new FileOutputStream(f2,true);
            //to overwrite the file.
            OutputStream out = new FileOutputStream(targetFile);

            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        } catch (FileNotFoundException ex) {
            System.out.println(ex.getMessage() + " in the specified directory.");
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * @param source     URL of the file to download
     * @param targetFile the target directory
     * @throws java.io.IOException
     */
    public static void downloadFile(String source, File targetFile) throws IOException {
        /*
        * Get a connection to the URL and start up
        * a buffered reader.
        */
        URL url = new URL(source);
        url.openConnection();
        InputStream reader = url.openStream();

        FileOutputStream writer = new FileOutputStream(targetFile);
        byte[] buffer = new byte[153600];
        int bytesRead = 0;
        while ((bytesRead = reader.read(buffer)) > 0) {
            writer.write(buffer, 0, bytesRead);
            buffer = new byte[153600];
        }
        writer.close();
        reader.close();
    }

    


    /**
     * Fetch the entire contents of a text file, and return it in a String.
     * This style of implementation does not throw Exceptions to the caller.
     * @param aFile is a file which already exists and can be read.
     * @return the content of the file
     */
    public static String getFileContents(File aFile) {
        StringBuilder contents = new StringBuilder();

        try {
            //use buffering, reading one line at a time
            //FileReader always assumes default encoding is OK!
            BufferedReader input = new BufferedReader(new FileReader(aFile));
            try {
                String line = null; //not declared within while loop
                /*
                * readLine is a bit quirky :
                * it returns the content of a line MINUS the newline.
                * it returns null only for the END of the stream.
                * it returns an empty String if two newlines appear in a row.
                */
                while ((line = input.readLine()) != null) {
                    contents.append(line);
                    contents.append(System.getProperty("line.separator"));
                }
            } finally {
                input.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return contents.toString();
    }


    /**
     * gets a random selection of files from a specified directory
     * @param inputDir
     * @param i
     * @return
     */
    public static Set<String> getRandomFiles(File inputDir, int i) {
        List<String> all = getFilteredFileList(inputDir, ".owl.xml");
        Collections.shuffle(all);
        if (all.size() <= i) {
            return new HashSet<String>(all);
        }
        return new HashSet<String>(all.subList(0, i));
    }
}
