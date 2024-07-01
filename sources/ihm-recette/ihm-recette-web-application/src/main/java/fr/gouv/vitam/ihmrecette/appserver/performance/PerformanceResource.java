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
package fr.gouv.vitam.ihmrecette.appserver.performance;

import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.VitamThreadFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * resource defining performance
 */
@Path("/v1/api/performances")
public class PerformanceResource {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(PerformanceResource.class);

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private ExecutorService performanceTestLauncher = Executors.newSingleThreadExecutor(
        VitamThreadFactory.getInstance()
    );

    private PerformanceService performanceService;

    public PerformanceResource(PerformanceService performanceService) {
        this.performanceService = performanceService;
    }

    /**
     * @param model
     * @return Response
     * @throws InterruptedException
     * @throws FileNotFoundException
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response launchPerformanceTest(
        @HeaderParam(GlobalDataRest.X_TENANT_ID) int tenantId,
        PerformanceModel model
    ) {
        if (performanceService.inProgress()) {
            return Response.accepted().build();
        }

        ParametersChecker.checkParameter("SIP path is a mandatory parameter", model.getFileName());

        if (!performanceService.sipExist(model.getFileName())) {
            LOGGER.error(String.format("SIP path invalid: %s", model.getFileName()));
            return Response.status(Response.Status.PRECONDITION_FAILED).entity("SIP path invalid").build();
        }

        if (model.getNumberOfIngest() == 0) {
            LOGGER.error("number of ingest must be greater than 0");
            return Response.status(Response.Status.PRECONDITION_FAILED)
                .entity("number of ingest must be greater than 0")
                .build();
        }

        if (model.getParallelIngest() != null && model.getDelay() != null) {
            LOGGER.error("unable to set parallel ingest and delay in same test");
            return Response.status(Response.Status.PRECONDITION_FAILED)
                .entity("unable to set parallel ingest and delay in same test")
                .build();
        }

        String fileName = format("report_%s.csv", LocalDateUtil.now().format(DATE_TIME_FORMATTER));

        performanceTestLauncher.submit(() -> {
            try {
                performanceService.launchPerformanceTest(model, fileName, tenantId);
            } catch (IOException e) {
                LOGGER.error("unable to launch performance test", e);
            }
        });

        return Response.accepted(fileName).build();
    }

    /**
     * @return 202 if test are in progress, 200 if the previous test are done
     */
    @HEAD
    public Response status() {
        if (performanceService.inProgress()) {
            return Response.accepted().build();
        }
        return Response.ok().build();
    }

    /**
     * return the  list of report
     *
     * @return Response 200 if get list of report
     * @throws IOException
     */
    @GET
    @Path("/reports")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listReport() throws IOException {
        List<java.nio.file.Path> paths = performanceService.listReportDirectory();

        return Response.ok(
            paths
                .stream()
                .map(java.nio.file.Path::getFileName)
                .map(java.nio.file.Path::toString)
                .collect(Collectors.toList())
        ).build();
    }

    /**
     * return the list of sip
     *
     * @return list of sip with relative path
     * @throws IOException
     */
    @GET
    @Path("/sips")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listSip() throws IOException {
        List<java.nio.file.Path> paths = performanceService.listSipDirectory();

        return Response.ok(paths.stream().map(java.nio.file.Path::toString).collect(Collectors.toList())).build();
    }

    /**
     * return the report
     *
     * @param fileName report fileName
     * @return 200 if report is ok, 404 if exception occurs
     * @throws IOException
     */
    @GET
    @Path("/reports/{fileName}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response report(@PathParam("fileName") String fileName) {
        InputStream inputStream;
        try {
            inputStream = performanceService.readReport(fileName);
            return Response.ok(inputStream).build();
        } catch (Exception e) {
            LOGGER.error(format("unable to open file: %s", fileName), e);
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
}
