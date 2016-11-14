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
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.client2.AbstractMockClient;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterDetail;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterStatus;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterSummary;
import fr.gouv.vitam.functional.administration.common.FileFormat;
import fr.gouv.vitam.functional.administration.common.FileRules;
import fr.gouv.vitam.functional.administration.common.RegisterValueDetail;
import fr.gouv.vitam.functional.administration.common.exception.DatabaseConflictException;
import fr.gouv.vitam.functional.administration.common.exception.FileFormatException;
import fr.gouv.vitam.functional.administration.common.exception.FileRulesException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;

/**
 * Mock client implementation for AdminManagement
 */
class AdminManagementClientMock extends AbstractMockClient implements AdminManagementClient {
    private static final String STREAM_IS_A_MANDATORY_PARAMETER = "stream is a mandatory parameter";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AdminManagementClientMock.class);

    private FileFormat createFileFormat(String id) {
        final List<String> testList = new ArrayList<>();
        testList.add("test1");
        return (FileFormat) new FileFormat()
            .setCreatedDate("now")
            .setExtension(testList)
            .setMimeType(testList)
            .setName("name")
            .setPriorityOverIdList(testList)
            .setPronomVersion("pronom version")
            .setPUID("puid")
            .setVersion("version")
            .append("_id", id);
    }

    private FileRules createFileRules(String ruleValue) {
        return new FileRules()
            .setRuleId("APP_00001")
            .setRuleType("AppraiseRule")
            .setRuleDescription("testList")
            .setRuleDuration("10")
            .setRuleMeasurement("Annee")
            .setRuleValue(ruleValue);

    }

    private RegisterValueDetail createRegisterValueDetail() {
        return new RegisterValueDetail()
            .setDeleted(50L)
            .setRemained(50L)
            .setTotal(100L);
    }

    private AccessionRegisterDetail createAccessionRegisterDetails(String id) {
        return new AccessionRegisterDetail()
            .setId(id)
            .setOriginatingAgency("USA")
            .setSubmissionAgency("AG2")
            .setLastUpdate("01/10/1990")
            .setTotalObjects(createRegisterValueDetail())
            .setTotalObjectGroups(createRegisterValueDetail())
            .setTotalUnits(createRegisterValueDetail())
            .setObjectSize(createRegisterValueDetail())
            .setStatus(AccessionRegisterStatus.STORED_AND_COMPLETED);
    }

    private AccessionRegisterSummary createAccessionRegister(String id) {
        return new AccessionRegisterSummary()
            .setId(id)
            .setOriginatingAgency("USA")
            .setTotalObjects(createRegisterValueDetail())
            .setTotalObjectGroups(createRegisterValueDetail())
            .setTotalUnits(createRegisterValueDetail())
            .setObjectSize(createRegisterValueDetail());
    }

    private String fileFormatListToJsonString(List<FileFormat> formatList)
        throws IOException, InvalidParseOperationException {
        return JsonHandler.writeAsString(formatList);
    }

    @Override
    public Status checkFormat(InputStream stream) throws FileFormatException {
        ParametersChecker.checkParameter(STREAM_IS_A_MANDATORY_PARAMETER, stream);
        LOGGER.debug("Check file format request:");
        StreamUtils.closeSilently(stream);
        return Status.OK;
    }

    @Override
    public void importFormat(InputStream stream) throws FileFormatException {
        ParametersChecker.checkParameter(STREAM_IS_A_MANDATORY_PARAMETER, stream);
        LOGGER.debug("Import file format request:");
        StreamUtils.closeSilently(stream);
    }

    @Override
    public void deleteFormat() throws FileFormatException {
        LOGGER.debug("Delete file format request:");
    }

    @Override
    public JsonNode getFormatByID(String id) throws FileFormatException, InvalidParseOperationException {
        ParametersChecker.checkParameter(STREAM_IS_A_MANDATORY_PARAMETER, id);
        LOGGER.debug("get format by id request:");
        final FileFormat file = createFileFormat(id);
        return JsonHandler.toJsonNode(file);
    }

    @Override
    public JsonNode getFormats(JsonNode query)
        throws FileFormatException, JsonGenerationException, JsonMappingException, InvalidParseOperationException,
        IOException {
        ParametersChecker.checkParameter(STREAM_IS_A_MANDATORY_PARAMETER, query);
        LOGGER.debug("get document format request:");
        final FileFormat file1 = createFileFormat(GUIDFactory.newGUID().toString());
        final FileFormat file2 = createFileFormat(GUIDFactory.newGUID().toString());
        final List<FileFormat> fileFormatList = new ArrayList<>();
        fileFormatList.add(file1);
        fileFormatList.add(file2);
        return JsonHandler.getFromString(fileFormatListToJsonString(fileFormatList));
    }

    @Override
    public Status checkRulesFile(InputStream stream) throws FileRulesException {
        ParametersChecker.checkParameter(STREAM_IS_A_MANDATORY_PARAMETER, stream);
        LOGGER.debug("Check file rules  request:");
        StreamUtils.closeSilently(stream);
        return Status.OK;
    }

    @Override
    public void importRulesFile(InputStream stream) throws FileRulesException, DatabaseConflictException {
        ParametersChecker.checkParameter(STREAM_IS_A_MANDATORY_PARAMETER, stream);
        LOGGER.debug("import file Rules request:");
        StreamUtils.closeSilently(stream);
    }

    @Override
    public void deleteRulesFile() throws FileRulesException {
        LOGGER.debug("Delete file rules request:");

    }

    @Override
    public JsonNode getRuleByID(String id) throws FileRulesException, InvalidParseOperationException {
        ParametersChecker.checkParameter(STREAM_IS_A_MANDATORY_PARAMETER, id);
        LOGGER.debug("get rule by id request:");
        final FileRules file = createFileRules(id);
        return JsonHandler.toJsonNode(file);
    }

    @Override
    public JsonNode getRules(JsonNode query)
        throws FileRulesException, InvalidParseOperationException, JsonGenerationException, JsonMappingException,
        IOException {
        ParametersChecker.checkParameter(STREAM_IS_A_MANDATORY_PARAMETER, query);
        LOGGER.debug("get document rules request:");

        final FileRules file1 = createFileRules(GUIDFactory.newGUID().toString());
        final FileRules file2 = createFileRules(GUIDFactory.newGUID().toString());
        final List<FileRules> fileRulesList = new ArrayList<>();
        fileRulesList.add(file1);
        fileRulesList.add(file2);
        return JsonHandler.getFromString(fileRulesListToJsonString(fileRulesList));
    }

    private String fileRulesListToJsonString(List<FileRules> fileRulesList)
        throws IOException, InvalidParseOperationException {
        return JsonHandler.writeAsString(fileRulesList);
    }

    @Override
    public void createorUpdateAccessionRegister(AccessionRegisterDetail register) throws DatabaseConflictException {
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
    public JsonNode getAccessionRegister(JsonNode query)
        throws InvalidParseOperationException, ReferentialException {
        ParametersChecker.checkParameter("stream is a mandatory parameter", query);
        LOGGER.debug("get document Register Fund request:");
        final AccessionRegisterSummary file1 = createAccessionRegister("1");
        final AccessionRegisterSummary file2 = createAccessionRegister("2");

        RequestResponseOK response = new RequestResponseOK();
        response.setHits(2, 0, 1);
        response.setQuery(null);
        response.addResult(JsonHandler.toJsonNode(file1));
        response.addResult(JsonHandler.toJsonNode(file2));
        return response.toJsonNode();
    }

    @Override
    public JsonNode getAccessionRegisterDetail(JsonNode query)
        throws InvalidParseOperationException, ReferentialException {
        ParametersChecker.checkParameter("stream is a mandatory parameter", query);
        LOGGER.debug("get document Accession Register request:");
        final AccessionRegisterDetail detail1 = createAccessionRegisterDetails("1");
        final AccessionRegisterDetail detail2 = createAccessionRegisterDetails("2");

        RequestResponseOK response = new RequestResponseOK();
        response.setHits(2, 0, 1);
        response.setQuery(null);
        response.addResult(JsonHandler.toJsonNode(detail1));
        response.addResult(JsonHandler.toJsonNode(detail2));
        return response.toJsonNode();
    }
}
