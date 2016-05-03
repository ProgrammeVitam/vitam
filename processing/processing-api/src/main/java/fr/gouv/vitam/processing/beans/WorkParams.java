package fr.gouv.vitam.processing.beans;

/**
 * 
 *
 */
public class WorkParams {

	private String containerName;
	private String ObjectName;
	private byte[] bytes;
	// Unit 's metaData (must be extracted)will be converted to @metaDataRequest
	private String metaDataUnit;
	// unit Request metadata (insert in mongo DB)
	private String metaDataRequest;

	public String getContainerName() {
		return containerName;
	}

	public void setContainerName(String containerName) {
		this.containerName = containerName;
	}

	public String getObjectName() {
		return ObjectName;
	}

	public void setObjectName(String objectName) {
		ObjectName = objectName;
	}

	public byte[] getBytes() {
		return bytes;
	}

	public void setBytes(byte[] bytes) {
		this.bytes = bytes;
	}

	public String getMetaDataUnit() {
		return metaDataUnit;
	}

	public void setMetaDataUnit(String metaDataUnit) {
		this.metaDataUnit = metaDataUnit;
	}

	public String getMetaDataRequest() {
		return metaDataRequest;
	}

	public void setMetaDataRequest(String metaDataRequest) {
		this.metaDataRequest = metaDataRequest;
	}

}
