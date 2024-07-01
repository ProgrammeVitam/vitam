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
package fr.gouv.vitam.ihmrecette.appserver.applicativetest;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.functionaltest.cucumber.step.World;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

/**
 * resource to manage system test
 */
@Path("/v1/api/applicative-test")
public class ApplicativeTestResource {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ApplicativeTestResource.class);

    private ApplicativeTestService applicativeTestService;
    private String testSystemSipDirectory;

    /**
     * @param applicativeTestService service
     * @param testSystemSipDirectory base path on feature
     */
    public ApplicativeTestResource(ApplicativeTestService applicativeTestService, String testSystemSipDirectory) {
        this.applicativeTestService = applicativeTestService;
        this.testSystemSipDirectory = testSystemSipDirectory;
        System.setProperty(World.TNR_BASE_DIRECTORY, testSystemSipDirectory);
    }

    /**
     * launch cucumber test
     *
     * @return 202 if test are in progress, 200 if the previous test are done
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response launchCucumberTest() {
        if (applicativeTestService.inProgress()) {
            return Response.accepted().build();
        }
        try {
            if (applicativeTestService.getIsTnrMasterActived().get()) {
                applicativeTestService.setIsTnrMasterActived(new AtomicBoolean(false));
                applicativeTestService.setTnrBranch("master");
                applicativeTestService.checkout(Paths.get(testSystemSipDirectory), "master");
            }
            String fileName = applicativeTestService.launchCucumberTest(Paths.get(testSystemSipDirectory));
            return Response.status(Response.Status.ACCEPTED).entity(fileName).build();
        } catch (Exception e) {
            LOGGER.error(e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * launch cucumber test
     *
     * @return 200 if the previous test are done
     */
    @POST
    @Path("/testFeature")
    @Produces(MediaType.APPLICATION_JSON)
    public Response launchCucumberPiecesTest(String pieces) {
        String results;

        try {
            if (!applicativeTestService.getIsTnrMasterActived().get()) {
                applicativeTestService.checkout(Paths.get(testSystemSipDirectory), "tnr_master");
                applicativeTestService.setTnrBranch("tnr_master");
                applicativeTestService.setIsTnrMasterActived(new AtomicBoolean(true));
            }
            results = applicativeTestService.launchPiecesCucumberTest(pieces + "\n");
        } catch (Exception e) {
            String stackTrace = getStack(e);
            LOGGER.error(e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(stackTrace).build();
        }
        return Response.status(Response.Status.OK).entity(results).build();
    }

    private String getStack(Throwable e) {
        String stackTrace = ExceptionUtils.getStackTrace(e);
        return stackTrace.replace(System.lineSeparator(), "<br/>\n");
    }

    /**
     * get status of the test
     *
     * @return 202 if test are in progress, 200 if the previous test are done
     */
    @HEAD
    public Response status() {
        if (applicativeTestService.inProgress()) {
            return Response.accepted().build();
        }
        return Response.ok().build();
    }

    /**
     * list the report of system test
     *
     * @return list of report
     * @throws IOException
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listReports() throws IOException {
        List<java.nio.file.Path> reports = applicativeTestService.reports();

        return Response.ok(
            reports.stream().map(java.nio.file.Path::getFileName).map(java.nio.file.Path::toString).collect(toList())
        ).build();
    }

    /**
     * list git branches
     *
     * @return list of git branches
     * @throws IOException
     */
    @GET
    @Path("/gitBranches")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listGitBranches() throws IOException, InterruptedException {
        applicativeTestService.fetch(Paths.get(testSystemSipDirectory));
        List<String> branchList = applicativeTestService.getBranches(Paths.get(testSystemSipDirectory));

        return Response.ok(branchList).build();
    }

    /**
     * PDL
     * return a specific report according to his name.
     *
     * @param fileName name of the report
     * @return 200 if report is ok, 404 if exception occurs
     */
    @GET
    @Path("/{report}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response reportByName(@PathParam("report") String fileName) {
        InputStream inputStream;
        try {
            inputStream = applicativeTestService.readReport(fileName);
            return Response.ok(inputStream).build();
        } catch (IOException e) {
            LOGGER.error(format("unable to open file: %s", fileName), e);
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    /**
     * synchronize git branch
     *
     * @return Status of the command
     */
    @POST
    @Path("/syncTnrPiecesWithBranch")
    @Produces(MediaType.APPLICATION_JSON)
    public Response synchronizedPiecesTestDirectoryWithBranch(String branch) throws IOException, InterruptedException {
        LOGGER.debug("synchronise " + branch);

        applicativeTestService.fetch(Paths.get(testSystemSipDirectory));
        List<String> branchList = applicativeTestService.getBranches(Paths.get(testSystemSipDirectory));
        if (!branchList.contains(branch)) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        int resetStatus = applicativeTestService.reset(Paths.get(testSystemSipDirectory), branch);
        int checkoutStatus = applicativeTestService.checkout(Paths.get(testSystemSipDirectory), branch);

        if (resetStatus == 0 && checkoutStatus == 0) {
            return Response.status(Response.Status.OK).entity(0).build();
        }

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(1).build();
    }

    private Response synchronizeGit(String tnrMaster) throws IOException, InterruptedException {
        applicativeTestService.checkout(Paths.get(testSystemSipDirectory), tnrMaster);
        int status = applicativeTestService.synchronizedTestDirectory(Paths.get(testSystemSipDirectory));
        return Response.ok().entity(status).build();
    }

    /**
     * synchronize tnr directory
     *
     * @return status of the command
     */

    @POST
    @Path("/sync")
    public Response synchronizedTestDirectory() throws IOException, InterruptedException {
        return synchronizeGit(applicativeTestService.getTnrBranch());
    }
}
