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
package fr.gouv.vitam.processing.worker.handler;

import java.net.URI;
import java.util.List;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.EngineResponse;
import fr.gouv.vitam.processing.common.model.OutcomeMessage;
import fr.gouv.vitam.processing.common.model.ProcessResponse;
import fr.gouv.vitam.processing.common.model.StatusCode;
import fr.gouv.vitam.processing.common.model.WorkParams;
import fr.gouv.vitam.processing.common.utils.ContainerExtractionUtils;
import fr.gouv.vitam.processing.common.utils.ContainerExtractionUtilsFactory;
import fr.gouv.vitam.processing.common.utils.ExtractUriResponse;
import fr.gouv.vitam.processing.common.utils.SedaUtils;
import fr.gouv.vitam.processing.common.utils.SedaUtilsFactory;

/**
 * Handler that checks that the number of digital objects stored in the workspace equals the number of digital objects
 * referenced in the manifest.xml file
 */
public class CheckObjectsNumberActionHandler extends ActionHandler {

    /**
     * Use to log vitam
     */
    public static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CheckObjectsNumberActionHandler.class);

    /**
     * Handler's ID
     */
    private static final String HANDLER_ID = "CheckObjectsNumber";

    private final SedaUtilsFactory sedaUtilsFactory;
    private final ContainerExtractionUtilsFactory containerExtractionUtilsFactory;


    /**
     * @param sedaUtilsFactory  sedaUtils factory 
     * @param containerExtractionUtilsFactory container Extraction utils factory
     */
    public CheckObjectsNumberActionHandler(SedaUtilsFactory sedaUtilsFactory,
        ContainerExtractionUtilsFactory containerExtractionUtilsFactory) {
        ParametersChecker.checkParameter("sedaUtilsFactory is a mandatory parameter", sedaUtilsFactory);
        ParametersChecker.checkParameter("containerExtractionUtilsFactory is a mandatory parameter", containerExtractionUtilsFactory);
        this.sedaUtilsFactory = sedaUtilsFactory;
        this.containerExtractionUtilsFactory = containerExtractionUtilsFactory;
    }


    /**
     * @return HANDLER_ID
     */
    public static final String getId() {
        return HANDLER_ID;
    }

    @Override
    public EngineResponse execute(WorkParams params) {
        ParametersChecker.checkParameter("params is a mandatory parameter", params);
        ParametersChecker.checkParameter("ServerConfiguration is a mandatory parameter",
            params.getServerConfiguration());
        LOGGER.info("CheckObjectsNumberActionHandler running ...");

        EngineResponse response = new ProcessResponse();
        response.setStatus(StatusCode.OK).setOutcomeMessages(HANDLER_ID, OutcomeMessage.CHECK_OBJECT_NUMBER_OK);

        try {

            final ExtractUriResponse extractUriResponse = getUriListFromManifest(params);

            if (extractUriResponse != null && !extractUriResponse.isErrorDuplicateUri()) {

                final List<URI> uriListFromManifest = extractUriResponse.getUriListManifest();
                final List<URI> uriListFromWorkspace = getUriListFromWorkspace(params);
                checkCountDigitalObjectConformity(uriListFromManifest, uriListFromWorkspace, response);

            } else if (extractUriResponse != null) {
                response.setStatus(StatusCode.KO)
                .setErrorNumber(extractUriResponse.getErrorNumber())
                .setOutcomeMessages(HANDLER_ID, OutcomeMessage.CHECK_OBJECT_NUMBER_KO);
            }

        } catch (ProcessingException e) {
            response.setStatus(StatusCode.FATAL).setOutcomeMessages(HANDLER_ID, OutcomeMessage.CHECK_OBJECT_NUMBER_KO);
        }

        return response;

    }


    /**
     * gets URI list of Digital object from the workspace, checks if there are duplicated URIs
     *
     * @param params worker parameter
     * @return ExtractUriResponse
     * @throws ProcessingException throws when error in execution
     */
    private ExtractUriResponse getUriListFromManifest(WorkParams params)
        throws ProcessingException {
        // get uri list from manifest
        final SedaUtils sedaUtils = sedaUtilsFactory.create();
        return sedaUtils.getAllDigitalObjectUriFromManifest(params);
    }


    /**
     * gets URI list of Digital object from the workspace, checks if there are duplicated URIs
     *
     * @param params worker parameter
     * @return List of uri
     * @throws ProcessingException throws when error in execution
     */
    private List<URI> getUriListFromWorkspace(WorkParams params) throws ProcessingException {
        final ContainerExtractionUtils containerExtractionUtils = containerExtractionUtilsFactory.create();
        return containerExtractionUtils.getDigitalObjectUriListFromWorkspace(params);
    }

    /**
     * Count the number of digital objects consistent between the manifest.xm file and the sip
     *
     * @param uriListManifest list of uri from manifest
     * @param uriListWorkspace list of uri from workspace
     * @param response of handler
     * @throws ProcessingException will be throwed when one or all arguments is null
     */
    private void checkCountDigitalObjectConformity(List<URI> uriListManifest, List<URI> uriListWorkspace,
        EngineResponse response) throws ProcessingException {
        ParametersChecker.checkParameter("Manifest uri list is a mandatory parameter", uriListManifest);
        ParametersChecker.checkParameter("Workspace uri list is a mandatory parameter", uriListWorkspace);
        ParametersChecker.checkParameter("EngineResponse is a mandatory parameter", response);
        // TODO
        // Use Java 8, Methods Reference, lambda expressions and streams

        /**
         * compare the size between list uri from manifest and list uri from workspace.
         */
        int countCompare = Math.abs(uriListManifest.size() - uriListWorkspace.size());
        
        if (countCompare > 0) {
            response.setStatus(StatusCode.KO)
            .setOutcomeMessages(HANDLER_ID, OutcomeMessage.CHECK_OBJECT_NUMBER_KO);
        } else {

            /**
             * count the number of object in the manifest found in the sip
             */
            long countConsistentDigitalObjectFromManifest = 0L;
            /**
             * count the number of digital object in the sip found in the manifest
             */
            long countConsistentDigitalObjectFromWorkspace = 0L;
            // TODO REVIEW since you have List, Set, you should use direct method (removeAll, containAll, isEmpty, ...)
            // faster and better
            for (final URI uriManifest : uriListManifest) {
                if (uriListWorkspace.contains(uriManifest)) {
                    countConsistentDigitalObjectFromManifest++;
                } else {
                    countCompare++;
                    response.setStatus(StatusCode.KO)
                    .setOutcomeMessages(HANDLER_ID, OutcomeMessage.CHECK_OBJECT_NUMBER_KO);
                }
            }

            for (final URI uriWorkspace : uriListWorkspace) {
                if (uriListManifest.contains(uriWorkspace)) {
                    countConsistentDigitalObjectFromWorkspace++;
                } else {
                    response.setStatus(StatusCode.KO);
                    countCompare++;
                }

            }

            final boolean countOK =
                countConsistentDigitalObjectFromManifest == countConsistentDigitalObjectFromWorkspace;
            final boolean countConsistent = countConsistentDigitalObjectFromManifest == uriListManifest.size();

            if (countOK && countConsistent) {
                response.setStatus(StatusCode.OK);

            } else {
                response.setStatus(StatusCode.KO)
                .setOutcomeMessages(HANDLER_ID, OutcomeMessage.CHECK_OBJECT_NUMBER_KO);
            }
        }
        
        response.setErrorNumber(countCompare);
    }
}
