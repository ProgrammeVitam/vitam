/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.storage.cold.server;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.storage.cold.server.simulator.InaService;
import fr.gouv.vitam.storage.engine.common.api.dto.TapeDriveSpec;
import fr.gouv.vitam.storage.engine.common.api.dto.TapeLibrarySpec;
import fr.gouv.vitam.storage.engine.common.api.exception.TapeCommandException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * JAX-RS Resource implementation for INA Tape Proxy
 */
@Path("/ina-tape-proxy/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "INA Tape Proxy")
public class InaTapeProxyResourceImpl extends ApplicationStatusResource {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(InaTapeProxyResourceImpl.class);

    private final InaService service;

    public InaTapeProxyResourceImpl(InaService service) {
        this.service = service;
        LOGGER.info("InaTapeProxyResourceImpl initialized with service");
    }

    // ========== DRIVE OPERATIONS ==========

    @GET
    @Path("drive/status")
    @Operation(
        summary = "Get tape drive status",
        description = "Returns detailed information about the current tape drive.",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Drive status retrieved successfully",
                content = @Content(schema = @Schema(implementation = TapeDriveSpec.class))
            ),
            @ApiResponse(responseCode = "500", description = "Error while retrieving drive status"),
        }
    )
    public TapeDriveSpec getDriveStatus() throws TapeCommandException {
        LOGGER.debug("GET /drive/status");
        return service.getDriveStatus();
    }

    @POST
    @Path("drive/move")
    @Operation(
        summary = "Move tape position",
        description = "Moves the tape a certain number of blocks forward or backward.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Tape moved successfully"),
            @ApiResponse(responseCode = "500", description = "Error while moving the tape"),
        }
    )
    public Response move(@QueryParam("position") int position, @QueryParam("backward") boolean backward)
        throws TapeCommandException {
        LOGGER.debug("POST /drive/move?position={}&backward={}", position, backward);
        service.move(position, backward);
        return Response.ok().build();
    }

    @POST
    @Path("drive/rewind")
    @Operation(
        summary = "Rewind tape",
        description = "Rewinds the current tape to the beginning.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Tape rewound successfully"),
            @ApiResponse(responseCode = "500", description = "Error while rewinding the tape"),
        }
    )
    public Response rewind() throws TapeCommandException {
        LOGGER.debug("POST /drive/rewind");
        service.rewind();
        return Response.ok().build();
    }

    @POST
    @Path("drive/eod")
    @Operation(
        summary = "Go to end of data (EOD)",
        description = "Moves the tape to the end of the recorded data.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Positioned at end of data"),
            @ApiResponse(responseCode = "500", description = "Error while positioning to EOD"),
        }
    )
    public Response goToEnd() throws TapeCommandException {
        LOGGER.debug("POST /drive/eod");
        service.goToEnd();
        return Response.ok().build();
    }

    @POST
    @Path("drive/eject")
    @Operation(
        summary = "Eject tape",
        description = "Ejects the tape from the drive.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Tape ejected successfully"),
            @ApiResponse(responseCode = "500", description = "Error while ejecting the tape"),
        }
    )
    public Response eject() throws TapeCommandException {
        LOGGER.debug("POST /drive/eject");
        service.eject();
        return Response.ok().build();
    }

    // ========== LIBRARY OPERATIONS ==========

    @GET
    @Path("library/status")
    @Operation(
        summary = "Get library status",
        description = "Returns the current state of the tape library, including slots and drives information.",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Library status retrieved successfully",
                content = @Content(schema = @Schema(implementation = TapeLibrarySpec.class))
            ),
            @ApiResponse(responseCode = "500", description = "Error while retrieving library status"),
        }
    )
    public TapeLibrarySpec getLibraryStatus() throws TapeCommandException {
        LOGGER.debug("GET /library/status");
        return service.getLibraryStatus();
    }

    @POST
    @Path("library/load")
    @Operation(
        summary = "Load a tape",
        description = "Loads a tape from a specified slot into a given drive.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Tape loaded successfully"),
            @ApiResponse(responseCode = "500", description = "Error while loading the tape"),
        }
    )
    public Response loadTape(@QueryParam("slot") int slot, @QueryParam("drive") int drive) throws TapeCommandException {
        LOGGER.info("POST /library/load?slot={}&drive={}", slot, drive);
        service.loadTape(slot, drive);
        return Response.ok().build();
    }

    @POST
    @Path("library/unload")
    @Operation(
        summary = "Unload a tape",
        description = "Unloads a tape from a drive back to a specified slot.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Tape unloaded successfully"),
            @ApiResponse(responseCode = "500", description = "Error while unloading the tape"),
        }
    )
    public Response unloadTape(@QueryParam("slot") int slot, @QueryParam("drive") int drive)
        throws TapeCommandException {
        LOGGER.info("POST /library/unload?slot={}&drive={}", slot, drive);
        service.unloadTape(slot, drive);
        return Response.ok().build();
    }

    // ========== I/O OPERATIONS ==========

    @POST
    @Path("io/write")
    @Operation(
        summary = "Write data to tape",
        description = "Transfers a file from the working directory to the mounted tape.",
        responses = {
            @ApiResponse(responseCode = "204", description = "Data written successfully"),
            @ApiResponse(responseCode = "500", description = "Error while writing to tape"),
        }
    )
    public void writeToTape(@QueryParam("inputPath") String inputPath) throws TapeCommandException {
        LOGGER.info("POST /io/write?inputPath={}", inputPath);
        service.writeToTape(inputPath);
    }

    @GET
    @Path("io/read")
    @Operation(
        summary = "Read data from tape",
        description = "Extracts a file from the mounted tape to the working directory.",
        responses = {
            @ApiResponse(responseCode = "204", description = "Data read successfully"),
            @ApiResponse(responseCode = "500", description = "Error while reading from tape"),
        }
    )
    public void readFromTape(@QueryParam("outputPath") String outputPath) throws TapeCommandException {
        LOGGER.info("GET /io/read?outputPath={}", outputPath);
        service.readFromTape(outputPath);
    }
}
