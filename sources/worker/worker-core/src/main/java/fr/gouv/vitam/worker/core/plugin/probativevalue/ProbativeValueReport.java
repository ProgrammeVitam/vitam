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
package fr.gouv.vitam.worker.core.plugin.probativevalue;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.functional.administration.common.BackupService;
import fr.gouv.vitam.functional.administration.common.exception.BackupServiceException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookOperation;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import static fr.gouv.vitam.common.json.JsonHandler.createJsonGenerator;


/**
 * ProbativeValueReport class
 */
public class ProbativeValueReport extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProbativeValueReport.class);

    private static final String PROBATIVE_VALUE_REPORTS = "PROBATIVE_VALUE_REPORTS";

    BackupService backupService = new BackupService();

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) {
        ItemStatus itemStatus = new ItemStatus(PROBATIVE_VALUE_REPORTS);

        File reportFile = handler.getNewLocalFile("report.json");

        try (FileOutputStream fileOutputStream = new FileOutputStream(reportFile);

            JsonGenerator jsonGenerator = createJsonGenerator(fileOutputStream);
            LogbookOperationsClient client = LogbookOperationsClientFactory.getInstance().getClient();
        ) {

            List<URI> uriListObjectsWorkspace =
                handler.getUriList(handler.getContainerName(), "reports");

            jsonGenerator.setPrettyPrinter(new MinimalPrettyPrinter(""));

            JsonNode jsonNode = client.selectOperationById(param.getContainerName());


            jsonGenerator.writeStartObject();

            jsonGenerator.writeFieldName("request");

            JsonNode request = JsonHandler.getFromFile(handler.getFileFromWorkspace("request" ));
            jsonGenerator.writeObject(request);


            gatherOperationInfo(param, jsonGenerator, jsonNode);

            operationAndDataChecks(handler, jsonGenerator, uriListObjectsWorkspace);

            reportForOperations(handler, jsonGenerator);

            jsonGenerator.writeEndObject();

            jsonGenerator.close();
            backupService
                .backup(new FileInputStream(reportFile), DataCategory.REPORT,
                    handler.getContainerName() + ".json");


        } catch (ContentAddressableStorageServerException |LogbookClientException| ContentAddressableStorageNotFoundException | IOException | InvalidParseOperationException | BackupServiceException |
            ProcessingException e) {

            LOGGER.error(e);
            return itemStatus.increment(StatusCode.FATAL);
        }
        itemStatus.increment(StatusCode.OK);
        return new ItemStatus(PROBATIVE_VALUE_REPORTS).setItemsStatus(PROBATIVE_VALUE_REPORTS, itemStatus);
    }

    public void gatherOperationInfo(WorkerParameters param, JsonGenerator jsonGenerator, JsonNode jsonNode)
        throws IOException {
        LogbookOperation logbookOperation =
            new LogbookOperation(jsonNode.get("$results").get(0));
        ObjectNode operation = JsonHandler.createObjectNode();
        operation.put("operationId", param.getContainerName());
        operation.put("operationDate",logbookOperation.getString("_lastPersistedDate"));
        operation.put("tenant",ParameterHelper.getTenantParameter());

        jsonGenerator.writeFieldName("OperationInfo");
        jsonGenerator.writeObject(operation);
    }

    public void operationAndDataChecks(HandlerIO handler, JsonGenerator jsonGenerator,
        List<URI> uriListObjectsWorkspace)
        throws IOException, ContentAddressableStorageNotFoundException, ContentAddressableStorageServerException,
        InvalidParseOperationException {

        jsonGenerator.writeFieldName("Versions Check Reports");

        jsonGenerator.writeStartArray();
        for (URI uri : uriListObjectsWorkspace) {


            jsonGenerator.writeStartObject();

            File file = handler.getFileFromWorkspace("reports" + File.separator + uri.getPath());

            ProbativeParameter parameter = JsonHandler.getFromFile(file, ProbativeParameter.class);
            // TODO iterate
          for(ProbativeUsageParameter usage  :   parameter.getUsageParameters().values()){

              binaryInfo(jsonGenerator, usage);

              binaryCheck(jsonGenerator, usage);

          }

            jsonGenerator.writeEndObject();



        }
        jsonGenerator.writeEndArray();

    }

    private void binaryInfo(JsonGenerator jsonGenerator, ProbativeUsageParameter parameter) throws IOException {


        jsonGenerator.writeFieldName("BinaryId");
        jsonGenerator.writeString(parameter.getVersionsModel().getId());

        jsonGenerator.writeFieldName("ObjectId");
        jsonGenerator.writeString(parameter.getVersionsModel().getDataObjectGroupId());

        jsonGenerator.writeFieldName("MessageDigest");
        jsonGenerator.writeString(parameter.getVersionsModel().getMessageDigest());


        jsonGenerator.writeFieldName("Algorithm");
        jsonGenerator.writeString(parameter.getVersionsModel().getAlgorithm());

        jsonGenerator.writeFieldName("binaryOpi");
        jsonGenerator.writeString(parameter.getVersionsModel().getOpi());

        jsonGenerator.writeFieldName("EvIdAppSession");
        jsonGenerator.writeString(parameter.getEvIdAppSession());

        jsonGenerator.writeFieldName("ArchivalAgreement");
        jsonGenerator.writeString(parameter.getArchivalAgreement());

        jsonGenerator.writeFieldName("SecuredOperationId");
        jsonGenerator.writeString(parameter.getSecuredOperationId());

        jsonGenerator.writeFieldName("SecureOperationIdForOpId");
        jsonGenerator.writeString(parameter.getSecureOperationIdForOpId());


    }

    private void binaryCheck(JsonGenerator jsonGenerator, ProbativeUsageParameter parameter) throws IOException {
        jsonGenerator.writeFieldName("Checks");
        jsonGenerator.writeStartArray();
        for (ProbativeCheckReport detail : parameter.getReports()) {
            jsonGenerator.writeObject(detail);

        }
        jsonGenerator.writeEndArray();
    }

    private void reportForOperations(HandlerIO handler, JsonGenerator jsonGenerator)
        throws ProcessingException, IOException, ContentAddressableStorageNotFoundException,
        ContentAddressableStorageServerException, InvalidParseOperationException {
        List<URI> uriListObjectsWorkspace;
        uriListObjectsWorkspace =
            handler.getUriList(handler.getContainerName(), "operationReport");

        jsonGenerator.writeFieldName("Operations Reports");

        jsonGenerator.writeStartArray();

        for (URI uri : uriListObjectsWorkspace) {
            File file = handler.getFileFromWorkspace("operationReport" + File.separator + uri.getPath());

            JsonNode operationReport = JsonHandler.getFromFile(file, JsonNode.class);
            jsonGenerator.writeObject(operationReport);
        }
        jsonGenerator.writeEndArray();
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
    }

}
