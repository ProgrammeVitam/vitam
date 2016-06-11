/**
 *
 */
package fr.gouv.vitam.builder.request.construct;

import static fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.FILTERARGS.CACHE;
import static fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.FILTERARGS.NOCACHE;
import static fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.FILTERARGS.OBJECTGROUPS;
import static fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.FILTERARGS.OBJECTS;
import static fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.FILTERARGS.UNITS;
import static fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.PROJECTIONARGS.ALL;
import static fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.PROJECTIONARGS.DUA;
import static fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.PROJECTIONARGS.FORMAT;
import static fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.PROJECTIONARGS.ID;
import static fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.PROJECTIONARGS.NBUNITS;
import static fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.PROJECTIONARGS.SIZE;
import static fr.gouv.vitam.builder.request.construct.configuration.ParserTokens.PROJECTIONARGS.TYPE;

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
        return ID.exactToken();
    }

    /**
     * @return #nbunits
     */
    public static final String nbunits() {
        return NBUNITS.exactToken();
    }

    /**
     * @return #all
     */
    public static final String all() {
        return ALL.exactToken();
    }

    /**
     * @return #size
     */
    public static final String size() {
        return SIZE.exactToken();
    }

    /**
     * @return #format
     */
    public static final String format() {
        return FORMAT.exactToken();
    }

    /**
     * @return #type
     */
    public static final String type() {
        return TYPE.exactToken();
    }

    /**
     * @return #dua
     */
    public static final String dua() {
        return DUA.exactToken();
    }

    /**
     * @return #units
     */
    public static final String units() {
        return UNITS.exactToken();
    }

    /**
     * @return #objectgroups
     */
    public static final String objectgroups() {
        return OBJECTGROUPS.exactToken();
    }

    /**
     * @return #objects
     */
    public static final String objects() {
        return OBJECTS.exactToken();
    }

    /**
     * @return #cache
     */
    public static final String cache() {
        return CACHE.exactToken();
    }

    /**
     * @return #nocache
     */
    public static final String nocache() {
        return NOCACHE.exactToken();
    }
}
