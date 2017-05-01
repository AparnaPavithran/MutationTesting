package MutationTesting.MutationTesting;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import javax.swing.JFileChooser;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;

public class Report {
	
	static String sep = "\n-------------------------------------------------------------------------------";
	static String projectFolderPrefix = null;
	static String reportloc = null;
	static String mutantsloc = null;
	static String mutantprojloc = null;
	static StringBuilder report = new StringBuilder();
	static String projectName = null;
	static String projectCopyLoc = null;

	public static String getProjPreLoc() {
		return projectFolderPrefix;
	}

	public static String getProjectCopyLoc() {
		return projectCopyLoc;
	}

	public static String getProjName() {
		return projectName;
	}

	public static String getMutantsloc() {
		return mutantsloc;
	}

	public static String getMutantLoc() {
		return mutantprojloc;
	}

	public static StringBuilder getReport() {
		return report;
	}

	public static String getSep() {
		return sep;
	}

	public static String getReportloc() {
		return reportloc;
	}

	// File Handling Section
	public static void getLoc() throws IOException{
		JFileChooser f = new JFileChooser();
		f.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES); 
		f.showSaveDialog(null);


		projectName = f.getSelectedFile().getName();
		report.append("Project Name ; "+ Report.getProjName());

		projectFolderPrefix = f.getSelectedFile().getAbsolutePath();
		report.append("\nProject Location ; "+getProjPreLoc() );

		projectCopyLoc = getProjPreLoc()+ File.separator+projectName+"_copy";

		reportloc = projectFolderPrefix + File.separator +"report";
		report.append("\nReport Location ; "+reportloc);

		mutantsloc = projectFolderPrefix + File.separator +"mutants" ;
		report.append("\nMutants Location ; "+getMutantsloc());

		mutantprojloc = projectFolderPrefix + File.separator + "MutatedProjects";
		report.append("\nMutated Projects Location ; "+getMutantLoc());

		checkAndDelete();

		FileUtils.copyDirectory(new File(getProjPreLoc()), new File(getProjPreLoc()+ File.separator+projectName+"_copy"));

		// create Report Directory
		createDir(reportloc);

		// create Mutants Directory
		createDir(getMutantsloc());

		// create mutated projects directory
		createDir(getMutantLoc());
		//createDir(getMutantLoc());

		FileUtils.copyDirectory(new File(getProjPreLoc()), new File(getProjPreLoc()+ File.separator+projectName+"_copy"));
		appendReport();
	}


	public static void checkAndDelete() throws IOException{
		File file = new File(projectCopyLoc);
		makeReport(sep);
		makeReport("Checking and deleting exiting files");
		if(file.exists()){
			makeReport("Deleting: "+projectCopyLoc);
			delete(file);
		}

		file = new File(reportloc);
		if(file.exists()){
			makeReport("Deleting: "+reportloc);
			delete(file);
		}

		file = new File(mutantsloc);
		if(file.exists()){
			makeReport("Deleting: "+mutantsloc);
			delete(file);
		}

		file = new File(mutantprojloc);
		if(file.exists()){
			makeReport("Deleting: "+mutantprojloc);
			delete(file);
		}
		makeReport(sep);

	}
	public static void appendReport(){
		report.append(sep);
	}

	public static void createMutantProject(int copyNumber) throws IOException {
		FileUtils.copyDirectory(new File(getProjectCopyLoc()), new File(getMutantLoc() + File.separator+ getProjName()+ "-" + copyNumber));
	}

	public static void deleteProjectCopy(int copyNumber) throws IOException {
		FileUtils.deleteDirectory(new File(getMutantLoc() + File.separator+ getProjName()+ "-" + copyNumber));
	}

	public static void createDir(String loc) throws IOException{
		File theDir = new File(loc);
		if (theDir.exists()) {
			delete(theDir);
		}

		try{
			theDir.mkdir();
		} 
		catch(SecurityException se){
			//handle it
			report.append(se.fillInStackTrace());
		}        


	}

	public static void delete(File file)
			throws IOException{

		if(file.isDirectory()){

			//directory is empty, then delete it
			if(file.list().length==0){

				file.delete();

			}else{

				//list all the directory contents
				String files[] = file.list();

				for (String temp : files) {
					//construct the file structure
					File fileDelete = new File(file, temp);

					//recursive delete
					delete(fileDelete);
				}

				//check the directory again, if empty then delete it
				if(file.list().length==0){
					file.delete();
				}
			}

		}else{
			//if file, then delete it
			file.delete();
		}
	}
	public static Collection<File> getFiles() {

		// All project files with code EXCLUDING test files.
		Collection<File> files = FileUtils.listFiles(new File(getProjPreLoc() + File.separator + "src" + File.separator
				+ "main" + File.separator + "java"), new RegexFileFilter("^(.*?)"), DirectoryFileFilter.DIRECTORY);

		report.append("\n Got Files List");
		appendReport();
		return files;
	}

	// Report handling section
	public static void makeReport(String str){
		report.append("\n"+str);
	}

	public static void printReport(){
		System.out.println(report);
	}
	public static void saveReport() throws IOException {

		// Save the report in Project folder

		File f = new File(reportloc + File.separator + "MutationInsertionReport.txt");
		if (f.exists() && !f.isDirectory()) {
			// delete previously generated report
			delete(f);
		}
		FileUtils.writeStringToFile(f, report.toString(), true);


		// Save the report in cuurent folder
		f = new File(new File(".").getCanonicalPath() + File.separator + "MutationReport.txt");
		if (f.exists() && !f.isDirectory()) {
			// delete previously generated report
			delete(f);
		}
		FileUtils.writeStringToFile(f, report.toString(), true);
	}

}
