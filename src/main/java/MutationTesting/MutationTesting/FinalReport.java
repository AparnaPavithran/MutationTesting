package MutationTesting.MutationTesting;

public class FinalReport {

	/* projectFolderPrefix - contains the target project folder location
	 * report - keeps track of the program
	 */
	private String sep = "\n-------------------------------------------------------------------------------";
	private String projectFolderPrefix = null;
	private String reportloc = null;
	private String mutantsloc = null;
	private String mutantprojloc = null;
	private StringBuilder report = new StringBuilder();
	private String projectName = null;

	public String getProjectFolderPrefix() {
		return projectFolderPrefix;
	}

	public String getProjectName() {
		return projectName;
	}

	public String getMutantsloc() {
		return mutantsloc;
	}

	public String getMutantprojloc() {
		return mutantprojloc;
	}
	
	public StringBuilder getReport() {
		return report;
	}

	public String getSep() {
		return sep;
	}

//	public void setSep(String sep) {
//		this.sep = sep;
//	}

	public String getReportloc() {
		return reportloc;
	}

	public void setReportloc(String reportloc) {
		this.reportloc = reportloc;
	}
}
