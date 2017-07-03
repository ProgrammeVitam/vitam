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
package fr.gouv.vitam.worker.core.handler;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;

import fr.gouv.vitam.common.CharsetUtils;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
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
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

/**
 * Handler checking that digital objects number in workspace matches with manifest.xml.
 *
 */
public class CheckObjectsNumberActionHandler extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CheckObjectsNumberActionHandler.class);

    /**
     * Handler's ID
     */
    private static final String HANDLER_ID = "CHECK_MANIFEST_OBJECTNUMBER";

    private HandlerIO handlerIO;

    /**
     * Default Constructor
     */
    public CheckObjectsNumberActionHandler() {
        // Nothing
    }

    /**
     * @return HANDLER_ID
     */
    public static final String getId() {
        return HANDLER_ID;
    }

    @Override
    public ItemStatus execute(WorkerParameters params, HandlerIO handlerIO)
        throws ContentAddressableStorageServerException {
        checkMandatoryParameters(params);

        final ItemStatus itemStatus = new ItemStatus(HANDLER_ID);

        try {
            checkMandatoryIOParameter(handlerIO);
            this.handlerIO = handlerIO;
            final ExtractUriResponse extractUriResponse = getUriListFromManifest(params);

            if (extractUriResponse != null && !extractUriResponse.isErrorDuplicateUri()) {

                final List<URI> uriListFromManifest = extractUriResponse.getUriListManifest();
                final List<URI> uriListFromWorkspace = getUriListFromWorkspace(params);
                checkCountDigitalObjectConformity(uriListFromManifest, uriListFromWorkspace, itemStatus);

            } else if (extractUriResponse != null) {
                itemStatus.increment(StatusCode.KO, extractUriResponse.getErrorNumber());
            }

        } catch (final ProcessingException e) {
            LOGGER.error(e);
            itemStatus.increment(StatusCode.FATAL);
        }

        return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
    }


    /**
     * gets URI list of Digital object from the workspace, checks if there are duplicated URIs
     *
     * @param params worker parameter
     * @return ExtractUriResponse
     * @throws ProcessingException throws when error in execution
     */
    private ExtractUriResponse getUriListFromManifest(WorkerParameters params)
        throws ProcessingException {
        // get uri list from manifest
        final SedaUtils sedaUtils = SedaUtilsFactory.create(handlerIO);
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
    private List<URI> getUriListFromWorkspace(WorkerParameters params)
        throws ProcessingException, ContentAddressableStorageServerException {
        return getDigitalObjectUriListFromWorkspace(params);
    }

    /**
     * Count the number of digital objects consistent between the manifest.xm file and the sip
     *
     * @param uriListManifest list of uri from manifest
     * @param uriListWorkspace list of uri from workspace
     * @param itemStatus itemStatus of handler
     * @throws ProcessingException will be throwed when one or all arguments is null
     */
    private void checkCountDigitalObjectConformity(List<URI> uriListManifest, List<URI> uriListWorkspace,
        ItemStatus itemStatus) throws ProcessingException {
        ParametersChecker.checkParameter("Manifest uri list is a mandatory parameter", uriListManifest);
        ParametersChecker.checkParameter("Workspace uri list is a mandatory parameter", uriListWorkspace);
        ParametersChecker.checkParameter("ItemStatus is a mandatory parameter", itemStatus);
        // TODO P1
        // Use Java 8, Methods Reference, lambda expressions and streams

        /**
         * compare the size between list uri from manifest and list uri from workspace.
         */
        int countCompare = Math.abs(uriListManifest.size() - uriListWorkspace.size());

        if (countCompare > 0) {
            // The number of object in manifest is the reference for number of OK
            if (uriListManifest.size() > uriListWorkspace.size()) {
                itemStatus.increment(StatusCode.OK, uriListManifest.size() - countCompare);
            } else {
                itemStatus.increment(StatusCode.OK, uriListManifest.size());
            }
            itemStatus.increment(StatusCode.KO, countCompare);
        } else {

            /**
             * count the number of object in the manifest found in the sip
             */
            int countConsistentDigitalObjectFromManifest = 0;
            /**
             * count the number of digital object in the sip found in the manifest
             */
            int countConsistentDigitalObjectFromWorkspace = 0;
            // TODO P0 REVIEW since you have List, Set, you should use direct method (removeAll, containAll, isEmpty,
            // ...)
            // faster and better
            for (final URI uriManifest : uriListManifest) {
                if (uriListWorkspace.contains(uriManifest)) {
                    countConsistentDigitalObjectFromManifest++;
                } else {
                    countCompare++;
                }
            }

            for (final URI uriWorkspace : uriListWorkspace) {
                if (uriListManifest.contains(uriWorkspace)) {
                    countConsistentDigitalObjectFromWorkspace++;
                } else {
                    countCompare++;
                }

            }

            final boolean countOK =
                countConsistentDigitalObjectFromManifest == countConsistentDigitalObjectFromWorkspace;
            final boolean countConsistent = countConsistentDigitalObjectFromManifest == uriListManifest.size();

            if (countOK && countConsistent) {
                itemStatus.increment(StatusCode.OK, uriListManifest.size());

            } else {
                // The number of object in manifest is the reference for number of OK
                itemStatus.increment(StatusCode.OK, countConsistentDigitalObjectFromManifest);
                itemStatus.increment(StatusCode.KO, countCompare);
            }
        }
        itemStatus.setData("errorNumber", countCompare);
    }

    /**
     * get the uri list of digital object from a container into the workspace *
     *
     * @param workParams parameters of workspace server
     * @return List of Uri
     * @throws ProcessingException - throw when workspace is unavailable.
     * @throws ContentAddressableStorageServerException
     *
     */
    private List<URI> getDigitalObjectUriListFromWorkspace(WorkerParameters workParams)
        throws ProcessingException, ContentAddressableStorageServerException {
        try (final WorkspaceClient workspaceClient = WorkspaceClientFactory.getInstance().getClient()) {

            // FIXME P1: We have to count here from SIP/Content and not from SIP
            // Remove one element is actually, with our SIP correct but not really true because it is not necessary
            // the manifest.xml and it is possible to have more than one file on the root SIP folder (it was removed
            // without check).
            // Cannot fix this now because, actually we have SIP archive with "Content" or "content" and the file system
            // is case sensitive (theoretically it's "Content" with upper 'c').
            // To fix this, uncomment the next line and remove what is comming next.
            // return workspaceClient.getListUriDigitalObjectFromFolder(workParams.getContainerName(), VitamConstants
            // .CONTENT_SIP_FOLDER);

            String encodedSeparator = URLEncoder.encode("/", CharsetUtils.UTF_8);

            final List<URI> uriListWorkspace =
                JsonHandler.getFromStringAsTypeRefence(workspaceClient
                    .getListUriDigitalObjectFromFolder(workParams.getContainerName(), VitamConstants.SIP_FOLDER)
                    .toJsonNode().get("$results").get(0).toString(), new TypeReference<List<URI>>() {});
            // FIXME P1: Ugly hack to remove (see above), just keep URI with "/" to avoid manifest.xml
            return uriListWorkspace.stream().filter(uri -> uri.toString().contains(encodedSeparator)).collect(Collectors
                .toList());
        } catch (InvalidParseOperationException | UnsupportedEncodingException e) {
            throw new ProcessingException(e);
        }
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // TODO P0 Add Workspace:SIP/manifest.xml and check it
    }
}
