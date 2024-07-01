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

import com.fasterxml.jackson.databind.JsonNode;
import cucumber.api.DataTable;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.AccessionRegisterDetailModel;
import fr.gouv.vitam.common.model.administration.AccessionRegisterSummaryModel;
import org.assertj.core.api.Fail;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step defining Accession register behavior
 */
public class AccessionRegisterStep extends CommonStep {

    private static final String ACCESSION_REGISTER_SUMMARY_ERROR_MESSAGE =
        "no accession register summary in result since an error occured: ";
    private static final String ACCESSION_REGISTER_DETAIL_ERROR_MESSAGE =
        "no accession register detail in result since an error occured: ";
    private RequestResponse requestResponse;

    public AccessionRegisterStep(World world) {
        super(world);
    }

    /**
     * Search accession register summary by query
     *
     * @throws Throwable
     */
    @When("^je recherche les registres de fond$")
    public void search_accession_register() throws Throwable {
        JsonNode queryJSON = JsonHandler.getFromString(world.getQuery());
        RequestResponse<AccessionRegisterSummaryModel> requestResponse = world
            .getAdminClient()
            .findAccessionRegister(
                new VitamContext(world.getTenantId())
                    .setAccessContract(world.getContractId())
                    .setApplicationSessionId(world.getApplicationSessionId()),
                queryJSON
            );
        this.requestResponse = requestResponse;
    }

    /**
     * Search accession register details by query
     *
     * @throws Throwable
     */
    @When("^je recherche les détails des registres de fonds$")
    public void search_accession_register_details() throws Throwable {
        JsonNode queryJSON = JsonHandler.getFromString(world.getQuery());
        RequestResponse<AccessionRegisterDetailModel> requestResponse = world
            .getAdminClient()
            .findAccessionRegisterDetails(
                new VitamContext(world.getTenantId())
                    .setAccessContract(world.getContractId())
                    .setApplicationSessionId(world.getApplicationSessionId()),
                queryJSON
            );
        this.requestResponse = requestResponse;
    }

    /**
     * Search accession register detail by query in originating agency
     *
     * @param originatingAgency originating agency
     * @throws Throwable
     */
    @When("^je recherche les détails des registres de fond pour le service producteur (.*)$")
    public void search_accession_regiter_detail(String originatingAgency) throws Throwable {
        JsonNode queryJSON = JsonHandler.getFromString(world.getQuery());
        RequestResponse requestResponse = world
            .getAdminClient()
            .getAccessionRegisterDetail(
                new VitamContext(world.getTenantId())
                    .setAccessContract(world.getContractId())
                    .setApplicationSessionId(world.getApplicationSessionId()),
                originatingAgency,
                queryJSON
            );
        this.requestResponse = requestResponse;
    }

    /**
     * Count the number of results for accession register summary result
     *
     * @param numberOfResults number of results expected
     * @throws Throwable
     */
    @Then("^le nombre de registres de fond est (\\d+)$")
    public void number_of_accession_register_summary_result_are(int numberOfResults) throws Throwable {
        if (requestResponse.isOk()) {
            assertThat(((RequestResponseOK) requestResponse).getResults()).hasSize(numberOfResults);
        } else {
            VitamError vitamError = (VitamError) requestResponse;
            Fail.fail(ACCESSION_REGISTER_SUMMARY_ERROR_MESSAGE + JsonHandler.prettyPrint(vitamError));
        }
    }

    /**
     * Count the number of results for accession details summary result
     *
     * @param numberOfResults number of results expected
     * @throws Throwable
     */
    @Then("^le nombre de détails du registre de fond est (\\d+)$")
    public void number_of_accession_register_detail_result_are(int numberOfResults) throws Throwable {
        if (requestResponse.isOk()) {
            assertThat(((RequestResponseOK) requestResponse).getResults()).hasSize(numberOfResults);
        } else {
            VitamError vitamError = (VitamError) requestResponse;
            Fail.fail(ACCESSION_REGISTER_DETAIL_ERROR_MESSAGE + JsonHandler.prettyPrint(vitamError));
        }
    }

    /**
     * Check accession register summary data for first result
     *
     * @param dataTable expected results
     * @throws Throwable
     */
    @Then("^les metadonnées pour le registre de fond sont$")
    public void metadata_accession_register_summary_are(DataTable dataTable) throws Throwable {
        metadata_accession_register_summary_are_for_particular_result(0, dataTable);
    }

    /**
     * Check accession register detail data for first result
     *
     * @param dataTable expected results
     * @throws Throwable
     */
    @Then("^les metadonnées pour le détail du registre de fond sont$")
    public void metadata_accession_register_detail_are(DataTable dataTable) throws Throwable {
        metadata_accession_register_detail_are_for_particular_result(0, dataTable);
    }

    /**
     * Check accession register summary data for result given
     *
     * @param resultNumber result number
     * @param dataTable expected results
     * @throws Throwable
     */
    @Then("^les metadonnées pour le registre de fond numéro (\\d+) sont$")
    public void metadata_accession_register_summary_are_for_particular_result(int resultNumber, DataTable dataTable)
        throws Throwable {
        if (requestResponse.isOk()) {
            world
                .getAccessService()
                .checkResultsForParticularData(
                    ((RequestResponseOK) requestResponse).getResultsAsJsonNodes(),
                    resultNumber,
                    dataTable
                );
        } else {
            VitamError vitamError = (VitamError) requestResponse;
            Fail.fail(ACCESSION_REGISTER_SUMMARY_ERROR_MESSAGE + JsonHandler.prettyPrint(vitamError));
        }
    }

    /**
     * Check accession register detail data for result given
     *
     * @param resultNumber result number
     * @param dataTable expected results
     * @throws Throwable
     */
    @Then("^les metadonnées pour le détail du registre de fond numéro (\\d+) sont$")
    public void metadata_accession_register_detail_are_for_particular_result(int resultNumber, DataTable dataTable)
        throws Throwable {
        if (requestResponse.isOk()) {
            world
                .getAccessService()
                .checkResultsForParticularData(
                    ((RequestResponseOK) requestResponse).getResultsAsJsonNodes(),
                    resultNumber,
                    dataTable
                );
        } else {
            VitamError vitamError = (VitamError) requestResponse;
            Fail.fail(ACCESSION_REGISTER_DETAIL_ERROR_MESSAGE + JsonHandler.prettyPrint(vitamError));
        }
    }
}
