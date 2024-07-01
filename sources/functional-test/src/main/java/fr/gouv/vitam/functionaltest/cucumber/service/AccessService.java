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
package fr.gouv.vitam.functionaltest.cucumber.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Iterables;
import cucumber.api.DataTable;
import fr.gouv.vitam.access.external.client.AccessExternalClient;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import org.assertj.core.api.Fail;

import java.util.List;
import java.util.Set;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.in;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.match;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Access service containing common code for access
 */
public class AccessService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AccessService.class);

    private static final String TITLE = "Title";

    /**
     * Search an AU by its tile (unique in sip) and operationId (of the sip operation)
     *
     * @param accessClient access client
     * @param tenantId tenant id
     * @param contractId access contract id
     * @param applicationSessionId application session id
     * @param operationId logbook operation id
     * @param auTitle au title
     * @return AU GUID
     * @throws InvalidCreateOperationException exception
     * @throws VitamClientException exception
     */
    public String findUnitGUIDByTitleAndOperationId(
        AccessExternalClient accessClient,
        int tenantId,
        String contractId,
        String applicationSessionId,
        String operationId,
        String auTitle
    ) throws InvalidCreateOperationException, VitamClientException {
        String auId = "";
        SelectMultiQuery searchQuery = new SelectMultiQuery();
        searchQuery.addQueries(
            and().add(match(TITLE, "\"" + auTitle + "\"")).add(in(VitamFieldsHelper.operations(), operationId))
        );
        RequestResponse requestResponse = accessClient.selectUnits(
            new VitamContext(tenantId).setAccessContract(contractId).setApplicationSessionId(applicationSessionId),
            searchQuery.getFinalSelect()
        );
        if (requestResponse.isOk()) {
            RequestResponseOK<JsonNode> requestResponseOK = (RequestResponseOK<JsonNode>) requestResponse;
            if (requestResponseOK.getHits().getTotal() == 0) {
                Fail.fail("Archive Unit not found : title = " + auTitle);
            }
            JsonNode firstJsonNode = Iterables.get(requestResponseOK.getResults(), 0);
            if (firstJsonNode.get(VitamFieldsHelper.id()) != null) {
                auId = firstJsonNode.get(VitamFieldsHelper.id()).textValue();
            }
        } else {
            VitamError vitamError = (VitamError) requestResponse;
            Fail.fail("request selectUnit return an error: " + vitamError.getCode());
        }
        return auId;
    }

    /**
     * Check in the given result the expected datas
     *
     * @param results all results
     * @param resultNumber index of result
     * @param dataTable expected datas
     * @throws Throwable
     */
    public void checkResultsForParticularData(List<JsonNode> results, int resultNumber, DataTable dataTable)
        throws Throwable {
        JsonNode jsonNode = Iterables.get(results, resultNumber);

        checkResultsForParticularData(jsonNode, dataTable);
    }

    public void checkResultsForParticularData(JsonNode jsonNode, DataTable dataTable) throws Throwable {
        List<List<String>> raws = dataTable.raw();

        for (List<String> raw : raws) {
            String key = raw.get(0);
            boolean isArray = false;
            boolean isOfArray = false;

            if (null != key && key.endsWith(".array[][]")) {
                key = key.replace(".array[][]", "");
                isOfArray = true;
            }

            if (null != key && key.endsWith(".array[]")) {
                key = key.replace(".array[]", "");
                isArray = true;
            }

            String resultValue = getResultValue(jsonNode, key);
            if (null != resultValue) {
                resultValue = resultValue.replace("\n", "").replace("\\n", "");
            }
            // String resultExpected = transformToGuid(raw.get(1));
            String resultExpected = new String(raw.get(1));
            if (null != resultExpected) {
                resultExpected = resultExpected.replace("\n", "").replace("\\n", "");
            }

            if (!isArray && !isOfArray) {
                assertThat(resultValue).contains(resultExpected);
            } else {
                if (isArray) {
                    Set<String> resultArray = JsonHandler.getFromStringAsTypeReference(
                        resultValue,
                        new TypeReference<Set<String>>() {}
                    );

                    Set<String> expectedrray = JsonHandler.getFromStringAsTypeReference(
                        resultExpected,
                        new TypeReference<Set<String>>() {}
                    );
                    assertThat(resultArray).isEqualTo(expectedrray);
                } else {
                    Set<Set<String>> resultArray = JsonHandler.getFromStringAsTypeReference(
                        resultValue,
                        new TypeReference<Set<Set<String>>>() {}
                    );

                    Set<Set<String>> expectedrray = JsonHandler.getFromStringAsTypeReference(
                        resultExpected,
                        new TypeReference<Set<Set<String>>>() {}
                    );

                    assertThat(expectedrray).isEqualTo(resultArray);
                }
            }
        }
    }

    /**
     * Retrieve result value
     *
     * @param lastJsonNode result
     * @param key key of search value
     * @return json if found
     * @throws Throwable
     */
    private String getResultValue(JsonNode lastJsonNode, String key) throws Throwable {
        // String rawCopy = transformToGuid(raw);
        String rawCopy = new String(key);
        String[] paths = rawCopy.split("\\.");
        for (String path : paths) {
            if (lastJsonNode != null) {
                if (lastJsonNode.isArray()) {
                    try {
                        int value = Integer.valueOf(path);
                        lastJsonNode = lastJsonNode.get(value);
                    } catch (NumberFormatException e) {
                        LOGGER.warn(e);
                    }
                } else {
                    lastJsonNode = lastJsonNode.get(path);
                }
            }
        }

        if (lastJsonNode != null) {
            return JsonHandler.unprettyPrint(lastJsonNode);
        } else {
            return "{}";
        }
    }
}
