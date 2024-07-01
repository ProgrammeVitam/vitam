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
package fr.gouv.vitam.metadata.rest;

import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.api.VitamRepositoryFactory;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.exception.LogbookClientBadRequestException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientNotFoundException;
import fr.gouv.vitam.logbook.common.exception.LogbookClientServerException;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.audit.core.MetadataAuditService;
import fr.gouv.vitam.metadata.core.config.ElasticsearchMetadataIndexManager;
import fr.gouv.vitam.metadata.core.config.MetaDataConfiguration;
import fr.gouv.vitam.metadata.core.mapping.MappingLoader;
import fr.gouv.vitam.processing.management.client.ProcessingManagementClientFactory;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/v1")
@Tag(name = "Metadata")
public class MetadataAuditResource {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MetadataAuditResource.class);

    private final MetadataAuditService metadataAuditService;

    public MetadataAuditResource(MetaDataConfiguration metaDataConfiguration) {
        this.metadataAuditService = new MetadataAuditService(
            WorkspaceClientFactory.getInstance(),
            LogbookOperationsClientFactory.getInstance(),
            VitamRepositoryFactory.get(),
            new ElasticsearchMetadataIndexManager(
                metaDataConfiguration,
                VitamConfiguration.getTenants(),
                new MappingLoader(metaDataConfiguration.getElasticsearchExternalMetadataMappings())
            ),
            metaDataConfiguration.getIsDataConsistencyAuditRunnable(),
            metaDataConfiguration.getDataConsistencyAuditOplogMaxSize(),
            metaDataConfiguration.getMongodShardsConf(),
            metaDataConfiguration.isDbAuthentication()
        );
        LOGGER.info("init MetaData Audit Resource server");
        ProcessingManagementClientFactory.changeConfigurationUrl(metaDataConfiguration.getUrlProcessing());
    }

    @GET
    @Path("/auditDataConsistency")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response runAuditDataConsistencyMongoEs() {
        LOGGER.debug("Audit data consistency : Running ...");
        try {
            VitamThreadUtils.getVitamSession().initIfAbsent(VitamConfiguration.getAdminTenant());
            return metadataAuditService.auditDataConsistencyMongoEs();
        } catch (LogbookClientNotFoundException | LogbookClientBadRequestException | LogbookClientServerException e) {
            LOGGER.error(e);
            return Response.serverError().build();
        }
    }
}
