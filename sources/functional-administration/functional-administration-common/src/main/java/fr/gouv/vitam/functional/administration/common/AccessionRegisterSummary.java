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
package fr.gouv.vitam.functional.administration.common;

import org.bson.Document;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import com.mongodb.client.model.IndexOptions;

import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;

/**
 * Accession Register Summary document
 */
public class AccessionRegisterSummary extends VitamDocument<AccessionRegisterSummary> {

    private static final long serialVersionUID = 3439757375656161919L;

    public static final String ORIGINATING_AGENCY = "OriginatingAgency";
    public static final String TOTAL_UNITS = "TotalUnits";
    public static final String TOTAL_OBJECTGROUPS = "TotalObjectGroups";
    public static final String TOTAL_OBJECTS = "TotalObjects";
    public static final String OBJECT_SIZE = "ObjectSize";
    public static final String TOTAL = "Total";
    public static final String DELETED = "Deleted";
    public static final String REMAINED = "Remained";
    public static final String CREATION_DATE = "creationDate";
    private static final String TENANT = "_tenant";

    private static final BasicDBObject[] indexes = {
        new BasicDBObject(ORIGINATING_AGENCY, 1)
    };

    /**
     * Empty Constructor
     */
    public AccessionRegisterSummary() {
        // Empty
        // FIXME P1
        append(TENANT, 0);
    }


    /**
     * Constructor
     *
     * @param document
     */
    public AccessionRegisterSummary(Document document) {
        super(document);
        // FIXME P1
        append(TENANT, 0);
    }

    /**
     * @param content
     */
    public AccessionRegisterSummary(JsonNode content) {
        super(content);
        // FIXME P1
        append(TENANT, 0);
    }


    /**
     * @param content
     */
    public AccessionRegisterSummary(String content) {
        super(content);
        // FIXME P1
        append(TENANT, 0);
    }


    /**
     * @param id
     * @return AccessionRegisterDetail
     */
    public AccessionRegisterSummary setId(String id) {
        append(VitamDocument.ID, id);
        return this;
    }

    /**
     * @param orgAgency
     * @return AccessionRegisterSummary
     */
    public AccessionRegisterSummary setOriginatingAgency(String orgAgency) {
        append(ORIGINATING_AGENCY, orgAgency);
        return this;
    }

    /**
     * @param totalUnits
     * @return AccessionRegisterSummary
     */
    public AccessionRegisterSummary setTotalUnits(RegisterValueDetail totalUnits) {
        append(TOTAL_UNITS, totalUnits);
        return this;
    }

    /**
     * @return String
     */
    public RegisterValueDetail getTotalUnits() {
        return new ObjectMapper().convertValue(this.get(TOTAL_UNITS), RegisterValueDetail.class);
    }

    /**
     * @param totalObjectGroups
     * @return AccessionRegisterSummary
     */
    public AccessionRegisterSummary setTotalObjectGroups(RegisterValueDetail totalObjectGroups) {
        append(TOTAL_OBJECTGROUPS, totalObjectGroups);
        return this;
    }

    /**
     * @return String
     */
    public RegisterValueDetail getTotalObjectGroups() {
        return new ObjectMapper().convertValue(this.get(TOTAL_OBJECTGROUPS), RegisterValueDetail.class);
    }

    /**
     * @param total
     * @return AccessionRegisterSummary
     */
    public AccessionRegisterSummary setTotalObjects(RegisterValueDetail total) {
        append(TOTAL_OBJECTS, total);
        return this;
    }

    /**
     * @return String
     */
    public RegisterValueDetail getTotalObjects() {
        return new ObjectMapper().convertValue(this.get(TOTAL_OBJECTS), RegisterValueDetail.class);
    }

    /**
     * @param objectSize
     * @return AccessionRegisterSummary
     */
    public AccessionRegisterSummary setObjectSize(RegisterValueDetail objectSize) {
        append(OBJECT_SIZE, objectSize);
        return this;
    }

    /**
     *
     * @return String
     */
    public RegisterValueDetail getTotalObjectSize() {
        return new ObjectMapper().convertValue(this.get(OBJECT_SIZE), RegisterValueDetail.class);
    }

    /**
     * Methods adding Indexes
     */

    public static void addIndexes() {
        // if not set, Unit and Tree are worst
        for (final BasicDBObject index : indexes) {
            if (index.containsField(ORIGINATING_AGENCY)) {
                FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getCollection().createIndex(
                    ((BasicDBObject) index.copy()).append(TENANT, 1),
                    new IndexOptions().unique(true));
            } else {
                FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getCollection().createIndex(index);
            }
        }
    }

    /**
     * @param creationDate
     * @return AccessionRegisterSummary
     */
    public AccessionRegisterSummary setCreationDate(String creationDate) {
        append(CREATION_DATE, creationDate);
        return this;
    }

    /**
     *
     * @return String
     */
    public String getOriginatingAgency() {
        return getString(ORIGINATING_AGENCY);
    }


}
