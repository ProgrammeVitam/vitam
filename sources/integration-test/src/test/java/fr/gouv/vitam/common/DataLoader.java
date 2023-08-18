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
package fr.gouv.vitam.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.IngestContractModel;
import fr.gouv.vitam.common.model.administration.ManagementContractModel;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.common.model.administration.ProfileModel;
import fr.gouv.vitam.common.ontology.OntologyTestHelper;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static fr.gouv.vitam.common.json.JsonHandler.toJsonNode;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * This class replace FunctionalAdminIT
 * As it test all referential import
 */
public class DataLoader {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(DataLoader.class);

    private final int tenant1 = 1;
    private final int tenantId = 0;
    private final String dataFodler;
    private final TypeReference<List<AccessContractModel>> valueTypeRef = new TypeReference<>() {
    };

    public DataLoader(String dataFodler) {
        this.dataFodler = dataFodler;
    }

    public void prepareData() throws InterruptedException {
        CompletableFuture<Void> process = CompletableFuture.runAsync(() -> {
            System.err.println("===============  initialize(); =======================");
            initialize();
        }, VitamThreadPoolExecutor.getDefaultExecutor());
        process.join();
    }

    public void prepareVitamSession() {
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
        VitamThreadUtils.getVitamSession().setContractId("aName");
        VitamThreadUtils.getVitamSession().setContextId("Context_IT");
    }

