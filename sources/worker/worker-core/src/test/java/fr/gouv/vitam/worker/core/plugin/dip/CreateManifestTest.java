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
package fr.gouv.vitam.worker.core.plugin.dip;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.DataObjectVersionType;
import fr.gouv.vitam.common.model.export.ExportRequest;
import fr.gouv.vitam.common.model.export.ExportRequestParameters;
import fr.gouv.vitam.common.model.processing.ProcessingUri;
import fr.gouv.vitam.common.model.processing.UriPrefix;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.common.utils.SupportedSedaVersions;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.xmlunit.builder.Input;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static fr.gouv.vitam.common.model.export.ExportRequest.EXPORT_QUERY_FILE_NAME;
import static fr.gouv.vitam.common.model.export.ExportType.ArchiveTransfer;
import static fr.gouv.vitam.worker.core.plugin.dip.CreateManifest.BINARIES_RANK;
import static fr.gouv.vitam.worker.core.plugin.dip.CreateManifest.GUID_TO_INFO_RANK;
import static fr.gouv.vitam.worker.core.plugin.dip.CreateManifest.MANIFEST_XML_RANK;
import static fr.gouv.vitam.worker.core.plugin.dip.CreateManifest.REPORT;
import static fr.gouv.vitam.worker.core.plugin.dip.CreateManifest.UNITS_JSONL_FILE;
import static junit.framework.TestCase.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xmlunit.matchers.EvaluateXPathMatcher.hasXPath;

