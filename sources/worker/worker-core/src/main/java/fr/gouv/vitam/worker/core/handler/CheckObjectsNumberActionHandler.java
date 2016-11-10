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

import java.net.URI;
import java.util.List;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.CompositeItemStatus;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.common.utils.ContainerExtractionUtils;
import fr.gouv.vitam.worker.common.utils.ContainerExtractionUtilsFactory;
import fr.gouv.vitam.worker.common.utils.ExtractUriResponse;
import fr.gouv.vitam.worker.common.utils.SedaUtils;
import fr.gouv.vitam.worker.common.utils.SedaUtilsFactory;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

/** 
 * Handler checking that digital objects number in workspace matches with manifest.xml.
 *
 */
public class CheckObjectsNumberActionHandler extends ActionHandler {

    /**
     * Use to log vitam
     */
    public static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CheckObjectsNumberActionHandler.class);

    /**
     * Handler's ID
     */
    private static final String HANDLER_ID = "CHECK_MANIFEST_OBJECTNUMBER";

    private final ContainerExtractionUtilsFactory containerExtractionUtilsFactory;
    private HandlerIO handlerIO;
    
    /**
     * Default Constructor
     */
    public CheckObjectsNumberActionHandler() {
        this.containerExtractionUtilsFactory = new ContainerExtractionUtilsFactory();
    }

    /**
     * Constructor for Junit Tests
     * 
     * @param containerExtractionUtilsFactory container Extraction utils factory
     */
    protected CheckObjectsNumberActionHandler(ContainerExtractionUtilsFactory containerExtractionUtilsFactory) {
        ParametersChecker.checkParameter("containerExtractionUtilsFactory is a mandatory parameter",
            containerExtractionUtilsFactory);
        this.containerExtractionUtilsFactory = containerExtractionUtilsFactory;
    }


    /**
     * @return HANDLER_ID
     */
    public static final String getId() {
        return HANDLER_ID;
    }

    @Override
    public CompositeItemStatus execute(WorkerParameters params, HandlerIO handlerIO) throws ContentAddressableStorageServerException {
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

        return new CompositeItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
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
        return sedaUtils.getAllDigitalObjectUriFromManifest(params);
    }


    /**
     * gets URI list of Digital object from the workspace, checks if there are duplicated URIs
     *
     * @param params worker parameter
     * @return List of uri
     * @throws ProcessingException throws when error in execution
     * @throws ContentAddressableStorageServerException 
     */
    private List<URI> getUriListFromWorkspace(WorkerParameters params) throws ProcessingException, ContentAddressableStorageServerException {
        final ContainerExtractionUtils containerExtractionUtils = containerExtractionUtilsFactory.create();
        return containerExtractionUtils.getDigitalObjectUriListFromWorkspace(params);
    }

    /**
     * Count the number of digital objects consistent between the manifest.xm file and the sip
     *
     * @param uriListManifest list of uri from manifest
     * @param uriListWorkspace list of uri from workspace
     * @param ItemStatus itemStatus of handler
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
            // TODO P0 REVIEW since you have List, Set, you should use direct method (removeAll, containAll, isEmpty, ...)
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


    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // TODO P0 Add Workspace:SIP/manifest.xml and check it
    }
}
