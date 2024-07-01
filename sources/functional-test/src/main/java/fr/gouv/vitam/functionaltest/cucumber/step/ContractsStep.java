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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cucumber.api.DataTable;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import fr.gouv.vitam.access.external.api.AdminCollections;
import fr.gouv.vitam.access.external.client.AdminExternalClient;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientException;
import fr.gouv.vitam.common.FileUtil;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.database.builder.query.action.SetAction;
import fr.gouv.vitam.common.database.builder.query.action.UpdateActionHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Update;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.ContextModel;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.model.administration.ManagementContractModel;
import fr.gouv.vitam.common.model.administration.PermissionModel;
import fr.gouv.vitam.ingest.external.api.exception.IngestExternalException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static org.assertj.core.api.Assertions.assertThat;

public class ContractsStep extends CommonStep {

    public static final String CONTEXT_IDENTIFIER = "CT-000001";
    public static final String INGEST_CONTRACT_NOT_IN_CONTEXT = "IngestContractNotInContext";

    public ContractsStep(World world) {
        super(world);
    }

    private String fileName;

    /**
     * @return generic Model
     */
    public JsonNode getModel() {
        return model;
    }

    public String getContractType() {
        return contractType;
    }

    public void setContractType(String contractType) {
        this.contractType = contractType;
    }

    public void setModel(JsonNode model) {
        this.model = model;
    }

    /**
     * generic model result
     */
    private JsonNode model;
    /**
     * type de contrat
     */
    private String contractType;

    /**
     * define a contract json file
     *
     * @param fileName name of contract json file
     */
    @Given("^un contract nommé (.*)$")
    public void a_contract_json_named(String fileName) {
        this.fileName = fileName;
    }

    /**
     * Use Only when the contract is not in the database
     *
     * @param type the type of contract
     * @throws IOException
     * @throws IngestExternalException
     */
    @Then("^j'importe ce contrat de type (.*)")
    public void upload_contract(String type) throws Exception {
        uploadContract(type, CONTEXT_IDENTIFIER, true);
    }

    /**
     * Tentative d'import d'un contrat si jamais il n'existe pas
     *
     * @param type
     * @throws IOException
     */
    @Then("^j'importe ce contrat sans échec de type (.*)")
    public void upload_contract_without_failure(String type) {
        try {
            uploadContract(type, CONTEXT_IDENTIFIER, null);
        } catch (Exception e) {
            // catch nothing
        }
    }

    /**
     * Upload a contract that will lead to an error
     *
     * @param type the type of contract
     * @throws IOException
     * @throws IngestExternalException
     */
    @Then("^j'importe ce contrat incorrect de type (.*)")
    public void upload_incorrect_contract(String type) throws Exception {
        uploadContract(type, CONTEXT_IDENTIFIER, false);
    }

    private void uploadContract(String type, String contextIdentifier, Boolean expectedSuccessStatus)
        throws IOException, InvalidParseOperationException, AccessExternalClientException, VitamClientException, InvalidCreateOperationException {
        Path sip = Paths.get(world.getBaseDirectory(), fileName);
        try (InputStream inputStream = Files.newInputStream(sip, StandardOpenOption.READ)) {
            RequestResponse<ContextModel> res = world
                .getAdminClient()
                .findContextById(
                    new VitamContext(world.getTenantId()).setApplicationSessionId(world.getApplicationSessionId()),
                    contextIdentifier
                );
            assertThat(res.isOk()).isTrue();
            ContextModel contextModel = ((RequestResponseOK<ContextModel>) res).getFirstResult();
            assertThat(contextModel).isNotNull();
            List<PermissionModel> permissions = contextModel.getPermissions();
            assertThat(permissions).isNotEmpty();

            AdminCollections collection = AdminCollections.valueOf(type);
            this.setContractType(collection.getName());
            if (AdminCollections.ACCESS_CONTRACTS.equals(collection)) {
                uploadAccessContract(contextIdentifier, expectedSuccessStatus, sip, inputStream, permissions);
            } else if (AdminCollections.INGEST_CONTRACTS.equals(collection)) {
                uploadIngestContract(contextIdentifier, expectedSuccessStatus, sip, inputStream, permissions);
            } else if (AdminCollections.MANAGEMENT_CONTRACTS.equals(collection)) {
                uploadManagementContract(contextIdentifier, expectedSuccessStatus, sip, inputStream, permissions);
            }
        }
    }

