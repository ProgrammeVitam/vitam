/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "https://cecill.info".
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

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.administration.RegisterValueDetailModel;
import org.bson.Document;

/**
 * Accession Register Summary document
 */
public class AccessionRegisterSummary extends VitamDocument<AccessionRegisterSummary> {

    private static final long serialVersionUID = 3439757375656161919L;

    /**
     * the OriginatingAgency field of accession register
     */
    public static final String ORIGINATING_AGENCY = "OriginatingAgency";
    /**
     * the TotalUnits field of accession register
     */
    public static final String TOTAL_UNITS = "TotalUnits";
    /**
     * the TotalObjectGroups field of accession register
     */
    public static final String TOTAL_OBJECTGROUPS = "TotalObjectGroups";
    /**
     * the TotalObjects field of accession register
     */
    public static final String TOTAL_OBJECTS = "TotalObjects";
    /**
     * the ObjectSize field of accession register
     */
    public static final String OBJECT_SIZE = "ObjectSize";
    /**
     * the ingested field of accession register
     */
    public static final String INGESTED = "ingested";
    /**
     * the deleted field of accession register
     */
    public static final String DELETED = "deleted";
    /**
     * the remained field of accession register
     */
    public static final String REMAINED = "remained";
    /**
     * the creationDate field of accession register
     */
    public static final String CREATION_DATE = "CreationDate";

    /**
     * Empty Constructor
     */
    public AccessionRegisterSummary() {}

    /**
     * Constructor
     *
     * @param document in format Document to create AccessionRegisterSummary
     */
    public AccessionRegisterSummary(Document document) {
        super(document);
    }

    /**
     * @param content in format JsonNode to create AccessionRegisterSummary
     */
    public AccessionRegisterSummary(JsonNode content) {
        super(content);
    }

    /**
     * @param content in format String to create AccessionRegisterSummary
     */
    public AccessionRegisterSummary(String content) {
        super(content);
    }

    @Override
    public VitamDocument<AccessionRegisterSummary> newInstance(JsonNode content) {
        return new AccessionRegisterSummary(content);
    }

    /**
     * @param id as String to set
     * @return AccessionRegisterDetail
     */
    public AccessionRegisterSummary setId(String id) {
        append(VitamDocument.ID, id);
        return this;
    }

    /**
     * @param orgAgency as Sting to set
     * @return AccessionRegisterSummary
     */
    public AccessionRegisterSummary setOriginatingAgency(String orgAgency) {
        append(ORIGINATING_AGENCY, orgAgency);
        return this;
    }

    /**
     * @param totalUnits to set
     * @return AccessionRegisterSummary
     */
    public AccessionRegisterSummary setTotalUnits(RegisterValueDetailModel totalUnits) {
        append(TOTAL_UNITS, totalUnits);
        return this;
    }

    /**
     * @return String
     */
    public RegisterValueDetailModel getTotalUnits() {
        try {
            return JsonHandler.getFromJsonNode(
                JsonHandler.toJsonNode(this.get(TOTAL_UNITS)),
                RegisterValueDetailModel.class
            );
        } catch (InvalidParseOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @param totalObjectGroups to set
     * @return AccessionRegisterSummary
     */
    public AccessionRegisterSummary setTotalObjectGroups(RegisterValueDetailModel totalObjectGroups) {
        append(TOTAL_OBJECTGROUPS, totalObjectGroups);
        return this;
    }

    /**
     * @return String
     */
    public RegisterValueDetailModel getTotalObjectGroups() {
        try {
            return JsonHandler.getFromJsonNode(
                JsonHandler.toJsonNode(this.get(TOTAL_OBJECTGROUPS)),
                RegisterValueDetailModel.class
            );
        } catch (InvalidParseOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @param total to set
     * @return AccessionRegisterSummary
     */
    public AccessionRegisterSummary setTotalObjects(RegisterValueDetailModel total) {
        append(TOTAL_OBJECTS, total);
        return this;
    }

    /**
     * @return String
     */
    public RegisterValueDetailModel getTotalObjects() {
        try {
            return JsonHandler.getFromJsonNode(
                JsonHandler.toJsonNode(this.get(TOTAL_OBJECTS)),
                RegisterValueDetailModel.class
            );
        } catch (InvalidParseOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @param objectSize to set
     * @return AccessionRegisterSummary
     */
    public AccessionRegisterSummary setObjectSize(RegisterValueDetailModel objectSize) {
        append(OBJECT_SIZE, objectSize);
        return this;
    }

    /**
     * @return String
     */
    public RegisterValueDetailModel getTotalObjectSize() {
        try {
            return JsonHandler.getFromJsonNode(
                JsonHandler.toJsonNode(this.get(OBJECT_SIZE)),
                RegisterValueDetailModel.class
            );
        } catch (InvalidParseOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @param creationDate to set
     * @return AccessionRegisterSummary
     */
    public AccessionRegisterSummary setCreationDate(String creationDate) {
        append(CREATION_DATE, creationDate);
        return this;
    }

    /**
     * @return String
     */
    public String getOriginatingAgency() {
        return getString(ORIGINATING_AGENCY);
    }
}
