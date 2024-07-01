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
package fr.gouv.vitam.functionaltest.cucumber.step;

import cucumber.api.java.After;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.When;
import fr.gouv.vitam.access.external.client.VitamPoolingClient;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.exception.InvalidGuidOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.guid.GUIDReader;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.ProcessState;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.logbook.LogbookEventOperation;
import fr.gouv.vitam.ingest.external.api.exception.IngestExternalException;
import fr.gouv.vitam.tools.SipTool;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;

import static fr.gouv.vitam.common.GlobalDataRest.X_REQUEST_ID;
import static fr.gouv.vitam.logbook.common.parameters.Contexts.DEFAULT_WORKFLOW;
import static fr.gouv.vitam.logbook.common.parameters.Contexts.FILING_SCHEME;
import static fr.gouv.vitam.logbook.common.parameters.Contexts.HOLDING_SCHEME;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class IngestStep extends CommonStep {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestStep.class);
    public static final String ID = "ID";
    public static final String _ID = "#id";

    private static boolean deleteSip = false;
    private static boolean attachMode = false;

    public IngestStep(World world) {
        super(world);
    }

    /**
     * define a sip
     *
     * @param fileName name of a sip
     */
    @Given("^un fichier SIP nommé (.*)$")
    public void a_sip_named(String fileName) {
        world.setSipFile(Paths.get(world.getBaseDirectory(), fileName));
    }

    /**
     * call vitam to upload the SIP
     *
     * @throws IOException
     * @throws IngestExternalException
     */
    @When("^je télécharge le SIP")
    public void upload_this_sip() throws VitamException, IOException {
        try (InputStream inputStream = Files.newInputStream(world.getSipFile(), StandardOpenOption.READ)) {
            RequestResponse response = world
                .getIngestClient()
                .ingest(
                    new VitamContext(world.getTenantId()).setApplicationSessionId(world.getApplicationSessionId()),
                    inputStream,
                    DEFAULT_WORKFLOW.name(),
                    ProcessAction.RESUME.name()
                );
            final String operationId = response.getHeaderString(GlobalDataRest.X_REQUEST_ID);
            world.setOperationId(operationId);
            final VitamPoolingClient vitamPoolingClient = new VitamPoolingClient(world.getAdminClient());
            boolean process_timeout = vitamPoolingClient.wait(
                world.getTenantId(),
                operationId,
                ProcessState.COMPLETED,
                1800,
                1_000L,
                TimeUnit.MILLISECONDS
            );
            if (!process_timeout) {
                fail("Sip processing not finished : operation (" + operationId + "). Timeout exceeded.");
            }
            assertThat(operationId).as(format("%s not found for request", X_REQUEST_ID)).isNotNull();
        }
    }

    /**
     * call vitam to upload the plan
     *
     * @throws IOException
     * @throws IngestExternalException
     */
    @When("^je télécharge le plan")
    public void upload_this_plan() throws IOException, VitamException {
        try (InputStream inputStream = Files.newInputStream(world.getSipFile(), StandardOpenOption.READ)) {
            RequestResponse<Void> response = world
                .getIngestClient()
                .ingest(
                    new VitamContext(world.getTenantId()).setApplicationSessionId(world.getApplicationSessionId()),
                    inputStream,
                    FILING_SCHEME.name(),
                    ProcessAction.RESUME.name()
                );

            final String operationId = response.getHeaderString(GlobalDataRest.X_REQUEST_ID);

            world.setOperationId(operationId);
            final VitamPoolingClient vitamPoolingClient = new VitamPoolingClient(world.getAdminClient());
            boolean process_timeout = vitamPoolingClient.wait(
                world.getTenantId(),
                operationId,
                ProcessState.COMPLETED,
                200,
                1_000L,
                TimeUnit.MILLISECONDS
            );
            if (!process_timeout) {
                fail("Sip processing not finished : operation (" + operationId + "). Timeout exceeded.");
            }
            assertThat(operationId).as(format("%s not found for request", X_REQUEST_ID)).isNotNull();
        }
        if (attachMode) {
            deleteSip = true;
        }
    }

    /**
     * call vitam to upload the tree
     *
     * @throws IOException
     * @throws IngestExternalException
     */
    @When("^je télécharge l'arbre")
    public void upload_this_tree() throws IOException, VitamException {
        try (InputStream inputStream = Files.newInputStream(world.getSipFile(), StandardOpenOption.READ)) {
            RequestResponse response = world
                .getIngestClient()
                .ingest(
                    new VitamContext(world.getTenantId()).setApplicationSessionId(world.getApplicationSessionId()),
                    inputStream,
                    HOLDING_SCHEME.name(),
                    ProcessAction.RESUME.name()
                );

            final String operationId = response.getHeaderString(GlobalDataRest.X_REQUEST_ID);

            world.setOperationId(operationId);
            final VitamPoolingClient vitamPoolingClient = new VitamPoolingClient(world.getAdminClient());
            boolean process_timeout = vitamPoolingClient.wait(
                world.getTenantId(),
                operationId,
                ProcessState.COMPLETED,
                100,
                1_000L,
                TimeUnit.MILLISECONDS
            );
            if (!process_timeout) {
                fail("Sip processing not finished : operation (" + operationId + "). Timeout exceeded.");
            }
            assertThat(operationId).as(format("%s not found for request", X_REQUEST_ID)).isNotNull();
        }
        if (attachMode) {
            deleteSip = true;
        }
    }

    @When("je construit le sip de rattachement avec le template")
    public void build_the_attachenment_by_systemid() throws IOException {
        world.setSipFile(
            SipTool.copyAndModifyManifestInZip(
                world.getSipFile(),
                SipTool.REPLACEMENT_STRING,
                world.getUnitId(),
                null,
                null
            )
        );
        attachMode = true;
    }

    @When(
        "j'utilise le template et construit un sip de rattachement avec comme nom et valeur de métadonnée (.*) et (.*)$"
    )
    public void build_the_attachenment_by_key_value(String metadataName, String metadataValue) throws IOException {
        if (_ID.equals(metadataName) && ID.equals(metadataValue)) {
            metadataValue = world.getUnitId();
        }
        world.setSipFile(
            SipTool.copyAndModifyManifestInZip(
                world.getSipFile(),
                SipTool.REPLACEMENT_NAME,
                metadataName,
                SipTool.REPLACEMENT_VALUE,
                metadataValue
            )
        );
        attachMode = true;
    }

    @When("je construit le SIP de rattachement au groupe d'objet existant avec le template")
    public void build_the_attachenment_to_existing_object_group() throws IOException {
        world.setSipFile(
            SipTool.copyAndModifyManifestInZip(
                world.getSipFile(),
                SipTool.REPLACEMENT_STRING,
                world.getObjectGroupId(),
                null,
                null
            )
        );
        attachMode = true;
    }

    /**
     * Execute an ingest OK and saves the operationId in test set map.
     *
     * @param fileName name of a sip
     */
    @Given("^les données du jeu de test du SIP nommé (.*)")
    public void use_test_set_from_sip(String fileName) {
        a_sip_named(fileName);
        if (!StringUtils.isNotBlank(World.getOperationId(fileName))) {
            try {
                upload_this_sip();
                LogbookEventOperation lastEvent = world
                    .getLogbookService()
                    .checkFinalStatusLogbook(
                        world.getAccessClient(),
                        world.getTenantId(),
                        world.getContractId(),
                        world.getApplicationSessionId(),
                        world.getOperationId(),
                        "OK"
                    );
                world.setLogbookEvent(lastEvent);
                World.setOperationId(fileName, world.getOperationId());
            } catch (VitamException | IOException e) {
                fail("Could not load test set : operation (" + world.getOperationId() + ") : ingest failure.", e);
            }
            return;
        }
        this.world.setOperationId(World.getOperationId(fileName));
    }

    @After
    public void afterScenario() throws IOException {
        if (world.getSipFile() != null && deleteSip) {
            try {
                // if we have a real guid, that means we were handling a created file, if not, that means the sip was
                // used in vitam-itests so we don't delete it !!
                GUIDReader.getGUID(world.getSipFile().getFileName().toString());
                Files.delete(world.getSipFile());
            } catch (final InvalidGuidOperationException e) {
                LOGGER.info("This is not a guid, no need to delete ", e);
            }
            deleteSip = false;
            attachMode = false;
        }
    }
}
