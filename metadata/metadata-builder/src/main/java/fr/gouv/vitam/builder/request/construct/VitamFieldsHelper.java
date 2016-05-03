/**
 * 
 */
package fr.gouv.vitam.builder.request.construct;

import static fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.FILTERARGS.*;
import static fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.PROJECTIONARGS.*;

/**
 * Vitam Field Helper to facilitate the usage of field names
 *
 */
public class VitamFieldsHelper {
	private VitamFieldsHelper() {
		// Empty
	}

	/**
	 * @return #id
	 */
	public static final String id() {
		return id.exactToken();
	}
	/**
	 * @return #nbunits
	 */
	public static final String nbunits() {
		return nbunits.exactToken();
	}
	/**
	 * @return #all
	 */
	public static final String all() {
		return all.exactToken();
	}
	/**
	 * @return #size
	 */
	public static final String size() {
		return size.exactToken();
	}
	/**
	 * @return #format
	 */
	public static final String format() {
		return format.exactToken();
	}
	/**
	 * @return #type
	 */
	public static final String type() {
		return type.exactToken();
	}
	/**
	 * @return #dua
	 */
	public static final String dua() {
		return dua.exactToken();
	}
	/**
	 * @return #units
	 */
	public static final String units() {
		return units.exactToken();
	}
	/**
	 * @return #objectgroups
	 */
	public static final String objectgroups() {
		return objectgroups.exactToken();
	}
	/**
	 * @return #objects
	 */
	public static final String objects() {
		return objects.exactToken();
	}
	/**
	 * @return #cache
	 */
	public static final String cache() {
		return cache.exactToken();
	}
	/**
	 * @return #nocache
	 */
	public static final String nocache() {
		return nocache.exactToken();
	}
}
