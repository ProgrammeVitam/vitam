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
package fr.gouv.vitam.storage.offers.tape.rest;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.stream.SizedInputStream;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.storage.offers.tape.TapeLibraryFactory;
import fr.gouv.vitam.storage.offers.tape.cas.BackupFileStorage;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;

@Path("/offer/v1")
@ApplicationPath("webresources")
@Tag(name = "Tape")
public class AdminTapeResource extends ApplicationStatusResource {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AdminTapeResource.class);

    private final BackupFileStorage backupFileStorage;

    @VisibleForTesting
    AdminTapeResource(BackupFileStorage backupFileStorage) {
        LOGGER.debug("AdminTapeResource initialized");
        this.backupFileStorage = backupFileStorage;
    }

    /**
     * TapeLibraryFactory should be already initialized in OfferCommonApplication while instan
     */
    public AdminTapeResource() {
        LOGGER.debug("AdminTapeResource initialized");
        this.backupFileStorage = TapeLibraryFactory.getInstance().getBackupFileStorage();
    }

    /**
     * Only used for tape offer
     *
     * @param objectId
     * @param input
     * @return
     */
    @PUT
    @Path("/backup/{objectId:.+}")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response putObject(@PathParam("objectId") String objectId, InputStream input) {
        final Digest digest = new Digest(VitamConfiguration.getDefaultDigestType());
        try (
            final SizedInputStream sis = new SizedInputStream(input);
            final InputStream digestInputStream = digest.getDigestInputStream(sis)
        ) {
            LOGGER.info("Writing backup '" + objectId);

            SanityChecker.checkParameter(objectId);

            backupFileStorage.writeFile(objectId, digestInputStream);

            ObjectNode response = JsonHandler.createObjectNode()
                .put("digest", digest.digestHex())
                .put("size", sis.getSize());

            return Response.status(Response.Status.CREATED).entity(response).build();
        } catch (Exception exc) {
            LOGGER.error("Cannot create object", exc);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            StreamUtils.closeSilently(input);
        }
    }
}
