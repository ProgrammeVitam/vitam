/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
package fr.gouv.vitam.storage.offers.tape.impl.catalog;

import static com.mongodb.client.model.Filters.eq;
import static fr.gouv.vitam.common.database.collections.VitamCollection.getMongoClientOptions;
import static org.assertj.core.api.Assertions.assertThat;

import com.mongodb.client.MongoCollection;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.mongodb.SimpleMongoDBAccess;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.storage.offers.tape.dto.TapeDrive;
import fr.gouv.vitam.storage.offers.tape.dto.TapeSlot;
import fr.gouv.vitam.storage.offers.tape.model.TapeModel;
import org.bson.Document;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test @TapeCatalogRepository
 */
public class TapeCatalogRepositoryTest {

    public static final String TAPE_CATALOG_COLLECTION = "TapeCatalog" + GUIDFactory.newGUID().getId();

    @Rule
    public MongoRule mongoRule = new MongoRule(getMongoClientOptions(), TAPE_CATALOG_COLLECTION);

    private TapeCatalogRepository tapeCatalogRepository;

    private MongoCollection<Document> tapeCollection;

    @Before
    public void setUp() {
        MongoDbAccess mongoDbAccess = new SimpleMongoDBAccess(mongoRule.getMongoClient(), MongoRule.VITAM_DB);
        tapeCatalogRepository = new TapeCatalogRepository(mongoDbAccess, TAPE_CATALOG_COLLECTION);
        tapeCollection = mongoRule.getMongoCollection(TAPE_CATALOG_COLLECTION);
    }

    @Test
    public void shouldCreateTape() throws InvalidParseOperationException {
        // Given
        GUID id = GUIDFactory.newGUID();

        TapeModel tapeModel = getTapeModel(id);

        // When
        tapeCatalogRepository.createTape(tapeModel);

        // Then
        Document document = tapeCollection.find(eq("_id", id.toString())).first();
        assertThat(document)
            .isNotNull()
            .containsEntry("code", "VIT0001")
            .containsEntry("label", "VIT-TAPE-1")
            .containsEntry("capacity", 10000);
    }

    @Test
    public void shouldFindTape() throws InvalidParseOperationException {
        // Given
        GUID id = GUIDFactory.newGUID();

        TapeModel tapeModel = getTapeModel(id);

        tapeCatalogRepository.createTape(tapeModel);

        // When
        TapeModel result = tapeCatalogRepository.findTapeById(tapeModel.getId());

        // Then
        assertThat(tapeModel.getId()).isEqualTo(id.toString());
    }

    @Test
    public void shouldUpdateTape() throws InvalidParseOperationException {
        // Given
        GUID id = GUIDFactory.newGUID();

        TapeModel tapeModel = getTapeModel(id);

        tapeCatalogRepository.createTape(tapeModel);

        // When
        TapeModel currentTape = tapeCatalogRepository.findTapeById(tapeModel.getId());
        assertThat(currentTape.getCode()).isEqualTo("VIT0001");
        currentTape.setCode("FakeCode");
        tapeCatalogRepository.updateTape(currentTape);

        // Then
        TapeModel updatedTape = tapeCatalogRepository.findTapeById(tapeModel.getId());
        assertThat(updatedTape.getCode()).isEqualTo("FakeCode");
    }

    private TapeModel getTapeModel(GUID id) {
        TapeModel tapeModel = new TapeModel();
        tapeModel.setId(id.toString());
        tapeModel.setCapacity(10000L);
        tapeModel.setUsedSize(5000L);
        tapeModel.setFileCount(200L);
        tapeModel.setCode("VIT0001");
        tapeModel.setLabel("VIT-TAPE-1");
        tapeModel.setLibrary("VIT-LIB-1");
        tapeModel.setType("LTO-6");
        tapeModel.setCompressed(false);
        tapeModel.setWorm(false);
        tapeModel.setCurrentLocation(new TapeDrive());
        tapeModel.setPreviousLocation(new TapeSlot());
        return tapeModel;
    }

}
