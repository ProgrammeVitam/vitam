package fr.gouv.vitam.api.model;

/** DatabaseCursor class
 *  	Show database position of request response
 */
// TODO REVIEW Fix comment with a correct vision (either adding <br> either adding ':'
// FIXME REVIEW should be final
public class DatabaseCursor {
	private int total;
	private int offset;
	private int limit;

	/**
	 * DatabaseCursor constructor
	 * @param total
	 * 			total of units inserted/modified
	 * @param offset
	 * 			the offset of unit in database
	 * @param limit
	 * 			number limit of unit per response
	 */
	public DatabaseCursor(int total, int offset, int limit) {
		// TODO REVIEW is there any illegal values ?
		this.total = total;
		this.offset = offset;
		this.limit = limit;
	}

	/**
	 * @return the total of units inserted/modified
	 */
	public int getTotal() {
		return total;
	}

	/**
	 * @param total of units as integer
	 * @return the DatabaseCursor with the total is setted
	 */
	public DatabaseCursor setTotal(int total) {
		this.total = total;
		return this;
	}

	/**
	 * @return the offset of units in database
	 */
	public int getOffset() {
		return offset;
	}


	/**
	 * @param offset the offset of units in database
	 * @return the DatabaseCursor with the offset is setted
	 */
	public DatabaseCursor setOffset(int offset) {
		this.offset = offset;
		return this;
	}

	/**
	 * @return the limit of units per response 
	 */
	public int getLimit() {
		return limit;
	}

	/**
	 * @param limit  limit of units as integer
	 * @return the DatabaseCursor with the limits of units is setted
	 */
	public DatabaseCursor setLimit(int limit) {
		this.limit = limit;
		return this;
	}
}