    private void initialize() {
        VitamServerRunner.cleanOffers();
        prepareVitamSession();
        LOGGER.error("++++++++ tryImportFile....");
        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
            client.importFormat(
                PropertiesUtils.getResourceAsStream(dataFodler + "/DROID_SignatureFile_V109.xml"),
                "DROID_SignatureFile_V109.xml");

            // Import ontologies
            int initialTenant = VitamThreadUtils.getVitamSession().getTenantId();
            VitamThreadUtils.getVitamSession().setTenantId(VitamConfiguration.getAdminTenant());
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
            List<OntologyModel> ontology =
                JsonHandler.getFromInputStreamAsTypeReference(OntologyTestHelper.loadOntologies(),
                    new TypeReference<>() {
                    });
            try (InputStream externalOntologyStream = PropertiesUtils
                .getResourceAsStream(dataFodler + "/addition_ext_ontology.json")) {
                List<OntologyModel> externalOntology = JsonHandler.getFromInputStreamAsTypeReference(
                    externalOntologyStream, new TypeReference<>() {
                    });
                ontology.addAll(externalOntology);
            } catch (FileNotFoundException e) {
                LOGGER.info("No addition_ext_ontology.json defined in dataFolder");
            }
            client.importOntologies(true, ontology);
            VitamThreadUtils.getVitamSession().setTenantId(initialTenant);

            // Import Rules
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
            client.importRulesFile(
                PropertiesUtils.getResourceAsStream(dataFodler + "/jeu_donnees_OK_regles_CSV_regles.csv"),
                "jeu_donnees_OK_regles_CSV_regles.csv");
            importAgenciesTenant1(client);
            client.importAgenciesFile(PropertiesUtils.getResourceAsStream("agencies.csv"), "agencies.csv");
            // lets check evdetdata for rules import
            LogbookOperationsClient logbookClient = LogbookOperationsClientFactory.getInstance().getClient();
            fr.gouv.vitam.common.database.builder.request.single.Select selectQuery =
                new fr.gouv.vitam.common.database.builder.request.single.Select();
            selectQuery.setQuery(QueryHelper.eq("evType", "STP_IMPORT_RULES"));
            JsonNode logbookResult = logbookClient.selectOperation(selectQuery.getFinalSelect());
            assertNotNull(logbookResult.get("$results").get(0).get("evDetData"));
            assertTrue(JsonHandler.writeAsString(logbookResult.get("$results").get(0).get("evDetData"))
                .contains("jeu_donnees_OK_regles_CSV_regles"));
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));

            File fileProfiles = PropertiesUtils.getResourceFile(dataFodler + "/OK_profil.json");
            List<ProfileModel> profileModelList =
                JsonHandler.getFromFileAsTypeReference(fileProfiles, new TypeReference<>() {
                });
            client.createProfiles(profileModelList);

            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
            RequestResponseOK<ProfileModel> response =
                (RequestResponseOK<ProfileModel>) client.findProfiles(new Select().getFinalSelect());
            client.importProfileFile(response.getResults().get(0).getIdentifier(),
                PropertiesUtils.getResourceAsStream(dataFodler + "/profil_ok.rng"));

            String managementContractId = importManagementContract(client);
            // import contract
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
            File fileContracts =
                PropertiesUtils.getResourceFile(dataFodler + "/referential_contracts_ok.json");
            List<IngestContractModel> ingestContractModelList = JsonHandler.getFromFileAsTypeReference(fileContracts,
                new TypeReference<>() {
                });
            if (null != ingestContractModelList && !ingestContractModelList.isEmpty() && null != managementContractId) {
                ingestContractModelList.get(0).setManagementContractId(managementContractId);
            }
            client.importIngestContracts(ingestContractModelList);

            // import access contract
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
            File fileAccessContracts = PropertiesUtils
                .getResourceFile(dataFodler + "/access_contracts.json");
            List<AccessContractModel> accessContractModelList = JsonHandler
                .getFromFileAsTypeReference(fileAccessContracts, valueTypeRef);
            client.importAccessContracts(accessContractModelList);

            importOptionnalContractTenant1(client);

            // Import Security Profile
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
            client.importSecurityProfiles(JsonHandler
                .getFromFileAsTypeReference(
                    PropertiesUtils.getResourceFile(dataFodler + "/security_profile_ok.json"),
                    new TypeReference<>() {
                    }));

            // Import Context
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
            client.importContexts(JsonHandler
                .getFromFileAsTypeReference(PropertiesUtils.getResourceFile(dataFodler + "/contexts.json"),
                    new TypeReference<>() {
                    }));

            // Import Archive Unit Profile
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
            client.createArchiveUnitProfiles(JsonHandler
                .getFromFileAsTypeReference(
                    PropertiesUtils.getResourceFile(dataFodler + "/archive-unit-profile.json"),
                    new TypeReference<>() {
                    }));

        } catch (final Exception e) {
            LOGGER.error(e);
        }
    }

    private void importAgenciesTenant1(AdminManagementClient client)
        throws ReferentialException, FileNotFoundException {
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenant1));
        VitamThreadUtils.getVitamSession().setTenantId(tenant1);
        client.importAgenciesFile(PropertiesUtils.getResourceAsStream("agencies.csv"), "agencies.csv");
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
    }

    private void importOptionnalContractTenant1(AdminManagementClient client)
        throws fr.gouv.vitam.common.exception.InvalidParseOperationException, AdminManagementClientServerException {
        File fileAccessContracts;
        List<AccessContractModel> accessContractModelList;
        try {
            importContract(client, "access_contract_tenant_1.json");
            importContract(client, "referential_contracts_ok.json");
        } catch (FileNotFoundException e) {
            LOGGER.info("no need to load tenant 1 contracts");
        }
    }

    private String importManagementContract(AdminManagementClient client) {
        try {
            VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenantId));
            File fileContracts = PropertiesUtils.getResourceFile(dataFodler + "/contracts_management_ark_ok.json");
            List<ManagementContractModel> managementContractList =
                JsonHandler.getFromFileAsTypeReference(fileContracts, new TypeReference<>() {
                });
            client.importManagementContracts(managementContractList);
            RequestResponse<ManagementContractModel> entity =
                client.findManagementContracts(new Select().getFinalSelect());

            JsonNode jsonNode = toJsonNode(entity);
            if (null != jsonNode) {
                JsonNode resultsNode = jsonNode.get("$results");
                if (resultsNode != null && resultsNode.isArray() && resultsNode.size() > 0) {
                    JsonNode lastResultNode = resultsNode.get(resultsNode.size() - 1);
                    if (lastResultNode != null) {
                        JsonNode identifierNode = lastResultNode.get("Identifier");
                        if (identifierNode != null) {
                            return identifierNode.textValue();
                        }
                    }
                }
            }
            throw new RuntimeException("Cannot import management contract");

        } catch (FileNotFoundException e) {
            // No management contract in data set
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Import Management Contract Error", e);
        }
    }

    private void importContract(AdminManagementClient client, String filename)
        throws FileNotFoundException, fr.gouv.vitam.common.exception.InvalidParseOperationException,
        AdminManagementClientServerException {
        File fileAccessContracts;
        List<AccessContractModel> accessContractModelList;
        fileAccessContracts = PropertiesUtils.getResourceFile(dataFodler + "/" + filename);
        accessContractModelList = JsonHandler.getFromFileAsTypeReference(fileAccessContracts, valueTypeRef);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newOperationLogbookGUID(tenant1));
        VitamThreadUtils.getVitamSession().setTenantId(tenant1);
        client.importAccessContracts(accessContractModelList);
        VitamThreadUtils.getVitamSession().setTenantId(tenantId);
    }
}
