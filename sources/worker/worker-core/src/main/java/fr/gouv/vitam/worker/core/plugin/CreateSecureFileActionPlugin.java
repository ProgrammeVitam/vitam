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
package fr.gouv.vitam.worker.core.plugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.StringJoiner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Iterables;

import fr.gouv.vitam.common.BaseXx;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookDocument;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleObjectGroup;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookLifeCycleUnit;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetadataInvalidSelectException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;

/**
 * CreateSecureFileAction Plugin.<br>
 *
 */
public abstract class CreateSecureFileActionPlugin extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CreateSecureFileActionPlugin.class);
    private static final String JSON_EXTENSION = ".json";
    private static final String SEPARATOR = " | ";
    private static final String COMA_SEPARATOR = ",";
    private static final String RESULTS = "$results";
    
    
    protected void storeLifecycle(JsonNode lifecycle, String lfGuid, HandlerIO handlerIO, String lifecycleType)
        throws ProcessingException {
        String folder = "";
        final File lifecycleGlobalTmpFile = handlerIO.getNewLocalFile(lfGuid);
        try (FileWriter fw = new FileWriter(lifecycleGlobalTmpFile);) {
            ArrayNode events = (ArrayNode) lifecycle.get(LogbookDocument.EVENTS);
            JsonNode lastEvent = (JsonNode) Iterables.getLast(events);
            String objectGroupId = lastEvent.get(LogbookMongoDbName.objectIdentifier.getDbname()).asText();
            String finalOutcome = lastEvent.get(LogbookMongoDbName.outcome.getDbname()).asText();
            String finalEvDateTime = lastEvent.get(LogbookMongoDbName.eventDateTime.getDbname()).asText();
            String hashLFCOgOrUnit = null;
            String specificListObjects = null;
            if (LogbookLifeCycleUnit.class.getName().equals(lifecycleType)) {
                folder = SedaConstants.LFC_UNITS_FOLDER;
                JsonNode unit = selectArchiveUnitById(objectGroupId);
                if (unit != null) {
                    hashLFCOgOrUnit = BaseXx.getBase64(JsonHandler.unprettyPrint(unit).getBytes());
                }
            } else if (LogbookLifeCycleObjectGroup.class.getName().equals(lifecycleType)) {
                folder = SedaConstants.LFC_OBJECTS_FOLDER;
                JsonNode og = selectObjectGroupById(objectGroupId);
                if (og != null) {
                    hashLFCOgOrUnit = BaseXx.getBase64(JsonHandler.unprettyPrint(og).getBytes());
                    specificListObjects = extractListObjectsFromJson(og);
                }
            }

            final String logbookLFCStr = JsonHandler.unprettyPrint(lifecycle);
            final String hashGlobalLFC = BaseXx.getBase64(logbookLFCStr.getBytes());


            fw.write(lifecycle.get(LogbookMongoDbName.eventIdentifierProcess.getDbname()).asText());
            fw.write(SEPARATOR);
            fw.write(lifecycle.get(LogbookMongoDbName.eventTypeProcess.getDbname()).asText());
            fw.write(SEPARATOR);
            fw.write(finalEvDateTime);
            fw.write(SEPARATOR);
            fw.write(lifecycle.get("_id").asText());
            fw.write(SEPARATOR);
            fw.write(finalOutcome);
            fw.write(SEPARATOR);
            fw.write(hashGlobalLFC);
            fw.write(SEPARATOR);
            fw.write(hashLFCOgOrUnit);
            if (specificListObjects != null) {
                fw.write(SEPARATOR);
                fw.write(specificListObjects);
            }
            fw.flush();
            fw.close();
            // TODO : this is not a json file
            handlerIO
                .transferFileToWorkspace(folder + "/" + lfGuid + JSON_EXTENSION,
                    lifecycleGlobalTmpFile, true, false);
        } catch (IOException e) {
            throw new ProcessingException("Could not write file", e);
        }
    }

    private JsonNode selectArchiveUnitById(String archiveUnitId) throws ProcessingException {
        try (MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient()) {
            final SelectParserMultiple selectRequest = new SelectParserMultiple();
            final SelectMultiQuery request = selectRequest.getRequest().reset();
            JsonNode existingArchiveUnit =
                metadataClient.selectUnitbyId(request.getFinalSelect(), archiveUnitId);

            if (existingArchiveUnit == null || existingArchiveUnit.get(RESULTS) == null ||
                existingArchiveUnit.get(RESULTS).size() == 0) {
                LOGGER.error("Existing archiveUnit " + archiveUnitId + " was not found {}", existingArchiveUnit);
                throw new ProcessingException("Existing archiveUnit " + archiveUnitId + " was not found");
            }
            return existingArchiveUnit.get(RESULTS).get(0);
        } catch (MetaDataExecutionException | MetaDataDocumentSizeException |
            MetaDataClientServerException | InvalidParseOperationException e) {
            throw new ProcessingException(e);
        }
    }

    private String extractListObjectsFromJson(JsonNode og) {
        JsonNode qualifiers = og.get("#qualifiers");
        if (qualifiers != null && qualifiers.isArray() && qualifiers.size() > 0) {
            StringJoiner sj = new StringJoiner(COMA_SEPARATOR, "[", "]");
            List<JsonNode> listQualifiers = JsonHandler.toArrayList((ArrayNode) qualifiers);
            for (final JsonNode qualifier : listQualifiers) {
                JsonNode versions = qualifier.get(SedaConstants.TAG_VERSIONS);
                if (versions.isArray() && versions.size() > 0) {
                    for (final JsonNode version : versions) {
                        sj.add("{\"id\":" + version.get("_id") +
                            ", \"object\": \"" + BaseXx.getBase64(JsonHandler.unprettyPrint(version).getBytes()) +
                            "\"}");
                    }
                }
            }
            return sj.toString();
        }
        return null;
    }

    private JsonNode selectObjectGroupById(String objectGroupId) throws ProcessingException {
        try (MetaDataClient metadataClient = MetaDataClientFactory.getInstance().getClient()) {
            final SelectParserMultiple selectRequest = new SelectParserMultiple();
            final SelectMultiQuery request = selectRequest.getRequest().reset();
            JsonNode existingObjectGroup =
                metadataClient.selectObjectGrouptbyId(request.getFinalSelect(), objectGroupId);

            if (existingObjectGroup == null || existingObjectGroup.get(RESULTS) == null ||
                existingObjectGroup.get(RESULTS).size() == 0) {
                LOGGER.error("Existing ObjectGroup " + objectGroupId + " was not found {}", existingObjectGroup);
                throw new ProcessingException("Existing ObjectGroup " + objectGroupId + " was not found");
            }
            return existingObjectGroup.get(RESULTS).get(0);
        } catch (MetaDataExecutionException | MetaDataDocumentSizeException | MetadataInvalidSelectException |
            MetaDataClientServerException | InvalidParseOperationException e) {
            throw new ProcessingException(e);
        }
    }

}
