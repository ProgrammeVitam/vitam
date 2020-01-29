/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.functional.administration.common;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.administration.AccessionRegisterStatus;
import fr.gouv.vitam.common.model.administration.RegisterValueDetailModel;
import fr.gouv.vitam.common.model.administration.RegisterValueEventModel;
import org.bson.Document;

/**
 * Accession Register Detail document
 */
public class AccessionRegisterDetail extends VitamDocument<AccessionRegisterDetail> {

    private static final long serialVersionUID = 3439757375656161919L;
    public static final String ACQUISITION_INFORMATION = "AcquisitionInformation";
    public static final String LEGAL_STATUS = "LegalStatus";
    public static final String ORIGINATING_AGENCY = "OriginatingAgency";
    public static final String SUBMISSION_AGENCY = "SubmissionAgency";
    public static final String ARCHIVALAGREEMENT = "ArchivalAgreement";
    public static final String START_DATE = "StartDate";
    public static final String END_DATE = "EndDate";
    public static final String LAST_UPDATE = "LastUpdate";
    public static final String TOTAL_UNITS = "TotalUnits";
    public static final String TOTAL_OBJECTGROUPS = "TotalObjectGroups";
    public static final String TOTAL_OBJECTS = "TotalObjects";
    public static final String OBJECT_SIZE = "ObjectSize";
    public static final String STATUS = "Status";
    public static final String OPC = "Opc";
    public static final String OPI = "Opi";
    public static final String ARCHIVAL_PROFILE = "ArchivalProfile";
    public static final String OPERATION_IDS = "OperationIds";
    public static final String EVENTS = "Events";
    public static final String TENANT = "_tenant";

    /**
     * Empty Constructor
     */
    public AccessionRegisterDetail() {
    }

    /**
     * Constructor
     *
     * @param document in format Document to create AccessionRegisterDetail
     */
    public AccessionRegisterDetail(Document document) {
        super(document);
    }

    /**
     * @param content in format JsonNode to create AccessionRegisterDetail
     */
    public AccessionRegisterDetail(JsonNode content) {
        super(content);
    }

    /**
     * @param content in format String to create AccessionRegisterDetail
     */
    public AccessionRegisterDetail(String content) {
        super(content);
    }

    /**
     * @param tenantId th working tenant
     */
    public AccessionRegisterDetail(Integer tenantId) {
        append(TENANT, tenantId);
    }

    /**
     * @param id to set
     * @return AccessionRegisterDetail
     */
    public AccessionRegisterDetail setId(String id) {
        append(VitamDocument.ID, id);
        return this;
    }

    @Override
    public VitamDocument<AccessionRegisterDetail> newInstance(JsonNode content) {
        return new AccessionRegisterDetail(content);
    }

    /**
     * @return String
     */
    public String getAcquisitionInformation() {
        return getString(ACQUISITION_INFORMATION);
    }

    /**
     * @param acquisitionInformation to set
     * @return AccessionRegisterDetail
     */
    public AccessionRegisterDetail setAcquisitionInformation(String acquisitionInformation) {
        append(ACQUISITION_INFORMATION, acquisitionInformation);
        return this;
    }

    /**
     * @return String
     */
    public String getLegalStatus() {
        return getString(LEGAL_STATUS);
    }

    /**
     * @param legalStatus to set
     * @return AccessionRegisterDetail
     */
    public AccessionRegisterDetail setLegalStatus(String legalStatus) {
        append(LEGAL_STATUS, legalStatus);
        return this;
    }

    /**
     * @param ArchivalProfile to set
     * @return AccessionRegisterDetail
     */
    public AccessionRegisterDetail setArchivalProfile(String ArchivalProfile) {
        append(ARCHIVAL_PROFILE, ArchivalProfile);
        return this;
    }

    /**
     * @return String
     */
    public String getArchivalProfile() {
        return getString(ARCHIVAL_PROFILE);
    }

    /**
     * @return String
     */
    public String getOriginatingAgency() {
        return getString(ORIGINATING_AGENCY);
    }

    /**
     * @param orgAgency to set
     * @return AccessionRegisterDetail
     */
    public AccessionRegisterDetail setOriginatingAgency(String orgAgency) {
        append(ORIGINATING_AGENCY, orgAgency);
        return this;
    }

    /**
     * @param subAgency to set
     * @return this
     */
    public AccessionRegisterDetail setSubmissionAgency(String subAgency) {
        append(SUBMISSION_AGENCY, subAgency);
        return this;
    }