    private void uploadIngestContract(
        String contextIdentifier,
        Boolean expectedSuccessStatus,
        Path sip,
        InputStream inputStream,
        List<PermissionModel> permissions
    ) throws InvalidParseOperationException, AccessExternalClientException, InvalidCreateOperationException {
        RequestResponse response = world
            .getAdminClient()
            .createIngestContracts(
                new VitamContext(world.getTenantId()).setApplicationSessionId(world.getApplicationSessionId()),
                inputStream
            );

        if (expectedSuccessStatus != null) {
            assertThat(response.isOk()).isEqualTo(expectedSuccessStatus);
        }
        final String operationId = response.getHeaderString(GlobalDataRest.X_REQUEST_ID);
        world.setOperationId(operationId);

        if (expectedSuccessStatus == null || expectedSuccessStatus) {
            final List<IngestContractModel> ingestContractModelList = JsonHandler.getFromFileAsTypeReference(
                sip.toFile(),
                new TypeReference<List<IngestContractModel>>() {}
            );

            if (ingestContractModelList != null && !ingestContractModelList.isEmpty() && response.isOk()) {
                Set<String> contractIdentifier = ingestContractModelList
                    .stream()
                    .map(ic -> ic.getIdentifier())
                    .collect(Collectors.toSet());

                // Remove, because TNR testing security control on ingest contract
                contractIdentifier.remove(INGEST_CONTRACT_NOT_IN_CONTEXT);

                boolean changed = false;
                for (PermissionModel p : permissions) {
                    if (p.getTenant() != world.getTenantId()) {
                        continue;
                    }

                    if (null == p.getIngestContract()) {
                        p.setIngestContract(new HashSet<>());
                    }
                    changed = p.getIngestContract().addAll(contractIdentifier) || changed;
                }

                if (changed) {
                    updateContext(
                        world.getAdminClient(),
                        world.getApplicationSessionId(),
                        contextIdentifier,
                        permissions,
                        expectedSuccessStatus
                    );
                }
            }
        }
    }

    private void uploadAccessContract(
        String contextIdentifier,
        Boolean expectedSuccessStatus,
        Path sip,
        InputStream inputStream,
        List<PermissionModel> permissions
    ) throws InvalidParseOperationException, AccessExternalClientException, InvalidCreateOperationException {
        RequestResponse response = world
            .getAdminClient()
            .createAccessContracts(
                new VitamContext(world.getTenantId()).setApplicationSessionId(world.getApplicationSessionId()),
                inputStream
            );

        final String operationId = response.getHeaderString(GlobalDataRest.X_REQUEST_ID);
        world.setOperationId(operationId);

        if (expectedSuccessStatus != null) {
            assertThat(response.isOk()).isEqualTo(expectedSuccessStatus);
        }

        if (expectedSuccessStatus == null || expectedSuccessStatus) {
            final List<IngestContractModel> accessContractModelList = JsonHandler.getFromFileAsTypeReference(
                sip.toFile(),
                new TypeReference<List<IngestContractModel>>() {}
            );

            if (accessContractModelList != null && !accessContractModelList.isEmpty()) {
                Set<String> contractIdentifier = accessContractModelList
                    .stream()
                    .map(ac -> ac.getIdentifier())
                    .collect(Collectors.toSet());

                boolean changed = false;
                for (PermissionModel p : permissions) {
                    if (p.getTenant() != world.getTenantId()) {
                        continue;
                    }
                    if (null == p.getAccessContract()) {
                        p.setAccessContract(new HashSet<>());
                    }
                    changed = p.getAccessContract().addAll(contractIdentifier) || changed;
                }

                if (changed) {
                    updateContext(
                        world.getAdminClient(),
                        world.getApplicationSessionId(),
                        contextIdentifier,
                        permissions,
                        expectedSuccessStatus
                    );
                }
            }
        }
    }

