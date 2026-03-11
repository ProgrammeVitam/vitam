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
package fr.gouv.vitam.storage.cold.server.rest;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.storage.cold.server.simulator.exception.InaTapeProxyException;
import fr.gouv.vitam.storage.cold.server.simulator.service.InaService;
import fr.gouv.vitam.storage.engine.common.api.dto.TapeDriveState;
import fr.gouv.vitam.storage.engine.common.api.dto.TapeLibraryState;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
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
public class InaTapeProxyResource extends ApplicationStatusResource {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(InaTapeProxyResource.class);

    private final InaService service;

    public InaTapeProxyResource(InaService service) {
        this.service = service;
        LOGGER.info("InaTapeProxyResourceImpl initialized with service");
    }

    // ========== DRIVE OPERATIONS ==========

    @GET
    @Path("drive/{driveIndex}/status")
    @Operation(
        summary = "Get tape drive status",
        description = "Returns detailed information about the current tape drive.",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Drive status retrieved successfully",
                content = @Content(schema = @Schema(implementation = TapeDriveState.class))
            ),
            @ApiResponse(responseCode = "500", description = "Error while retrieving drive status"),
        }
    )
    public TapeDriveState getDriveStatus(@PathParam("driveIndex") int driveIndex) throws InaTapeProxyException {
        LOGGER.info("GET /drive/{}/status", driveIndex);
        return service.getDriveStatus(driveIndex);
    }

    @POST
    @Path("drive/{driveIndex}/move")
    @Operation(
        summary = "Move tape position",
        description = "Moves the tape a certain number of blocks forward or backward.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Tape moved successfully"),
            @ApiResponse(responseCode = "500", description = "Error while moving the tape"),
        }
    )
    public Response move(
        @PathParam("driveIndex") int driveIndex,
        @QueryParam("position") int position,
        @QueryParam("backward") boolean backward
    ) throws InaTapeProxyException {
        LOGGER.info("POST /drive/{}/move?position={}&backward={}", driveIndex, position, backward);
        service.move(driveIndex, position, backward);
        return Response.ok().build();
    }

    @POST
    @Path("drive/{driveIndex}/rewind")
    @Operation(
        summary = "Rewind tape",
        description = "Rewinds the current tape to the beginning.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Tape rewound successfully"),
            @ApiResponse(responseCode = "500", description = "Error while rewinding the tape"),
        }
    )
    public Response rewind(@PathParam("driveIndex") int driveIndex) throws InaTapeProxyException {
        LOGGER.info("POST /drive/{}/rewind", driveIndex);
        service.rewind(driveIndex);
        return Response.ok().build();
    }

    @POST
    @Path("drive/{driveIndex}/eod")
    @Operation(
        summary = "Go to end of data (EOD)",
        description = "Moves the tape to the end of the recorded data.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Positioned at end of data"),
            @ApiResponse(responseCode = "500", description = "Error while positioning to EOD"),
        }
    )
    public Response goToEnd(@PathParam("driveIndex") int driveIndex) throws InaTapeProxyException {
        LOGGER.info("POST /drive/{}/eod", driveIndex);
        service.goToEnd(driveIndex);
        return Response.ok().build();
    }

    @POST
    @Path("drive/{driveIndex}/eject")
    @Operation(
        summary = "Eject tape",
        description = "Ejects the tape from the drive.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Tape ejected successfully"),
            @ApiResponse(responseCode = "500", description = "Error while ejecting the tape"),
        }
    )
    public Response eject(@PathParam("driveIndex") int driveIndex) throws InaTapeProxyException {
        LOGGER.info("POST /drive/{}/eject", driveIndex);
        service.eject(driveIndex);
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
                content = @Content(schema = @Schema(implementation = TapeLibraryState.class))
            ),
            @ApiResponse(responseCode = "500", description = "Error while retrieving library status"),
        }
    )
    public TapeLibraryState getLibraryStatus() throws InaTapeProxyException {
        LOGGER.info("GET /library/status");
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
    public Response loadTape(@QueryParam("slotNumber") int slotNumber, @QueryParam("drive") int drive)
        throws InaTapeProxyException {
        LOGGER.info("POST /library/load?slotNumber={}&drive={}", slotNumber, drive);
        service.loadTape(slotNumber, drive);
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
    public Response unloadTape(@QueryParam("slotNumber") int slotNumber, @QueryParam("drive") int driveIndex)
        throws InaTapeProxyException {
        LOGGER.info("POST /library/unload?slotNumber={}&drive={}", slotNumber, driveIndex);
        service.unloadTape(slotNumber, driveIndex);
        return Response.ok().build();
    }

    // ========== I/O OPERATIONS ==========

    @POST
    @Path("drives/{driveIndex}/io/write")
    @Operation(
        summary = "Write data to tape",
        description = "Transfers a file from the working directory to the mounted tape.",
        responses = {
            @ApiResponse(responseCode = "202", description = "Data written successfully"),
            @ApiResponse(responseCode = "500", description = "Error while writing to tape"),
        }
    )
    public Response writeToTape(@PathParam("driveIndex") int driveIndex, @QueryParam("inputPath") String inputPath)
        throws InaTapeProxyException {
        LOGGER.info("POST /io/{}/write?inputPath={}", driveIndex, inputPath);
        service.writeToTape(driveIndex, inputPath);
        return Response.accepted().build();
    }

    @GET
    @Path("drives/{driveIndex}/io/read")
    @Operation(
        summary = "Read data from tape",
        description = "Extracts a file from the mounted tape to the working directory.",
        responses = {
            @ApiResponse(responseCode = "202", description = "Data read successfully"),
            @ApiResponse(responseCode = "500", description = "Error while reading from tape"),
        }
    )
    public Response readFromTape(@PathParam("driveIndex") int driveIndex, @QueryParam("outputPath") String outputPath)
        throws InaTapeProxyException {
        LOGGER.info("GET /io/{}/read?outputPath={}", driveIndex, outputPath);
        service.readFromTape(driveIndex, outputPath);
        return Response.accepted().build();
    }
}
