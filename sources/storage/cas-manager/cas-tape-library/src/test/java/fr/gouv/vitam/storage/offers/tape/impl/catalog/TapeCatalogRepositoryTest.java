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
package fr.gouv.vitam.storage.offers.tape.impl.catalog;

import com.google.common.collect.Maps;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.database.server.mongodb.SimpleMongoDBAccess;
import fr.gouv.vitam.common.database.server.query.QueryCriteria;
import fr.gouv.vitam.common.database.server.query.QueryCriteriaOperator;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.mongo.MongoRule;
import fr.gouv.vitam.storage.engine.common.collection.OfferCollections;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalog;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalogLabel;
import fr.gouv.vitam.storage.engine.common.model.TapeLocation;
import fr.gouv.vitam.storage.engine.common.model.TapeLocationType;
import fr.gouv.vitam.storage.engine.common.model.TapeState;
import fr.gouv.vitam.storage.offers.tape.exception.TapeCatalogException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test @TapeCatalogRepository
 */
public class TapeCatalogRepositoryTest {

    public static final String TAPE_CATALOG_COLLECTION =
        OfferCollections.TAPE_CATALOG.getName() + GUIDFactory.newGUID().getId();

    @ClassRule
    public static MongoRule mongoRule = new MongoRule(
        MongoDbAccess.getMongoClientSettingsBuilder(),
        TAPE_CATALOG_COLLECTION
    );

    private static TapeCatalogRepository tapeCatalogRepository;

    @BeforeClass
    public static void setUpBeforeClass() {
        MongoDbAccess mongoDbAccess = new SimpleMongoDBAccess(mongoRule.getMongoClient(), MongoRule.VITAM_DB);
        tapeCatalogRepository = new TapeCatalogRepository(
            mongoDbAccess.getMongoDatabase().getCollection(TAPE_CATALOG_COLLECTION)
        );
    }

    @After
    public void after() {
        mongoRule.handleAfter();
    }

    @AfterClass
    public static void setDownAfterClass() {
        mongoRule.handleAfterClass();
    }

    @Test
    public void shouldCreateTape() throws TapeCatalogException {
        // Given
        TapeCatalog tapeCatalog = getTapeModel();

        String id = tapeCatalog.getId();

        // When
        tapeCatalogRepository.createTape(tapeCatalog);

        // Then
        TapeCatalog tape = tapeCatalogRepository.findTapeById(id);
        assertThat(tape).isNotNull();
        assertThat(tape.getCode()).isEqualTo("VIT0001");
        assertThat(tape.getLabel().getCode()).isEqualTo("VIT-TAPE-1");
        assertThat(tape.getCapacity()).isEqualTo(10000);
        assertThat(tape.getVersion()).isEqualTo(0);
    }

    @Test
    public void shouldFindTapeById() throws TapeCatalogException {
        // Given
        TapeCatalog tapeCatalog = getTapeModel();

        String id = tapeCatalog.getId();

        tapeCatalogRepository.createTape(tapeCatalog);

        // When
        TapeCatalog result = tapeCatalogRepository.findTapeById(id);

        // Then
        assertThat(result.getCode()).isEqualTo(tapeCatalog.getCode());
    }

