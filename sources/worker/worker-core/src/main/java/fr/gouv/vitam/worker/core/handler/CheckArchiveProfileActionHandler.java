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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SedaConstants;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.IngestWorkflowConstants;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.ProfileFormat;
import fr.gouv.vitam.common.model.administration.ProfileModel;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.xml.ValidationXsdUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.Profile;
import fr.gouv.vitam.functional.administration.common.exception.ProfileNotFoundException;
import fr.gouv.vitam.functional.administration.common.exception.ProfilePathFileNotFoundException;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXException;

import javax.ws.rs.core.Response;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Check Archive Profile Handler - verify profil in manifest
 */
public class CheckArchiveProfileActionHandler extends ActionHandler {

    private static final String NOT_FOUND = " not found";
    private static final String UNKNOWN_TECHNICAL_EXCEPTION = "Unknown technical exception";
    private static final String VALIDATION_ERROR = "ValidationError";
    private static final String CAN_NOT_SEARCH_PROFILE = "Can not search profile";
    private static final String PROFILE_NOT_FOUND = "Profile not found";
    private static final String PROFILE_PATH_NOT_FOUND = "There is no Profile path file for";
    private static final String CAN_NOT_GET_FILE_MANIFEST = "Can not get file manifest";
    private static final String FILE_NOT_FOUND = "File not found";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(CheckArchiveProfileActionHandler.class);

    private static final String HANDLER_ID = "CHECK_ARCHIVEPROFILE";
    private static final int PROFILE_IDENTIFIER_RANK = 0;

    private final AdminManagementClientFactory adminManagementClientFactory;

    /**
     * Constructor with parameter SedaUtilsFactory
     */
    public CheckArchiveProfileActionHandler() {
        this(AdminManagementClientFactory.getInstance());
    }

