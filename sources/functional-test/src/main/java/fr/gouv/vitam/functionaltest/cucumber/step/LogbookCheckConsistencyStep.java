package fr.gouv.vitam.functionaltest.cucumber.step;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.model.coherence.LogbookCheckResult;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;

/**
 * Logbook check consistency step.
 */
public class LogbookCheckConsistencyStep {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookCheckConsistencyStep.class);
    public static final String INTERNAL_SERVER_ERROR = "Internal Server Error";

    private final World world;
    private LogbookCheckResult result;

    /**
     * constructor.
     *
     * @param world
     */
    public LogbookCheckConsistencyStep(World world) {
        this.world = world;
        new VitamContext(world.getTenantId());
    }

    @When("^je lance le test de cohérence des journaux")
    public void logbook_consistency_check() {
        runInVitamThread(() -> {
            try {
                // call checkLogbookCoherence service.
                VitamThreadUtils.getVitamSession().setTenantId(world.getTenantId());
                 result = world.getLogbookOperationsClient().checkLogbookCoherence();
            } catch (VitamException e) {
                LOGGER.error(INTERNAL_SERVER_ERROR);
                fail(INTERNAL_SERVER_ERROR);
            }
        });
    }

    @Then("^je verifie que le rapport du test de cohérence des journaux ne contient pas d'erreur")
    public void the_logbook_consistency_check_report_does_not_contain_errors()
        throws StorageException, StorageServerClientException, InvalidParseOperationException {
        assertThat(result).as("Le rapport du test de cohérence des journaux n'est pas disponible").isNotNull();
        String resultAsString = JsonHandler.prettyPrint(JsonHandler.toJsonNode(result));
        assertThat(result.getCheckErrors()).as(
                "Le rapport du test de cohérence des journaux contient une ou plusieurs erreurs : " + resultAsString)
                .isNullOrEmpty();
    }

    /**
     * runInVitamThread.
     *
     * @param
     */
    private void runInVitamThread(Runnable r) {
        Thread thread = VitamThreadFactory.getInstance().newThread(r);
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}

