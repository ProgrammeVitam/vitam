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
package fr.gouv.vitam.worker.core.plugin.evidence;

import com.fasterxml.jackson.databind.JsonNode;
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
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import static fr.gouv.vitam.common.json.JsonHandler.getFromFile;

/**
 * EvidenceAuditListSecuredFiles class
 */
public class EvidenceAuditListSecuredFiles extends ActionHandler {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(EvidenceAuditListSecuredFiles.class);

    private static final String EVIDENCE_AUDIT_LIST_SECURED_FILES_TO_DOWNLOAD =
        "EVIDENCE_AUDIT_LIST_SECURED_FILES_TO_DOWNLOAD";
    private static final String DATA = "data";
    private static final String FILE_NAME = "FileName";

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handlerIO)
        throws ProcessingException {
        ItemStatus itemStatus = new ItemStatus(EVIDENCE_AUDIT_LIST_SECURED_FILES_TO_DOWNLOAD);

        List<URI> uriListObjectsWorkspace =
            handlerIO.getUriList(handlerIO.getContainerName(), DATA);
        Map<String, List<String>> securedFilenameList = new HashMap<>();

        try {

            for (URI element : uriListObjectsWorkspace) {
                File file = handlerIO.getFileFromWorkspace(DATA + File.separator + element.getPath());

                JsonNode jsonName = getFromFile(file);
                JsonNode fileNameNode = jsonName.get(FILE_NAME);
                if (fileNameNode == null) {
                    continue;
                }
                String identifier = fileNameNode.textValue();
                if (securedFilenameList.get(identifier) == null) {
                    ArrayList<String> listIds = new ArrayList<>();
                    listIds.add(element.toString());
                    securedFilenameList.put(identifier, listIds);
                    continue;
                }
                securedFilenameList.get(identifier).add(element.toString());
            }
            Set<Entry<String, List<String>>> entrySet = securedFilenameList.entrySet();

            for (Entry<String, List<String>> me : entrySet) {
                File file = handlerIO.getNewLocalFile(me.getKey());
                JsonHandler.writeAsFile(me.getValue(), file);
                handlerIO.transferFileToWorkspace("fileNames" + "/" + me.getKey(), file, true, false);
            }

        } catch (ContentAddressableStorageNotFoundException | InvalidParseOperationException | ContentAddressableStorageServerException | IOException e) {
            LOGGER.error(e);

            return itemStatus.increment(StatusCode.FATAL);

        }

        itemStatus.increment(StatusCode.OK);
        return new ItemStatus(EVIDENCE_AUDIT_LIST_SECURED_FILES_TO_DOWNLOAD)
            .setItemsStatus(EVIDENCE_AUDIT_LIST_SECURED_FILES_TO_DOWNLOAD, itemStatus);
    }


}
