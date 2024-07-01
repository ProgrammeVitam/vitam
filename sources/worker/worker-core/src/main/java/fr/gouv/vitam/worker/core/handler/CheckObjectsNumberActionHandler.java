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
package fr.gouv.vitam.worker.core.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamKoRuntimeException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.VitamConstants;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.common.utils.ExtractUriResponse;
import fr.gouv.vitam.worker.common.utils.SedaUtils;
import fr.gouv.vitam.worker.common.utils.SedaUtilsFactory;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import org.apache.commons.collections4.SetUtils;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.model.VitamConstants.URL_ENCODED_SEPARATOR;

/**
 * Handler checking that digital objects number in workspace matches with manifest.xml.
 */
public class CheckObjectsNumberActionHandler extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CheckObjectsNumberActionHandler.class);

    /**
     * Handler's ID
     */
    private static final String HANDLER_ID = "CHECK_MANIFEST_OBJECTNUMBER";
    private static final String SUBTASK_INVALID_URI = "INVALID_URI";
    private static final String SUBTASK_MANIFEST_INFERIOR_BDO = "MANIFEST_INFERIOR_BDO";
    private static final String SUBTASK_MANIFEST_SUPERIOR_BDO = "MANIFEST_SUPERIOR_BDO";
    private static final String ERROR_IN_MANIFEST = "manifestError";
    private static final String ERROR_IN_CONTENT = "contentError";

    private final SedaUtilsFactory sedaUtilsFactory;

    public CheckObjectsNumberActionHandler() {
        this(SedaUtilsFactory.getInstance());
    }

    public CheckObjectsNumberActionHandler(SedaUtilsFactory sedaUtilsFactory) {
        this.sedaUtilsFactory = sedaUtilsFactory;
    }

    /**
     * @return HANDLER_ID
     */
    public static final String getId() {
        return HANDLER_ID;
    }

    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO handlerIO) {
        checkMandatoryParameters(params);

        final ItemStatus itemStatus = new ItemStatus(HANDLER_ID);

        try {
            checkMandatoryIOParameter(handlerIO);
            final ExtractUriResponse extractUriResponse = getUriListFromManifest(handlerIO);

            if (extractUriResponse != null && !extractUriResponse.isErrorDuplicateUri()) {
                final Set<URI> uriSetFromManifest = extractUriResponse.getUriSetManifest();
                final List<URI> uriListFromWorkspace = getUriListFromWorkspace(handlerIO, params);
                checkCountDigitalObjectConformity(uriSetFromManifest, uriListFromWorkspace, itemStatus);
            } else if (extractUriResponse != null) {
                itemStatus.increment(StatusCode.KO, extractUriResponse.getErrorNumber());
            }
        } catch (final VitamKoRuntimeException e) {
            ObjectNode evdev = JsonHandler.createObjectNode();
            evdev.put(SedaConstants.EV_DET_TECH_DATA, e.getMessage());
            itemStatus.setEvDetailData(JsonHandler.unprettyPrint(evdev));
            itemStatus.increment(StatusCode.KO);
        } catch (final ProcessingException | ContentAddressableStorageServerException e) {
            LOGGER.error(e);
            itemStatus.increment(StatusCode.FATAL);
        }

        return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
    }

    /**
     * gets URI list of Digital object from the workspace, checks if there are duplicated URIs
     *
     * @return ExtractUriResponse
     * @throws ProcessingException throws when error in execution
     */
    private ExtractUriResponse getUriListFromManifest(HandlerIO handlerIO) throws ProcessingException {
        // get uri list from manifest
        final SedaUtils sedaUtils = sedaUtilsFactory.createSedaUtilsWithSedaIngestParams(handlerIO);
        return sedaUtils.getAllDigitalObjectUriFromManifest();
    }

    /**
     * gets URI list of Digital object from the workspace, checks if there are duplicated URIs
     *
     * @param params worker parameter
     * @return List of uri
     * @throws ProcessingException throws when error in execution
     * @throws ContentAddressableStorageServerException
     */
    private List<URI> getUriListFromWorkspace(HandlerIO handlerIO, WorkerParameters params)
        throws ProcessingException, ContentAddressableStorageServerException {
        return getDigitalObjectUriListFromWorkspace(handlerIO, params);
    }

    /**
     * Count the number of digital objects consistent between the manifest.xm file and the sip
     *
     * @param uriManifestSet set of uri from manifest
     * @param uriListWorkspace list of uri from workspace
     * @param itemStatus itemStatus of handler
     * @throws ProcessingException will be throwed when one or all arguments is null
     */
    private void checkCountDigitalObjectConformity(
        Set<URI> uriManifestSet,
        List<URI> uriListWorkspace,
        ItemStatus itemStatus
    ) throws ProcessingException {
        ParametersChecker.checkParameter("Manifest uri set is a mandatory parameter", uriManifestSet);
        ParametersChecker.checkParameter("Workspace uri list is a mandatory parameter", uriListWorkspace);
        ParametersChecker.checkParameter("ItemStatus is a mandatory parameter", itemStatus);

        HashSet<URI> uriWorkspaceSet = new HashSet<>(uriListWorkspace);

        Set<URI> notInWorkspace = SetUtils.difference(uriManifestSet, uriWorkspaceSet).toSet();
        Set<URI> notInManifest = SetUtils.difference(uriWorkspaceSet, uriManifestSet).toSet();

        // The number of object in manifest is the reference for number of OK
        int errorCount = notInWorkspace.size() + notInManifest.size();
        int okCount = uriWorkspaceSet.size() - notInManifest.size();

        if (errorCount == 0) {
            itemStatus.increment(StatusCode.OK, okCount);
        } else {
            itemStatus.increment(StatusCode.OK, okCount);
            itemStatus.increment(StatusCode.KO, errorCount);

            String status;
            if (uriManifestSet.size() > uriListWorkspace.size()) {
                status = SUBTASK_MANIFEST_SUPERIOR_BDO;
            } else if (uriManifestSet.size() < uriListWorkspace.size()) {
                status = SUBTASK_MANIFEST_INFERIOR_BDO;
            } else {
                status = SUBTASK_INVALID_URI;
            }

            updateDetailItemStatus(
                itemStatus,
                getMessageItemStatusInvalidURIandIncorrectTotals(notInWorkspace, notInManifest),
                status
            );
        }

        itemStatus.setData("errorNumber", errorCount);
    }

    private String getMessageItemStatusInvalidURIandIncorrectTotals(Set<URI> notInWorkspace, Set<URI> notInManifest) {
        ObjectNode error = JsonHandler.createObjectNode();

        if (!notInWorkspace.isEmpty()) {
            ArrayNode errorDetailsManifest = JsonHandler.createArrayNode();
            notInWorkspace.forEach(uri -> {
                errorDetailsManifest.add(uri.getPath());
            });
            error.set(ERROR_IN_MANIFEST, errorDetailsManifest);
        }
        if (!notInManifest.isEmpty()) {
            ArrayNode errorDetailsContent = JsonHandler.createArrayNode();
            notInManifest.forEach(uri -> {
                errorDetailsContent.add(uri.getPath());
            });
            error.set(ERROR_IN_CONTENT, errorDetailsContent);
        }
        return JsonHandler.unprettyPrint(error);
    }

    /**
     * get the uri list of digital object from a container into the workspace *
     *
     * @param workParams parameters of workspace server
     * @return List of Uri
     * @throws ProcessingException - throw when workspace is unavailable.
     * @throws ContentAddressableStorageServerException
     */
    private List<URI> getDigitalObjectUriListFromWorkspace(HandlerIO handlerIO, WorkerParameters workParams)
        throws ProcessingException, ContentAddressableStorageServerException {
        try (final WorkspaceClient workspaceClient = handlerIO.getWorkspaceClientFactory().getClient()) {
            // FIXME P1: We have to count here from SIP/Content and not from SIP
            // Remove one element is actually, with our SIP correct but not really true because it is not necessary
            // the manifest.xml and it is possible to have more than one file on the root SIP folder (it was removed
            // without check).
            // Cannot fix this now because, actually we have SIP archive with "Content" or "content" and the file system
            // is case sensitive (theoretically it's "Content" with upper 'c').
            // To fix this, uncomment the next line and remove what is comming next.
            // return workspaceClient.getListUriDigitalObjectFromFolder(workParams.getContainerName(), VitamConstants
            // .CONTENT_SIP_FOLDER);
            final List<URI> uriListWorkspace = JsonHandler.getFromStringAsTypeReference(
                workspaceClient
                    .getListUriDigitalObjectFromFolder(workParams.getContainerName(), VitamConstants.SIP_FOLDER)
                    .toJsonNode()
                    .get("$results")
                    .get(0)
                    .toString(),
                new TypeReference<List<URI>>() {}
            );
            // FIXME P1: Ugly hack to remove (see above), just keep URI with "/" to avoid manifest.xml
            return uriListWorkspace
                .stream()
                .filter(uri -> uri.toString().contains(URL_ENCODED_SEPARATOR))
                .collect(Collectors.toList());
        } catch (InvalidParseOperationException | InvalidFormatException e) {
            throw new ProcessingException(e);
        }
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {}
}
