/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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

import com.fasterxml.jackson.core.JsonProcessingException;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import fr.gouv.vitam.collect.internal.dto.TransactionDto;
import fr.gouv.vitam.collect.internal.helpers.TransactionDtoBuilder;
import fr.gouv.vitam.common.FileUtil;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;

import java.nio.file.Path;
import java.nio.file.Paths;

public class CollectStep {

    private String transactionGuuid;
    private final World world;

    public CollectStep(World world) {
        this.world = world;
    }

    /**
     * define a query from a file to reuse it after
     *
     * @param queryFilename name of the file containing the query
     * @throws Throwable
     */
    @When("^j'utilise le fichier de requête (.*)$")
    public void i_use_the_following_file_query(String queryFilename) throws Throwable {
        Path queryFile = Paths.get(world.getBaseDirectory(), queryFilename);
        String query = FileUtil.readFile(queryFile.toFile());
        world.setQuery(query);
    }

    @Given("^un utilisateur possédant le rôle (.*)$")
    public void checkUserAcces(String access) {
        //TODO : To complete after define Collect user access
        //Nothing for the moment
    }

    @When("^j'initialise une transaction$")
    public void initTransaction() throws InvalidParseOperationException {
        TransactionDto transactionDto = new TransactionDtoBuilder()
                .withArchivalAgencyIdentifier("ArchivalAgencyIdentifier")
                .withTransferingAgencyIdentifier("TransferingAgencyIdentifier")
                .withOriginatingAgencyIdentifier("FRAN_NP_009913")
                .withArchivalProfile("ArchiveProfile")
                .withComment("Comments")
                .build();

        //RequestResponseOK<TransactionDto> response = world.getCollectClient().initTransaction(transactionDto);
        //assertThat(response.isOk()).isTrue();
        //transactionGuuid = response.getFirstResult().getId();
    }

    @Then("^le service de collecte me retourne le guuid de la transaction$")
    public void checkTransactionGuuid() {
        //assertThat(transactionGuuid).isNotNull();
    }

    @When("^j'envoie une AU$")
    public void uploadArchiveUnit() throws InvalidParseOperationException, JsonProcessingException {
/*         JsonNode archiveUnitJson =  JsonHandler.getFromString(world.getQuery());
         RequestResponseOK<JsonNode> response = world.getCollectClient().uploadArchiveUnit(transactionGuuid, archiveUnitJson);
         assertThat(response.isOk()).isTrue();
         assertThat(response.getFirstResult()).isNotNull();*/
    }

}
