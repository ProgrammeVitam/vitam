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
package fr.gouv.vitam.collect.internal.resource;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.collect.common.enums.TransactionStatus;
import fr.gouv.vitam.collect.common.exception.CollectInternalException;
import fr.gouv.vitam.collect.internal.core.common.ProjectModel;
import fr.gouv.vitam.collect.internal.core.common.TransactionModel;
import fr.gouv.vitam.collect.internal.core.helpers.CollectHelper;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.utils.ScrollSpliterator;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponseOK;
import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import static io.restassured.RestAssured.given;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.OK;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class TransactionInternalResourceIT extends CollectInternalResourceBaseIT {
    @Test
    public void shouldUpdateUnits() throws CollectInternalException {
        final String metadataResourcePath = "transaction/units/update/metadata.csv";
        final String projectsResourcePath = "transaction/units/update/projects.json";
        final String transactionsResourcePath = "transaction/units/update/transactions.json";
        final String getUnitsResultsResourcePath = "transaction/units/update/get-units-results.json";
        final String updateResultsResourcePath = "transaction/units/update/update-results.json";

        try {
            final ProjectModel[] projects =
                JsonHandler.getFromString(PropertiesUtils.getResourceAsString(projectsResourcePath),
                    ProjectModel[].class);
            final TransactionModel[] transactions =
                JsonHandler.getFromString(PropertiesUtils.getResourceAsString(transactionsResourcePath),
                    TransactionModel[].class);
            final JsonNode getUnitsResults = JsonHandler.getFromString(PropertiesUtils.getResourceAsString(getUnitsResultsResourcePath), JsonNode.class);

            final JsonNode updateResults =
                JsonHandler.getFromString(PropertiesUtils.getResourceAsString(updateResultsResourcePath),
                    JsonNode.class);
            final ProjectModel project = projects[0];
            final TransactionModel transaction = transactions[0];
            final SelectMultiQuery select = new SelectMultiQuery();
            try {
                select.addQueries(QueryHelper.eq(VitamFieldsHelper.initialOperation(), transaction.getId()));
            } catch (InvalidCreateOperationException e) {
                throw new RuntimeException(e);
            }
            select.addUsedProjection(VitamFieldsHelper.id(), "Title", VitamFieldsHelper.unitups(),
                VitamFieldsHelper.allunitups());

            when(transactionService.findTransaction(transaction.getId())).thenReturn(Optional.of(transaction));
            when(transactionService.checkStatus(any(TransactionModel.class), eq(TransactionStatus.OPEN))).thenReturn(
                true);
            when(projectService.findProject(project.getId())).thenReturn(
                Optional.of(CollectHelper.convertProjectModeltoProjectDto(project)));
            when(metadataRepository.selectUnits(any(SelectMultiQuery.class), eq(transaction.getId()))).thenReturn(new ScrollSpliterator<>(select, selectMultiQuery -> {
                try {
                    return JsonHandler.getFromJsonNode(getUnitsResults, RequestResponseOK.class, JsonNode.class);
                } catch (InvalidParseOperationException e) {
                    throw new RuntimeException(e);
                }
            }, VitamConfiguration.getElasticSearchScrollTimeoutInMilliseconds(), VitamConfiguration.getElasticSearchScrollLimit()));
            when(metadataRepository.atomicBulkUpdate(any())).thenReturn(new RequestResponseOK<>(updateResults));

            try (final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(metadataResourcePath)) {
                given()
                    .contentType(ContentType.BINARY)
                    .header(GlobalDataRest.X_TENANT_ID, 0)
                    .body(resourceAsStream)
                    .when()
                    .put("transactions/" + transaction.getId() + "/units")
                    .then()
                    .statusCode(OK.getStatusCode());
            } catch (FileNotFoundException e) {
                Assert.fail(String.format("File not found on %s: %s", metadataResourcePath, e.getLocalizedMessage()));
            } catch (IOException e) {
                Assert.fail(String.format("IO exception on %s: %s", metadataResourcePath, e.getLocalizedMessage()));
            }
        } catch (FileNotFoundException e) {
            Assert.fail("File not found: " + e.getLocalizedMessage());
        } catch (InvalidParseOperationException e) {
            Assert.fail("Fail while parsing resources: " + e.getLocalizedMessage());
        }
    }

    @Test
    public void shouldGetErrorWhenUpdateWithCsvWithCommasSeparators() throws CollectInternalException {
        final String metadataResourcePath = "transaction/units/update/metadata-with-commas.csv";
        final String projectsResourcePath = "transaction/units/update/projects.json";
        final String transactionsResourcePath = "transaction/units/update/transactions.json";
        final String getUnitsResultsResourcePath = "transaction/units/update/get-units-results.json";
        final String updateResultsResourcePath = "transaction/units/update/update-results.json";

        try {
            final ProjectModel[] projects =
                JsonHandler.getFromString(PropertiesUtils.getResourceAsString(projectsResourcePath),
                    ProjectModel[].class);
            final TransactionModel[] transactions =
                JsonHandler.getFromString(PropertiesUtils.getResourceAsString(transactionsResourcePath),
                    TransactionModel[].class);
            final JsonNode getUnitsResults = JsonHandler.getFromString(PropertiesUtils.getResourceAsString(getUnitsResultsResourcePath), JsonNode.class);

            final JsonNode updateResults =
                JsonHandler.getFromString(PropertiesUtils.getResourceAsString(updateResultsResourcePath),
                    JsonNode.class);
            final ProjectModel project = projects[0];
            final TransactionModel transaction = transactions[0];
            final SelectMultiQuery select = new SelectMultiQuery();
            try {
                select.addQueries(QueryHelper.eq(VitamFieldsHelper.initialOperation(), transaction.getId()));
            } catch (InvalidCreateOperationException e) {
                throw new RuntimeException(e);
            }
            select.addUsedProjection(VitamFieldsHelper.id(), "Title", VitamFieldsHelper.unitups(),
                VitamFieldsHelper.allunitups());

            when(transactionService.findTransaction(transaction.getId())).thenReturn(Optional.of(transaction));
            when(transactionService.checkStatus(any(TransactionModel.class), eq(TransactionStatus.OPEN))).thenReturn(
                true);
            when(projectService.findProject(project.getId())).thenReturn(
                Optional.of(CollectHelper.convertProjectModeltoProjectDto(project)));
            when(metadataRepository.selectUnits(any(SelectMultiQuery.class), eq(transaction.getId()))).thenReturn(new ScrollSpliterator<>(select, selectMultiQuery -> {
                try {
                    return JsonHandler.getFromJsonNode(getUnitsResults, RequestResponseOK.class, JsonNode.class);
                } catch (InvalidParseOperationException e) {
                    throw new RuntimeException(e);
                }
            }, VitamConfiguration.getElasticSearchScrollTimeoutInMilliseconds(), VitamConfiguration.getElasticSearchScrollLimit()));
            when(metadataRepository.atomicBulkUpdate(any())).thenReturn(new RequestResponseOK<>(updateResults));

            try (final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(metadataResourcePath)) {
                given()
                    .contentType(ContentType.BINARY)
                    .header(GlobalDataRest.X_TENANT_ID, 0)
                    .body(resourceAsStream)
                    .when()
                    .put("transactions/" + transaction.getId() + "/units")
                    .then()
                    .statusCode(INTERNAL_SERVER_ERROR.getStatusCode())
                    .body("message", Matchers.equalTo("Internal Server Error"))
                    .body("description", Matchers.equalTo("Mapping for File not found, expected one of [File,Content.DescriptionLevel,Content.Title,Content.Description,Content.StartDate,Content.EndDate]"));
            } catch (FileNotFoundException e) {
                Assert.fail(String.format("File not found on %s: %s", metadataResourcePath, e.getLocalizedMessage()));
            } catch (IOException e) {
                Assert.fail(String.format("IO exception on %s: %s", metadataResourcePath, e.getLocalizedMessage()));
            }
        } catch (FileNotFoundException e) {
            Assert.fail("File not found: " + e.getLocalizedMessage());
        } catch (InvalidParseOperationException e) {
            Assert.fail("Fail while parsing resources: " + e.getLocalizedMessage());
        }
    }

    @Test
    public void shouldGetErrorWhenUpdateUnitsWithNoFilesInCsv() throws CollectInternalException {
        final String metadataResourcePath = "transaction/units/update/metadata-with-no-file.csv";
        final String projectsResourcePath = "transaction/units/update/projects.json";
        final String transactionsResourcePath = "transaction/units/update/transactions.json";
        final String getUnitsResultsResourcePath = "transaction/units/update/get-units-results.json";
        final String updateResultsResourcePath = "transaction/units/update/update-results.json";

        try {
            final ProjectModel[] projects =
                JsonHandler.getFromString(PropertiesUtils.getResourceAsString(projectsResourcePath),
                    ProjectModel[].class);
            final TransactionModel[] transactions =
                JsonHandler.getFromString(PropertiesUtils.getResourceAsString(transactionsResourcePath),
                    TransactionModel[].class);
            final JsonNode getUnitsResults = JsonHandler.getFromString(PropertiesUtils.getResourceAsString(getUnitsResultsResourcePath), JsonNode.class);

            final JsonNode updateResults =
                JsonHandler.getFromString(PropertiesUtils.getResourceAsString(updateResultsResourcePath),
                    JsonNode.class);
            final ProjectModel project = projects[0];
            final TransactionModel transaction = transactions[0];
            final SelectMultiQuery select = new SelectMultiQuery();
            try {
                select.addQueries(QueryHelper.eq(VitamFieldsHelper.initialOperation(), transaction.getId()));
            } catch (InvalidCreateOperationException e) {
                throw new RuntimeException(e);
            }
            select.addUsedProjection(VitamFieldsHelper.id(), "Title", VitamFieldsHelper.unitups(),
                VitamFieldsHelper.allunitups());

            when(transactionService.findTransaction(transaction.getId())).thenReturn(Optional.of(transaction));
            when(transactionService.checkStatus(any(TransactionModel.class), eq(TransactionStatus.OPEN))).thenReturn(
                true);
            when(projectService.findProject(project.getId())).thenReturn(
                Optional.of(CollectHelper.convertProjectModeltoProjectDto(project)));
            when(metadataRepository.selectUnits(any(SelectMultiQuery.class), eq(transaction.getId()))).thenReturn(new ScrollSpliterator<>(select, selectMultiQuery -> {
                try {
                    return JsonHandler.getFromJsonNode(getUnitsResults, RequestResponseOK.class, JsonNode.class);
                } catch (InvalidParseOperationException e) {
                    throw new RuntimeException(e);
                }
            }, VitamConfiguration.getElasticSearchScrollTimeoutInMilliseconds(), VitamConfiguration.getElasticSearchScrollLimit()));
            when(metadataRepository.atomicBulkUpdate(any())).thenReturn(new RequestResponseOK<>(updateResults));

            try (final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(metadataResourcePath)) {
                given()
                    .contentType(ContentType.BINARY)
                    .header(GlobalDataRest.X_TENANT_ID, 0)
                    .body(resourceAsStream)
                    .when()
                    .put("transactions/" + transaction.getId() + "/units")
                    .then()
                    .statusCode(INTERNAL_SERVER_ERROR.getStatusCode())
                    .body("message", Matchers.containsString("no update data found !"))
                    .body("description", Matchers.equalTo(null));
            } catch (FileNotFoundException e) {
                Assert.fail(String.format("File not found on %s: %s", metadataResourcePath, e.getLocalizedMessage()));
            } catch (IOException e) {
                Assert.fail(String.format("IO exception on %s: %s", metadataResourcePath, e.getLocalizedMessage()));
            }
        } catch (FileNotFoundException e) {
            Assert.fail("File not found: " + e.getLocalizedMessage());
        } catch (InvalidParseOperationException e) {
            Assert.fail("Fail while parsing resources: " + e.getLocalizedMessage());
        }
    }
    @Test
    public void shouldGetErrorWhenUpdateUnitsWithDuplicateFilesInCsv() throws CollectInternalException {
        final String metadataResourcePath = "transaction/units/update/metadata-with-duplicates.csv";
        final String projectsResourcePath = "transaction/units/update/projects.json";
        final String transactionsResourcePath = "transaction/units/update/transactions.json";
        final String getUnitsResultsResourcePath = "transaction/units/update/get-units-results.json";
        final String updateResultsResourcePath = "transaction/units/update/update-results.json";

        try {
            final ProjectModel[] projects =
                JsonHandler.getFromString(PropertiesUtils.getResourceAsString(projectsResourcePath),
                    ProjectModel[].class);
            final TransactionModel[] transactions =
                JsonHandler.getFromString(PropertiesUtils.getResourceAsString(transactionsResourcePath),
                    TransactionModel[].class);
            final JsonNode getUnitsResults = JsonHandler.getFromString(PropertiesUtils.getResourceAsString(getUnitsResultsResourcePath), JsonNode.class);

            final JsonNode updateResults =
                JsonHandler.getFromString(PropertiesUtils.getResourceAsString(updateResultsResourcePath),
                    JsonNode.class);
            final ProjectModel project = projects[0];
            final TransactionModel transaction = transactions[0];
            final SelectMultiQuery select = new SelectMultiQuery();
            try {
                select.addQueries(QueryHelper.eq(VitamFieldsHelper.initialOperation(), transaction.getId()));
            } catch (InvalidCreateOperationException e) {
                throw new RuntimeException(e);
            }
            select.addUsedProjection(VitamFieldsHelper.id(), "Title", VitamFieldsHelper.unitups(),
                VitamFieldsHelper.allunitups());

            when(transactionService.findTransaction(transaction.getId())).thenReturn(Optional.of(transaction));
            when(transactionService.checkStatus(any(TransactionModel.class), eq(TransactionStatus.OPEN))).thenReturn(
                true);
            when(projectService.findProject(project.getId())).thenReturn(
                Optional.of(CollectHelper.convertProjectModeltoProjectDto(project)));
            when(metadataRepository.selectUnits(any(SelectMultiQuery.class), eq(transaction.getId()))).thenReturn(new ScrollSpliterator<>(select, selectMultiQuery -> {
                try {
                    return JsonHandler.getFromJsonNode(getUnitsResults, RequestResponseOK.class, JsonNode.class);
                } catch (InvalidParseOperationException e) {
                    throw new RuntimeException(e);
                }
            }, VitamConfiguration.getElasticSearchScrollTimeoutInMilliseconds(), VitamConfiguration.getElasticSearchScrollLimit()));
            when(metadataRepository.atomicBulkUpdate(any())).thenReturn(new RequestResponseOK<>(updateResults));

            try (final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(metadataResourcePath)) {
                given()
                    .contentType(ContentType.BINARY)
                    .header(GlobalDataRest.X_TENANT_ID, 0)
                    .body(resourceAsStream)
                    .when()
                    .put("transactions/" + transaction.getId() + "/units")
                    .then()
                    .statusCode(INTERNAL_SERVER_ERROR.getStatusCode())
                    .body("message", Matchers.containsString("Internal Server Error"))
                    .body("description", Matchers.containsString("Duplicate key versement/pastis.json"));
            } catch (FileNotFoundException e) {
                Assert.fail(String.format("File not found on %s: %s", metadataResourcePath, e.getLocalizedMessage()));
            } catch (IOException e) {
                Assert.fail(String.format("IO exception on %s: %s", metadataResourcePath, e.getLocalizedMessage()));
            }
        } catch (FileNotFoundException e) {
            Assert.fail("File not found: " + e.getLocalizedMessage());
        } catch (InvalidParseOperationException e) {
            Assert.fail("Fail while parsing resources: " + e.getLocalizedMessage());
        }
    }

    @Test
    public void shouldGetErrorWhenUpdateUnitsWithWrongFilesPathInCsv() throws CollectInternalException {
        final String metadataResourcePath = "transaction/units/update/metadata-with-wrong-file-path.csv";
        final String projectsResourcePath = "transaction/units/update/projects.json";
        final String transactionsResourcePath = "transaction/units/update/transactions.json";
        final String getUnitsResultsResourcePath = "transaction/units/update/get-units-results.json";
        final String updateResultsResourcePath = "transaction/units/update/update-results.json";

        try {
            final ProjectModel[] projects =
                JsonHandler.getFromString(PropertiesUtils.getResourceAsString(projectsResourcePath),
                    ProjectModel[].class);
            final TransactionModel[] transactions =
                JsonHandler.getFromString(PropertiesUtils.getResourceAsString(transactionsResourcePath),
                    TransactionModel[].class);
            final JsonNode getUnitsResults = JsonHandler.getFromString(PropertiesUtils.getResourceAsString(getUnitsResultsResourcePath), JsonNode.class);

            final JsonNode updateResults =
                JsonHandler.getFromString(PropertiesUtils.getResourceAsString(updateResultsResourcePath),
                    JsonNode.class);
            final ProjectModel project = projects[0];
            final TransactionModel transaction = transactions[0];
            final SelectMultiQuery select = new SelectMultiQuery();
            try {
                select.addQueries(QueryHelper.eq(VitamFieldsHelper.initialOperation(), transaction.getId()));
            } catch (InvalidCreateOperationException e) {
                throw new RuntimeException(e);
            }
            select.addUsedProjection(VitamFieldsHelper.id(), "Title", VitamFieldsHelper.unitups(),
                VitamFieldsHelper.allunitups());

            when(transactionService.findTransaction(transaction.getId())).thenReturn(Optional.of(transaction));
            when(transactionService.checkStatus(any(TransactionModel.class), eq(TransactionStatus.OPEN))).thenReturn(
                true);
            when(projectService.findProject(project.getId())).thenReturn(
                Optional.of(CollectHelper.convertProjectModeltoProjectDto(project)));
            when(metadataRepository.selectUnits(any(SelectMultiQuery.class), eq(transaction.getId()))).thenReturn(new ScrollSpliterator<>(select, selectMultiQuery -> {
                try {
                    return JsonHandler.getFromJsonNode(getUnitsResults, RequestResponseOK.class, JsonNode.class);
                } catch (InvalidParseOperationException e) {
                    throw new RuntimeException(e);
                }
            }, VitamConfiguration.getElasticSearchScrollTimeoutInMilliseconds(), VitamConfiguration.getElasticSearchScrollLimit()));
            when(metadataRepository.atomicBulkUpdate(any())).thenReturn(new RequestResponseOK<>(updateResults));

            try (final InputStream resourceAsStream = PropertiesUtils.getResourceAsStream(metadataResourcePath)) {
                given()
                    .contentType(ContentType.BINARY)
                    .header(GlobalDataRest.X_TENANT_ID, 0)
                    .body(resourceAsStream)
                    .when()
                    .put("transactions/" + transaction.getId() + "/units")
                    .then()
                    .statusCode(INTERNAL_SERVER_ERROR.getStatusCode())
                    .body("message", Matchers.containsString("Cannot find unit with path no-dir"));
            } catch (FileNotFoundException e) {
                Assert.fail(String.format("File not found on %s: %s", metadataResourcePath, e.getLocalizedMessage()));
            } catch (IOException e) {
                Assert.fail(String.format("IO exception on %s: %s", metadataResourcePath, e.getLocalizedMessage()));
            }
        } catch (FileNotFoundException e) {
            Assert.fail("File not found: " + e.getLocalizedMessage());
        } catch (InvalidParseOperationException e) {
            Assert.fail("Fail while parsing resources: " + e.getLocalizedMessage());
        }
    }
}