public class CreateManifestTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Mock
    private MetaDataClientFactory metaDataClientFactory;

    private static final int TENANT_ID = 0;

    private CreateManifest createManifest;
    private HandlerIO handlerIO;

    private static final Map<String, String> prefix2Uri = new HashMap<>();

    static {
        prefix2Uri.put("vitam", "fr:gouv:culture:archivesdefrance:seda:v2.2");
    }

    @Before
    public void setUp() throws Exception {
        createManifest = new CreateManifest(metaDataClientFactory);
        handlerIO = mock(HandlerIO.class);
    }

    @Test
    @RunWithCustomExecutor
    public void should_create_manifest() throws Exception {
        // Given
        MetaDataClient metaDataClient = mock(MetaDataClient.class);
        given(metaDataClientFactory.getClient()).willReturn(metaDataClient);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        AccessContractModel accessContractModel = new AccessContractModel();
        accessContractModel.setEveryDataObjectVersion(true);
        accessContractModel.setEveryOriginatingAgency(true);

        VitamThreadUtils.getVitamSession().setContract(accessContractModel);

        JsonNode queryUnit = loadJson("/CreateManifest/query.json");

        JsonNode queryObjectGroup = loadJson("/CreateManifest/queryObjectGroup.json");

        given(metaDataClient.selectUnits(queryUnit.deepCopy())).willReturn(
            loadJson("/CreateManifest/resultMetadata.json")
        );

        given(metaDataClient.selectObjectGroups(queryObjectGroup)).willReturn(
            loadJson("/CreateManifest/resultObjectGroup.json")
        );

        File manifestFile = mockHandlerIO(MANIFEST_XML_RANK);
        mockHandlerIO(REPORT);
        File guidToPathFile = mockHandlerIO(GUID_TO_INFO_RANK);
        File binaryFile = mockHandlerIO(BINARIES_RANK);

        File unitsJsonlFile = tempFolder.newFile();
        given(handlerIO.getNewLocalFile(UNITS_JSONL_FILE)).willReturn(unitsJsonlFile);

        ExportRequest exportRequest = new ExportRequest();
        exportRequest.setExportWithLogBookLFC(true);
        exportRequest.setDslRequest(queryUnit);
        given(handlerIO.getJsonFromWorkspace(EXPORT_QUERY_FILE_NAME)).willReturn(JsonHandler.toJsonNode(exportRequest));

        WorkerParameters wp = WorkerParametersFactory.newWorkerParameters();

        // When
        ItemStatus itemStatus = createManifest.execute(wp, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);

        Map<String, Map<String, Object>> linkBetweenBinaryIdAndFileName = JsonHandler.getFromFileAsTypeReference(
            guidToPathFile,
            new TypeReference<>() {}
        );

        assertThat(linkBetweenBinaryIdAndFileName)
            .containsKey("aeaaaaaaaaerxz5cbmt2yal5tkullgiaaaaq")
            .containsKey("aeaaaaaaaaerxz5cbmt2yal5tk4lihqaaaba")
            .containsKey("aeaaaaaaaabhu53raawyuak7tm2uaqqaaaba")
            .doesNotContainKey("aeaaaaaaaabhu53raawyuak7tm2uaqiaaaaq");

        assertThat(
            linkBetweenBinaryIdAndFileName.get("aeaaaaaaaaerxz5cbmt2yal5tkullgiaaaaq").get("FILE_NAME")
        ).isEqualTo("Content/aeaaaaaaaaerxz5cbmt2yal5tkullgiaaaaq.jpeg");
        assertThat(
            linkBetweenBinaryIdAndFileName.get("aeaaaaaaaaerxz5cbmt2yal5tkullgiaaaaq").get("strategyId")
        ).isEqualTo("default-fake");

        assertThat(
            linkBetweenBinaryIdAndFileName.get("aeaaaaaaaaerxz5cbmt2yal5tk4lihqaaaba").get("FILE_NAME")
        ).isEqualTo("Content/aeaaaaaaaaerxz5cbmt2yal5tk4lihqaaaba.gif");
        assertThat(
            linkBetweenBinaryIdAndFileName.get("aeaaaaaaaaerxz5cbmt2yal5tk4lihqaaaba").get("strategyId")
        ).isEqualTo("default-fake");

        assertThat(linkBetweenBinaryIdAndFileName.get("aeaaaaaaaabhu53raawyuak7tm2uaqiaaaaq")).isNull();

        assertThat(
            linkBetweenBinaryIdAndFileName.get("aeaaaaaaaabhu53raawyuak7tm2uaqqaaaba").get("FILE_NAME")
        ).isEqualTo("Content/aeaaaaaaaabhu53raawyuak7tm2uaqqaaaba.pdf");
        assertThat(
            linkBetweenBinaryIdAndFileName.get("aeaaaaaaaabhu53raawyuak7tm2uaqqaaaba").get("strategyId")
        ).isEqualTo("default-fake-2");

        ArrayNode fromFile = (ArrayNode) JsonHandler.getFromFile(binaryFile);

        assertThat(fromFile)
            .hasSize(3)
            .extracting(JsonNode::asText)
            .containsExactlyInAnyOrder(
                "aeaaaaaaaaerxz5cbmt2yal5tkullgiaaaaq",
                "aeaaaaaaaaerxz5cbmt2yal5tk4lihqaaaba",
                "aeaaaaaaaabhu53raawyuak7tm2uaqqaaaba"
            )
            .doesNotContain("aeaaaaaaaabhu53raawyuak7tm2uaqiaaaaq");

        assertThat(
            Input.fromFile(manifestFile),
            hasXPath(
                "//vitam:ArchiveDeliveryRequestReply/vitam:DataObjectPackage/vitam:DataObjectGroup/vitam:BinaryDataObject/vitam:Uri",
                equalTo("Content/aeaaaaaaaaerxz5cbmt2yal5tkullgiaaaaq.jpeg")
            ).withNamespaceContext(prefix2Uri)
        );
        assertThat(
            Input.fromFile(manifestFile),
            hasXPath(
                "(//vitam:ArchiveDeliveryRequestReply/vitam:DataObjectPackage/vitam:DataObjectGroup/vitam:BinaryDataObject/vitam:Uri)[2]",
                equalTo("Content/aeaaaaaaaaerxz5cbmt2yal5tk4lihqaaaba.gif")
            ).withNamespaceContext(prefix2Uri)
        );
        assertThat(
            Input.fromFile(manifestFile),
            hasXPath(
                "//vitam:ArchiveDeliveryRequestReply/vitam:DataObjectPackage/vitam:ManagementMetadata/vitam:OriginatingAgencyIdentifier",
                equalTo("FRAN_NP_005568")
            ).withNamespaceContext(prefix2Uri)
        );
        assertThat(
            Input.fromFile(manifestFile),
            hasXPath(
                "//vitam:ArchiveDeliveryRequestReply/vitam:DataObjectPackage/vitam:DataObjectGroup/vitam:PhysicalDataObject/vitam:PhysicalId",
                equalTo("1 Num 1/204-4")
            ).withNamespaceContext(prefix2Uri)
        );
        assertThat(
            Input.fromFile(manifestFile),
            hasXPath(
                "//vitam:ArchiveDeliveryRequestReply/vitam:DataObjectPackage/vitam:DescriptiveMetadata/vitam:ArchiveUnit/vitam:Management/vitam:LogBook/vitam:Event/vitam:EventIdentifier",
                equalTo("aedqaaaaacaam7mxaaaamakvhiv4rsqaaaaq")
            ).withNamespaceContext(prefix2Uri)
        );
    }

    @Test
    @RunWithCustomExecutor
    public void testAccessControlManifestCreation() throws Exception {
        // Given
        MetaDataClient metaDataClient = mock(MetaDataClient.class);
        given(metaDataClientFactory.getClient()).willReturn(metaDataClient);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        AccessContractModel accessContractModel = new AccessContractModel();
        accessContractModel.setEveryDataObjectVersion(false);
        accessContractModel.setDataObjectVersion(new HashSet<>(Arrays.asList("PhysicalMaster", "BinaryMaster")));
        accessContractModel.setEveryOriginatingAgency(true);

        VitamThreadUtils.getVitamSession().setContract(accessContractModel);

        JsonNode queryUnit = loadJson("/CreateManifest/query.json");

        JsonNode queryObjectGroup = loadJson("/CreateManifest/queryObjectGroup.json");

        given(metaDataClient.selectUnits(queryUnit.deepCopy())).willReturn(
            loadJson("/CreateManifest/resultMetadata.json")
        );

        given(metaDataClient.selectObjectGroups(queryObjectGroup)).willReturn(
            loadJson("/CreateManifest/resultObjectGroup.json")
        );

        File manifestFile = mockHandlerIO(MANIFEST_XML_RANK);
        mockHandlerIO(REPORT);
        File guidToPathFile = mockHandlerIO(GUID_TO_INFO_RANK);
        File unitsJsonlFile = tempFolder.newFile();
        given(handlerIO.getNewLocalFile(UNITS_JSONL_FILE)).willReturn(unitsJsonlFile);
        File binaryFile = mockHandlerIO(BINARIES_RANK);

        ExportRequest exportRequest = new ExportRequest();
        exportRequest.setExportWithLogBookLFC(true);
        exportRequest.setDslRequest(queryUnit);
        ExportRequestParameters exportRequestParameters = new ExportRequestParameters();
        exportRequestParameters.setMessageRequestIdentifier(GUIDFactory.newGUID().getId());
        exportRequestParameters.setArchivalAgencyIdentifier("ArchivalAgency");
        exportRequestParameters.setRequesterIdentifier("Vitam-Bis");

        exportRequest.setExportRequestParameters(exportRequestParameters);

        given(handlerIO.getJsonFromWorkspace(EXPORT_QUERY_FILE_NAME)).willReturn(JsonHandler.toJsonNode(exportRequest));

        // When
        ItemStatus itemStatus = createManifest.execute(WorkerParametersFactory.newWorkerParameters(), handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);

        Map<String, Map<String, Object>> linkBetweenBinaryIdAndFileName = JsonHandler.getFromFileAsTypeReference(
            guidToPathFile,
            new TypeReference<>() {}
        );

        assertThat(linkBetweenBinaryIdAndFileName).hasSize(1);
        assertThat(linkBetweenBinaryIdAndFileName).containsKey("aeaaaaaaaaerxz5cbmt2yal5tkullgiaaaaq");

        assertThat(
            linkBetweenBinaryIdAndFileName.get("aeaaaaaaaaerxz5cbmt2yal5tkullgiaaaaq").get("FILE_NAME")
        ).isEqualTo("Content/aeaaaaaaaaerxz5cbmt2yal5tkullgiaaaaq.jpeg");

        ArrayNode fromFile = (ArrayNode) JsonHandler.getFromFile(binaryFile);

        assertThat(fromFile)
            .hasSize(1)
            .extracting(JsonNode::asText)
            .containsExactlyInAnyOrder("aeaaaaaaaaerxz5cbmt2yal5tkullgiaaaaq");

        assertThat(
            Input.fromFile(manifestFile),
            hasXPath(
                "//vitam:ArchiveDeliveryRequestReply/vitam:DataObjectPackage/vitam:DataObjectGroup/vitam:BinaryDataObject/vitam:Uri",
                equalTo("Content/aeaaaaaaaaerxz5cbmt2yal5tkullgiaaaaq.jpeg")
            ).withNamespaceContext(prefix2Uri)
        );
        assertThat(
            Input.fromFile(manifestFile),
            hasXPath(
                "//vitam:ArchiveDeliveryRequestReply/vitam:DataObjectPackage/vitam:ManagementMetadata/vitam:OriginatingAgencyIdentifier",
                equalTo("FRAN_NP_005568")
            ).withNamespaceContext(prefix2Uri)
        );
    }

    @Test
    @RunWithCustomExecutor
    public void givenSipWhenUnitsLinkedToOneDataObjectGroupThenDipContainOneDataObjectGroupElement() throws Exception {
        // Given
        MetaDataClient metaDataClient = mock(MetaDataClient.class);
        given(metaDataClientFactory.getClient()).willReturn(metaDataClient);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        AccessContractModel accessContractModel = new AccessContractModel();
        accessContractModel.setEveryDataObjectVersion(false);
        accessContractModel.setDataObjectVersion(new HashSet<>(Arrays.asList("PhysicalMaster", "BinaryMaster_1")));
        accessContractModel.setEveryOriginatingAgency(true);

        VitamThreadUtils.getVitamSession().setContract(accessContractModel);

        JsonNode queryUnit = loadJson("/CreateManifest/querybug5160.json");

        JsonNode queryObjectGroup = loadJson("/CreateManifest/queryObjectGroupbug5160.json");

        given(metaDataClient.selectUnits(queryUnit.deepCopy())).willReturn(
            loadJson("/CreateManifest/resultMetadatabug5160.json")
        );

        given(metaDataClient.selectObjectGroups(queryObjectGroup)).willReturn(
            loadJson("/CreateManifest/resultObjectGroup5160.json")
        );

        File manifestFile = mockHandlerIO(MANIFEST_XML_RANK);
        mockHandlerIO(REPORT);
        mockHandlerIO(GUID_TO_INFO_RANK);
        File unitsJsonlFile = tempFolder.newFile();
        given(handlerIO.getNewLocalFile(UNITS_JSONL_FILE)).willReturn(unitsJsonlFile);
        mockHandlerIO(BINARIES_RANK);

        ExportRequest exportRequest = new ExportRequest();
        exportRequest.setExportWithLogBookLFC(true);
        exportRequest.setDslRequest(queryUnit);
        given(handlerIO.getJsonFromWorkspace("export_query.json")).willReturn(JsonHandler.toJsonNode(exportRequest));

        // When
        ItemStatus itemStatus = createManifest.execute(WorkerParametersFactory.newWorkerParameters(), handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);

        assertThat(
            StringUtils.countMatches(
                Files.readAllLines(Paths.get(manifestFile.getPath()), Charset.defaultCharset()).get(0),
                "<DataObjectGroup id=\"aebaaaaaaefjz7wkabvpoalnfgzdwfyaaaaq\"><"
            ),
            equalTo(1)
        );
    }

    @Test
    @RunWithCustomExecutor
    public void should_create_report() throws Exception {
        // Given
        MetaDataClient metaDataClient = mock(MetaDataClient.class);
        given(metaDataClientFactory.getClient()).willReturn(metaDataClient);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        AccessContractModel accessContractModel = new AccessContractModel();
        accessContractModel.setEveryDataObjectVersion(true);
        accessContractModel.setEveryOriginatingAgency(true);

        VitamThreadUtils.getVitamSession().setContract(accessContractModel);

        JsonNode queryUnit = loadJson("/CreateManifest/query.json");

        JsonNode queryObjectGroup = loadJson("/CreateManifest/queryObjectGroup.json");

        given(metaDataClient.selectUnits(queryUnit.deepCopy())).willReturn(
            loadJson("/CreateManifest/resultMetadata.json")
        );

        given(metaDataClient.selectObjectGroups(queryObjectGroup)).willReturn(
            loadJson("/CreateManifest/resultObjectGroup.json")
        );

        mockHandlerIO(MANIFEST_XML_RANK);
        File reportFile = mockHandlerIO(REPORT);
        mockHandlerIO(GUID_TO_INFO_RANK);
        mockHandlerIO(BINARIES_RANK);
        File unitsJsonlFile = tempFolder.newFile();
        given(handlerIO.getNewLocalFile(UNITS_JSONL_FILE)).willReturn(unitsJsonlFile);

        ExportRequest exportRequest = getExportRequest(queryUnit);
        given(handlerIO.getJsonFromWorkspace(EXPORT_QUERY_FILE_NAME)).willReturn(JsonHandler.toJsonNode(exportRequest));

        WorkerParameters wp = WorkerParametersFactory.newWorkerParameters();

        // When
        ItemStatus itemStatus = createManifest.execute(wp, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
        List<String> lines = Files.lines(reportFile.toPath()).map(String::trim).collect(Collectors.toList());
        assertThat(lines.size()).isEqualTo(6); // 4 units + 2 report header
        assertEquals("{\"id\":\"aeaqaaaaaadf6mc4aathcak7tmtgdnaaaaba\",\"status\":\"OK\"}", lines.get(2));
        assertEquals("{\"id\":\"aeaqaaaaaadf6mc4aathcak7tmtgdmyaaaba\",\"status\":\"OK\"}", lines.get(3));
        assertEquals("{\"id\":\"aeaqaaaaaadf6mc4aathcak7tmtgdayaaaca\",\"status\":\"OK\"}", lines.get(4));
        assertEquals("{\"id\":\"aeaqaaaaaadf6mc4aathcak7tmtgdniaaaba\",\"status\":\"OK\"}", lines.get(5));
        verify(handlerIO).transferInputStreamToWorkspace(
            eq(VitamThreadUtils.getVitamSession().getRequestId() + ".jsonl"),
            any(InputStream.class),
            eq(null),
            eq(false)
        );
    }

    @Test
    @RunWithCustomExecutor
    public void should_transfer_with_warning() throws Exception {
        // Given
        MetaDataClient metaDataClient = mock(MetaDataClient.class);
        given(metaDataClientFactory.getClient()).willReturn(metaDataClient);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        AccessContractModel accessContractModel = new AccessContractModel();
        accessContractModel.setEveryDataObjectVersion(true);
        accessContractModel.setEveryOriginatingAgency(true);

        VitamThreadUtils.getVitamSession().setContract(accessContractModel);

        JsonNode queryUnit = loadJson("/CreateManifest/query.json");

        JsonNode queryObjectGroup = loadJson("/CreateManifest/queryObjectGroup.json");

        given(metaDataClient.selectUnits(queryUnit.deepCopy())).willReturn(
            loadJson("/CreateManifest/resultMetadataWithTransfer.json")
        );

        given(metaDataClient.selectObjectGroups(queryObjectGroup)).willReturn(
            loadJson("/CreateManifest/resultObjectGroup.json")
        );

        mockHandlerIO(MANIFEST_XML_RANK);
        mockHandlerIO(REPORT);
        mockHandlerIO(GUID_TO_INFO_RANK);
        mockHandlerIO(BINARIES_RANK);
        File unitsJsonlFile = tempFolder.newFile();
        given(handlerIO.getNewLocalFile(UNITS_JSONL_FILE)).willReturn(unitsJsonlFile);

        ExportRequest exportRequest = getExportRequest(queryUnit);
        given(handlerIO.getJsonFromWorkspace(EXPORT_QUERY_FILE_NAME)).willReturn(JsonHandler.toJsonNode(exportRequest));

        WorkerParameters wp = WorkerParametersFactory.newWorkerParameters();

        // When
        ItemStatus itemStatus = createManifest.execute(wp, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.WARNING);
        verify(handlerIO).transferInputStreamToWorkspace(
            eq(VitamThreadUtils.getVitamSession().getRequestId() + ".jsonl"),
            any(InputStream.class),
            eq(null),
            eq(false)
        );
    }

    @Test
    @RunWithCustomExecutor
    public void should_return_KO_status_when_exceeding_threshold() throws Exception {
        // Given
        MetaDataClient metaDataClient = mock(MetaDataClient.class);
        given(metaDataClientFactory.getClient()).willReturn(metaDataClient);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        AccessContractModel accessContractModel = new AccessContractModel();
        accessContractModel.setEveryDataObjectVersion(true);
        accessContractModel.setEveryOriginatingAgency(true);

        VitamThreadUtils.getVitamSession().setContract(accessContractModel);

        JsonNode queryUnit = loadJson("/CreateManifest/query.json");

        JsonNode queryObjectGroup = loadJson("/CreateManifest/queryObjectGroup.json");

        given(metaDataClient.selectUnits(queryUnit.deepCopy())).willReturn(
            loadJson("/CreateManifest/resultMetadataWithTransfer.json")
        );

        given(metaDataClient.selectObjectGroups(queryObjectGroup)).willReturn(
            loadJson("/CreateManifest/resultObjectGroup.json")
        );

        mockHandlerIO(MANIFEST_XML_RANK);
        mockHandlerIO(REPORT);
        File unitsJsonlFile = tempFolder.newFile();
        given(handlerIO.getNewLocalFile(UNITS_JSONL_FILE)).willReturn(unitsJsonlFile);

        ExportRequest exportRequest = getExportRequest(queryUnit);
        exportRequest.setMaxSizeThreshold(500000L);
        given(handlerIO.getJsonFromWorkspace(EXPORT_QUERY_FILE_NAME)).willReturn(JsonHandler.toJsonNode(exportRequest));

        WorkerParameters wp = WorkerParametersFactory.newWorkerParameters();

        // When
        ItemStatus itemStatus = createManifest.execute(wp, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
    }

    @Test
    @RunWithCustomExecutor
    public void should_return_KO_status_when_exceeding_threshold_with_only_manifest() throws Exception {
        // Given
        MetaDataClient metaDataClient = mock(MetaDataClient.class);
        given(metaDataClientFactory.getClient()).willReturn(metaDataClient);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        AccessContractModel accessContractModel = new AccessContractModel();
        accessContractModel.setEveryDataObjectVersion(true);
        accessContractModel.setEveryOriginatingAgency(true);

        VitamThreadUtils.getVitamSession().setContract(accessContractModel);

        JsonNode queryUnit = loadJson("/CreateManifest/query.json");

        JsonNode queryObjectGroup = loadJson("/CreateManifest/queryObjectGroup.json");

        given(metaDataClient.selectUnits(queryUnit.deepCopy())).willReturn(
            loadJson("/CreateManifest/resultMetadataWithTransfer.json")
        );

        given(metaDataClient.selectObjectGroups(queryObjectGroup)).willReturn(
            JsonHandler.createObjectNode().set(RequestResponseOK.TAG_RESULTS, JsonHandler.createArrayNode())
        );

        mockHandlerIO(MANIFEST_XML_RANK);
        File unitsJsonlFile = tempFolder.newFile();
        given(handlerIO.getNewLocalFile(UNITS_JSONL_FILE)).willReturn(unitsJsonlFile);
        mockHandlerIO(REPORT);

        ExportRequest exportRequest = getExportRequest(queryUnit);
        // We set threshold to 10 to be sure it's smaller than manifest size
        exportRequest.setMaxSizeThreshold(10L);
        given(handlerIO.getJsonFromWorkspace(EXPORT_QUERY_FILE_NAME)).willReturn(JsonHandler.toJsonNode(exportRequest));

        WorkerParameters wp = WorkerParametersFactory.newWorkerParameters();

        // When
        ItemStatus itemStatus = createManifest.execute(wp, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.KO);
    }

    private ExportRequest getExportRequest(JsonNode queryUnit) {
        ExportRequest exportRequest = new ExportRequest();
        exportRequest.setExportWithLogBookLFC(true);
        exportRequest.setDslRequest(queryUnit);
        exportRequest.setExportType(ArchiveTransfer);
        exportRequest.setSedaVersion(SupportedSedaVersions.SEDA_2_2.getVersion());
        ExportRequestParameters exportRequestParameters = new ExportRequestParameters();
        exportRequestParameters.setArchivalAgreement("ArchivalAgreement");
        exportRequestParameters.setOriginatingAgencyIdentifier("OriginatingAgencyIdentifier");
        exportRequestParameters.setComment("Comment");
        exportRequestParameters.setSubmissionAgencyIdentifier("SubmissionAgencyIdentifier");
        exportRequestParameters.setRelatedTransferReference(Collections.singletonList("RelatedTransferReference"));
        exportRequestParameters.setTransferRequestReplyIdentifier("TransferRequestReplyIdentifier");
        exportRequestParameters.setArchivalAgencyIdentifier("ArchivalAgencyIdentifier");
        exportRequestParameters.setTransferringAgency("TransferringAgency");
        exportRequest.setExportRequestParameters(exportRequestParameters);
        return exportRequest;
    }

    @Test
    @RunWithCustomExecutor
    public void should_not_duplicate_object_groups_in_manifest_file() throws Exception {
        // Given
        final int maxElementsInQuery = 2; // Should be lower than units size to make sure there are several partitions

        final JsonNode metadataJson = loadJson("/CreateManifest/resultMetadataWithCommonObjectGroup.json");
        final List<JsonNode> unitsJson = StreamSupport.stream(
            metadataJson.get("$results").spliterator(),
            false
        ).collect(Collectors.toList());

        // Making sure that maxElementsInQuery is lower than units size
        assertThat(maxElementsInQuery).isLessThan(unitsJson.size());

        // Override maxElementsInQuery in createManifest to have multiple partitions
        createManifest = new CreateManifest(metaDataClientFactory, maxElementsInQuery);

        MetaDataClient metaDataClient = mock(MetaDataClient.class);
        given(metaDataClientFactory.getClient()).willReturn(metaDataClient);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        AccessContractModel accessContractModel = new AccessContractModel();
        accessContractModel.setEveryDataObjectVersion(true);
        accessContractModel.setEveryOriginatingAgency(true);

        VitamThreadUtils.getVitamSession().setContract(accessContractModel);

        JsonNode queryUnit = loadJson("/CreateManifest/query.json");

        given(metaDataClient.selectUnits(queryUnit.deepCopy())).willReturn(metadataJson);

        // Make selectObjectGroups return object groups corresponding to the ids in the query
        final ArgumentCaptor<JsonNode> jsonNodeArgumentCaptor = ArgumentCaptor.forClass(JsonNode.class);
        when(metaDataClient.selectObjectGroups(jsonNodeArgumentCaptor.capture())).thenAnswer(invocation -> {
            final List<String> objectGroupIds = StreamSupport.stream(
                jsonNodeArgumentCaptor.getValue().get("$query").get("$in").get("#id").spliterator(),
                false
            )
                .map(JsonNode::asText)
                .collect(Collectors.toList());
            return JsonHandler.getFromString(
                "{'$results': [" +
                objectGroupIds
                    .stream()
                    .map(
                        ogId ->
                            "{'#id': '" +
                            ogId +
                            "', '#qualifiers': [{'versions': [{'DataObjectVersion': 'PhysicalMaster_1', '#id': 'aeaaaaaaaabgthlqabqgsalltyqvysiaaaaq'}], 'qualifier': '" +
                            DataObjectVersionType.BINARY_MASTER.getName() +
                            "'}]}"
                    )
                    .collect(Collectors.joining(",")) +
                "]}"
            );
        });

        File manifestFile = mockHandlerIO(MANIFEST_XML_RANK);
        mockHandlerIO(REPORT);
        mockHandlerIO(GUID_TO_INFO_RANK);
        mockHandlerIO(BINARIES_RANK);
        File unitsJsonlFile = tempFolder.newFile();
        given(handlerIO.getNewLocalFile(UNITS_JSONL_FILE)).willReturn(unitsJsonlFile);

        ExportRequest exportRequest = new ExportRequest();
        exportRequest.setExportWithLogBookLFC(true);
        exportRequest.setDslRequest(queryUnit);
        given(handlerIO.getJsonFromWorkspace(EXPORT_QUERY_FILE_NAME)).willReturn(JsonHandler.toJsonNode(exportRequest));

        WorkerParameters wp = WorkerParametersFactory.newWorkerParameters();

        // When
        ItemStatus itemStatus = createManifest.execute(wp, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);

        final Set<String> expectedObjectGroupIds = unitsJson
            .stream()
            .map(unit -> unit.get("#object").asText())
            .collect(Collectors.toSet());
        final String manifestXml = FileUtils.readFileToString(manifestFile, "UTF-8");
        expectedObjectGroupIds.forEach(
            expectedObjectGroupId ->
                assertThat(manifestXml).containsOnlyOnce("<DataObjectGroup id=\"" + expectedObjectGroupId + "\"")
        );
    }

    private File mockHandlerIO(int output) throws IOException {
        final File file = tempFolder.newFile();
        given(handlerIO.getOutput(output)).willReturn(new ProcessingUri(UriPrefix.WORKSPACE, file.getPath()));
        given(handlerIO.getNewLocalFile(file.getPath())).willReturn(file);
        return file;
    }

    private JsonNode loadJson(String fileName) throws InvalidParseOperationException {
        return JsonHandler.getFromInputStream(getClass().getResourceAsStream(fileName));
    }
}