    @Test
    public void shouldFindTapesByCriteria() throws TapeCatalogException {
        // Given

        TapeCatalog tapeCatalog = getTapeModel();

        tapeCatalogRepository.createTape(tapeCatalog);

        // When
        List<QueryCriteria> criteria = new ArrayList<>();
        criteria.add(new QueryCriteria(TapeCatalog.CODE, tapeCatalog.getCode(), QueryCriteriaOperator.EQ));
        criteria.add(new QueryCriteria(TapeCatalog.CAPACITY, 10000L, QueryCriteriaOperator.GTE));
        List<TapeCatalog> result = tapeCatalogRepository.findTapes(criteria);

        // Then
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.get(0).getCapacity()).isEqualTo(tapeCatalog.getCapacity());
    }

    @Test
    public void shouldUpdateTape() throws TapeCatalogException {
        // Given
        TapeCatalog tapeCatalog = getTapeModel();

        String id = tapeCatalog.getId();

        tapeCatalogRepository.createTape(tapeCatalog);

        // When
        TapeCatalog currentTape = tapeCatalogRepository.findTapeById(id);
        assertThat(currentTape.getCode()).isEqualTo("VIT0001");
        assertThat(currentTape.getVersion()).isEqualTo(0);
        assertThat(currentTape.getFileCount()).isEqualTo(200L);

        Map<String, Object> updates = Maps.newHashMap();
        updates.put(TapeCatalog.CODE, "FakeCode");
        updates.put(TapeCatalog.FILE_COUNT, 201);
        tapeCatalogRepository.updateTape(id, updates);

        // Then
        TapeCatalog updatedTape = tapeCatalogRepository.findTapeById(id);
        assertThat(updatedTape.getCode()).isEqualTo("FakeCode");
        assertThat(updatedTape.getVersion()).isEqualTo(1);
        assertThat(updatedTape.getFileCount()).isEqualTo(201L);
    }

    @Test
    public void shouldReplaceTape() throws TapeCatalogException {
        // Given

        TapeCatalog tapeCatalog = getTapeModel();

        String id = tapeCatalogRepository.createTape(tapeCatalog);

        // When
        TapeCatalog currentTape = tapeCatalogRepository.findTapeById(id);
        assertThat(currentTape.getCode()).isEqualTo("VIT0001");
        assertThat(currentTape.getVersion()).isEqualTo(0);

        currentTape.setCode("FakeCode");
        tapeCatalogRepository.replaceTape(currentTape);

        // Then
        TapeCatalog updatedTape = tapeCatalogRepository.findTapeById(id);
        assertThat(updatedTape.getCode()).isEqualTo("FakeCode");
        assertThat(updatedTape.getVersion()).isEqualTo(1);
    }

    @Test
    public void shouldCountTapesByState() throws TapeCatalogException {
        TapeCatalog tapeCatalog1 = getTapeModel();
        tapeCatalog1.setTapeState(TapeState.FULL);
        tapeCatalogRepository.createTape(tapeCatalog1);

        TapeCatalog tapeCatalog2 = getTapeModel();
        tapeCatalog2.setTapeState(TapeState.FULL);
        tapeCatalogRepository.createTape(tapeCatalog2);

        TapeCatalog tapeCatalog3 = getTapeModel();
        tapeCatalog3.setTapeState(TapeState.EMPTY);
        tapeCatalogRepository.createTape(tapeCatalog3);

        TapeCatalog tapeCatalog4 = getTapeModel();
        tapeCatalog4.setTapeState(TapeState.OPEN);
        tapeCatalogRepository.createTape(tapeCatalog4);

        TapeCatalog tapeCatalog5 = getTapeModel();
        tapeCatalog5.setTapeState(TapeState.OPEN);
        tapeCatalogRepository.createTape(tapeCatalog5);

        TapeCatalog tapeCatalog6 = getTapeModel();
        tapeCatalog6.setTapeState(TapeState.OPEN);
        tapeCatalogRepository.createTape(tapeCatalog6);

        // When
        Map<TapeState, Integer> stats = tapeCatalogRepository.countByState();

        // Then
        assertThat(stats).isEqualTo(Map.of(TapeState.FULL, 2, TapeState.EMPTY, 1, TapeState.OPEN, 3));
        assertThat(stats.getOrDefault(TapeState.CONFLICT, 0)).isEqualTo(0);
    }

    private TapeCatalog getTapeModel() {
        TapeCatalog tapeCatalog = new TapeCatalog();
        tapeCatalog.setCapacity(10000L);
        tapeCatalog.setTapeState(TapeState.OPEN);
        tapeCatalog.setFileCount(200);
        tapeCatalog.setCode("VIT0001");
        tapeCatalog.setLabel(new TapeCatalogLabel(GUIDFactory.newGUID().getId(), "VIT-TAPE-1"));
        tapeCatalog.setLibrary("VIT-LIB-1");
        tapeCatalog.setType("LTO-6");
        tapeCatalog.setCompressed(false);
        tapeCatalog.setWorm(false);
        tapeCatalog.setCurrentLocation(new TapeLocation(1, TapeLocationType.DRIVE));
        tapeCatalog.setPreviousLocation(new TapeLocation(2, TapeLocationType.SLOT));
        return tapeCatalog;
    }
}