    @VisibleForTesting
    public CheckArchiveProfileActionHandler(AdminManagementClientFactory adminManagementClientFactory) {
        this.adminManagementClientFactory = adminManagementClientFactory;
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
        final String profileIdentifier = (String) handlerIO.getInput(PROFILE_IDENTIFIER_RANK);
        ObjectNode infoNode = JsonHandler.createObjectNode();

        Boolean isValid = true;
        try (AdminManagementClient adminClient = adminManagementClientFactory.getClient()) {
            Select select = new Select();
            select.setQuery(QueryHelper.eq(Profile.IDENTIFIER, profileIdentifier));
            RequestResponse<ProfileModel> response = adminClient.findProfiles(select.getFinalSelect());
            ProfileModel profile = null;
            if (response.isOk() && ((RequestResponseOK<ProfileModel>) response).getResults().size() > 0) {
                profile = ((RequestResponseOK<ProfileModel>) response).getResults().get(0);
            }

            if (profile != null) {
                checkProfilePath(profile);
                Response downloadResponse = null;
                InputStream stream = null;
                File tmpFile;
                try {
                    downloadResponse = adminClient.downloadProfileFile(profileIdentifier);
                    stream = downloadResponse.readEntity(InputStream.class);
                    tmpFile = PropertiesUtils.fileFromTmpFolder(profile.getPath());
                    OutputStream outputStream = new FileOutputStream(tmpFile);
                    IOUtils.copy(stream, outputStream);
                    outputStream.close();
                } finally {
                    StreamUtils.closeSilently(stream);
                    StreamUtils.consumeAnyEntityAndClose(downloadResponse);
                }

                if (profile.getFormat().equals(ProfileFormat.XSD)) {
                    isValid = ValidationXsdUtils.getInstance()
                        .checkFileXSD(
                            handlerIO.getInputStreamFromWorkspace(
                                IngestWorkflowConstants.SEDA_FOLDER + "/" + IngestWorkflowConstants.SEDA_FILE
                            ),
                            tmpFile
                        );
                }

                if (profile.getFormat().equals(ProfileFormat.RNG)) {
                    isValid = ValidationXsdUtils.getInstance()
                        .checkFileRNG(
                            handlerIO.getInputStreamFromWorkspace(
                                IngestWorkflowConstants.SEDA_FOLDER + "/" + IngestWorkflowConstants.SEDA_FILE
                            ),
                            tmpFile
                        );
                }
            } else {
                throw new ProfileNotFoundException(profileIdentifier + NOT_FOUND);
            }
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            LOGGER.error(CAN_NOT_SEARCH_PROFILE, e);
            return getItemStatus(
                itemStatus,
                SedaConstants.EV_DET_TECH_DATA,
                CAN_NOT_SEARCH_PROFILE + " " + profileIdentifier,
                infoNode,
                StatusCode.KO
            );
        } catch (ProfileNotFoundException e) {
            LOGGER.error(PROFILE_NOT_FOUND, e);
            return getItemStatus(
                itemStatus,
                SedaConstants.EV_DET_TECH_DATA,
                PROFILE_NOT_FOUND + " " + profileIdentifier,
                infoNode,
                StatusCode.KO
            );
        } catch (ProfilePathFileNotFoundException e) {
            LOGGER.error(PROFILE_PATH_NOT_FOUND, e);
            return getItemStatus(
                itemStatus,
                SedaConstants.EV_DET_TECH_DATA,
                PROFILE_PATH_NOT_FOUND + " " + profileIdentifier,
                infoNode,
                StatusCode.KO
            );
        } catch (ContentAddressableStorageNotFoundException | ContentAddressableStorageServerException e) {
            LOGGER.error(CAN_NOT_GET_FILE_MANIFEST, e);
            return getItemStatus(
                itemStatus,
                SedaConstants.EV_DET_TECH_DATA,
                CAN_NOT_GET_FILE_MANIFEST + " manifest.xml",
                infoNode,
                StatusCode.FATAL
            );
        } catch (IOException | XMLStreamException e) {
            LOGGER.error(FILE_NOT_FOUND, e);
            return getItemStatus(
                itemStatus,
                SedaConstants.EV_DET_TECH_DATA,
                FILE_NOT_FOUND + " " + profileIdentifier,
                infoNode,
                StatusCode.KO
            );
        } catch (SAXException e) {
            LOGGER.error(VALIDATION_ERROR, e);
            infoNode.put(SedaConstants.EV_DET_TECH_DATA, e.getMessage());
            String evdev = JsonHandler.unprettyPrint(infoNode);
            itemStatus.setEvDetailData(evdev);
            itemStatus.setMasterData(LogbookParameterName.eventDetailData.name(), evdev);
            isValid = false;
        } catch (Exception e) {
            LOGGER.error(UNKNOWN_TECHNICAL_EXCEPTION, e);
            itemStatus.increment(StatusCode.FATAL);
            return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
        }

        if (isValid) {
            return getItemStatus(
                itemStatus,
                SedaConstants.TAG_ARCHIVE_PROFILE,
                profileIdentifier,
                infoNode,
                StatusCode.OK
            );
        } else {
            return getItemStatus(
                itemStatus,
                SedaConstants.TAG_ARCHIVE_PROFILE,
                profileIdentifier,
                infoNode,
                StatusCode.KO
            );
        }
    }

    private ItemStatus getItemStatus(
        ItemStatus itemStatus,
        String logbookField,
        String evTechDataMessage,
        ObjectNode infoNode,
        StatusCode statusCode
    ) {
        itemStatus.increment(statusCode);
        infoNode.put(logbookField, evTechDataMessage);
        String evdev = JsonHandler.unprettyPrint(infoNode);
        itemStatus.setEvDetailData(evdev);
        itemStatus.setMasterData(LogbookParameterName.eventDetailData.name(), evdev);
        return new ItemStatus(HANDLER_ID).setItemsStatus(HANDLER_ID, itemStatus);
    }

    private void checkProfilePath(ProfileModel profile) throws ProfilePathFileNotFoundException {
        if (StringUtils.isBlank(profile.getPath())) {
            throw new ProfilePathFileNotFoundException("The profile path for (XSD or RNG) file not Found");
        }
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {}
}
