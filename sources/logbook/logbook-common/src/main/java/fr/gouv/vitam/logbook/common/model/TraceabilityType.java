package fr.gouv.vitam.logbook.common.model;

/**
 * Type of logbook traceability
 */
public enum TraceabilityType {

	OPERATION("operations"),
	LIFECYCLE("lifecycles"),
	STORAGE("storage");
	
	private String fileName;
	
	TraceabilityType(String fileName) {
		this.fileName = fileName;
	}
	
	public String getFileName() {
		return fileName;
	}
}
