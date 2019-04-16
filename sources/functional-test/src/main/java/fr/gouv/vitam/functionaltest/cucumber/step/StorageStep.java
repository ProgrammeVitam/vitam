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


import com.fasterxml.jackson.databind.JsonNode;

import cucumber.api.DataTable;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import fr.gouv.vitam.common.client.VitamRequestIterator;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageAlreadyExistsClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageNotFoundClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageAlreadyExistException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

import static org.assertj.core.api.Assertions.assertThat;

import javax.ws.rs.core.Response;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * Storage step
 */
public class StorageStep {
    private String fileName;
    private final World world;
    private static final String TEST_URI = "testStorage";
    private String guid;
    private StoredInfoResult info;
    private Response.StatusType responseStatus;
    private VitamRequestIterator<JsonNode> result;

    public StorageStep(World world) throws FileNotFoundException, InvalidParseOperationException {
        this.world = world;
        guid = GUIDFactory.newStorageOperationGUID(world.getTenantId(), true).getId();
    }


    /**
     * define a sip
     *
     * @param fileName name of a sip
     */
    @Given("^un fichier nommé (.*)$")
    public void a_file_named(String fileName) {
        this.fileName = fileName;
    }

    @When("^je sauvegarde le fichier dans la strategie (.*)")
    public void save_this_file(String strategy) throws IOException {
        save(strategy);
        assertThat(info).isNotNull();
        assertThat(info.getId()).isEqualTo(guid);
    }

    private void save(String strategy) {
        runInVitamThread(() -> {
            Path sip = Paths.get(world.getBaseDirectory(), fileName);
            try {
                VitamThreadUtils.getVitamSession().setTenantId(world.getTenantId());
                store(sip, TEST_URI, strategy, guid);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * @param r runnable
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



    /**
     * Check that the file is stored in the offers
     *
     * @param dataTable the datatable
     * @throws StorageException une exeption
     */
    @Then("^le fichier est bien stocké dans les strategies suivantes")
    public void the_sip_is_stored_in_offers(DataTable dataTable) throws StorageException {
        List<List<String>> raws = dataTable.raw();
        for (List<String> raw : raws.subList(1, raws.size())) {
            responseStatus = null;
            String strategy = raw.get(1);
            the_sip_is_stored_in_offer(strategy);
            assertThat(responseStatus).isEqualTo(Response.Status.OK);
        }
    }

    @Then("^je verifie que toutes ces strategies contiennent des fichiers")
    public void list_srategy(DataTable dataTable) throws StorageException, StorageServerClientException {
        List<List<String>> raws = dataTable.raw();
        for (List<String> raw : raws.subList(1, raws.size())) {
            result = null;
            String strategy = raw.get(1);
            container_has_files(strategy);
            assertThat(result).isNotNull();
            assertThat(result.hasNext()).isTrue();

        }
    }

    private void container_has_files(String strategy) throws StorageServerClientException {
        runInVitamThread(() -> {
            try {
                VitamThreadUtils.getVitamSession().setTenantId(world.getTenantId());
                try {
                    result = world.storageClient.listContainer(strategy, DataCategory.OBJECT);
                } catch (StorageServerClientException e) {
                    throw new RuntimeException(e);
                }
            } catch (Exception | AssertionError e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void the_sip_is_stored_in_offer(String strategy) {
        //ugly
        runInVitamThread(() -> {

            Response response = null;
            try {
                VitamThreadUtils.getVitamSession().setTenantId(world.getTenantId());
                response = world.storageClient.getContainerAsync(strategy, guid, DataCategory.OBJECT);
                responseStatus = response.getStatusInfo();

            } catch (Exception | AssertionError e) {

                throw new RuntimeException(e);
            } finally {
                world.storageClient.consumeAnyEntityAndClose(response);
            }
        });
    }

    /**
     * Store a file
     *
     * @param sip
     * @param uri
     * @param guid
     * @return true
     * @throws StorageServerClientException
     * @throws StorageNotFoundClientException
     * @throws StorageAlreadyExistsClientException
     * @throws ContentAddressableStorageServerException
     * @throws ContentAddressableStorageAlreadyExistException
     * @throws ContentAddressableStorageNotFoundException
     * @throws IOException
     */
    public boolean store(Path sip, String uri, String strategy, String guid)
        throws StorageServerClientException, StorageNotFoundClientException, StorageAlreadyExistsClientException,
        ContentAddressableStorageServerException, ContentAddressableStorageAlreadyExistException,
        ContentAddressableStorageNotFoundException, IOException {
        try (InputStream inputStream = Files.newInputStream(sip, StandardOpenOption.READ)) {
            WorkspaceClient workspaceClient = WorkspaceClientFactory.getInstance().getClient();
            workspaceClient.createContainer(guid);
            workspaceClient.putObject(guid, uri, inputStream);
            final ObjectDescription description = new ObjectDescription();
            description.setWorkspaceContainerGUID(guid);
            description.setWorkspaceObjectURI(uri);
            store_from_workSpace(guid, description, strategy);
            workspaceClient.deleteObject(guid, uri);

            return true;
        }
    }

    private void store_from_workSpace(String uri, ObjectDescription description, String strategy)
        throws StorageNotFoundClientException, StorageServerClientException, StorageAlreadyExistsClientException {
        try (StorageClient storageClient = StorageClientFactory.getInstance().getClient()) {
            info = storageClient.storeFileFromWorkspace(strategy, DataCategory.OBJECT, uri, description);
        }
    }
}
