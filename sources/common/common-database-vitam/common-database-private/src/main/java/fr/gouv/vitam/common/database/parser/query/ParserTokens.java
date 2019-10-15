/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.database.parser.query;

import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.logging.SysErrLogger;

public class ParserTokens extends BuilderToken {
    public static final char DEFAULT_HASH_PREFIX_CHAR = '#';
    public static final char DEFAULT_UNDERSCORE_PREFIX_CHAR = '_';
    private static final String DEFAULT_HASH_PREFIX = "#";

    private ParserTokens() {
        // Empty
    }

    private static boolean isSingleProtectedVariable(String name) {
        // FIXME Patch for Single collection
        switch (name) {
            case "Identifier":
            case "PUID":
                return true;
            default:
        }
        return false;
    }

    /**
     * specific fields: nbunits, dua, ... <br>
     * $fields : [ #nbunits:1, #dua:1, #all:1... ]
     * <p>
     * #all:1 means all, while #all:0 means none
     */
    public enum PROJECTIONARGS {
        /**
         * Id of the item
         */
        ID("id"),
        /**
         * Number of units immediate children from this Unit
         */
        NBUNITS("nbunits"),
        /**
         * Number of objects within ObjectGroup
         */
        NBOBJECTS("nbobjects"),
        /**
         * All Dua for the result
         */
        DUA("dua"),
        /**
         * All fields for the result or None except Id
         */
        ALL("all"),
        /**
         * Qualifiers field
         */
        QUALIFIERS("qualifiers"),
        /**
         * Object size
         */
        SIZE("size"),
        /**
         * Object format
         */
        FORMAT("format"),
        /**
         * Unit/ObjectGroup type
         */
        TYPE("type"),
        /**
         * Unit/ObjectGroup Tenant
         */
        TENANT("tenant"),
        /**
         * Unit's ObjectGroup
         */
        OBJECT("object"),
        /**
         * Unit's immediate parents
         */
        UNITUPS("unitups"),
        /**
         * Unit's MIN distance from root
         */
        MIN("min"),
        /**
         * Unit's MAX distance from root
         */
        MAX("max"),
        /**
         * All Unit's parents
         */
        ALLUNITUPS("allunitups"),
        /**
         * Management bloc
         */
        MANAGEMENT("management"),
        /**
         * unit type bloc
         */
        UNITTYPE("unitType"),
        /**
         * parents arrays
         */
        UDS("uds"),
        /**
         * Unit or GOT's list of participating operations
         */
        OPERATIONS("operations"),
        /**
         * Unit or GOT's initial operation
         */
        OPI("opi"),
        /**
         * originating agency
         */
        ORIGINATING_AGENCY("originating_agency"),
        /**
         * originating agencies
         */
        ORIGINATING_AGENCIES("originating_agencies"),
        /**
         * Storage in OG
         */
        STORAGE("storage"),
        /**
         * Document's version (number of update on document)
         */
        VERSION("version"),
        /**
         * Document's version (number of update on document)
         */
        ATOMIC_VERSION("atomic_version"),
        /**
         * Document's usage (BINARY_MASTER, PHYSICAL_MASTER, DISSEMINATION, ...)
         */
        USAGE("usage"),
        /**
         * Document scoring according to research
         */
        SCORE("score"),
        /**
         * Last persisted date (logbook operation & lifecycle documents)
         */
        LAST_PERSISTED_DATE("lastPersistedDate"),
        /**
         * Parent unit graph
         */
        GRAPH("graph"),
        /**
         * elimination
         */
        ELIMINATION("elimination"),
        /**
         * Graph last peristed date
         */
        GRAPH_LAST_PERISTED_DATE("graph_last_persisted_date"),
        /**
         * Originating agency
         */
        PARENT_ORIGINATING_AGENCIES("parent_originating_agencies"),
        /**
         * Parent unit history
         */
        HISTORY("history"),
        /**
         * Seda Version
         */
        SEDAVERSION("sedaVersion"),
        /**
         * Vitam Implementation Version
         */
        IMPLEMENTATIONVERSION("implementationVersion"),
        /**
         * Vitam computedInheritedRules field
         */
        COMPUTEDINHERITEDRULES("computedInheritedRules"),
        VALIDCOMPUTEDINHERITEDRULES("validComputedInheritedRules"),
        /**
         * TRANSFER
         */
        OPTS("opts");


