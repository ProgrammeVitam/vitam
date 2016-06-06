package fr.gouv.vitam.processing.worker.handler;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.processing.common.CheckObjectsNumberMessage;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.model.EngineResponse;
import fr.gouv.vitam.processing.common.model.ProcessResponse;
import fr.gouv.vitam.processing.common.model.StatusCode;
import fr.gouv.vitam.processing.common.model.WorkParams;
import fr.gouv.vitam.processing.common.utils.ContainerExtractionUtils;
import fr.gouv.vitam.processing.common.utils.ContainerExtractionUtilsFactory;
import fr.gouv.vitam.processing.common.utils.ExtractUriResponse;
import fr.gouv.vitam.processing.common.utils.SedaUtils;
import fr.gouv.vitam.processing.common.utils.SedaUtilsFactory;

public class CheckObjectsNumberActionHandler extends ActionHandler {

    public static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ExtractSedaActionHandler.class);
    public static final String HANDLER_ID = "CheckObjectsNumberAction";

    private final SedaUtilsFactory sedaUtilsFactory;
    private final ContainerExtractionUtilsFactory containerExtractionUtilsFactory;



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


        List<String> messages = new ArrayList<>();
        EngineResponse response = new ProcessResponse().setStatus(StatusCode.OK).setMessages(messages);

        try {

            ExtractUriResponse extractUriResponse = getUriListFromManifest(params);

            if (extractUriResponse != null && !extractUriResponse.isErrorDuplicateUri()) {

                List<URI> uriListFromManifest = extractUriResponse.getUriListManifest();
                List<URI> uriListFromWorkspace = getUriListFromWorkspace(params);
                checkDuplicatedUriFromWorkspace(uriListFromWorkspace, response);
                checkCountDigitalObjectConformity(uriListFromManifest, uriListFromWorkspace, response);

            } else if (response != null && extractUriResponse != null) {
                response.setStatus(StatusCode.KO).setMessages(extractUriResponse.getMessages());
            }

        } catch (XMLStreamException e) {
            response.setStatus(StatusCode.FATAL);
        } catch (ProcessingException e) {
            response.setStatus(StatusCode.FATAL);
        } catch (Exception e) {
            response.setStatus(StatusCode.FATAL);
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
        SedaUtils sedaUtils = sedaUtilsFactory.create();
        ExtractUriResponse extractUriResponse = sedaUtils.getAllDigitalObjectUriFromManifest(params);
        return extractUriResponse;
    }


    /**
     * gets URI list of Digital object from the workspace, checks if there are duplicated URIs
     * 
     * @param params
     * @return List<URI>
     * @throws ProcessingException if params is null, or some params' attributes are null
     */
    private List<URI> getUriListFromWorkspace(WorkParams params) throws ProcessingException {
        ContainerExtractionUtils containerExtractionUtils = containerExtractionUtilsFactory.create();
        List<URI> uriListFromWorkspace = containerExtractionUtils.getDigitalObjectUriListFromWorkspace(params);
        return uriListFromWorkspace;
    }



    /**
     * Find duplicated URI for URI List extracted from the workspace
     * 
     * @param uriListFromWorkspace
     * @param response
     */
    private void checkDuplicatedUriFromWorkspace(List<URI> uriListFromWorkspace, EngineResponse response) {
        final Set<String> setDuplicatedUri = new HashSet<String>();
        final Set<URI> set = new HashSet<URI>();

        for (URI uri : uriListFromWorkspace) {
            if (!set.add(uri)) {
                setDuplicatedUri.add(
                    CheckObjectsNumberMessage.DUPLICATED_DIGITAL_OBJECT_WORKSPACE.getMessage().concat(uri.toString()));
            }
        }
        if (setDuplicatedUri != null && !setDuplicatedUri.isEmpty()) {
            response.getMessages().addAll(setDuplicatedUri);
            response.setStatus(StatusCode.KO);
        } ;
    }

    /**
     * Count the number of digital objects consistent between the manifest.xm file and the sip
     * 
     * @param uriListManifest
     * @param uriListWorkspace
     * @param response
     */
    private void checkCountDigitalObjectConformity(List<URI> uriListManifest, List<URI> uriListWorkspace,
        EngineResponse response) {

        // TODO
        // Use Java 8, Methods Reference, lambda expressions and streams

        if (response == null) {
            response = new ProcessResponse();
        }
        /**
         * compare the size between list uri from manifest and list uri from workspace.
         */

        int countCompare = 0;
        if (uriListManifest != null && uriListWorkspace != null) {
            countCompare = Integer.compare(uriListManifest.size(), uriListWorkspace.size());
        }
        if (countCompare != 0) {
            response.setStatus(StatusCode.KO);
            response.getMessages().add(CheckObjectsNumberMessage.COUNT_DIGITAL_OBJECT_SIP.getMessage()
                .concat(new Integer(uriListWorkspace.size()).toString()));
            response.getMessages().add(CheckObjectsNumberMessage.COUNT_DIGITAL_OBJECT_MANIFEST.getMessage()
                .concat(new Integer(uriListManifest.size()).toString()));

            // found not declared digital object in the manifest
            foundUnreferencedDigitalObject(uriListManifest, uriListWorkspace, response,
                CheckObjectsNumberMessage.NOT_FOUND_DIGITAL_OBJECT_WORKSPACE.getMessage());
            // found not declared digital object in the sip
            foundUnreferencedDigitalObject(uriListWorkspace, uriListManifest, response,
                CheckObjectsNumberMessage.NOT_FOUND_DIGITAL_OBJECT_MANIFEST.getMessage());
        } else {

            /**
             * count the number of object in the manifest found in the sip
             */
            long countConsistentDigitalObjectFromManifest = 0l;
            /**
             * count the number of digital object in the sip found in the manifest
             */
            long countConsistentDigitalObjectFromWorkspace = 0l;

            for (Iterator<URI> iterator = uriListManifest.iterator(); iterator.hasNext();) {
                URI uriManifest = iterator.next();

                if (uriListWorkspace.contains(uriManifest)) {
                    countConsistentDigitalObjectFromManifest++;
                } else {
                    response.setStatus(StatusCode.KO);
                    response.getMessages().add(CheckObjectsNumberMessage.NOT_FOUND_DIGITAL_OBJECT_WORKSPACE.getMessage()
                        .concat(uriManifest.toString()));
                }
            }

            for (Iterator<URI> iterator = uriListWorkspace.iterator(); iterator.hasNext();) {
                URI uriWorkspace = iterator.next();

                if (uriListManifest.contains(uriWorkspace)) {
                    countConsistentDigitalObjectFromWorkspace++;
                } else {
                    response.setStatus(StatusCode.KO);
                    response.getMessages().add(CheckObjectsNumberMessage.NOT_FOUND_DIGITAL_OBJECT_MANIFEST.getMessage()
                        .concat(uriWorkspace.toString()));
                }

            }

            boolean countOK = (countConsistentDigitalObjectFromManifest == countConsistentDigitalObjectFromWorkspace);
            boolean countConsistent = countConsistentDigitalObjectFromManifest == uriListManifest.size();

            if (countOK && countConsistent) {
                response.setStatus(StatusCode.OK).getMessages()
                    .add(CheckObjectsNumberMessage.COUNT_DIGITAL_OBJECT_CONSISTENT.getMessage());

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
        EngineResponse response, String element) {

        Set<String> uriNotFoundSet = new HashSet<String>();

        for (Iterator<URI> iterator = uriListToCompared.iterator(); iterator.hasNext();) {
            URI uriManifest = iterator.next();

            if (!uriListReference.contains(uriManifest)) {
                uriNotFoundSet.add(element
                    .concat(uriManifest.toString()));
            }

            if (!uriNotFoundSet.isEmpty()) {
                for (String s : uriNotFoundSet) {
                    if (response != null && response.getMessages() != null) {
                        response.getMessages().add(s);
                    }
                }
            }
        }
    }
}
