package fr.gouv.vitam.functionaltest.cucumber.step;

import com.fasterxml.jackson.databind.JsonNode;
import cucumber.api.DataTable;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;

import javax.ws.rs.core.Response;

import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.client.VitamRequestIterator;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Logbook check consistency step.
 */
public class LogbookCheckConsistencyStep {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookCheckConsistencyStep.class);
    public static final String INTERNAL_SERVER_ERROR = "Internal Server Error";

    private final World world;
    private VitamRequestIterator<JsonNode> result;

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
                Response repResponse = world.getLogbookOperationsClient().checkLogbookCoherence();
                LOGGER.debug("response -> ", repResponse.getStatus());
                repResponse.close();
            } catch (VitamException e) {
                LOGGER.error(INTERNAL_SERVER_ERROR);
                fail(INTERNAL_SERVER_ERROR);
            }
        });
    }

    @Then("^je verifie que la strategie contient le rapport de cohérence")
    public void the_logbook_consistency_check_report_list_srategy(DataTable dataTable)
        throws StorageException, StorageServerClientException {
        List<List<String>> raws = dataTable.raw();
        result = null;
        String strategy = raws.get(0).get(1);
        runInVitamThread(() -> {
            try {
                VitamThreadUtils.getVitamSession().setTenantId(world.getTenantId());
                result = world.storageClient.listContainer(strategy, DataCategory.CHECKLOGBOOKREPORTS);
            } catch (StorageServerClientException e) {
                throw new RuntimeException(e);
            }
        });
        assertThat(result).isNotNull();
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

