/*******************************************************************************
 * This file is part of Vitam Project.
 *
 * Copyright Vitam (2012, 2016)
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL license as circulated
 * by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.common.database.parser.query;

import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;

/**
 * Main language definition
 *
 *
 */
public class ParserTokens extends BuilderToken {
    
    /**
     * Default prefix for internal variable
     */
    public static final String DEFAULT_HASH_PREFIX = "#";
    /**
     * Default prefix for internal variable
     */
    public static final char DEFAULT_HASH_PREFIX_CHAR = '#';

    private ParserTokens() {
        
    }


    /**
     * specific fields: nbunits, dua, ... <br>
     * $fields : [ #nbunits:1, #dua:1, #all:1... ]
     *
     * #all:1 means all, while #all:0 means none
     */
    public static enum PROJECTIONARGS {
        /**
         * Id of the item
         */
        ID("id"),
        /**
         * Number of units from each result (Unit = subUnit, ObjectGroup = objects)
         */
        NBUNITS("nbunits"),
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
        TYPE("type");

        private static final String NOT_FOUND = "Not found";
        private final String exactToken;

        /**
         * Constructor Add DEFAULT_HASH_PREFIX before the exactToken (#+exactToken)
         */
        private PROJECTIONARGS(String realName) {
            exactToken = DEFAULT_HASH_PREFIX + realName;
        }

        /**
         * @return the exact token
         */
        public final String exactToken() {
            return exactToken;
        }

        /**
         *
         * @param name String
         * @return the corresponding PROJECTIONARGS
         * @throws IllegalArgumentException if not found
         */
        public static final PROJECTIONARGS parse(String name) {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException(NOT_FOUND);
            }
            if (name.charAt(0) == ParserTokens.DEFAULT_HASH_PREFIX_CHAR) {
                try {
                    return PROJECTIONARGS.valueOf(name.substring(1).toUpperCase());
                } catch (final Exception e) {
                    throw new IllegalArgumentException(NOT_FOUND, e);
                }
            }
            throw new IllegalArgumentException(NOT_FOUND);
        }

        /**
         *
         * @param name String
         * @return True if this value is not allowed on set (insert, update)
         */
        public static boolean notAllowedOnSet(String name) {
            if (name == null || name.isEmpty()) {
                return false;
            }
            if (name.charAt(0) == ParserTokens.DEFAULT_HASH_PREFIX_CHAR) {
                try {
                    final PROJECTIONARGS proj = PROJECTIONARGS.valueOf(name.substring(1).toUpperCase());
                    switch (proj) {
                        case ALL:
                        case FORMAT:
                        case ID:
                        case NBUNITS:
                        case QUALIFIERS:
                        case SIZE:
                            return true;
                        default:
                    }
                } catch (final Exception e) {// NOSONAR
                    // Ignore
                }
            }
            return false;
        }
    }
}
