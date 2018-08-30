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

import static fr.gouv.vitam.common.json.JsonHandler.getFromFile;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.evidence.exception.EvidenceStatus;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

/**
 * EvidenceAuditListSecuredFiles class
 */
public class ProbativeValueListSecuredFiles extends ActionHandler {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ProbativeValueListSecuredFiles.class);

    private static final String PROBATIVE_VALUE_LIST_SECURED_FILES_TO_DOWNLOAD =
        "PROBATIVE_VALUE_LIST_SECURED_FILES_TO_DOWNLOAD";
    private static final String DATA = "data";


    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handlerIO)
        throws ProcessingException {
        ItemStatus itemStatus = new ItemStatus(PROBATIVE_VALUE_LIST_SECURED_FILES_TO_DOWNLOAD);

        List<URI> uriListObjectsWorkspace =
            handlerIO.getUriList(handlerIO.getContainerName(), DATA);

        Map<String, List<String>> secureOperationMap = new HashMap<>();

        Map<String, List<String>> secureOperationOpiMap = new HashMap<>();

        try {

            for (URI element : uriListObjectsWorkspace) {

                File file = handlerIO.getFileFromWorkspace(DATA + File.separator + element.getPath());


                ProbativeParameter parameters = getFromFile(file, ProbativeParameter.class);

                if (!parameters.getEvidenceStatus().equals(EvidenceStatus.OK)) {
                    continue;
                }

                for (ProbativeUsageParameter parameter : parameters.getUsageParameters().values()
                    ) {
                    addToSecureOperationMap(parameter.getSecuredOperationId(), element.toString(), secureOperationMap);

                    addToSecureOperationMap(parameter.getSecureOperationIdForOpId(),element.toString(), secureOperationOpiMap);
                }
            }



            transferMapElementsToWorkspace(handlerIO, secureOperationMap, "operation");

            transferMapElementsToWorkspace(handlerIO, secureOperationOpiMap, "operationForOpi");


        } catch (ContentAddressableStorageNotFoundException | InvalidParseOperationException | ContentAddressableStorageServerException | IOException e) {
            LOGGER.error(e);
            return itemStatus.increment(StatusCode.FATAL);

        }
        itemStatus.increment(StatusCode.OK);

        return new ItemStatus(PROBATIVE_VALUE_LIST_SECURED_FILES_TO_DOWNLOAD)
            .setItemsStatus(PROBATIVE_VALUE_LIST_SECURED_FILES_TO_DOWNLOAD, itemStatus);
    }

    private void transferMapElementsToWorkspace(HandlerIO handlerIO, Map<String, List<String>> securedFilenameMap,
        String type)
        throws ProcessingException, InvalidParseOperationException {

        Set<Entry<String, List<String>>> entrySet = securedFilenameMap.entrySet();

        for (Entry<String, List<String>> me : entrySet) {
            File file = handlerIO.getNewLocalFile(me.getKey());
            JsonHandler.writeAsFile(me.getValue(), file);
            handlerIO.transferFileToWorkspace(type + File.separator + me.getKey(), file, true, false);
        }
    }



    private void addToSecureOperationMap(String index, String element,
        Map<String, List<String>> map) {

        if (map.get(index) == null) {

            ArrayList<String> listIds = new ArrayList<>();

            listIds.add(element);

            map.put(index, listIds);

        } else {
            map.get(index).add(element);
        }
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException { /*Nothing to do */ }

}
