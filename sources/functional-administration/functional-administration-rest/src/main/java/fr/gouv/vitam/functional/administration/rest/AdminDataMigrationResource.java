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
package fr.gouv.vitam.functional.administration.rest;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.i18n.VitamLogbookMessages;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.logbook.common.exception.LogbookClientAlreadyExistsException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterHelper;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/adminmanagement/v1")
@ApplicationPath("webresources")
@Tag(name = "Functional-Administration")
public class AdminDataMigrationResource {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AdminDataMigrationResource.class);
    private LogbookOperationsClientFactory logbookOperationsClientFactory;
    private ProcessingManagementClientFactory processingManagementClientFactory;
    private WorkspaceClientFactory workspaceClientFactory;

    AdminDataMigrationResource() {
        this(
            LogbookOperationsClientFactory.getInstance(),
            ProcessingManagementClientFactory.getInstance(),
            WorkspaceClientFactory.getInstance()
        );
    }

    /**
     * Constructor
     *
     * @param logbookOperationsClientFactory logbookOperationsClientFactory
     * @param processingManagementClientFactory processingManagementClientFactory
     * @param workspaceClientFactory workspaceClientFactory
     */
    @VisibleForTesting
    private AdminDataMigrationResource(
        LogbookOperationsClientFactory logbookOperationsClientFactory,
        ProcessingManagementClientFactory processingManagementClientFactory,
        WorkspaceClientFactory workspaceClientFactory
    ) {
        this.logbookOperationsClientFactory = logbookOperationsClientFactory;
        this.processingManagementClientFactory = processingManagementClientFactory;
        this.workspaceClientFactory = workspaceClientFactory;
    }

    private VitamError getErrorEntity(Response.Status status, String message) {
        String aMessage = (message != null && !message.trim().isEmpty())
            ? message
            : (status.getReasonPhrase() != null ? status.getReasonPhrase() : status.name());
        return new VitamError(status.name())
            .setHttpCode(status.getStatusCode())
            .setMessage(status.getReasonPhrase())
            .setDescription(aMessage);
    }

    private void createOperation(GUID guid) throws LogbookClientBadRequestException {
        try (LogbookOperationsClient client = logbookOperationsClientFactory.getClient()) {
            final LogbookOperationParameters initParameter = LogbookParameterHelper.newLogbookOperationParameters(
                guid,
                "DATA_MIGRATION",
                guid,
                LogbookTypeProcess.DATA_MIGRATION,
                StatusCode.STARTED,
                VitamLogbookMessages.getLabelOp("DATA_MIGRATION.STARTED") + " : " + guid,
                guid
            );
            client.create(initParameter);
        } catch (LogbookClientAlreadyExistsException | LogbookClientServerException e) {
            throw new VitamRuntimeException("Internal server error ", e);
        }
    }
}
