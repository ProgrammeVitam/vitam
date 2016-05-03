package fr.gouv.vitam.processing.constants;

/**
 * 
 * 
 *         different constants status code for workflow , action handler and
 *         process
 *
 */
public enum StatusCode {

	OK("OK"), KO("KO"), ERROR("ERROR");

	private String value;

	private StatusCode(String value) {
		this.value = value;
	}

	public String value() {
		return value;
	}

}
