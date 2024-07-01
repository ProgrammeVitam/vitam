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
package fr.gouv.vitam.worker.core.plugin.evidence;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.SystemPropertyUtil;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.common.database.builder.query.BooleanQuery;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.MetadataType;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.logbook.common.server.database.collections.LogbookMongoDbName;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.referential.model.OfferReference;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageStrategy;
import fr.gouv.vitam.worker.core.plugin.evidence.exception.EvidenceAuditException;
import fr.gouv.vitam.worker.core.plugin.evidence.exception.EvidenceStatus;
import fr.gouv.vitam.worker.core.plugin.evidence.report.EvidenceAuditParameters;
import org.jboss.resteasy.specimpl.BuiltResponse;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.gte;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.lte;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EvidenceServiceTest {

    private static final Integer TENANT_ID = 0;
    private static final String RESULT_SELECT_ISLAST = "evidenceAudit/RESULT_SELECT.json";

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static JsonNode OFFERS_INFO;
    private static String RESULT_SELECT_LOGBOOK_SECUR_OP = "evidenceAudit/RESULT_SELECT_LOGBOOK_SECUR_OP.json";
    private static String result = "evidenceAudit/result.json";
    private static String offersInfo = "evidenceAudit/offersInfo.json";

    static {
        try {
            OFFERS_INFO = JsonHandler.getFromFile(PropertiesUtils.getResourceFile(offersInfo));
        } catch (InvalidParseOperationException | FileNotFoundException e) {}
    }

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Mock
    MetaDataClientFactory metaDataClientFactory;

    @Mock
    MetaDataClient metaDataClient;

    @Mock
    LogbookOperationsClientFactory logbookOperationsClientFactory;

    @Mock
    LogbookOperationsClient logbookOperationsClient;

    @Mock
    LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;

    @Mock
    LogbookLifeCyclesClient logbookLifeCyclesClient;

    @Mock
    StorageClientFactory storageClientFactory;

    @Mock
    StorageClient storageClient;

    @Before
    public void setUp() throws Exception {
        File vitamTempFolder = temporaryFolder.newFolder();
        SystemPropertyUtil.set("vitam.tmp.folder", vitamTempFolder.getAbsolutePath());

        when(metaDataClientFactory.getClient()).thenReturn(metaDataClient);
        when(logbookOperationsClientFactory.getClient()).thenReturn(logbookOperationsClient);
        when(logbookLifeCyclesClientFactory.getClient()).thenReturn(logbookLifeCyclesClient);
        when(storageClientFactory.getClient()).thenReturn(storageClient);
    }

    @RunWithCustomExecutor
    @Test
    public void auditEvidenceNominalCaseForUnit() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        //GIVEN
        EvidenceService evidenceService = new EvidenceService(
            metaDataClientFactory,
            logbookOperationsClientFactory,
            logbookLifeCyclesClientFactory,
            storageClientFactory
        );

        JsonNode unitMd = getUnitMd();
        JsonNode liceCycle = getLifcycle();
        JsonNode logbook = getLogbook();

        //WHEN
        RequestResponseOK<JsonNode> response1 = new RequestResponseOK<JsonNode>().addResult(unitMd);

        when(metaDataClient.getUnitByIdRaw("aeaqaaaaaaguu2zzaazsualbwlwdgwaaaaaq")).thenReturn(response1);
        when(logbookLifeCyclesClient.getRawUnitLifeCycleById("aeaqaaaaaaguu2zzaazsualbwlwdgwaaaaaq")).thenReturn(
            liceCycle
        );

        JsonNode select = getSelectlogbookLCsecure();

        JsonNode select2 = getSelect2();

        when(logbookOperationsClient.selectOperationById(anyString())).thenReturn(logbook);
        when(logbookOperationsClient.selectOperation(select)).thenReturn(
            JsonHandler.getFromFile(PropertiesUtils.getResourceFile(RESULT_SELECT_LOGBOOK_SECUR_OP))
        );

        when(logbookOperationsClient.selectOperation(select2)).thenReturn(
            JsonHandler.getFromFile(PropertiesUtils.getResourceFile(RESULT_SELECT_ISLAST))
        );
        when(storageClient.getInformation(anyString(), eq(DataCategory.UNIT), anyString(), any(), eq(true))).thenReturn(
            OFFERS_INFO
        );

        EvidenceAuditParameters parameters = evidenceService.evidenceAuditsChecks(
            "aeaqaaaaaaguu2zzaazsualbwlwdgwaaaaaq",
            MetadataType.UNIT,
            loadStorageStrategiesMock()
        );
        EvidenceAuditParameters expected = JsonHandler.getFromFile(
            PropertiesUtils.getResourceFile(result),
            EvidenceAuditParameters.class
        );
        assertThat(parameters.getHashLfcFromDatabase()).isEqualTo(expected.getHashLfcFromDatabase());
        assertThat(parameters.getHashMdFromDatabase()).isEqualTo(expected.getHashMdFromDatabase());
        assertThat(parameters.getLfcVersion()).isEqualTo(expected.getLfcVersion());

        assertThat(parameters.getObjectStorageMetadataResultMap()).isNull();
        assertThat(parameters.getMdOptimisticStorageInfo().getStrategy()).isEqualTo("default");
    }

    @RunWithCustomExecutor
    @Test
    public void auditEvidenceWhenUnitNotSecure() throws Exception {
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);

        //GIVEN
        EvidenceService evidenceService = new EvidenceService(
            metaDataClientFactory,
            logbookOperationsClientFactory,
            logbookLifeCyclesClientFactory,
            storageClientFactory
        );

        JsonNode unitMd = getUnitMd();
        JsonNode liceCycle = getLifcycle();
        JsonNode logbook = getLogbook();

        //WHEN
        RequestResponseOK<JsonNode> response1 = new RequestResponseOK<JsonNode>().addResult(unitMd);

        when(metaDataClient.getUnitByIdRaw("aeaqaaaaaaguu2zzaazsualbwlwdgwaaaaaq")).thenReturn(response1);
        when(logbookLifeCyclesClient.getRawUnitLifeCycleById("aeaqaaaaaaguu2zzaazsualbwlwdgwaaaaaq")).thenReturn(
            liceCycle
        );

        JsonNode select = getSelectlogbookLCsecure();

        when(logbookOperationsClient.selectOperationById(anyString())).thenReturn(logbook);

        when(logbookOperationsClient.selectOperation(select)).thenReturn(
            JsonHandler.toJsonNode(new RequestResponseOK<JsonNode>())
        );

        EvidenceAuditParameters parameters = evidenceService.evidenceAuditsChecks(
            "aeaqaaaaaaguu2zzaazsualbwlwdgwaaaaaq",
            MetadataType.UNIT,
            loadStorageStrategiesMock()
        );

        assertThat(parameters.getEvidenceStatus()).isEqualTo(EvidenceStatus.WARN);
        assertThat(parameters.getAuditMessage()).contains("No traceability operation found matching date");
        assertThat(parameters.getMdOptimisticStorageInfo().getStrategy()).isEqualTo("default");
    }

    private JsonNode getSelectlogbookLCsecure() throws Exception {
        Select select = new Select();
        BooleanQuery query = and()
            .add(
                QueryHelper.eq(LogbookMongoDbName.eventType.getDbname(), "LOGBOOK_UNIT_LFC_TRACEABILITY"),
                QueryHelper.in(
                    "events.outDetail",
                    "LOGBOOK_UNIT_LFC_TRACEABILITY.OK",
                    "LOGBOOK_UNIT_LFC_TRACEABILITY.WARNING"
                ),
                QueryHelper.exists("events.evDetData.FileName"),
                lte("events.evDetData.StartDate", "2018-02-20T11:14:54.872"),
                gte("events.evDetData.EndDate", "2018-02-20T11:14:54.872")
            );

        select.setQuery(query);
        select.setLimitFilter(0, 1);
        select.addOrderByDescFilter("events.evDateTime");
        return select.getFinalSelect();
    }

    private JsonNode getSelect2() throws Exception {
        Select select = new Select();

        BooleanQuery query = and()
            .add(
                QueryHelper.eq(LogbookMongoDbName.eventType.getDbname(), "LOGBOOK_UNIT_LFC_TRACEABILITY"),
                QueryHelper.in(
                    "events.outDetail",
                    "LOGBOOK_UNIT_LFC_TRACEABILITY.OK",
                    "LOGBOOK_UNIT_LFC_TRACEABILITY.WARNING"
                ),
                QueryHelper.exists("events.evDetData.FileName")
            );

        select.setQuery(query);
        select.setLimitFilter(0, 1);
        select.addOrderByDescFilter("events.evDateTime");

        return select.getFinalSelect();
    }

    private JsonNode getUnitMd() throws FileNotFoundException, InvalidParseOperationException {
        return JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream("evidenceAudit/unitMd.json"));
    }

    private JsonNode getLifcycle() throws FileNotFoundException, InvalidParseOperationException {
        return JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream("evidenceAudit/lifeCycle.json"));
    }

    private JsonNode getLogbook() throws FileNotFoundException, InvalidParseOperationException {
        return JsonHandler.getFromInputStream(PropertiesUtils.getResourceAsStream("evidenceAudit/logbookSecure.json"));
    }

    @Test
    public void downloadAndExtractDataFromStorage() throws Exception {
        EvidenceService evidenceService = new EvidenceService(
            metaDataClientFactory,
            logbookOperationsClientFactory,
            logbookLifeCyclesClientFactory,
            storageClientFactory
        );

        try (
            InputStream in = PropertiesUtils.getResourceAsStream(
                "evidenceAudit/0_LogbookLifecycles_20180220_111512.zip"
            )
        ) {
            Response responseMock = mock(BuiltResponse.class);
            doReturn(in).when(responseMock).readEntity(eq(InputStream.class));
            when(
                storageClient.getContainerAsync(
                    eq(VitamConfiguration.getDefaultStrategy()),
                    anyString(),
                    eq(DataCategory.LOGBOOK),
                    any()
                )
            ).thenReturn(responseMock);
            assertThat(
                evidenceService.downloadAndExtractDataFromStorage(
                    "0_LogbookLifecycles_20180220_111512.zip",
                    "data.txt",
                    ".zip",
                    true
                )
            ).isNotNull();

            when(
                storageClient.getContainerAsync(
                    VitamConfiguration.getDefaultStrategy(),
                    "test",
                    DataCategory.LOGBOOK,
                    AccessLogUtils.getNoLogAccessLog()
                )
            ).thenThrow(StorageNotFoundException.class);

            assertThatThrownBy(
                () -> evidenceService.downloadAndExtractDataFromStorage("test", "data.txt", ".zip", true)
            )
                .isInstanceOf(EvidenceAuditException.class)
                .hasMessage("Could not retrieve traceability zip file 'test'");
        }
    }

    private List<StorageStrategy> loadStorageStrategiesMock() {
        StorageStrategy defaultStrategy = new StorageStrategy();
        defaultStrategy.setId("default");
        OfferReference offer1 = new OfferReference();
        offer1.setId("offer-fs-1.service.consul");
        List<OfferReference> offers = new ArrayList<>();
        offers.add(offer1);
        defaultStrategy.setOffers(offers);
        return Collections.singletonList(defaultStrategy);
    }
}
