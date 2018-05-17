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
package fr.gouv.vitam.worker.core.plugin.migration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.utils.ScrollSpliterator;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.model.ChainedFileModel;
import fr.gouv.vitam.worker.common.HandlerIO;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.StreamSupport;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.exists;
import static fr.gouv.vitam.common.json.JsonHandler.createObjectNode;

/**
 * MigrationHelper class
 */
class MigrationHelper {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MigrationHelper.class);

    private static final String FIELDS_KEY = "$fields";
    private static final String CHAINED_FILE = "chainedFile_";
    private static final String ID = "#id";
    private static final String JSON = ".json";



    /**
     * Create SelectMultiQuery for selecting Id
     *
     * @return the SelectMultiQuery
     * @throws InvalidParseOperationException  InvalidParseOperationException
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
     * @param handler       the handler
     * @param folder        the workspace folder to save into
     * @param bachSize
     */
    static void createAndSaveLinkedFilesInWorkSpaceFromScrollRequest(final ScrollSpliterator<JsonNode> scrollRequest,
        final HandlerIO handler, final String folder, int bachSize) {

        final AtomicInteger itemCounter = new AtomicInteger(0);
        final AtomicInteger chainedFileCounter = new AtomicInteger(0);

        final List<String> identifierLists = new ArrayList<>();

        StreamSupport.stream(scrollRequest, false).forEach(
            item -> {
                final int itemNumber = itemCounter.getAndIncrement();
                if (itemNumber == bachSize) {

                    createAndSaveChainedFiles(chainedFileCounter, identifierLists, handler, folder, false);
                    //Clear the list
                    identifierLists.clear();
                }

                final String identifier = item.get(ID).asText();
                // add to the list
                identifierLists.add(identifier);
            });

        createAndSaveChainedFiles(chainedFileCounter, identifierLists, handler, folder, true);
    }

    private static void createAndSaveChainedFiles(final AtomicInteger chainedFileCounter,
        final List<String> identifierLists,
        final HandlerIO handler, final String folder, boolean isLastLinkedFile) {

        // actual File
        final int numberOfActualChainedFile = chainedFileCounter.get();
        final String nameOfActualFile = CHAINED_FILE + numberOfActualChainedFile + JSON;

        // next File
        final int numberOfNextChainedFile = chainedFileCounter.incrementAndGet();
        final String nameOfNextFile = CHAINED_FILE + numberOfNextChainedFile + JSON;

        final ChainedFileModel model = new ChainedFileModel();

        model.setElements(identifierLists);

        if (!isLastLinkedFile) {
            model.setNextFile(nameOfNextFile);
        }

        final File file = handler.getNewLocalFile(nameOfActualFile);

        try {
            JsonHandler.writeAsFile(model, file);

            handler
                .transferFileToWorkspace(folder + "/" + nameOfActualFile,
                    file, true, false);

        } catch (InvalidParseOperationException | ProcessingException e) {
            LOGGER.error(e);
            throw new VitamRuntimeException(e);
        }
    }
}
