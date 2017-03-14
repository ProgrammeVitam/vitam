/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.functionaltest.cucumber.step;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.in;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.assertj.core.api.Fail;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Iterables;

import cucumber.api.DataTable;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.multiple.Select;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;

/**
 * step defining access glue
 */
public class AccessStep {

    private static final String OPERATION_ID = "Operation-Id";

    private static final String UNIT_PREFIX = "unit:";

    private static final String REGEX = "(\\{\\{(.*?)\\}\\})";

    private static final String TITLE = "Title";

    private List<JsonNode> results;

    private World world;

    private String query;

    public AccessStep(World world) {
        this.world = world;
    }

    /**
     * check if the metadata are valid.
     * @param dataTable
     * @throws Throwable
     */
    @Then("^les metadonnées sont$")
    public void metadata_are(DataTable dataTable) throws Throwable {

        JsonNode firstJsonNode = Iterables.get(results, 0);
        
        List<List<String>> raws = dataTable.raw();

        for (List<String> raw : raws) {
            String resultValue = getResultValue(firstJsonNode, raw.get(0)); 
            String resultExpected = transformToGuid(raw.get(1)); 
            assertThat(resultValue).contains(resultExpected);
        }
    }

    /**
     * @param lastJsonNode
     * @param raw
     * @return
     * @throws Throwable 
     */
    private String getResultValue(JsonNode lastJsonNode, String raw) throws Throwable {
        String rawCopy = transformToGuid(raw);
        String[] paths = rawCopy.split("\\.");
        for (String path: paths ) {
            lastJsonNode = lastJsonNode.get(path);
        }
        
        return JsonHandler.unprettyPrint(lastJsonNode);
    }
    
    private String transformToGuid(String raw) throws Throwable {

        Matcher matcher = Pattern.compile(REGEX)
            .matcher(raw);
        String rawCopy = new String(raw);
        Map<String, String> unitToGuid = new HashMap<>();
        while (matcher.find()) {
          String unit = matcher.group(1);
          String unitTitle = unit.substring(2, unit.length()-2).replace(UNIT_PREFIX, "").trim();
          String unitGuid = "";
          if (unitToGuid.get(unitTitle) != null) {
              unitGuid = unitToGuid.get(unitTitle);
          } else {
              unitGuid = replaceTitleByGUID(unitTitle);
              unitToGuid.put(unitTitle, unitGuid);
          }
          rawCopy = rawCopy.replace(unit, unitGuid);
        }
        return rawCopy;
    }

    /**
     * @param substring
     */
    private String replaceTitleByGUID(String auTitle) throws Throwable {
        String auId = "";
        Select searchQuery = new Select();
        searchQuery.addQueries(
            and().add(eq(TITLE, auTitle)).add(in(VitamFieldsHelper.operations(), world.getOperationId())).setDepthLimit(20));
        RequestResponse requestResponse = world.getAccessClient().selectUnits(searchQuery.getFinalSelect(), world.getTenantId());
        if (requestResponse.isOk()) {
            RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) requestResponse;
            if (requestResponseOK.getHits().getTotal() == 0) {
                Fail.fail("Archive Unit not found : title = " + auTitle); 
            }
            JsonNode firstJsonNode = Iterables.get(requestResponseOK.getResults(), 0);
            if (firstJsonNode.get(VitamFieldsHelper.id()) != null) {
                auId =  firstJsonNode.get(VitamFieldsHelper.id()).textValue();
            }
        } else {
            VitamError vitamError = (VitamError) requestResponse;
            Fail.fail("request selectUnit return an error: " + vitamError.getCode());
        }
        return auId;
    }

    /**
     * check if the number of result is OK
     * @param numberOfResult number of result.
     * @throws Throwable
     */
    @Then("^le nombre de résultat est (\\d+)$")
    public void number_of_result_are(int numberOfResult) throws Throwable {
        assertThat(results).hasSize(numberOfResult);
    }

    /**
     * define a query to reuse it after
     * @param query
     * @throws Throwable
     */
    @When("^j'utilise la requête suivante$")
    public void i_use_the_following_query(String query) throws Throwable {
        this.query = query.replace(OPERATION_ID, world.getOperationId());
    }

    /**
     * search an archive unit according to the query define before
     * @throws Throwable
     */
    @When("^je recherche les unités archivistiques$")
    public void search_archive_unit() throws Throwable {
        JsonNode queryJSON = JsonHandler.getFromString(query);
        RequestResponse<JsonNode> requestResponse = world.getAccessClient().selectUnits(queryJSON, world.getTenantId());
        if (requestResponse.isOk()) {
            RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) requestResponse;
            results = requestResponseOK.getResults();
        } else {
            VitamError vitamError = (VitamError) requestResponse;
            Fail.fail("request selectUnit return an error: " + vitamError.getCode());
        }
    }

}
