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
package fr.gouv.vitam.worker.core.plugin.migration;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.utils.ScrollSpliterator;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.distribution.ChainedFileWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.stream.StreamSupport;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.exists;
import static fr.gouv.vitam.common.json.JsonHandler.createJsonGenerator;
import static fr.gouv.vitam.common.json.JsonHandler.createObjectNode;

/**
 * MigrationHelper class
 */
class MigrationHelper {

    private static final String FIELDS_KEY = "$fields";
    private static final String CHAINED_FILE = "chainedFile.json";
    private static final String ID = "#id";
    private static final String EVENTS_FIELDNAME = "events";

    /**
     * Create SelectMultiQuery for selecting Id
     *
     * @return the SelectMultiQuery
     * @throws InvalidParseOperationException InvalidParseOperationException
     * @throws InvalidCreateOperationException InvalidCreateOperationException
     */
    static SelectMultiQuery getSelectMultiQuery()
        throws InvalidParseOperationException, InvalidCreateOperationException {
        SelectMultiQuery selectMultiQuery = new SelectMultiQuery();
        selectMultiQuery.setQuery(exists(ID));

        ObjectNode objectNode = createObjectNode();
        objectNode.put(VitamFieldsHelper.id(), 1);
        objectNode.put(VitamFieldsHelper.object(), 1);
        JsonNode projection = createObjectNode().set(FIELDS_KEY, objectNode);
        selectMultiQuery.setProjection(projection);
        return selectMultiQuery;
    }

    /**
     * Create linked Files and save them in workspace
     *  @param scrollRequest scroll Request
     * @param handler the handler
     * @param folder the workspace folder to save into
     * @param bachSize linked file batch size
     */
    static void exportToReportAndLinkedFiles(
        final ScrollSpliterator<JsonNode> scrollRequest,
        final HandlerIO handler, final String folder, int bachSize, String reportFilename) throws ProcessingException {

        File report = handler.getNewLocalFile("report.json");

        try (ChainedFileWriter chainedFileWriter = new ChainedFileWriter(handler, folder + "/" + CHAINED_FILE, bachSize);
            OutputStream outputStream = new FileOutputStream(report);
            JsonGenerator jsonGenerator = createJsonGenerator(outputStream)) {
            jsonGenerator.writeStartArray();

            StreamSupport.stream(scrollRequest, false).forEach(
                item -> {
                    final String id = item.get(ID).asText();
                    try {
                        chainedFileWriter.addEntry(id);
                        jsonGenerator.writeString(id);
                    } catch (IOException | InvalidParseOperationException e) {
                        throw new VitamRuntimeException(e);
                    }
                });
            jsonGenerator.writeEndArray();
        } catch (IOException | InvalidParseOperationException e) {
            throw new ProcessingException("Could not save linked files", e);
        }

        handler.transferFileToWorkspace(reportFilename, report, true, false);
    }

    static boolean checkMigrationEvents(JsonNode lfc, String eventType ) {
        JsonNode lastEvent = null;
        if (lfc!=null && lfc.get(EVENTS_FIELDNAME)!= null) {
            lastEvent =  lfc.get(EVENTS_FIELDNAME).get((lfc.get(EVENTS_FIELDNAME)).size() - 1);
        }

        return (lastEvent != null && !lastEvent.get("evType").asText().equals(eventType));
    }
}
