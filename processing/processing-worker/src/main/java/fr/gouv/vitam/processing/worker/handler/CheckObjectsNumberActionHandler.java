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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.processing.common.CheckObjectsNumberMessage;
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

    // FIXME REVIEW since getId => private
    /**
     * Handler's ID
     */
    public static final String HANDLER_ID = "CheckObjectsNumber";

    private final SedaUtilsFactory sedaUtilsFactory;
    private final ContainerExtractionUtilsFactory containerExtractionUtilsFactory;


    // FIXME REVIEW check null

    /**
     * @param sedaUtilsFactory
     * @param containerExtractionUtilsFactory
     */
    public CheckObjectsNumberActionHandler(SedaUtilsFactory sedaUtilsFactory,
        ContainerExtractionUtilsFactory containerExtractionUtilsFactory) {
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


        final List<String> messages = new ArrayList<>();
        final EngineResponse response = new ProcessResponse();
        response.setStatus(StatusCode.OK).setDetailMessages(messages).setOutcomeMessages(HANDLER_ID, OutcomeMessage.CHECK_OBJECT_NUMBER_OK);

        try {

            final ExtractUriResponse extractUriResponse = getUriListFromManifest(params);

            if (extractUriResponse != null && !extractUriResponse.isErrorDuplicateUri()) {

                final List<URI> uriListFromManifest = extractUriResponse.getUriListManifest();
                final List<URI> uriListFromWorkspace = getUriListFromWorkspace(params);
                checkCountDigitalObjectConformity(uriListFromManifest, uriListFromWorkspace, response);

            } else if (extractUriResponse != null) {
                response.setStatus(StatusCode.KO)
                .setDetailMessages(extractUriResponse.getDetailMessages())
                .setOutcomeMessages(HANDLER_ID, OutcomeMessage.CHECK_OBJECT_NUMBER_KO);
            }

        } catch (XMLStreamException | ProcessingException | NullPointerException e) {
            response.setStatus(StatusCode.FATAL).setOutcomeMessages(HANDLER_ID, OutcomeMessage.CHECK_OBJECT_NUMBER_KO);
        }

        return response;

    }


    /**
     * gets URI list of Digital object from the workspace, checks if there are duplicated URIs
     *
     * @param params
     * @return
     * @throws XMLStreamException
     * @throws ProcessingException
     */
    private ExtractUriResponse getUriListFromManifest(WorkParams params)
        throws ProcessingException, XMLStreamException {
        // get uri list from manifest
        final SedaUtils sedaUtils = sedaUtilsFactory.create();
        return sedaUtils.getAllDigitalObjectUriFromManifest(params);
    }


    /**
     * gets URI list of Digital object from the workspace, checks if there are duplicated URIs
     *
     * @param params
     * @return List<URI>
     * @throws ProcessingException if params is null, or some params' attributes are null
     */
    private List<URI> getUriListFromWorkspace(WorkParams params) throws ProcessingException {
        final ContainerExtractionUtils containerExtractionUtils = containerExtractionUtilsFactory.create();
        return containerExtractionUtils.getDigitalObjectUriListFromWorkspace(params);
    }

    /**
     * Count the number of digital objects consistent between the manifest.xm file and the sip
     *
     * @param uriListManifest
     * @param uriListWorkspace
     * @param response
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
        final int countCompare = Integer.compare(uriListManifest.size(), uriListWorkspace.size());
        if (countCompare != 0) {
            response.setStatus(StatusCode.KO);
            response.getDetailMessages().add(CheckObjectsNumberMessage.COUNT_DIGITAL_OBJECT_SIP.getMessage()
                .concat(Integer.toString(uriListWorkspace.size())));
            response.getDetailMessages().add(CheckObjectsNumberMessage.COUNT_DIGITAL_OBJECT_MANIFEST.getMessage()
                .concat(Integer.toString(uriListManifest.size())));

            try {
                // found not declared digital object in the manifest
                foundUnreferencedDigitalObject(uriListManifest, uriListWorkspace, response,
                    CheckObjectsNumberMessage.NOT_FOUND_DIGITAL_OBJECT_WORKSPACE.getMessage());
                // found not declared digital object in the sip
                foundUnreferencedDigitalObject(uriListWorkspace, uriListManifest, response,
                    CheckObjectsNumberMessage.NOT_FOUND_DIGITAL_OBJECT_MANIFEST.getMessage());
            } catch (final IllegalAccessException e) {
                throw new ProcessingException("Some arguments were null", e);
            }
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
                    response.setStatus(StatusCode.KO);
                    response.getDetailMessages().add(CheckObjectsNumberMessage.NOT_FOUND_DIGITAL_OBJECT_WORKSPACE.getMessage()
                        .concat(uriManifest.toString()));
                }
            }

            for (final URI uriWorkspace : uriListWorkspace) {
                if (uriListManifest.contains(uriWorkspace)) {
                    countConsistentDigitalObjectFromWorkspace++;
                } else {
                    response.setStatus(StatusCode.KO);
                    response.getDetailMessages().add(CheckObjectsNumberMessage.NOT_FOUND_DIGITAL_OBJECT_MANIFEST.getMessage()
                        .concat(uriWorkspace.toString()));
                }

            }

            final boolean countOK =
                countConsistentDigitalObjectFromManifest == countConsistentDigitalObjectFromWorkspace;
            final boolean countConsistent = countConsistentDigitalObjectFromManifest == uriListManifest.size();

            if (countOK && countConsistent) {
                response.setStatus(StatusCode.OK);

            } else {
                response.setStatus(StatusCode.KO);
            }
        }

    }

    /**
     * Found the undeclared digital object either in the manifest or in the sip
     *
     * @param uriListManifest
     * @param uriListWorkspace
     * @param response
     * @param element
     */
    private void foundUnreferencedDigitalObject(List<URI> uriListToCompared, List<URI> uriListReference,
        EngineResponse response, String element) throws IllegalAccessException {

        final Set<String> uriNotFoundSet = new HashSet<>();

        if (uriListToCompared == null || uriListReference == null) {
            throw new IllegalAccessException("uriListToCompared or uriListReference must not be null");
        }

        for (final URI uriManifest : uriListToCompared) {
            if (!uriListReference.contains(uriManifest)) {
                uriNotFoundSet.add(element
                    .concat(uriManifest.toString()));
            }

            if (!uriNotFoundSet.isEmpty()) {
                for (final String s : uriNotFoundSet) {
                    if (response != null && response.getOutcomeMessages() != null) {
                        response.getDetailMessages().add(s);
                    }
                }
            }
        }
    }
}
