package fr.gouv.vitam.worker.core.plugin.lfc_traceability;

import com.google.common.collect.Lists;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.processing.IOParameter;
import fr.gouv.vitam.common.model.processing.ProcessingUri;
import fr.gouv.vitam.common.model.processing.UriPrefix;
import fr.gouv.vitam.common.security.merkletree.MerkleTreeAlgo;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.logbook.common.exception.LogbookClientException;
import fr.gouv.vitam.logbook.common.model.TraceabilityFile;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.worker.core.distribution.JsonLineIterator;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class LogbookUnitLifeCycleTraceabilityHelperTest {
    private static final String FILE_NAME = "0_operations_20171031_151118.zip";
    private static final String LOGBOOK_OPERATION_START_DATE = "2017-08-17T14:01:07.52";
    private static final String LAST_OPERATION_HASH =
        "MIIEljAVAgEAMBAMDk9wZXJhdGlvbiBPa2F5MIIEewYJKoZIhvcNAQcCoIIEbDCCBGgCAQMxDzANBglghkgBZQMEAgMFADCBgAYLKoZIhvcNAQkQAQSgcQRvMG0CAQEGASkwUTANBglghkgBZQMEAgMFAARA25rjfWLQKTkp0ETrnTQ1b/iZ8fIhx8lsguGt2wv8UuYjtbD5PYZ/LydCEVWBSi5HwJ8E1PZbRIvsH+R2sGhy4QIBARgPMjAxNzA4MTcxNTAxMDZaMYIDzTCCA8kCAQEwfjB4MQswCQYDVQQGEwJmcjEMMAoGA1UECAwDaWRmMQ4wDAYDVQQHDAVwYXJpczEOMAwGA1UECgwFdml0YW0xFDASBgNVBAsMC2F1dGhvcml0aWVzMSUwIwYDVQQDDBxjYV9pbnRlcm1lZGlhdGVfdGltZXN0YW1waW5nAgIAujANBglghkgBZQMEAgMFAKCCASAwGgYJKoZIhvcNAQkDMQ0GCyqGSIb3DQEJEAEEMBwGCSqGSIb3DQEJBTEPFw0xNzA4MTcxNTAxMDZaMC0GCSqGSIb3DQEJNDEgMB4wDQYJYIZIAWUDBAIDBQChDQYJKoZIhvcNAQENBQAwTwYJKoZIhvcNAQkEMUIEQLja866EXeQzwV2WyARNL+C3Gh9jbJQDtmlAtLxbgFjNZkkPHGjY83b0imbLbpCeU7kr3jrvo+dLIOJgSh/IfXMwZAYLKoZIhvcNAQkQAi8xVTBTMFEwTzALBglghkgBZQMEAgMEQCURZjpTzSgWrppLklHIw5xgA8HXuv0mqAnhOCqmsyuuiWcWjCT3H42RDJSWaTCtFP/xa6tgHOynRG+4X5CHKmQwDQYJKoZIhvcNAQENBQAEggIANO7owkRFd4iTLs+RmM0cNrGvy6LhrkaV2r6862E3G5jBmC2Ao8WkI0chahPy+2gHi90M2ykpwTicoRbYBL4s9XlZn2KJ0fA2HZ/f283nacB4ARO+tdQRs7p8vXgyPYC9kO59fa/he7B1o0Mdo6uwba053r7JJplx6hNnCiJM3bB5jTBoxpdb3A2o+cq/TdGqM4MVwYms1jbswF4UDzWBLnKwY4cw/vGCuelw2AU+Q5B11QxrHjXVHaeeVm6ju27YtkGOWthwF3KQ6LEe6xKka+XQZ8kwxHIh523WjrMpoH+B8BTNRerO6KnhxVfHKUKTDO/zpYhPXyKjibg2d3lkRCUa1jtFoBKIBsdvDz0cEoN2XuOkIm9tMpe5pE4gvPRVToTJe7YxZePrvlvmJfwM5RNuNvMqvWlq3CgPj77BePzZGCfSgG91/h0TCAwQXJDEyvk9PJOrjNt4ABNJ6YOxCRF/IeQyEtUpJ9yP13JXOTTRaKkDuueObjnemxvS68rs5h1elqgFdWDCfT9TJtpEmERIT9+8+uf/v8fpSkAFpHKIaZjoAQjIBvJYSvyGZlUCrEUoAtxVgVWGMOXZITc6UADOasv8Fjm10lg+2bgX/KP1S+hH68/lMH6RNKmsC1/BKEsH9+cnsdTJySPS2HjZmfZ5FK695FRQpjL5wfgKbK8=";
    private static final String HANDLER_ID = "FINALIZE_LC_TRACEABILITY";
    private static final String LAST_OPERATION = "LogbookLifeCycleTraceabilityHelperTest/lastOperation.json";
    private static final String TRACEABILITY_INFO =
        "LogbookLifeCycleTraceabilityHelperTest/traceabilityInformation.json";
    private static final String TRACEABILITY_DATA =
        "LogbookLifeCycleTraceabilityHelperTest/traceabilityData.jsonl";
    private static final String TRACEABILITY_STATISTICS =
        "LogbookLifeCycleTraceabilityHelperTest/unitTraceabilityStats.json";

    private static LocalDateTime LOGBOOK_OPERATION_EVENT_DATE;
    private HandlerIOImpl handlerIO;
    private List<IOParameter> in;
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private WorkspaceClient workspaceClient;

    @Mock
    private WorkspaceClientFactory workspaceClientFactory;

    @Mock
    private LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;

    @Mock
    private LogbookLifeCyclesClient logbookLifeCyclesClient;

    @Mock
    private LogbookOperationsClientFactory logbookOperationsClientFactory;
    @Mock
    private LogbookOperationsClient logbookOperationsClient;

    private ItemStatus itemStatus;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {

        File vitamTempFolder = folder.newFolder();
        SystemPropertyUtil.set("vitam.tmp.folder", vitamTempFolder.getAbsolutePath());

        LOGBOOK_OPERATION_EVENT_DATE = LocalDateUtil.fromDate(LocalDateUtil.getDate("2016-06-10T11:56:35.914"));


        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        when(logbookOperationsClientFactory.getClient()).thenReturn(logbookOperationsClient);
        when(logbookLifeCyclesClientFactory.getClient()).thenReturn(logbookLifeCyclesClient);
        String objectId = "objectId";
        handlerIO = new HandlerIOImpl(workspaceClientFactory, logbookLifeCyclesClientFactory, "Test", "workerId",
            Lists.newArrayList(objectId));
        handlerIO.setCurrentObjectId(objectId);

        in = new ArrayList<>();
        in.add(new IOParameter()
            .setUri(new ProcessingUri(UriPrefix.MEMORY, "Operations/lastOperation.json")));
        in.add(new IOParameter()
            .setUri(new ProcessingUri(UriPrefix.MEMORY, "Operations/traceabilityInformation.json")));
        in.add(new IOParameter()
            .setUri(new ProcessingUri(UriPrefix.MEMORY, "Operations/traceabilityData.jsonl")));
        in.add(new IOParameter()
            .setUri(new ProcessingUri(UriPrefix.MEMORY, "Operations/traceabilityStats.json")));
        itemStatus = new ItemStatus(HANDLER_ID);
    }

    @Test
    @RunWithCustomExecutor
    public void should_extract_correctly_startDate_from_last_event() throws Exception {
        // Given
        GUID guid = GUIDFactory.newOperationLogbookGUID(0);
        handlerIO.addOutIOParameters(in);
        handlerIO.addOutputResult(0, PropertiesUtils.getResourceFile(LAST_OPERATION), false);
        handlerIO.addOutputResult(1, PropertiesUtils.getResourceFile(TRACEABILITY_INFO), false);
        handlerIO.addOutputResult(3, PropertiesUtils.getResourceFile(TRACEABILITY_STATISTICS), false);
        handlerIO.addInIOParameters(in);

        LogbookUnitLifeCycleTraceabilityHelper helper =
            new LogbookUnitLifeCycleTraceabilityHelper(handlerIO, logbookOperationsClient, itemStatus, guid.getId(),
                workspaceClientFactory, null);

        // When
        helper.initialize();

        // Then
        assertThat(helper.getTraceabilityStartDate())
            .isEqualTo(LocalDateUtil.getFormattedDateForMongo(LOGBOOK_OPERATION_EVENT_DATE));
    }

    @Test
    @RunWithCustomExecutor
    public void should_extract_correctly_statistics() throws Exception {
        // Given
        GUID guid = GUIDFactory.newOperationLogbookGUID(0);
        handlerIO.addOutIOParameters(in);
        handlerIO.addOutputResult(0, PropertiesUtils.getResourceFile(LAST_OPERATION), false);
        handlerIO.addOutputResult(1, PropertiesUtils.getResourceFile(TRACEABILITY_INFO), false);
        handlerIO.addOutputResult(3, PropertiesUtils.getResourceFile(TRACEABILITY_STATISTICS), false);
        handlerIO.addInIOParameters(in);

        LogbookUnitLifeCycleTraceabilityHelper helper =
            new LogbookUnitLifeCycleTraceabilityHelper(handlerIO, logbookOperationsClient, itemStatus, guid.getId(),
                workspaceClientFactory, null);

        // When
        helper.initialize();

        // Then
        assertThat(helper.getTraceabilityStatistics().getUnits().getNbOK()).isEqualTo(1);
        assertThat(helper.getTraceabilityStatistics().getUnits().getNbWarnings()).isEqualTo(2);
        assertThat(helper.getTraceabilityStatistics().getUnits().getNbErrors()).isEqualTo(3);
        assertThat(helper.getTraceabilityStatistics().getObjectGroups()).isNull();
        assertThat(helper.getTraceabilityStatistics().getObjects()).isNull();
    }

    @Test
    @RunWithCustomExecutor
    public void should_correctly_save_data_and_compute_start_date_for_first_traceability() throws Exception {
        // Given
        GUID guid = GUIDFactory.newOperationLogbookGUID(0);
        handlerIO.addOutIOParameters(in);
        handlerIO.addOutputResult(0, PropertiesUtils.getResourceFile(LAST_OPERATION), false);
        handlerIO.addOutputResult(1, PropertiesUtils.getResourceFile(TRACEABILITY_INFO), false);
        handlerIO.addOutputResult(3, PropertiesUtils.getResourceFile(TRACEABILITY_STATISTICS), false);
        handlerIO.addInIOParameters(in);

        when(logbookOperationsClient.selectOperation(any()))
            .thenThrow(new LogbookClientException("LogbookClientException"));

        JsonLineIterator entriesIterator = new JsonLineIterator(PropertiesUtils.getResourceAsStream(TRACEABILITY_DATA));

        LogbookUnitLifeCycleTraceabilityHelper helper =
            new LogbookUnitLifeCycleTraceabilityHelper(handlerIO, logbookOperationsClient, itemStatus, guid.getId(),
                workspaceClientFactory, entriesIterator);

        final MerkleTreeAlgo algo = new MerkleTreeAlgo(VitamConfiguration.getDefaultDigestType());

        File zipFile = new File(folder.newFolder(), String.format(FILE_NAME));
        TraceabilityFile file = new TraceabilityFile(zipFile);

        // When
        helper.saveDataInZip(algo, file);
        file.close();

        // Then
        assertThat(Files.size(Paths.get(zipFile.getPath()))).isEqualTo(150L);
    }

    @Test
    @RunWithCustomExecutor
    public void should_extract_correctly_date_and_token_from_last_event() throws Exception {
        // Given
        GUID guid = GUIDFactory.newOperationLogbookGUID(0);
        handlerIO.addOutIOParameters(in);
        handlerIO.addOutputResult(0, PropertiesUtils.getResourceFile(LAST_OPERATION), false);
        handlerIO.addOutputResult(1, PropertiesUtils.getResourceFile(TRACEABILITY_INFO), false);
        handlerIO.addOutputResult(3, PropertiesUtils.getResourceFile(TRACEABILITY_STATISTICS), false);
        handlerIO.addInIOParameters(in);

        LogbookUnitLifeCycleTraceabilityHelper helper =
            new LogbookUnitLifeCycleTraceabilityHelper(handlerIO, logbookOperationsClient, itemStatus, guid.getId(),
                workspaceClientFactory, null);

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
        GUID guid = GUIDFactory.newOperationLogbookGUID(0);
        handlerIO.addOutIOParameters(in);
        handlerIO.addOutputResult(0, PropertiesUtils.getResourceFile(LAST_OPERATION), false);
        handlerIO.addOutputResult(1, PropertiesUtils.getResourceFile(TRACEABILITY_INFO), false);
        handlerIO.addOutputResult(3, PropertiesUtils.getResourceFile(TRACEABILITY_STATISTICS), false);
        handlerIO.addInIOParameters(in);

        LogbookUnitLifeCycleTraceabilityHelper helper =
            new LogbookUnitLifeCycleTraceabilityHelper(handlerIO, logbookOperationsClient, itemStatus, guid.getId(),
                workspaceClientFactory, null);

        helper.initialize();

        // When
        Long size = helper.getDataSize();

        // Then
        assertThat(size).isEqualTo(2);
    }
}
