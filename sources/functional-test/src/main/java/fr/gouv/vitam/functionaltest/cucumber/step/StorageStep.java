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
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.VitamRequestIterator;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.stream.StreamUtils;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functionaltest.services.Storage;
import fr.gouv.vitam.storage.driver.model.StorageGetResult;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.referential.StorageOfferProvider;
import fr.gouv.vitam.storage.engine.common.referential.StorageOfferProviderFactory;
import fr.gouv.vitam.storage.engine.common.referential.StorageStrategyProvider;
import fr.gouv.vitam.storage.engine.common.referential.StorageStrategyProviderFactory;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageOffer;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Storage step
 */
public class StorageStep {
    private String fileName;
    private final World world;
    private Storage storageService = new Storage();
    private static final String TEST_URI = "testStorage";
    private static final String OFFER_FILENAME = "static-offer.json";
    private String guid;
    private Map<String, StorageOffer> storageOffers;

    public StorageStep(World world) throws FileNotFoundException, InvalidParseOperationException {
        this.world = world;
        guid = GUIDFactory.newStorageOperationGUID(world.getTenantId(), true).getId();
        storageService = new Storage();
        loadStrategies();
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

    private static final StorageOfferProvider OFFER_PROVIDER = StorageOfferProviderFactory.getDefaultProvider();
    private static final StorageStrategyProvider STRATEGY_PROVIDER =
        StorageStrategyProviderFactory.getDefaultProvider();

    @When("^je sauvegarde le fichier dans les offres")
    public void save_this_file() throws IOException {
        storageOffers.entrySet().forEach((v) -> save(v.getKey()));
    }

    private void loadStrategies() throws FileNotFoundException, InvalidParseOperationException {
        StorageOffer[] storageOffersArray = JsonHandler
            .getFromFileLowerCamelCase(PropertiesUtils.findFile(OFFER_FILENAME), StorageOffer[].class);
        storageOffers = new HashMap<>();
        for (StorageOffer offer : storageOffersArray) {
            storageOffers.put(offer.getId(), offer);
        }
    }

    private void save(String strategy) {
        runInVitamThread(() -> {
            Path sip = Paths.get(world.getBaseDirectory(), fileName);
            try {
                VitamThreadUtils.getVitamSession().setTenantId(world.getTenantId());
                storageService.store(sip, TEST_URI, strategy, guid);
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
     * @throws StorageException une exeption
     */
    @Then("^le fichier est bien stocké dans les offres")
    public void the_sip_is_stored_in_offers() throws StorageException {
        storageOffers.entrySet().forEach((v) -> assertThat(the_sip_is_stored_in_offer(v.getKey())).isTrue());
    }

    @Then("^je verifie que toutes ces strategies contiennent des fichiers")
    public void container_has_files() throws StorageServerClientException {
        runInVitamThread(() -> {
            try {
                VitamThreadUtils.getVitamSession().setTenantId(world.getTenantId());
                storageOffers.entrySet().forEach((v) -> {
                    VitamRequestIterator<JsonNode> result;
                    try {
                        result = world.storageClient.listContainer(v.getKey(), DataCategory.OBJECT);
                    } catch (StorageServerClientException e) {
                        throw new RuntimeException(e);
                    }
                    assertThat(result).isNotNull();
                    assertThat(result.hasNext()).isTrue();
                });
            } catch (Exception | AssertionError e) {
                throw new RuntimeException(e);
            }
        });
    }

    private boolean the_sip_is_stored_in_offer(String offer) {
        //ugly
        final boolean[] response = {true};
        runInVitamThread(() -> {
            Path sip = Paths.get(world.getBaseDirectory(), fileName);
            try {
                VitamThreadUtils.getVitamSession().setTenantId(world.getTenantId());
                Map<String, StorageGetResult> result =
                    storageService.getContainerObject(offer, guid, DataCategory.OBJECT);
                assertThat(result).isNotNull();
                assertThat(result.get(offer)).isNotNull();
                assertThat(result.get(offer).getObject()).isNotNull();
                InputStream stream = (InputStream) result.get(offer).getObject().getEntity();
                FileInputStream outputStream = new FileInputStream(sip.toFile());
                assertThat(stream).hasSameContentAs(outputStream);

                StreamUtils.closeSilently(outputStream);
                StreamUtils.closeSilently(stream);

            } catch (Exception | AssertionError e) {
                response[0] = false;
                throw new RuntimeException(e);
            }
        });
        return response[0];
    }
}