    private void uploadManagementContract(
        String contextIdentifier,
        Boolean expectedSuccessStatus,
        Path sip,
        InputStream inputStream,
        List<PermissionModel> permissions
    ) throws InvalidParseOperationException, AccessExternalClientException, InvalidCreateOperationException {
        RequestResponse response = world
            .getAdminClient()
            .createManagementContracts(
                new VitamContext(world.getTenantId()).setApplicationSessionId(world.getApplicationSessionId()),
                inputStream
            );

        final String operationId = response.getHeaderString(GlobalDataRest.X_REQUEST_ID);
        world.setOperationId(operationId);

        if (expectedSuccessStatus != null) {
            assertThat(response.isOk()).isEqualTo(expectedSuccessStatus);
        }
    }

    public static void updateContext(
        AdminExternalClient adminExternalClient,
        String applicationSessionId,
        String contextIdentifier,
        List<PermissionModel> permissions,
        Boolean expectedSuccessStatus
    ) throws InvalidParseOperationException, InvalidCreateOperationException, AccessExternalClientException {
        // update contexte
        ObjectNode permissionsNode = JsonHandler.createObjectNode();
        permissionsNode.set(ContextModel.TAG_PERMISSIONS, JsonHandler.toJsonNode(permissions));
        VitamContext context = new VitamContext(1);
        context.setApplicationSessionId(applicationSessionId);
        final SetAction setPermission = UpdateActionHelper.set(permissionsNode);
        final Update update = new Update();
        update.addActions(setPermission);
        JsonNode queryDsl = update.getFinalUpdateById();

        RequestResponse<ContextModel> requestResponse = adminExternalClient.updateContext(
            context,
            contextIdentifier,
            queryDsl
        );

        if (expectedSuccessStatus != null) {
            assertThat(requestResponse.isOk()).isEqualTo(expectedSuccessStatus);
        }
    }

    @When("^je cherche un contrat de type (.*) et nommé (.*)")
    public void search_contracts(String type, String name)
        throws AccessExternalClientException, InvalidParseOperationException, InvalidCreateOperationException, VitamClientException {
        this.setModel(retriveContract(type, name));
    }

    private JsonNode retriveContract(String type, String name)
        throws InvalidCreateOperationException, VitamClientException, InvalidParseOperationException {
        AdminCollections collection = AdminCollections.valueOf(type);
        final fr.gouv.vitam.common.database.builder.request.single.Select select =
            new fr.gouv.vitam.common.database.builder.request.single.Select();

        select.setQuery(eq("Name", name));

        final JsonNode query = select.getFinalSelect();
        switch (collection) {
            case ACCESS_CONTRACTS:
                RequestResponse<AccessContractModel> accessResponse = world
                    .getAdminClient()
                    .findAccessContracts(
                        new VitamContext(world.getTenantId())
                            .setAccessContract(null)
                            .setApplicationSessionId(world.getApplicationSessionId()),
                        query
                    );

                assertThat(accessResponse.isOk()).isTrue();

                List<JsonNode> accessContracts =
                    ((RequestResponseOK<AccessContractModel>) accessResponse).getResultsAsJsonNodes();

                if (accessContracts != null && !accessContracts.isEmpty()) {
                    return accessContracts.get(0);
                }
                return null;
            case INGEST_CONTRACTS:
                RequestResponse<IngestContractModel> ingestResponse = world
                    .getAdminClient()
                    .findIngestContracts(
                        new VitamContext(world.getTenantId())
                            .setAccessContract(null)
                            .setApplicationSessionId(world.getApplicationSessionId()),
                        query
                    );

                assertThat(ingestResponse.isOk()).isTrue();

                List<JsonNode> ingestContracts =
                    ((RequestResponseOK<IngestContractModel>) ingestResponse).getResultsAsJsonNodes();

                if (ingestContracts != null && !ingestContracts.isEmpty()) {
                    return ingestContracts.get(0);
                }
                return null;
            case MANAGEMENT_CONTRACTS:
                RequestResponse<ManagementContractModel> managementResponse = world
                    .getAdminClient()
                    .findManagementContracts(
                        new VitamContext(world.getTenantId())
                            .setAccessContract(null)
                            .setApplicationSessionId(world.getApplicationSessionId()),
                        query
                    );

                assertThat(managementResponse.isOk()).isTrue();

                List<JsonNode> managementContracts =
                    ((RequestResponseOK<ManagementContractModel>) managementResponse).getResultsAsJsonNodes();

                if (managementContracts != null && !managementContracts.isEmpty()) {
                    return managementContracts.get(0);
                }
                return null;
            default:
                throw new VitamClientException("Contract type not valid");
        }
    }

