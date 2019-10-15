/**
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
/**
 *
 */
package fr.gouv.vitam.common.database.builder.query;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FILTERARGS.CACHE;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FILTERARGS.NOCACHE;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FILTERARGS.OBJECTGROUPS;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.FILTERARGS.UNITS;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.ALL;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.ALLUNITUPS;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.COMPUTED_INHERITED_RULES;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.DUA;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.ELIMINATION;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.FORMAT;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.GRAPH;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.GRAPH_LAST_PERSISTED_DATE;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.HISTORY;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.ID;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.IMPLEMENTATIONVERSION;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.INITIAL_OPERATION;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.LAST_PERSISTED_DATE;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.MANAGEMENT;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.MAX;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.MIN;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.NBCHILD;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.NBOBJECTS;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.NBUNITS;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.OBJECT;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.OPERATIONS;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.OPERATION_TRANSFERS;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.ORIGINATING_AGENCIES;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.ORIGINATING_AGENCY;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.PARENTS;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.PARENT_ORIGINATING_AGENCIES;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.QUALIFIERS;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.SCORE;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.SEDAVERSION;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.SIZE;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.STORAGE;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.TENANT;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.TYPE;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.UNITTYPE;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.UNITUPS;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.USAGE;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.VALID_COMPUTED_INHERITED_RULES;
import static fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS.VERSION;



/**
 * Vitam Field Helper to facilitate the usage of field names
 */
public class VitamFieldsHelper {

    private VitamFieldsHelper() {
        // Empty
    }

    /**
     * @return #id
     */
    public static String id() {
        return ID.exactToken();
    }

    /**
     * @return #nbunits
     */
    public static String nbunits() {
        return NBUNITS.exactToken();
    }

    /**
     * @return #nbobjects
     */
    public static String nbobjects() {
        return NBOBJECTS.exactToken();
    }

    /**
     * @return #nbc
     */
    public static String nbc() {
        return NBCHILD.exactToken();
    }

    /**
     * @return #all
     */
    public static String all() {
        return ALL.exactToken();
    }

    /**
     * @return #size
     */
    public static String size() {
        return SIZE.exactToken();
    }

    /**
     * @return #format
     */
    public static String format() {
        return FORMAT.exactToken();
    }

    /**
     * @return #type
     */
    public static String type() {
        return TYPE.exactToken();
    }

    /**
     * @return #tenant
     */
    public static String tenant() {
        return TENANT.exactToken();
    }

    /**
     * @return #dua
     */
    public static String dua() {
        return DUA.exactToken();
    }

    /**
     * @return #units
     */
    public static String units() {
        return UNITS.exactToken();
    }

    /**
     * @return #objectgroups
     */
    public static String objectgroups() {
        return OBJECTGROUPS.exactToken();
    }

    /**
     * @return #cache
     */
    public static String cache() {
        return CACHE.exactToken();
    }

    /**
     * @return #nocache
     */
    public static String nocache() {
        return NOCACHE.exactToken();
    }

    /**
     * @return #qualifiers
     */
    public static String qualifiers() {
        return QUALIFIERS.exactToken();
    }

    /**
     * @return #object
     */
    public static String object() {
        return OBJECT.exactToken();
    }

    /**
     * @return #unitups
     */
    public static String unitups() {
        return UNITUPS.exactToken();
    }

    /**
     * @return #allunitups
     */
    public static String allunitups() {
        return ALLUNITUPS.exactToken();
    }

    /**
     * @return #min
     */
    public static String min() {
        return MIN.exactToken();
    }

    /**
     * @return #max
     */
    public static String max() {
        return MAX.exactToken();
    }

    /**
     * @return #max
     */
    public static String uds() {
        return PARENTS.exactToken();
    }

    /**
     * @return #graph
     */
    public static String graph() {
        return GRAPH.exactToken();
    }

    /**
     * @return #graph_last_persisted_date
     */
    public static String graph_last_persisted_date() {
        return GRAPH_LAST_PERSISTED_DATE.exactToken();
    }

    /**
     * @return #management
     */
    public static String management() {
        return MANAGEMENT.exactToken();
    }

    /**
     * @return #operations
     */
    public static String operations() {
        return OPERATIONS.exactToken();
    }

    /**
     * @return #unitType
     */
    public static String unitType() {
        return UNITTYPE.exactToken();
    }

    /**
     * @return #originating_agency
     */
    public static String originatingAgency() {
        return ORIGINATING_AGENCY.exactToken();
    }

    /**
     * @return #originating_agencies
     */
    public static String originatingAgencies() {
        return ORIGINATING_AGENCIES.exactToken();
    }

    /**
     * @return #parent_originating_agencies
     */
    public static String parentOriginatingAgencies() {
        return PARENT_ORIGINATING_AGENCIES.exactToken();
    }

    /**
     * @return #version
     */
    public static String version() {
        return VERSION.exactToken();
    }

    /**
     * @return #storage
     */
    public static String storage() {
        return STORAGE.exactToken();
    }

    /**
     * @return #usage
     */
    public static String usage() {
        return USAGE.exactToken();
    }

    /**
     * @return #score
     */
    public static String score() {
        return SCORE.exactToken();
    }

    /**
     * @return #lastPersistedDate
     */
    public static String lastPersistedDate() {
        return LAST_PERSISTED_DATE.exactToken();
    }

    /**
     * @return #opi
     */
    public static String initialOperation() {
        return INITIAL_OPERATION.exactToken();
    }

    /**
     * @return #history
     */
    public static String history() {
        return HISTORY.exactToken();
    }
    /**
     * @return #version
     */
    public static String sedaVersion() {
        return SEDAVERSION.exactToken();
    }
    /**
     * @return #version
     */
    public static String implementationVersion() {
        return IMPLEMENTATIONVERSION.exactToken();
    }

    public static String computedInheritedRules() {
        return COMPUTED_INHERITED_RULES.exactToken();
    }

    public static String validComputedInheritedRules() { return VALID_COMPUTED_INHERITED_RULES.exactToken(); }

    public static String opts() { return OPERATION_TRANSFERS.exactToken(); }



    /**
     * @return #elimination
     */
    public static String elimination() {
        return ELIMINATION.exactToken();
    }

    public static JsonNode removeHash(JsonNode jsonNode) {
        ObjectNode objectNode = (ObjectNode) jsonNode;
        JsonNode hashId = objectNode.remove(VitamFieldsHelper.id());
        if (hashId != null) {
            objectNode.set("_id", hashId);
        }
        JsonNode hashTenant = objectNode.remove(VitamFieldsHelper.tenant());
        if (hashTenant != null) {
            objectNode.set("_tenant", hashTenant);
        }

        JsonNode hashVersion = objectNode.remove(VitamFieldsHelper.version());
        if (hashTenant != null) {
            objectNode.set("_v", hashVersion);
        }

        return objectNode;
    }
}