    /**
     * @param archivalAgreement Archival Agreement id
     * @return this
     */
    public AccessionRegisterDetail setArchivalAgreement(String archivalAgreement) {
        append(ARCHIVALAGREEMENT, archivalAgreement);
        return this;
    }

    /**
     * @param startDate to set
     * @return AccessionRegisterDetail
     */
    public AccessionRegisterDetail setStartDate(String startDate) {
        append(START_DATE, LocalDateUtil.getFormattedDateForMongo(startDate));
        return this;
    }

    /**
     * @return String
     */
    public String getEndDate() {
        return getString(END_DATE);
    }

    /**
     * @param endDate to set
     * @return AccessionRegisterDetail
     */
    public AccessionRegisterDetail setEndDate(String endDate) {
        append(END_DATE, LocalDateUtil.getFormattedDateForMongo(endDate));
        return this;
    }

    /**
     * @param lastUpdate to set
     * @return AccessionRegisterDetail
     */
    public AccessionRegisterDetail setLastUpdate(String lastUpdate) {
        append(LAST_UPDATE, LocalDateUtil.getFormattedDateForMongo(lastUpdate));
        return this;
    }

    /**
     * @return String
     */
    public RegisterValueDetailModel getTotalUnits() {
        try {
            return JsonHandler
                .getFromJsonNode(JsonHandler.toJsonNode(this.get(TOTAL_UNITS)), RegisterValueDetailModel.class);
        } catch (InvalidParseOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @param totalUnits to set
     * @return AccessionRegisterDetail
     */
    public AccessionRegisterDetail setTotalUnits(RegisterValueDetailModel totalUnits) {
        append(TOTAL_UNITS, totalUnits);
        return this;
    }

    /**
     * @return RegisterValueDetail
     */
    public RegisterValueDetailModel getTotalObjectGroups() {
        try {
            return JsonHandler
                .getFromJsonNode(JsonHandler.toJsonNode(this.get(TOTAL_OBJECTGROUPS)), RegisterValueDetailModel.class);
        } catch (InvalidParseOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @param totalObjectGroups to set
     * @return AccessionRegisterDetail
     */
    public AccessionRegisterDetail setTotalObjectGroups(RegisterValueDetailModel totalObjectGroups) {
        append(TOTAL_OBJECTGROUPS, totalObjectGroups);
        return this;
    }

    /**
     * @return RegisterValueDetail
     */
    public RegisterValueDetailModel getTotalObjects() {
        try {
            return JsonHandler
                .getFromJsonNode(JsonHandler.toJsonNode(this.get(TOTAL_OBJECTS)), RegisterValueDetailModel.class);
        } catch (InvalidParseOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @param total to set
     * @return this
     */
    public AccessionRegisterDetail setTotalObjects(RegisterValueDetailModel total) {
        append(TOTAL_OBJECTS, total);
        return this;
    }

    /**
     * @param objectSize to set
     * @return AccessionRegisterDetail
     */
    public AccessionRegisterDetail setObjectSize(RegisterValueDetailModel objectSize) {
        append(OBJECT_SIZE, objectSize);
        return this;
    }

    /**
     * @return RegisterValueDetail
     */
    public RegisterValueDetailModel getTotalObjectSize() {
        try {
            return JsonHandler
                .getFromJsonNode(JsonHandler.toJsonNode(this.get(OBJECT_SIZE)), RegisterValueDetailModel.class);
        } catch (InvalidParseOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @param status to set
     * @return AccessionRegisterDetail
     */
    public AccessionRegisterDetail setStatus(AccessionRegisterStatus status) {
        append(STATUS, status.name());
        return this;
    }

    public AccessionRegisterDetail setOpc(String opc) {
        append(OPC, opc);
        return this;
    }

    public String getOpc() {
        return (String) get(OPC);
    }

    public AccessionRegisterDetail setOpi(String opi) {
        append(OPI, opi);
        return this;
    }

    public AccessionRegisterDetail setOperationIds(List<String> operationIds) {
        if (!operationIds.isEmpty()) {
            final List<String> ids = new ArrayList<>();
            ids.addAll(operationIds);
            append(OPERATION_IDS, ids);
        }
        return this;
    }

    public AccessionRegisterDetail setEvents(List<RegisterValueEventModel> events) {
        if (!events.isEmpty()) {
            final List<RegisterValueEventModel> ids = new ArrayList<>();
            ids.addAll(events);
            append(EVENTS, ids);
        }
        return this;
    }

}
