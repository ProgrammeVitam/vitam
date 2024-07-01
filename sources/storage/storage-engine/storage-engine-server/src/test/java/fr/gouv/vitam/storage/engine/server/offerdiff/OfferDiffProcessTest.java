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
package fr.gouv.vitam.storage.engine.server.offerdiff;

import com.google.common.collect.ImmutableMap;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.collection.CloseableIteratorUtils;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.storage.ObjectEntry;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.referential.model.OfferReference;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageStrategy;
import fr.gouv.vitam.storage.engine.server.distribution.StorageDistribution;
import org.apache.commons.collections4.IteratorUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

@RunWithCustomExecutor
public class OfferDiffProcessTest {

    private static final String OFFER1 = "offer1";
    private static final String OFFER2 = "offer2";
    private static final DataCategory DATA_CATEGORY = DataCategory.UNIT;
    private static final int TENANT_ID = 2;

    @ClassRule
    public static RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private StorageDistribution distribution;

    private static ExecutorService executorService;

    @BeforeClass
    public static void beforeClass() {
        executorService = Executors.newFixedThreadPool(4, VitamThreadFactory.getInstance());
    }

    @AfterClass
    public static void afterClass() {
        executorService.shutdown();
    }

    @Before
    public void setup() throws StorageException {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(TENANT_ID));
        StorageStrategy storageStrategy = new StorageStrategy();
        storageStrategy.setOffers(Arrays.asList(new OfferReference("offer1"), new OfferReference("offer2")));
        doReturn(ImmutableMap.of("default", storageStrategy)).when(distribution).getStrategies();
    }

    @Test
    @RunWithCustomExecutor
    public void synchronizeEmptyOffer() throws Exception {
        // Given
        CloseableIterator<ObjectEntry> entries1 = CloseableIteratorUtils.toCloseableIterator(
            IteratorUtils.emptyIterator()
        );
        doReturn(entries1).when(distribution).listContainerObjectsForOffer(DATA_CATEGORY, OFFER1, true);

        CloseableIterator<ObjectEntry> entries2 = CloseableIteratorUtils.toCloseableIterator(
            IteratorUtils.emptyIterator()
        );
        doReturn(entries2).when(distribution).listContainerObjectsForOffer(DATA_CATEGORY, OFFER2, true);

        OfferDiffProcess instance = new OfferDiffProcess(distribution, OFFER1, OFFER2, DATA_CATEGORY);

        // When
        instance.run();

        // Then
        assertThat(instance.isRunning()).isFalse();
        assertThat(instance.getOfferDiffStatus().getContainer()).isEqualTo(DATA_CATEGORY.getCollectionName());
        assertThat(instance.getOfferDiffStatus().getStartDate()).isNotNull();
        assertThat(instance.getOfferDiffStatus().getEndDate()).isNotNull();
        assertThat(instance.getOfferDiffStatus().getOffer1()).isEqualTo(OFFER1);
        assertThat(instance.getOfferDiffStatus().getOffer2()).isEqualTo(OFFER2);
        assertThat(instance.getOfferDiffStatus().getTotalObjectCount()).isEqualTo(0L);
        assertThat(instance.getOfferDiffStatus().getErrorCount()).isEqualTo(0L);
        assertThat(instance.getOfferDiffStatus().getStatusCode()).isEqualTo(StatusCode.OK);
        assertThat(instance.getOfferDiffStatus().getReportFileName()).isNotNull();
        assertThat(new File(instance.getOfferDiffStatus().getReportFileName())).hasContent("");
        assertThat(instance.getOfferDiffStatus().getTenantId()).isEqualTo(TENANT_ID);
        assertThat(instance.getOfferDiffStatus().getRequestId()).isEqualTo(
            VitamThreadUtils.getVitamSession().getRequestId()
        );
    }

    @Test
    @RunWithCustomExecutor
    public void synchronizeIsoOffers() throws Exception {
        // Given
        CloseableIterator<ObjectEntry> entries1 = CloseableIteratorUtils.toCloseableIterator(
            Arrays.asList(
                new ObjectEntry().setObjectId("obj5").setSize(10L),
                new ObjectEntry().setObjectId("obj2").setSize(30L)
            ).iterator()
        );
        doReturn(entries1).when(distribution).listContainerObjectsForOffer(DATA_CATEGORY, OFFER1, true);

        CloseableIterator<ObjectEntry> entries2 = CloseableIteratorUtils.toCloseableIterator(
            Arrays.asList(
                new ObjectEntry().setObjectId("obj2").setSize(30L),
                new ObjectEntry().setObjectId("obj5").setSize(10L)
            ).iterator()
        );
        doReturn(entries2).when(distribution).listContainerObjectsForOffer(DATA_CATEGORY, OFFER2, true);

        OfferDiffProcess instance = new OfferDiffProcess(distribution, OFFER1, OFFER2, DATA_CATEGORY);

        // When
        instance.run();

        // Then
        assertThat(instance.isRunning()).isFalse();
        assertThat(instance.getOfferDiffStatus().getContainer()).isEqualTo(DATA_CATEGORY.getCollectionName());
        assertThat(instance.getOfferDiffStatus().getStartDate()).isNotNull();
        assertThat(instance.getOfferDiffStatus().getEndDate()).isNotNull();
        assertThat(instance.getOfferDiffStatus().getOffer1()).isEqualTo(OFFER1);
        assertThat(instance.getOfferDiffStatus().getOffer2()).isEqualTo(OFFER2);
        assertThat(instance.getOfferDiffStatus().getTotalObjectCount()).isEqualTo(2L);
        assertThat(instance.getOfferDiffStatus().getErrorCount()).isEqualTo(0L);
        assertThat(instance.getOfferDiffStatus().getStatusCode()).isEqualTo(StatusCode.OK);
        assertThat(instance.getOfferDiffStatus().getReportFileName()).isNotNull();
        assertThat(new File(instance.getOfferDiffStatus().getReportFileName())).hasContent("");
        assertThat(instance.getOfferDiffStatus().getTenantId()).isEqualTo(TENANT_ID);
        assertThat(instance.getOfferDiffStatus().getRequestId()).isEqualTo(
            VitamThreadUtils.getVitamSession().getRequestId()
        );
    }

    @Test
    @RunWithCustomExecutor
    public void synchronizeLargeIsoOffers() throws Exception {
        // Given
        int nbEntries = 1000;

        CloseableIterator<ObjectEntry> entries1 = CloseableIteratorUtils.toCloseableIterator(
            IntStream.range(0, nbEntries)
                // Quick shuffle
                .map(i -> (i * 7) % nbEntries)
                .mapToObj(i -> new ObjectEntry().setObjectId("obj" + i).setSize(i))
                .iterator()
        );
        doReturn(entries1).when(distribution).listContainerObjectsForOffer(DATA_CATEGORY, OFFER1, true);

        CloseableIterator<ObjectEntry> entries2 = CloseableIteratorUtils.toCloseableIterator(
            IntStream.range(0, nbEntries)
                // Quick shuffle
                .map(i -> (i * 13) % nbEntries)
                .mapToObj(i -> new ObjectEntry().setObjectId("obj" + i).setSize(i))
                .iterator()
        );
        doReturn(entries2).when(distribution).listContainerObjectsForOffer(DATA_CATEGORY, OFFER2, true);

        OfferDiffProcess instance = new OfferDiffProcess(distribution, OFFER1, OFFER2, DATA_CATEGORY);

        // When
        instance.run();

        // Then
        assertThat(instance.isRunning()).isFalse();
        assertThat(instance.getOfferDiffStatus().getOffer1()).isEqualTo(OFFER1);
        assertThat(instance.getOfferDiffStatus().getOffer2()).isEqualTo(OFFER2);
        assertThat(instance.getOfferDiffStatus().getTenantId()).isEqualTo(TENANT_ID);
        assertThat(instance.getOfferDiffStatus().getContainer()).isEqualTo(DATA_CATEGORY.getCollectionName());
        assertThat(instance.getOfferDiffStatus().getStartDate()).isNotNull();
        assertThat(instance.getOfferDiffStatus().getEndDate()).isNotNull();
        assertThat(instance.getOfferDiffStatus().getTotalObjectCount()).isEqualTo((long) nbEntries);
        assertThat(instance.getOfferDiffStatus().getErrorCount()).isEqualTo(0L);
        assertThat(instance.getOfferDiffStatus().getStatusCode()).isEqualTo(StatusCode.OK);
        assertThat(instance.getOfferDiffStatus().getReportFileName()).isNotNull();
        assertThat(new File(instance.getOfferDiffStatus().getReportFileName())).hasContent("");
        assertThat(instance.getOfferDiffStatus().getRequestId()).isEqualTo(
            VitamThreadUtils.getVitamSession().getRequestId()
        );
    }

    @Test
    @RunWithCustomExecutor
    public void synchronizeUnknownOffer() throws Exception {
        // Given
        CloseableIterator<ObjectEntry> entries1 = CloseableIteratorUtils.toCloseableIterator(
            IteratorUtils.emptyIterator()
        );
        doReturn(entries1).when(distribution).listContainerObjectsForOffer(DATA_CATEGORY, OFFER1, true);

        CloseableIterator<ObjectEntry> entries2 = CloseableIteratorUtils.toCloseableIterator(
            IteratorUtils.emptyIterator()
        );
        doReturn(entries2).when(distribution).listContainerObjectsForOffer(DATA_CATEGORY, "UNKNOWN", true);

        OfferDiffProcess instance = new OfferDiffProcess(distribution, OFFER1, "UNKNOWN", DATA_CATEGORY);

        // When
        instance.run();

        // Then
        assertThat(instance.isRunning()).isFalse();
        assertThat(instance.getOfferDiffStatus().getOffer1()).isEqualTo(OFFER1);
        assertThat(instance.getOfferDiffStatus().getOffer2()).isEqualTo("UNKNOWN");
        assertThat(instance.getOfferDiffStatus().getContainer()).isEqualTo(DATA_CATEGORY.getCollectionName());
        assertThat(instance.getOfferDiffStatus().getTenantId()).isEqualTo(TENANT_ID);
        assertThat(instance.getOfferDiffStatus().getStartDate()).isNotNull();
        assertThat(instance.getOfferDiffStatus().getEndDate()).isNotNull();
        assertThat(instance.getOfferDiffStatus().getTotalObjectCount()).isEqualTo(0L);
        assertThat(instance.getOfferDiffStatus().getErrorCount()).isEqualTo(0L);
        assertThat(instance.getOfferDiffStatus().getStatusCode()).isEqualTo(StatusCode.KO);
        assertThat(instance.getOfferDiffStatus().getReportFileName()).isNull();
        assertThat(instance.getOfferDiffStatus().getRequestId()).isEqualTo(
            VitamThreadUtils.getVitamSession().getRequestId()
        );
    }

    @Test
    @RunWithCustomExecutor
    public void synchronizeLargeOffersWithErrors() throws Exception {
        // Given
        CloseableIterator<ObjectEntry> entries1 = CloseableIteratorUtils.toCloseableIterator(
            Arrays.asList(
                new ObjectEntry().setObjectId("obj1").setSize(1L),
                new ObjectEntry().setObjectId("obj2").setSize(2L),
                new ObjectEntry().setObjectId("obj4").setSize(4L),
                new ObjectEntry().setObjectId("obj5").setSize(5L),
                new ObjectEntry().setObjectId("obj6").setSize(6L)
            ).iterator()
        );
        doReturn(entries1).when(distribution).listContainerObjectsForOffer(DATA_CATEGORY, OFFER1, true);

        CloseableIterator<ObjectEntry> entries2 = CloseableIteratorUtils.toCloseableIterator(
            Arrays.asList(
                new ObjectEntry().setObjectId("obj4").setSize(40L),
                new ObjectEntry().setObjectId("obj2").setSize(2L),
                new ObjectEntry().setObjectId("obj3").setSize(3L),
                new ObjectEntry().setObjectId("obj1").setSize(1L),
                new ObjectEntry().setObjectId("obj5").setSize(5L)
            ).iterator()
        );
        doReturn(entries2).when(distribution).listContainerObjectsForOffer(DATA_CATEGORY, OFFER2, true);

        OfferDiffProcess instance = new OfferDiffProcess(distribution, OFFER1, OFFER2, DATA_CATEGORY);

        // When
        instance.run();

        // Then
        assertThat(instance.isRunning()).isFalse();
        assertThat(instance.getOfferDiffStatus().getOffer1()).isEqualTo(OFFER1);
        assertThat(instance.getOfferDiffStatus().getOffer2()).isEqualTo(OFFER2);
        assertThat(instance.getOfferDiffStatus().getTenantId()).isEqualTo(TENANT_ID);
        assertThat(instance.getOfferDiffStatus().getContainer()).isEqualTo(DATA_CATEGORY.getCollectionName());
        assertThat(instance.getOfferDiffStatus().getStartDate()).isNotNull();
        assertThat(instance.getOfferDiffStatus().getEndDate()).isNotNull();
        assertThat(instance.getOfferDiffStatus().getTotalObjectCount()).isEqualTo(6L);
        assertThat(instance.getOfferDiffStatus().getErrorCount()).isEqualTo(3L);
        assertThat(instance.getOfferDiffStatus().getStatusCode()).isEqualTo(StatusCode.WARNING);
        assertThat(instance.getOfferDiffStatus().getReportFileName()).isNotNull();
        assertThat(new File(instance.getOfferDiffStatus().getReportFileName())).hasContent(
            "" +
            "{\"objectId\":\"obj3\",\"sizeInOffer1\":null,\"sizeInOffer2\":3}\n" +
            "{\"objectId\":\"obj4\",\"sizeInOffer1\":4,\"sizeInOffer2\":40}\n" +
            "{\"objectId\":\"obj6\",\"sizeInOffer1\":6,\"sizeInOffer2\":null}"
        );
        assertThat(instance.getOfferDiffStatus().getRequestId()).isEqualTo(
            VitamThreadUtils.getVitamSession().getRequestId()
        );
    }
}
