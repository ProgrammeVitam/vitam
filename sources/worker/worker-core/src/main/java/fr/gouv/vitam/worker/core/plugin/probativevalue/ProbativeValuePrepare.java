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

import static fr.gouv.vitam.common.json.JsonHandler.createJsonGenerator;
import static fr.gouv.vitam.common.json.JsonHandler.createObjectNode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.utils.ScrollSpliterator;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ProbativeValueRequest;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.core.database.configuration.GlobalDatasDb;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.distribution.ChainedFileWriter;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.ScrollSpliteratorHelper;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

/**
 * EvidenceAuditPrepare class
 */
public class ProbativeValuePrepare extends ActionHandler {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProbativeValuePrepare.class);

    private static final String PROBATIVE_VALUE_LIST_OBJECT = "PROBATIVE_VALUE_LIST_OBJECT";
    private static final String FIELDS_KEY = "$fields";
    private static final String CHAINED_FILE = "chainedFile.json";

    private MetaDataClientFactory metaDataClientFactory;
    private final int bachSize;
    private static final String OBJECT = "#object";

    public ProbativeValuePrepare() {
        this(MetaDataClientFactory.getInstance(), GlobalDatasDb.LIMIT_LOAD);
    }

    @VisibleForTesting
    public ProbativeValuePrepare(MetaDataClientFactory metaDataClientFactory, int bachSize) {
        this.metaDataClientFactory = metaDataClientFactory;
        this.bachSize = bachSize;
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handlerIO)
        throws ProcessingException {
        ItemStatus itemStatus = new ItemStatus(PROBATIVE_VALUE_LIST_OBJECT);
        try (MetaDataClient client = metaDataClientFactory.getClient()) {

            ProbativeValueRequest probativeValueRequest = JsonHandler
                .getFromInputStream(handlerIO.getInputStreamFromWorkspace("request"), ProbativeValueRequest.class);

            SelectMultiQuery select = constructSelectMultiQuery(probativeValueRequest);

            ScrollSpliterator<JsonNode> scrollRequest = ScrollSpliteratorHelper
                .createUnitScrollSplitIterator(client, select);

            parseRequestAndCreateLinkedFile(handlerIO, scrollRequest);

            if (ScrollSpliteratorHelper.checkNumberOfResultQuery(itemStatus, scrollRequest.estimateSize())) {
                return new ItemStatus(PROBATIVE_VALUE_LIST_OBJECT)
                    .setItemsStatus(PROBATIVE_VALUE_LIST_OBJECT, itemStatus);
            }

        } catch (InvalidParseOperationException | ContentAddressableStorageServerException | ContentAddressableStorageNotFoundException | IOException e) {
            LOGGER.error(e);
            return itemStatus.increment(StatusCode.FATAL);
        }

        itemStatus.increment(StatusCode.OK);
        return new ItemStatus(PROBATIVE_VALUE_LIST_OBJECT)
            .setItemsStatus(PROBATIVE_VALUE_LIST_OBJECT, itemStatus);
    }

    private SelectMultiQuery constructSelectMultiQuery(ProbativeValueRequest probativeValueRequest)
        throws InvalidParseOperationException {

        SelectParserMultiple parser = new SelectParserMultiple();

        parser.parse(probativeValueRequest.getDslQuery());

        SelectMultiQuery select = parser.getRequest();
        ObjectNode objectNode = createObjectNode();
        objectNode.put(VitamFieldsHelper.id(), 1);
        objectNode.put(VitamFieldsHelper.object(), 1);

        JsonNode projection = createObjectNode().set(FIELDS_KEY, objectNode);
        select.setProjection(projection);
        return select;
    }

    private void        parseRequestAndCreateLinkedFile(HandlerIO handlerIO,
        ScrollSpliterator<JsonNode> scrollRequest) throws ProcessingException {


        File report = handlerIO.getNewLocalFile("report.json");

        try (ChainedFileWriter chainedFileWriter = new ChainedFileWriter(handlerIO,
            "Object" + File.separator + CHAINED_FILE, bachSize);
            OutputStream outputStream = new FileOutputStream(report)) {

            JsonGenerator jsonGenerator = createJsonGenerator(outputStream);
            jsonGenerator.writeStartArray();

            StreamSupport.stream(scrollRequest, false).forEach(
                item -> {
                    if (item.get(OBJECT) != null) {
                        final String id = item.get(OBJECT).asText();
                        try {
                            chainedFileWriter.addEntry(id);
                            jsonGenerator.writeString(id);
                        } catch (IOException | InvalidParseOperationException e) {
                            throw new VitamRuntimeException(e);
                        }
                    }
                });

            jsonGenerator.writeEndArray();
            jsonGenerator.close();
        } catch (IOException | InvalidParseOperationException e) {
            throw new ProcessingException("Could not save linked files", e);
        }
        handlerIO.transferFileToWorkspace("objectIds.json", report, true, false);
    }


    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException { /* Nothing */ }
}
