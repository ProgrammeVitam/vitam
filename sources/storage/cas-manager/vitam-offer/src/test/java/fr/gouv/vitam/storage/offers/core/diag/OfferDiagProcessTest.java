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

package fr.gouv.vitam.storage.offers.core.diag;

import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.storage.ObjectEntry;
import fr.gouv.vitam.common.storage.cas.container.api.ContentAddressableStorage;
import fr.gouv.vitam.common.storage.cas.container.api.ObjectListingListener;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.tmp.TempFolderRule;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.OfferLogAction;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.storage.offers.core.DefaultOfferService;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class OfferDiagProcessTest {

    private static final String CONTAINER = "CONTAINER";
    private static final int TENANT = 4;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    // Initializes tmp folders for Vitam
    @Rule
    public TempFolderRule tempFolder = new TempFolderRule();

    @Mock
    ContentAddressableStorage contentAddressableStorage;

    @Mock
    DefaultOfferService offerService;

    @Before
    public void setUp() {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT);
        VitamThreadUtils.getVitamSession().setRequestId(GUIDFactory.newRequestIdGUID(TENANT));
    }

    @Test
    @RunWithCustomExecutor
    public void testEmptyContainer() throws Exception {
        // Given
        doNothing().when(contentAddressableStorage).listContainer(eq(CONTAINER), any());
        doReturn(emptyList()).when(offerService).getOfferLogs(CONTAINER, null, 10000, Order.ASC);
        OfferDiagProcess offerDiagProcess = new OfferDiagProcess(
            contentAddressableStorage,
            offerService,
            CONTAINER,
            10
        );

        // When
        offerDiagProcess.run();

        // Then
        assertThat(offerDiagProcess.getOfferDiagStatus().getStatusCode()).isEqualTo(StatusCode.OK);
        assertThat(offerDiagProcess.getOfferDiagStatus().getContainer()).isEqualTo(CONTAINER);
        assertThat(offerDiagProcess.getOfferDiagStatus().getTenantId()).isEqualTo(TENANT);
        assertThat(offerDiagProcess.getOfferDiagStatus().getTotalObjectCount()).isEqualTo(0);
        assertThat(offerDiagProcess.getOfferDiagStatus().getErrorCount()).isEqualTo(0);
        assertThat(offerDiagProcess.getOfferDiagStatus().getStartDate()).isNotNull();
        assertThat(offerDiagProcess.getOfferDiagStatus().getEndDate()).isNotNull();
        assertThat(offerDiagProcess.getOfferDiagStatus().getReportFileName()).isNotNull();
        File reportFile = new File(offerDiagProcess.getOfferDiagStatus().getReportFileName());
        assertThat(reportFile).exists();
        assertThat(reportFile).hasContent("");

        verify(contentAddressableStorage).listContainer(eq(CONTAINER), any());
        verify(offerService).getOfferLogs(CONTAINER, null, 10, Order.ASC);
        verifyNoMoreInteractions(contentAddressableStorage);
        verifyNoMoreInteractions(offerService);
    }

    @Test
    @RunWithCustomExecutor
    public void testValidOfferState() throws Exception {
        // Given
        doAnswer(args -> {
            ObjectListingListener listener = args.getArgument(1);
            listener.handleObjectEntry(new ObjectEntry("obj2", 2L));
            listener.handleObjectEntry(new ObjectEntry("obj3", 3L));
            listener.handleObjectEntry(new ObjectEntry("obj7", 7L));
            listener.handleObjectEntry(new ObjectEntry("obj5", 5L));
            listener.handleObjectEntry(new ObjectEntry("obj1", 1L));
            return null;
        })
            .when(contentAddressableStorage)
            .listContainer(eq(CONTAINER), any());

        doReturn(
            List.of(
                new OfferLog(1L, LocalDateTime.now(), CONTAINER, "obj1", OfferLogAction.WRITE),
                new OfferLog(2L, LocalDateTime.now(), CONTAINER, "obj1", OfferLogAction.WRITE),
                new OfferLog(3L, LocalDateTime.now(), CONTAINER, "obj1", OfferLogAction.WRITE),
                new OfferLog(4L, LocalDateTime.now(), CONTAINER, "obj2", OfferLogAction.WRITE),
                new OfferLog(5L, LocalDateTime.now(), CONTAINER, "obj3", OfferLogAction.WRITE),
                new OfferLog(6L, LocalDateTime.now(), CONTAINER, "obj2", OfferLogAction.WRITE),
                new OfferLog(7L, LocalDateTime.now(), CONTAINER, "obj1", OfferLogAction.WRITE),
                new OfferLog(8L, LocalDateTime.now(), CONTAINER, "obj4", OfferLogAction.WRITE),
                new OfferLog(9L, LocalDateTime.now(), CONTAINER, "obj5", OfferLogAction.WRITE),
                new OfferLog(10L, LocalDateTime.now(), CONTAINER, "obj6", OfferLogAction.WRITE)
            )
        )
            .when(offerService)
            .getOfferLogs(CONTAINER, null, 10, Order.ASC);

        doReturn(
            List.of(
                new OfferLog(11L, LocalDateTime.now(), CONTAINER, "obj1", OfferLogAction.WRITE),
                new OfferLog(12L, LocalDateTime.now(), CONTAINER, "obj6", OfferLogAction.DELETE),
                new OfferLog(13L, LocalDateTime.now(), CONTAINER, "obj5", OfferLogAction.WRITE),
                new OfferLog(14L, LocalDateTime.now(), CONTAINER, "obj7", OfferLogAction.WRITE),
                new OfferLog(15L, LocalDateTime.now(), CONTAINER, "obj8", OfferLogAction.WRITE),
                new OfferLog(16L, LocalDateTime.now(), CONTAINER, "obj8", OfferLogAction.DELETE),
                new OfferLog(17L, LocalDateTime.now(), CONTAINER, "obj1", OfferLogAction.WRITE),
                new OfferLog(18L, LocalDateTime.now(), CONTAINER, "obj4", OfferLogAction.WRITE),
                new OfferLog(19L, LocalDateTime.now(), CONTAINER, "obj3", OfferLogAction.WRITE),
                new OfferLog(50L, LocalDateTime.now(), CONTAINER, "obj5", OfferLogAction.WRITE)
            )
        )
            .when(offerService)
            .getOfferLogs(CONTAINER, 11L, 10, Order.ASC);

        doReturn(List.of(new OfferLog(55L, LocalDateTime.now(), CONTAINER, "obj4", OfferLogAction.DELETE)))
            .when(offerService)
            .getOfferLogs(CONTAINER, 51L, 10, Order.ASC);

        OfferDiagProcess offerDiagProcess = new OfferDiagProcess(
            contentAddressableStorage,
            offerService,
            CONTAINER,
            10
        );

        // When
        offerDiagProcess.run();

        // Then
        assertThat(offerDiagProcess.getOfferDiagStatus().getStatusCode()).isEqualTo(StatusCode.OK);
        assertThat(offerDiagProcess.getOfferDiagStatus().getContainer()).isEqualTo(CONTAINER);
        assertThat(offerDiagProcess.getOfferDiagStatus().getTenantId()).isEqualTo(TENANT);
        assertThat(offerDiagProcess.getOfferDiagStatus().getTotalObjectCount()).isEqualTo(8);
        assertThat(offerDiagProcess.getOfferDiagStatus().getErrorCount()).isEqualTo(0);
        assertThat(offerDiagProcess.getOfferDiagStatus().getStartDate()).isNotNull();
        assertThat(offerDiagProcess.getOfferDiagStatus().getEndDate()).isNotNull();
        assertThat(offerDiagProcess.getOfferDiagStatus().getReportFileName()).isNotNull();
        File reportFile = new File(offerDiagProcess.getOfferDiagStatus().getReportFileName());
        assertThat(reportFile).exists();
        assertThat(reportFile).hasContent("");

        verify(contentAddressableStorage).listContainer(eq(CONTAINER), any());
        verify(offerService).getOfferLogs(CONTAINER, null, 10, Order.ASC);
        verify(offerService).getOfferLogs(CONTAINER, 11L, 10, Order.ASC);
        verify(offerService).getOfferLogs(CONTAINER, 51L, 10, Order.ASC);
        verifyNoMoreInteractions(contentAddressableStorage);
        verifyNoMoreInteractions(offerService);
    }

    @Test
    @RunWithCustomExecutor
    public void testOfferWithInvalidObjectState() throws Exception {
        // Given
        doAnswer(args -> {
            ObjectListingListener listener = args.getArgument(1);
            listener.handleObjectEntry(new ObjectEntry("obj3", 1L));
            listener.handleObjectEntry(new ObjectEntry("obj10", 10L));
            listener.handleObjectEntry(new ObjectEntry("obj7", 7L));
            listener.handleObjectEntry(new ObjectEntry("obj5", 5L));
            listener.handleObjectEntry(new ObjectEntry("obj9", 9L));
            listener.handleObjectEntry(new ObjectEntry("obj1", 1L));
            listener.handleObjectEntry(new ObjectEntry("obj8", 8L));
            return null;
        })
            .when(contentAddressableStorage)
            .listContainer(eq(CONTAINER), any());

        doReturn(
            List.of(
                new OfferLog(1L, LocalDateTime.now(), CONTAINER, "obj1", OfferLogAction.WRITE),
                new OfferLog(2L, LocalDateTime.now(), CONTAINER, "obj1", OfferLogAction.WRITE),
                new OfferLog(3L, LocalDateTime.now(), CONTAINER, "obj1", OfferLogAction.WRITE),
                new OfferLog(4L, LocalDateTime.now(), CONTAINER, "obj2", OfferLogAction.WRITE),
                new OfferLog(5L, LocalDateTime.now(), CONTAINER, "obj3", OfferLogAction.WRITE),
                new OfferLog(6L, LocalDateTime.now(), CONTAINER, "obj2", OfferLogAction.WRITE),
                new OfferLog(7L, LocalDateTime.now(), CONTAINER, "obj1", OfferLogAction.WRITE),
                new OfferLog(8L, LocalDateTime.now(), CONTAINER, "obj4", OfferLogAction.WRITE),
                new OfferLog(9L, LocalDateTime.now(), CONTAINER, "obj5", OfferLogAction.WRITE),
                new OfferLog(10L, LocalDateTime.now(), CONTAINER, "obj6", OfferLogAction.WRITE)
            )
        )
            .when(offerService)
            .getOfferLogs(CONTAINER, null, 10, Order.ASC);

        doReturn(
            List.of(
                new OfferLog(11L, LocalDateTime.now(), CONTAINER, "obj1", OfferLogAction.WRITE),
                new OfferLog(12L, LocalDateTime.now(), CONTAINER, "obj6", OfferLogAction.DELETE),
                new OfferLog(13L, LocalDateTime.now(), CONTAINER, "obj5", OfferLogAction.WRITE),
                new OfferLog(14L, LocalDateTime.now(), CONTAINER, "obj7", OfferLogAction.WRITE),
                new OfferLog(15L, LocalDateTime.now(), CONTAINER, "obj8", OfferLogAction.WRITE),
                new OfferLog(16L, LocalDateTime.now(), CONTAINER, "obj8", OfferLogAction.DELETE),
                new OfferLog(17L, LocalDateTime.now(), CONTAINER, "obj1", OfferLogAction.WRITE),
                new OfferLog(18L, LocalDateTime.now(), CONTAINER, "obj4", OfferLogAction.WRITE),
                new OfferLog(19L, LocalDateTime.now(), CONTAINER, "obj3", OfferLogAction.WRITE),
                new OfferLog(50L, LocalDateTime.now(), CONTAINER, "obj5", OfferLogAction.WRITE)
            )
        )
            .when(offerService)
            .getOfferLogs(CONTAINER, 11L, 10, Order.ASC);

        doReturn(List.of(new OfferLog(55L, LocalDateTime.now(), CONTAINER, "obj4", OfferLogAction.DELETE)))
            .when(offerService)
            .getOfferLogs(CONTAINER, 51L, 10, Order.ASC);

        OfferDiagProcess offerDiagProcess = new OfferDiagProcess(
            contentAddressableStorage,
            offerService,
            CONTAINER,
            10
        );

        // When
        offerDiagProcess.run();

        // Then
        assertThat(offerDiagProcess.getOfferDiagStatus().getStatusCode()).isEqualTo(StatusCode.KO);
        assertThat(offerDiagProcess.getOfferDiagStatus().getContainer()).isEqualTo(CONTAINER);
        assertThat(offerDiagProcess.getOfferDiagStatus().getTenantId()).isEqualTo(TENANT);
        assertThat(offerDiagProcess.getOfferDiagStatus().getTotalObjectCount()).isEqualTo(10);
        assertThat(offerDiagProcess.getOfferDiagStatus().getErrorCount()).isEqualTo(4);
        assertThat(offerDiagProcess.getOfferDiagStatus().getStartDate()).isNotNull();
        assertThat(offerDiagProcess.getOfferDiagStatus().getEndDate()).isNotNull();
        assertThat(offerDiagProcess.getOfferDiagStatus().getReportFileName()).isNotNull();
        File reportFile = new File(offerDiagProcess.getOfferDiagStatus().getReportFileName());
        assertThat(reportFile).exists();
        List<String> reportLines = FileUtils.readLines(reportFile, StandardCharsets.UTF_8);
        assertThat(reportLines).hasSize(4);
        OfferDiagReportEntry entry0 = JsonHandler.getFromString(reportLines.get(0), OfferDiagReportEntry.class);
        OfferDiagReportEntry entry1 = JsonHandler.getFromString(reportLines.get(1), OfferDiagReportEntry.class);
        OfferDiagReportEntry entry2 = JsonHandler.getFromString(reportLines.get(2), OfferDiagReportEntry.class);
        OfferDiagReportEntry entry3 = JsonHandler.getFromString(reportLines.get(3), OfferDiagReportEntry.class);

        assertThat(entry0.getObjectId()).isEqualTo("obj10");
        assertThat(entry0.getSizeInOffer()).isEqualTo(10L);
        assertThat(entry0.getExpectedState()).isEqualTo(OfferDiagReportEntry.ObjectState.ABSENT);
        assertThat(entry0.getActualState()).isEqualTo(OfferDiagReportEntry.ObjectState.PRESENT);
        assertThat(entry0.getLastEventDateTime()).isNull();

        assertThat(entry1.getObjectId()).isEqualTo("obj2");
        assertThat(entry1.getSizeInOffer()).isNull();
        assertThat(entry1.getExpectedState()).isEqualTo(OfferDiagReportEntry.ObjectState.PRESENT);
        assertThat(entry1.getActualState()).isEqualTo(OfferDiagReportEntry.ObjectState.ABSENT);
        assertThat(entry1.getLastEventDateTime()).isNotNull();

        assertThat(entry2.getObjectId()).isEqualTo("obj8");
        assertThat(entry2.getSizeInOffer()).isEqualTo(8L);
        assertThat(entry2.getExpectedState()).isEqualTo(OfferDiagReportEntry.ObjectState.ABSENT);
        assertThat(entry2.getActualState()).isEqualTo(OfferDiagReportEntry.ObjectState.PRESENT);
        assertThat(entry2.getLastEventDateTime()).isNotNull();

        assertThat(entry3.getObjectId()).isEqualTo("obj9");
        assertThat(entry3.getSizeInOffer()).isEqualTo(9L);
        assertThat(entry3.getExpectedState()).isEqualTo(OfferDiagReportEntry.ObjectState.ABSENT);
        assertThat(entry3.getActualState()).isEqualTo(OfferDiagReportEntry.ObjectState.PRESENT);
        assertThat(entry3.getLastEventDateTime()).isNull();

        verify(contentAddressableStorage).listContainer(eq(CONTAINER), any());
        verify(offerService).getOfferLogs(CONTAINER, null, 10, Order.ASC);
        verify(offerService).getOfferLogs(CONTAINER, 11L, 10, Order.ASC);
        verify(offerService).getOfferLogs(CONTAINER, 51L, 10, Order.ASC);
        verifyNoMoreInteractions(contentAddressableStorage);
        verifyNoMoreInteractions(offerService);
    }
}