        private static final String NOT_FOUND = "Not found";
        private final String exactToken;

        /**
         * Constructor Add DEFAULT_HASH_PREFIX before the exactToken (#+exactToken)
         */
        PROJECTIONARGS(String realName) {
            exactToken = DEFAULT_HASH_PREFIX + realName;
        }

        /**
         * Used in projection for getObject
         *
         * @param token the token to valid
         * @return True if this token is valid, even starting with a "_"
         */
        public static boolean isValid(String token) {
            // Exception for getObject sliced projection
            return token.startsWith("_qualifiers.") || token.equals("mgt") || token.startsWith("_mgt.") ||
                token.startsWith("_storage.");
        }

        /**
         * Parse the given name
         *
         * @param name name of the field to parse
         * @return the corresponding PROJECTIONARGS
         * @throws IllegalArgumentException if not found
         */
        public static PROJECTIONARGS parse(String name) {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException(NOT_FOUND);
            }
            try {
                return PROJECTIONARGS.valueOf(name.toUpperCase());
            } catch (final Exception e) {
                throw new IllegalArgumentException(NOT_FOUND, e);
            }
        }

        /**
         * This function check the field name for know if it's allowed to inserting or updating the given field from
         * external. This check is done by the API-internal by the VarNameAdapter.
         *
         * @param name field name
         * @return True if the field is not allowed, false
         */
        public static boolean notAllowedOnSetExternal(String name) {
            if (name == null || name.isEmpty()) {
                return true;
            }
            return name.charAt(0) == ParserTokens.DEFAULT_UNDERSCORE_PREFIX_CHAR;
        }

        /**
         * This function check the field name for know if it's allowed to inserting or updating the given field from
         * external. This check is done by the API-internal by the VarNameAdapter.
         *
         * @param name field name
         * @return True if this value is not allowed on set (insert, update)
         */
        public static boolean notAllowedOnSet(String name) {
            if (name == null || name.isEmpty()) {
                return false;
            }
            if (name.charAt(0) == ParserTokens.DEFAULT_HASH_PREFIX_CHAR) {
                // Check on prefix (preceding '.')
                int pos = name.indexOf('.');
                final String realname;
                if (pos > 1) {
                    realname = name.substring(1, pos);
                } else {
                    realname = name.substring(1);
                }
                try {
                    final PROJECTIONARGS proj = PROJECTIONARGS.valueOf(realname.toUpperCase());
                    switch (proj) {
                        case ALL:
                        case FORMAT:
                        case ID:
                        case NBUNITS:
                        case NBOBJECTS:
                        case QUALIFIERS:
                        case SIZE:
                        case OBJECT:
                        case UNITUPS:
                        case ALLUNITUPS:
                        case TENANT:
                        case MIN:
                        case MAX:
                        case UNITTYPE:
                        case ORIGINATING_AGENCY:
                        case ORIGINATING_AGENCIES:
                        case VERSION:
                        case USAGE:
                        case OPERATIONS:
                        case OPI:
                        case SCORE:
                        case LAST_PERSISTED_DATE:
                        case GRAPH:
                        case GRAPH_LAST_PERISTED_DATE:
                        case HISTORY:
                        case ELIMINATION:
                        case PARENT_ORIGINATING_AGENCIES:
                        case SEDAVERSION:
                        case IMPLEMENTATIONVERSION:
                        case STORAGE:
                        case COMPUTEDINHERITEDRULES:
                            return true;
                        default:
                    }
                } catch (final Exception e) {
                    // Ignore
                    SysErrLogger.FAKE_LOGGER.ignoreLog(e);
                }
            }
            return ParserTokens.isSingleProtectedVariable(name);
        }

        /**
         * @return the exact token
         */
        public final String exactToken() {
            return exactToken;
        }
    }
}
