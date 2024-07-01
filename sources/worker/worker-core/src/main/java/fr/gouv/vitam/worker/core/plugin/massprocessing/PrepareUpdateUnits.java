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
package fr.gouv.vitam.worker.core.plugin.massprocessing;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.database.builder.request.configuration.GlobalDatas;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.utils.ScrollSpliterator;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.worker.core.distribution.JsonLineWriter;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.ScrollSpliteratorHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.stream.StreamSupport;

/**
 * Prepare the archive units to update
 */
public class PrepareUpdateUnits extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ActionHandler.class);

    /**
     * PREPARE_UPDATE_UNIT_LIST
     */
    private static final String PREPARE_UPDATE_UNIT_LIST = "PREPARE_UPDATE_UNIT_LIST";

    /**
     * DISTRIBUTION_FILE_RANK
     */
    private static final int DISTRIBUTION_FILE_RANK = 0;

    /**
     * metaDataClientFactory
     */
    private final MetaDataClientFactory metaDataClientFactory;

    /**
     * Batch size
     */
    private final int batchSize;

    /**
     * Constructor.
     */
    public PrepareUpdateUnits() {
        this(MetaDataClientFactory.getInstance(), GlobalDatas.LIMIT_LOAD);
    }

    /**
     * Constructor.
     *
     * @param metaDataClientFactory
     */
    @VisibleForTesting
    PrepareUpdateUnits(MetaDataClientFactory metaDataClientFactory, int batchSize) {
        this.metaDataClientFactory = metaDataClientFactory;
        this.batchSize = batchSize;
    }

    /**
     * Execute an action
     *
     * @param param {@link WorkerParameters}
     * @param handler the handlerIo
     * @return CompositeItemStatus:response contains a list of functional message and status code
     * @throws ProcessingException if an error is encountered when executing the action
     */
    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) throws ProcessingException {
        final ItemStatus itemStatus = new ItemStatus(PREPARE_UPDATE_UNIT_LIST);

        try (MetaDataClient client = metaDataClientFactory.getClient()) {
            // parse multi select query
            JsonNode queryNode = handler.getJsonFromWorkspace("query.json");
            SelectMultiQuery multiQuery = getSelectQueryFromJson(queryNode);

            // get distribution file name
            final String distribFileName = handler.getOutput(DISTRIBUTION_FILE_RANK).getPath();
            final File distribFile = handler.getNewLocalFile(distribFileName);

            // create spliterator based on the select query
            final ScrollSpliterator<JsonNode> scrollRequest = ScrollSpliteratorHelper.createUnitScrollSplitIterator(
                client,
                multiQuery,
                batchSize
            );

            // create temporary file with units as JSONL
            createDistributionFile(scrollRequest, distribFile);

            // move file to workspace
            handler.transferFileToWorkspace(distribFileName, distribFile, true, false);

            // set status OK
            itemStatus.increment(StatusCode.OK);
        } catch (InvalidParseOperationException | ProcessingException e) {
            LOGGER.error(e);
            itemStatus.increment(StatusCode.FATAL);
        }
        return new ItemStatus(PREPARE_UPDATE_UNIT_LIST).setItemsStatus(PREPARE_UPDATE_UNIT_LIST, itemStatus);
    }

    /**
     * create distribution file
     *
     * @param scrollRequest
     * @param distribFile
     * @throws ProcessingException
     */
    private void createDistributionFile(final ScrollSpliterator<JsonNode> scrollRequest, File distribFile)
        throws ProcessingException {
        try (JsonLineWriter jsonLineWriter = new JsonLineWriter(new FileOutputStream(distribFile))) {
            StreamSupport.stream(scrollRequest, false).forEach(item -> {
                try {
                    jsonLineWriter.addEntry(getJsonLineForItem(item));
                } catch (IOException e) {
                    throw new VitamRuntimeException(e);
                }
            });
        } catch (IOException | VitamRuntimeException | IllegalStateException e) {
            throw new ProcessingException("Could not generate and save file", e);
        }
    }

    private JsonLineModel getJsonLineForItem(JsonNode item) {
        return new JsonLineModel(item.get("#id").textValue(), null, null);
    }

    /**
     * getUpdateQueryFromJson
     *
     * @param queryNode
     * @return
     * @throws InvalidParseOperationException
     */
    private SelectMultiQuery getSelectQueryFromJson(JsonNode queryNode) throws InvalidParseOperationException {
        // parse multi query
        SelectParserMultiple parser = new SelectParserMultiple();
        parser.parse(queryNode);
        SelectMultiQuery multiQuery = parser.getRequest();

        // set projection to get only the id
        multiQuery.setProjection(JsonHandler.getFromString("{\"$fields\": { \"#id\": 1}}"));

        return multiQuery;
    }
}
