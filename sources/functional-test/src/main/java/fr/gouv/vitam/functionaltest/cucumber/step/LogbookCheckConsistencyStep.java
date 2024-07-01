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

import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.model.coherence.LogbookCheckResult;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Logbook check consistency step.
 */
public class LogbookCheckConsistencyStep extends CommonStep {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(LogbookCheckConsistencyStep.class);
    public static final String INTERNAL_SERVER_ERROR = "Internal Server Error";

    private LogbookCheckResult result;

    /**
     * constructor.
     *
     * @param world
     */
    public LogbookCheckConsistencyStep(World world) {
        super(world);
        new VitamContext(world.getTenantId());
    }

    @When("^je lance le test de cohérence des journaux")
    public void logbook_consistency_check() {
        runInVitamThread(() -> {
            try {
                // call checkLogbookCoherence service.
                VitamThreadUtils.getVitamSession().setTenantId(world.getTenantId());
                result = world.getLogbookOperationsClient().checkLogbookCoherence();
                assertThat(result).as("Le rapport du test de cohérence des journaux n'est pas disponible").isNotNull();
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
        assertThat(result.getCheckErrors())
            .as("Le rapport du test de cohérence des journaux contient une ou plusieurs erreurs : " + resultAsString)
            .isNullOrEmpty();
    }
}
