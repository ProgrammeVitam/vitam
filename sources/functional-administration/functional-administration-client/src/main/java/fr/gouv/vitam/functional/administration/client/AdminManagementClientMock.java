/*******************************************************************************
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
 *******************************************************************************/
package fr.gouv.vitam.functional.administration.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client.AbstractMockClient;
import fr.gouv.vitam.common.client.ClientMockResultHelper;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.model.AccessContractModel;
import fr.gouv.vitam.functional.administration.client.model.AccessionRegisterDetailModel;
import fr.gouv.vitam.functional.administration.client.model.AccessionRegisterSummaryModel;
import fr.gouv.vitam.functional.administration.client.model.FileFormatModel;
import fr.gouv.vitam.functional.administration.client.model.IngestContractModel;
import fr.gouv.vitam.functional.administration.client.model.RegisterValueDetailModel;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterStatus;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.DatabaseConflictException;
import fr.gouv.vitam.functional.administration.common.exception.FileFormatException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialNotFoundException;

/**
 * Mock client implementation for AdminManagement
 */
class AdminManagementClientMock extends AbstractMockClient implements AdminManagementClient {
    private static final String STREAM_IS_A_MANDATORY_PARAMETER = "stream is a mandatory parameter";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AdminManagementClientMock.class);

    @Override
    public Status checkFormat(InputStream stream) throws FileFormatException {
        ParametersChecker.checkParameter(STREAM_IS_A_MANDATORY_PARAMETER, stream);
        LOGGER.debug("Check file format request:");
        StreamUtils.closeSilently(stream);
        return Status.OK;
    }

    @Override
    public Status importFormat(InputStream stream) throws FileFormatException {
        ParametersChecker.checkParameter(STREAM_IS_A_MANDATORY_PARAMETER, stream);
        LOGGER.debug("Import file format request:");
        StreamUtils.closeSilently(stream);
        return Status.CREATED;
    }

    @Override
    public JsonNode getFormatByID(String id) throws FileFormatException, InvalidParseOperationException {
        ParametersChecker.checkParameter(STREAM_IS_A_MANDATORY_PARAMETER, id);
        LOGGER.debug("get format by id request:");
        return ClientMockResultHelper.getFormat().toJsonNode();
    }

    @Override
    public RequestResponse<FileFormatModel> getFormats(JsonNode query)
        throws FileFormatException, JsonGenerationException, JsonMappingException, InvalidParseOperationException,
        IOException {
        ParametersChecker.checkParameter(STREAM_IS_A_MANDATORY_PARAMETER, query);
        LOGGER.debug("get document format request:");

        return ClientMockResultHelper.getFormatList();
    }

    @Override
    public Status checkRulesFile(InputStream stream) throws FileRulesException {
        ParametersChecker.checkParameter(STREAM_IS_A_MANDATORY_PARAMETER, stream);
        LOGGER.debug("Check file rules  request:");
        StreamUtils.closeSilently(stream);
        return Status.OK;
    }

    @Override
    public Status importRulesFile(InputStream stream) throws FileRulesException, DatabaseConflictException {
        ParametersChecker.checkParameter(STREAM_IS_A_MANDATORY_PARAMETER, stream);
        LOGGER.debug("import file Rules request:");
        StreamUtils.closeSilently(stream);
        return Status.CREATED;
    }

    @Override
    public JsonNode getRuleByID(String id) throws FileRulesException, InvalidParseOperationException {
        ParametersChecker.checkParameter(STREAM_IS_A_MANDATORY_PARAMETER, id);
        LOGGER.debug("get rule by id request:");
        return ClientMockResultHelper.getRuleList().toJsonNode();
    }

    @Override
    public JsonNode getRules(JsonNode query) throws FileRulesException, InvalidParseOperationException,
        JsonGenerationException, JsonMappingException, IOException {
        ParametersChecker.checkParameter(STREAM_IS_A_MANDATORY_PARAMETER, query);
        LOGGER.debug("get document rules request:");
        return ClientMockResultHelper.getRule().toJsonNode();
    }

    @Override
    public void createorUpdateAccessionRegister(AccessionRegisterDetailModel register)
        throws DatabaseConflictException {
        String result;
        try {
            result = JsonHandler.writeAsString(register);
        } catch (final InvalidParseOperationException e) {
            LOGGER.error("Cannot serialize parameters", e);
            result = "{}";
        }
        LOGGER.info("AccessionRegister: " + result);
    }

    @Override
    public RequestResponse getAccessionRegister(JsonNode query)
        throws InvalidParseOperationException, ReferentialException {
        AccessionRegisterSummaryModel model = new AccessionRegisterSummaryModel();
        RegisterValueDetailModel totalObjectsGroups = new RegisterValueDetailModel();
        RegisterValueDetailModel totalUnits = new RegisterValueDetailModel();
        RegisterValueDetailModel totalObjects = new RegisterValueDetailModel();
        RegisterValueDetailModel objectSize = new RegisterValueDetailModel();
        String modelJson = "";
        ParametersChecker.checkParameter("stream is a mandatory parameter", query);
        LOGGER.debug("get document Register Fund request:");

        model.setId("aefaaaaaaaaam7mxaa2gyakygejizayaaaaq")
            .setTenant(0)
            .setOriginatingAgency("FRAN_NP_005568");

        totalObjects.setTotal(12)
            .setDeleted(0)
            .setRemained(12);
        model.setTotalObjects(totalObjects);

        totalObjectsGroups.setTotal(3)
            .setDeleted(0)
            .setRemained(3);
        model.setTotalObjectsGroups(totalObjectsGroups);

        totalUnits.setTotal(3)
            .setDeleted(0)
            .setRemained(3);
        model.setTotalUnits(totalUnits);

        objectSize.setTotal(1035126)
            .setDeleted(0)
            .setRemained(1035126);
        model.setObjectSize(objectSize)
            .setCreationDate("2016-11-04T20:40:49.030");
        modelJson = JsonHandler.writeAsString(model);
        return ClientMockResultHelper.createReponse(modelJson);
    }

    @Override
    public RequestResponse getAccessionRegisterDetail(JsonNode query)
        throws InvalidParseOperationException, ReferentialException {
        RegisterValueDetailModel totalObjectsGroups = new RegisterValueDetailModel(1, 0, 1);
        RegisterValueDetailModel totalUnits = new RegisterValueDetailModel(1, 0, 1);
        RegisterValueDetailModel totalObjects = new RegisterValueDetailModel(4, 0, 4);
        RegisterValueDetailModel objectSize = new RegisterValueDetailModel(345042, 0, 345042);
        ParametersChecker.checkParameter("stream is a mandatory parameter", query);
        LOGGER.debug("get document Accession Register request:");

        AccessionRegisterDetailModel detailBuider = new AccessionRegisterDetailModel();
        detailBuider.setId("aedqaaaaacaam7mxabsakakygeje2uyaaaaq")
            .setTenant(0)
            .setOriginatingAgency("FRAN_NP_005568")
            .setSubmissionAgency("FRAN_NP_005061")
            .setArchivalAgreement("Something")
            .setEndDate("2016-11-04T21:40:47.912+01:00")
            .setStartDate("2016-11-04T21:40:47.912+01:00")
            .setStatus(AccessionRegisterStatus.STORED_AND_COMPLETED)
            .setTotalObjects(totalObjects)
            .setTotalObjectsGroups(totalObjectsGroups)
            .setTotalUnits(totalUnits)
            .setObjectSize(objectSize);
        return ClientMockResultHelper.createReponse(detailBuider);
    }

    @Override
    public Status importIngestContracts(List<IngestContractModel> ingestContractModelList)
        throws InvalidParseOperationException, AdminManagementClientServerException {
        LOGGER.debug("import Ingest contracts request ");
        return Status.OK;
    }

    @Override
    public Status importAccessContracts(List<AccessContractModel> accessContractModelList) throws InvalidParseOperationException, AdminManagementClientServerException {
        LOGGER.debug("import access contracts request ");
        return Status.OK;
    }

    @Override
    public RequestResponse findAccessContracts(JsonNode queryDsl) throws InvalidParseOperationException, AdminManagementClientServerException {
        LOGGER.debug("find access contracts request ");
        if (VitamThreadUtils.getVitamSession().getTenantId() == null) {
            VitamThreadUtils.getVitamSession().setTenantId(0);
        }
        AccessContractModel model = JsonHandler.getFromString(ClientMockResultHelper.ACCESS_CONTRACTS, AccessContractModel.class);
        return ClientMockResultHelper.createReponse(model);
    }

    @Override
    public RequestResponse findAccessContractsByID(String documentId) throws InvalidParseOperationException, AdminManagementClientServerException {
        LOGGER.debug("find access contracts by id request ");
        return ClientMockResultHelper.getAccessContracts();
    }

    @Override
    public RequestResponse<IngestContractModel> findIngestContracts(JsonNode query)
        throws InvalidParseOperationException, AdminManagementClientServerException {
        LOGGER.debug("find ingest contracts request");
        IngestContractModel ingestContract = JsonHandler.getFromString(ClientMockResultHelper.INGEST_CONTRACTS, IngestContractModel.class);
        return ClientMockResultHelper.createReponse(ingestContract);
    }

    @Override
    public RequestResponse<IngestContractModel> findIngestContractsByID(String id)
        throws InvalidParseOperationException, AdminManagementClientServerException, ReferentialNotFoundException {
        LOGGER.debug("find ingest contracts by id request ");
        return ClientMockResultHelper.getIngestContracts();
    }
}