    @Then("^le contrat existe$")
    public void contract_found() {
        assertThat(this.getModel()).isNotNull();
    }

    @Then("^le contrat n'existe pas$")
    public void contract_not_found() {
        assertThat(this.getModel()).isNull();
    }

    @Then("^les métadonnées du contrat sont$")
    public void metadata_are(DataTable dataTable) throws Throwable {
        List<List<String>> raws = dataTable.raw();
        for (List<String> raw : raws) {
            String index = raw.get(0);
            String value = raw.get(1);
            assertThat(value).contains(this.getModel().get(index).asText());
        }
    }

    @When(
        "^je modifie un contrat de type (.*) avec le fichier de requête suivant (.*) le statut de la requête est (.*)$"
    )
    public void update_contract_by_query(String type, String queryFilename, Integer statusCode)
        throws IOException, InvalidParseOperationException, AccessExternalClientException, VitamClientException {
        AdminCollections collection = AdminCollections.valueOf(type);
        String contractIdentifier = getModel().get("Identifier").asText();

        Path queryFile = Paths.get(world.getBaseDirectory(), queryFilename);
        String query = FileUtil.readFile(queryFile.toFile());
        JsonNode queryDsl = JsonHandler.getFromString(query);
        RequestResponse requestResponse = null;
        switch (collection) {
            case ACCESS_CONTRACTS:
                requestResponse = world
                    .getAdminClient()
                    .updateAccessContract(
                        new VitamContext(world.getTenantId()).setApplicationSessionId(world.getApplicationSessionId()),
                        contractIdentifier,
                        queryDsl
                    );
                assertThat(statusCode).isEqualTo(requestResponse.getStatus());
                break;
            case INGEST_CONTRACTS:
                requestResponse = world
                    .getAdminClient()
                    .updateIngestContract(
                        new VitamContext(world.getTenantId()).setApplicationSessionId(world.getApplicationSessionId()),
                        contractIdentifier,
                        queryDsl
                    );
                assertThat(statusCode).isEqualTo(requestResponse.getStatus());
                break;
            case MANAGEMENT_CONTRACTS:
                requestResponse = world
                    .getAdminClient()
                    .updateManagementContract(
                        new VitamContext(world.getTenantId()).setApplicationSessionId(world.getApplicationSessionId()),
                        contractIdentifier,
                        queryDsl
                    );
                assertThat(statusCode).isEqualTo(requestResponse.getStatus());
                break;
            default:
                throw new VitamClientException("Contract type not valid");
        }

        final String operationId = requestResponse.getHeaderString(GlobalDataRest.X_REQUEST_ID);
        world.setOperationId(operationId);
    }

    @When("^je modifie un contrat d'accès et le statut de la requête est (.*)$")
    public void update_access_contract(Integer statusCode)
        throws InvalidParseOperationException, AccessExternalClientException {
        String contractIdentifier = getModel().get("Identifier").asText();

        JsonNode queryDsl = JsonHandler.getFromString(world.getQuery());
        RequestResponse requestResponse = null;

        requestResponse = world
            .getAdminClient()
            .updateAccessContract(
                new VitamContext(world.getTenantId()).setApplicationSessionId(world.getApplicationSessionId()),
                contractIdentifier,
                queryDsl
            );
        assertThat(statusCode).isEqualTo(requestResponse.getStatus());

        final String operationId = requestResponse.getHeaderString(GlobalDataRest.X_REQUEST_ID);
        world.setOperationId(operationId);
    }

    /**
     * check if contract are imported or import them
     *
     * @param contractNames list of contract's name to verify
     * @param type contract type
     * @param fileName contract json file
     * @throws Exception
     */
    @Then("^le[s]? contract[s]? (.*) de type (.*) (?:définie|définies) dans le fichier (.*)$")
    public void verify_contrat_or_import(List<String> contractNames, String type, String fileName) throws Exception {
        boolean shouldImport = false;
        for (String contractName : contractNames) {
            JsonNode jsonNode = retriveContract(type, contractName);
            if (jsonNode == null) {
                shouldImport = true;
                break;
            }
        }

        if (shouldImport) {
            a_contract_json_named(fileName);
            upload_contract(type);
        }
    }
}
