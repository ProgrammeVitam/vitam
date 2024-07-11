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
package fr.gouv.vitam.logbook.administration.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.client.MongoCursor;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.mongo.FakeMongoCursor;
import fr.gouv.vitam.common.security.merkletree.MerkleTreeAlgo;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.logbook.common.model.TraceabilityFile;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookOperation;
import fr.gouv.vitam.logbook.common.traceability.LogbookTraceabilityHelper;
import fr.gouv.vitam.logbook.operations.api.LogbookOperations;
import org.apache.commons.codec.binary.Base64;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class LogbookOperationTraceabilityHelperTest {

    private static final String LOGBOOK_OPERATION_WITH_TOKEN = "/logbookOperationWithToken.json";
    public static final Integer OPERATION_TRACEABILITY_TEMPORIZATION_DELAY = 300;
    private static final String FILE_NAME = "0_operations_20171031_151118.zip";
    private static final String LOGBOOK_OPERATION_START_DATE = "2017-10-31T15:11:15.405";
    public static final int TRACEABILITY_EXPIRATION_IN_SECONDS = 24 * 60 * 60;
    private static LocalDateTime LOGBOOK_OPERATION_EVENT_DATE;

    private static final String LAST_OPERATION_HASH =
        "AQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQBAgMEAQIDBAECAwQ=";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @BeforeClass
    public static void init() throws ParseException {
        LOGBOOK_OPERATION_EVENT_DATE = LocalDateUtil.fromDate(LocalDateUtil.getDate("2017-10-31T15:11:18.569"));
    }

    @Test
    @RunWithCustomExecutor
    public void should_extract_correctly_timestamp_token() throws Exception {
        // Given
        LogbookOperations logbookOperations = mock(LogbookOperations.class);
        GUID guid = GUIDFactory.newOperationLogbookGUID(0);

        LogbookOperationTraceabilityHelper helper = new LogbookOperationTraceabilityHelper(
            logbookOperations,
            guid,
            OPERATION_TRACEABILITY_TEMPORIZATION_DELAY,
            TRACEABILITY_EXPIRATION_IN_SECONDS
        );

        InputStream stream = getClass().getResourceAsStream(LOGBOOK_OPERATION_WITH_TOKEN);
        JsonNode jsonNode = JsonHandler.getFromInputStream(stream);
        LogbookOperation logbookOperation = new LogbookOperation(jsonNode);

        // When
        byte[] token = helper.extractTimestampToken(logbookOperation);

        // Then
        assertThat(Base64.encodeBase64String(token)).isEqualTo(LAST_OPERATION_HASH);
    }

    @Test
    @RunWithCustomExecutor
    public void should_extract_correctly_startDate_from_last_event() throws Exception {
        // Given
        LogbookOperations logbookOperations = mock(LogbookOperations.class);
        GUID guid = GUIDFactory.newOperationLogbookGUID(0);

        LogbookOperationTraceabilityHelper helper = new LogbookOperationTraceabilityHelper(
            logbookOperations,
            guid,
            OPERATION_TRACEABILITY_TEMPORIZATION_DELAY,
            TRACEABILITY_EXPIRATION_IN_SECONDS
        );

        InputStream stream = getClass().getResourceAsStream(LOGBOOK_OPERATION_WITH_TOKEN);
        JsonNode jsonNode = JsonHandler.getFromInputStream(stream);
        LogbookOperation logbookOperation = new LogbookOperation(jsonNode);

        given(logbookOperations.findLastTraceabilityOperationOK()).willReturn(logbookOperation);

        // When
        LocalDateTime beforeInit = LocalDateUtil.now();
        helper.initialize();
        LocalDateTime afterInit = LocalDateUtil.now();

        // Then
        assertThat(helper.getTraceabilityStartDate()).isEqualTo(
            LocalDateUtil.getFormattedDateForMongo(LOGBOOK_OPERATION_EVENT_DATE)
        );
        assertThat(LocalDateUtil.parseMongoFormattedDate(helper.getTraceabilityEndDate()))
            .isAfterOrEqualTo(beforeInit.minusSeconds(OPERATION_TRACEABILITY_TEMPORIZATION_DELAY))
            .isBeforeOrEqualTo(afterInit.minusSeconds(OPERATION_TRACEABILITY_TEMPORIZATION_DELAY));
    }

    @Test
    @RunWithCustomExecutor
    public void should_extract_correctly_startDate_from_no_event() throws Exception {
        // Given
        LogbookOperations logbookOperations = mock(LogbookOperations.class);
        GUID guid = GUIDFactory.newOperationLogbookGUID(0);

        LogbookOperationTraceabilityHelper helper = new LogbookOperationTraceabilityHelper(
            logbookOperations,
            guid,
            OPERATION_TRACEABILITY_TEMPORIZATION_DELAY,
            TRACEABILITY_EXPIRATION_IN_SECONDS
        );

        given(logbookOperations.findLastTraceabilityOperationOK()).willReturn(null);

        // When
        helper.initialize();

        // Then
        assertThat(helper.getTraceabilityStartDate()).isEqualTo(
            LocalDateUtil.getFormattedDateForMongo(LogbookTraceabilityHelper.INITIAL_START_DATE)
        );
    }

    @Test
    @RunWithCustomExecutor
    public void should_correctly_save_data_and_compute_start_date_for_first_traceability() throws Exception {
        // Given
        LogbookOperations logbookOperations = mock(LogbookOperations.class);
        GUID guid = GUIDFactory.newOperationLogbookGUID(0);

        LogbookOperationTraceabilityHelper helper = new LogbookOperationTraceabilityHelper(
            logbookOperations,
            guid,
            OPERATION_TRACEABILITY_TEMPORIZATION_DELAY,
            TRACEABILITY_EXPIRATION_IN_SECONDS
        );

        InputStream stream = getClass().getResourceAsStream(LOGBOOK_OPERATION_WITH_TOKEN);
        JsonNode jsonNode = JsonHandler.getFromInputStream(stream);
        LogbookOperation logbookOperation = new LogbookOperation(jsonNode);
        MongoCursor<LogbookOperation> cursor = getMongoCursorFor(logbookOperation);

        given(logbookOperations.selectOperationsByLastPersistenceDateInterval(any(), any())).willReturn(cursor);

        final MerkleTreeAlgo algo = new MerkleTreeAlgo(VitamConfiguration.getDefaultDigestType());

        File zipFile = new File(folder.newFolder(), String.format(FILE_NAME));
        TraceabilityFile file = new TraceabilityFile(zipFile);

        // When
        helper.saveDataInZip(algo, file);
        file.close();

        // Then
        assertThat(Files.size(Paths.get(zipFile.getPath()))).isEqualTo(5534L);
    }

    @Test
    @RunWithCustomExecutor
    public void should_correctly_save_data_and_not_update_startDate_for_not_first_traceability() throws Exception {
        // Given
        LogbookOperations logbookOperations = mock(LogbookOperations.class);
        GUID guid = GUIDFactory.newOperationLogbookGUID(0);

        LogbookOperationTraceabilityHelper helper = new LogbookOperationTraceabilityHelper(
            logbookOperations,
            guid,
            OPERATION_TRACEABILITY_TEMPORIZATION_DELAY,
            TRACEABILITY_EXPIRATION_IN_SECONDS
        );

        InputStream stream = getClass().getResourceAsStream(LOGBOOK_OPERATION_WITH_TOKEN);
        JsonNode jsonNode = JsonHandler.getFromInputStream(stream);
        LogbookOperation logbookOperation = new LogbookOperation(jsonNode);
        MongoCursor<LogbookOperation> cursor = getMongoCursorFor(logbookOperation);

        given(logbookOperations.selectOperationsByLastPersistenceDateInterval(any(), any())).willReturn(cursor);

        final MerkleTreeAlgo algo = new MerkleTreeAlgo(VitamConfiguration.getDefaultDigestType());

        File zipFile = new File(folder.newFolder(), String.format(FILE_NAME));
        TraceabilityFile file = new TraceabilityFile(zipFile);

        // When
        helper.saveDataInZip(algo, file);
        file.close();

        // Then
        assertThat(Files.size(Paths.get(zipFile.getPath()))).isEqualTo(5534L);
    }

    @Test
    @RunWithCustomExecutor
    public void should_return_null_date_and_token_if_no_previous_logbook() throws Exception {
        // Given
        LogbookOperations logbookOperations = mock(LogbookOperations.class);
        GUID guid = GUIDFactory.newOperationLogbookGUID(0);

        LogbookOperationTraceabilityHelper helper = new LogbookOperationTraceabilityHelper(
            logbookOperations,
            guid,
            OPERATION_TRACEABILITY_TEMPORIZATION_DELAY,
            TRACEABILITY_EXPIRATION_IN_SECONDS
        );

        // When
        String date = helper.getPreviousStartDate();
        byte[] token = helper.getPreviousTimestampToken();

        // Then
        assertThat(date).isNull();
        assertThat(token).isNull();
    }

    @Test
    @RunWithCustomExecutor
    public void should_extract_correctly_date_and_token_from_last_event() throws Exception {
        // Given
        LogbookOperations logbookOperations = mock(LogbookOperations.class);
        GUID guid = GUIDFactory.newOperationLogbookGUID(0);

        LogbookOperationTraceabilityHelper helper = new LogbookOperationTraceabilityHelper(
            logbookOperations,
            guid,
            OPERATION_TRACEABILITY_TEMPORIZATION_DELAY,
            TRACEABILITY_EXPIRATION_IN_SECONDS
        );

        InputStream stream = getClass().getResourceAsStream(LOGBOOK_OPERATION_WITH_TOKEN);
        JsonNode jsonNode = JsonHandler.getFromInputStream(stream);
        LogbookOperation logbookOperation = new LogbookOperation(jsonNode);

        given(logbookOperations.findLastTraceabilityOperationOK()).willReturn(logbookOperation);
        helper.initialize();

        // When
        String date = helper.getPreviousStartDate();
        byte[] token = helper.getPreviousTimestampToken();

        // Then
        assertThat(date).isEqualTo(LOGBOOK_OPERATION_START_DATE);
        assertThat(Base64.encodeBase64String(token)).isEqualTo(LAST_OPERATION_HASH);
    }

    @Test
    @RunWithCustomExecutor
    public void should_extract_correctly_event_number() throws Exception {
        // Given
        LogbookOperations logbookOperations = mock(LogbookOperations.class);
        GUID guid = GUIDFactory.newOperationLogbookGUID(0);

        LogbookOperationTraceabilityHelper helper = new LogbookOperationTraceabilityHelper(
            logbookOperations,
            guid,
            OPERATION_TRACEABILITY_TEMPORIZATION_DELAY,
            TRACEABILITY_EXPIRATION_IN_SECONDS
        );

        InputStream stream = getClass().getResourceAsStream(LOGBOOK_OPERATION_WITH_TOKEN);
        JsonNode jsonNode = JsonHandler.getFromInputStream(stream);
        LogbookOperation logbookOperation = new LogbookOperation(jsonNode);
        MongoCursor<LogbookOperation> cursor = getMongoCursorFor(logbookOperation);

        given(logbookOperations.selectOperationsByLastPersistenceDateInterval(any(), any())).willReturn(cursor);

        final MerkleTreeAlgo algo = new MerkleTreeAlgo(VitamConfiguration.getDefaultDigestType());

        File zipFile = new File(folder.newFolder(), String.format(FILE_NAME));
        TraceabilityFile file = new TraceabilityFile(zipFile);
        helper.saveDataInZip(algo, file);

        // When
        Long size = helper.getDataSize();

        // Then
        assertThat(size).isEqualTo(1);
    }

    @Test
    @RunWithCustomExecutor
    public void should_not_require_logbook_operation_traceability_when_last_traceability_not_expired_and_no_activity()
        throws Exception {
        // Given
        LogbookOperations logbookOperations = mock(LogbookOperations.class);
        GUID guid = GUIDFactory.newOperationLogbookGUID(0);

        LocalDateTime lastTraceabilityDate = LocalDateUtil.parseMongoFormattedDate("2017-10-31T15:11:18.569");
        LocalDateTime now = LocalDateUtil.now();
        long elapsedSeconds =
            now.minusSeconds(OPERATION_TRACEABILITY_TEMPORIZATION_DELAY).toEpochSecond(ZoneOffset.UTC) -
            lastTraceabilityDate.toEpochSecond(ZoneOffset.UTC);
        int traceabilityExpirationDelayInSeconds = (int) elapsedSeconds + 60;

        LogbookOperationTraceabilityHelper helper = new LogbookOperationTraceabilityHelper(
            logbookOperations,
            guid,
            OPERATION_TRACEABILITY_TEMPORIZATION_DELAY,
            traceabilityExpirationDelayInSeconds
        );

        InputStream stream = getClass().getResourceAsStream(LOGBOOK_OPERATION_WITH_TOKEN);
        JsonNode jsonNode = JsonHandler.getFromInputStream(stream);
        LogbookOperation logbookOperation = new LogbookOperation(jsonNode);

        given(logbookOperations.findLastTraceabilityOperationOK()).willReturn(logbookOperation);
        given(
            logbookOperations.checkNewEligibleLogbookOperationsSinceLastTraceabilityOperation(any(), any())
        ).willReturn(false);

        helper.initialize();

        // When
        boolean traceabilityOperationRequired = helper.isTraceabilityOperationRequired();

        // Then
        assertThat(traceabilityOperationRequired).isFalse();
        verify(logbookOperations).checkNewEligibleLogbookOperationsSinceLastTraceabilityOperation(any(), any());
    }

    @Test
    @RunWithCustomExecutor
    public void should_require_logbook_operation_traceability_when_last_traceability_not_expired_and_existing_activity()
        throws Exception {
        // Given
        LogbookOperations logbookOperations = mock(LogbookOperations.class);
        GUID guid = GUIDFactory.newOperationLogbookGUID(0);

        LocalDateTime lastTraceabilityDate = LocalDateUtil.parseMongoFormattedDate("2017-10-31T15:11:18.569");
        LocalDateTime now = LocalDateUtil.now();
        long elapsedSeconds =
            now.minusSeconds(OPERATION_TRACEABILITY_TEMPORIZATION_DELAY).toEpochSecond(ZoneOffset.UTC) -
            lastTraceabilityDate.toEpochSecond(ZoneOffset.UTC);
        int traceabilityExpirationDelayInSeconds = (int) elapsedSeconds + 60;

        LogbookOperationTraceabilityHelper helper = new LogbookOperationTraceabilityHelper(
            logbookOperations,
            guid,
            OPERATION_TRACEABILITY_TEMPORIZATION_DELAY,
            traceabilityExpirationDelayInSeconds
        );

        InputStream stream = getClass().getResourceAsStream(LOGBOOK_OPERATION_WITH_TOKEN);
        JsonNode jsonNode = JsonHandler.getFromInputStream(stream);
        LogbookOperation logbookOperation = new LogbookOperation(jsonNode);

        given(logbookOperations.findLastTraceabilityOperationOK()).willReturn(logbookOperation);
        given(
            logbookOperations.checkNewEligibleLogbookOperationsSinceLastTraceabilityOperation(any(), any())
        ).willReturn(true);

        helper.initialize();

        // When
        boolean traceabilityOperationRequired = helper.isTraceabilityOperationRequired();

        // Then
        assertThat(traceabilityOperationRequired).isTrue();
        verify(logbookOperations).checkNewEligibleLogbookOperationsSinceLastTraceabilityOperation(any(), any());
    }

    @Test
    @RunWithCustomExecutor
    public void should_require_logbook_operation_traceability_when_last_traceability_expired() throws Exception {
        // Given
        LogbookOperations logbookOperations = mock(LogbookOperations.class);
        GUID guid = GUIDFactory.newOperationLogbookGUID(0);

        LogbookOperationTraceabilityHelper helper = new LogbookOperationTraceabilityHelper(
            logbookOperations,
            guid,
            OPERATION_TRACEABILITY_TEMPORIZATION_DELAY,
            TRACEABILITY_EXPIRATION_IN_SECONDS
        );

        InputStream stream = getClass().getResourceAsStream(LOGBOOK_OPERATION_WITH_TOKEN);
        JsonNode jsonNode = JsonHandler.getFromInputStream(stream);
        LogbookOperation logbookOperation = new LogbookOperation(jsonNode);

        given(logbookOperations.findLastTraceabilityOperationOK()).willReturn(logbookOperation);

        helper.initialize();

        // When
        boolean traceabilityOperationRequired = helper.isTraceabilityOperationRequired();

        // Then
        assertThat(traceabilityOperationRequired).isTrue();
        verify(logbookOperations, never()).checkNewEligibleLogbookOperationsSinceLastTraceabilityOperation(
            any(),
            any()
        );
    }

    private MongoCursor<LogbookOperation> getMongoCursorFor(LogbookOperation logbookOperation) {
        List<LogbookOperation> logbookList = new ArrayList<LogbookOperation>();
        logbookList.add(logbookOperation);

        return getMongoCursorFor(logbookList);
    }

    private MongoCursor<LogbookOperation> getMongoCursorFor(List<LogbookOperation> logbookList) {
        return new FakeMongoCursor<>(logbookList);
    }
}
