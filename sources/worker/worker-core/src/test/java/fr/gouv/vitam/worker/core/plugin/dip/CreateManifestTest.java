/*
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
package fr.gouv.vitam.worker.core.plugin.dip;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.export.ExportRequest;
import fr.gouv.vitam.common.model.export.ExportRequestParameters;
import fr.gouv.vitam.common.model.processing.ProcessingUri;
import fr.gouv.vitam.common.model.processing.UriPrefix;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.BackupService;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.common.HandlerIO;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.xmlunit.builder.Input;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.model.dip.DipExportRequest.DIP_REQUEST_FILE_NAME;
import static fr.gouv.vitam.common.model.export.ExportType.ArchiveTransfer;
import static fr.gouv.vitam.worker.core.plugin.dip.CreateManifest.BINARIES_RANK;
import static fr.gouv.vitam.worker.core.plugin.dip.CreateManifest.GUID_TO_INFO_RANK;
import static fr.gouv.vitam.worker.core.plugin.dip.CreateManifest.MANIFEST_XML_RANK;
import static fr.gouv.vitam.worker.core.plugin.dip.CreateManifest.REPORT;
import static junit.framework.TestCase.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.xmlunit.matchers.EvaluateXPathMatcher.hasXPath;

public class CreateManifestTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Mock
    private MetaDataClientFactory metaDataClientFactory;

    @Mock
    private BackupService backupService;

    private static final int TENANT_ID = 0;

    private CreateManifest createManifest;

    private static Map<String, String> prefix2Uri = new HashMap<>();

    static {
        prefix2Uri.put("vitam", "fr:gouv:culture:archivesdefrance:seda:v2.1");
    }

    @Before
    public void setUp() throws Exception {
        createManifest = new CreateManifest(metaDataClientFactory, backupService);
    }

    @Test
    @RunWithCustomExecutor
    public void should_create_manifest() throws Exception {
        // Given
        HandlerIO handlerIO = mock(HandlerIO.class);
        MetaDataClient metaDataClient = mock(MetaDataClient.class);
        given(metaDataClientFactory.getClient()).willReturn(metaDataClient);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        AccessContractModel accessContractModel = new AccessContractModel();
        accessContractModel.setEveryDataObjectVersion(true);
        accessContractModel.setEveryOriginatingAgency(true);

        VitamThreadUtils.getVitamSession().setContract(accessContractModel);

        JsonNode queryUnit =
            JsonHandler.getFromInputStream(getClass().getResourceAsStream("/CreateManifest/query.json"));

        JsonNode queryUnitWithTree =
            JsonHandler
                .getFromInputStream(getClass().getResourceAsStream("/CreateManifest/queryWithTreeProjection.json"));

        JsonNode queryObjectGroup =
            JsonHandler.getFromInputStream(getClass().getResourceAsStream("/CreateManifest/queryObjectGroup.json"));

        given(handlerIO.getJsonFromWorkspace("query.json")).willReturn(queryUnit);

        given(metaDataClient.selectUnits(queryUnit.deepCopy())).willReturn(
            JsonHandler.getFromInputStream(getClass().getResourceAsStream("/CreateManifest/resultMetadata.json")));

        given(metaDataClient.selectUnits(queryUnitWithTree)).willReturn(
            JsonHandler.getFromInputStream(getClass().getResourceAsStream("/CreateManifest/resultMetadataTree.json")));

        given(metaDataClient.selectObjectGroups(queryObjectGroup)).willReturn(
            JsonHandler.getFromInputStream(getClass().getResourceAsStream("/CreateManifest/resultObjectGroup.json")));

        File manifestFile = tempFolder.newFile();
        given(handlerIO.getOutput(MANIFEST_XML_RANK))
            .willReturn(new ProcessingUri(UriPrefix.WORKSPACE, manifestFile.getPath()));
        given(handlerIO.getNewLocalFile(manifestFile.getPath())).willReturn(manifestFile);

        File reportFile = tempFolder.newFile();
        given(handlerIO.getOutput(REPORT)).willReturn(new ProcessingUri(UriPrefix.WORKSPACE, reportFile.getPath()));
        given(handlerIO.getNewLocalFile(reportFile.getPath())).willReturn(reportFile);

        File guidToPathFile = tempFolder.newFile();
        given(handlerIO.getOutput(GUID_TO_INFO_RANK))
            .willReturn(new ProcessingUri(UriPrefix.WORKSPACE, guidToPathFile.getPath()));
        given(handlerIO.getNewLocalFile(guidToPathFile.getPath())).willReturn(guidToPathFile);

        File binaryFile = tempFolder.newFile();
        given(handlerIO.getOutput(BINARIES_RANK))
            .willReturn(new ProcessingUri(UriPrefix.WORKSPACE, binaryFile.getPath()));
        given(handlerIO.getNewLocalFile(binaryFile.getPath())).willReturn(binaryFile);

        ExportRequest exportRequest = new ExportRequest();
        exportRequest.setExportWithLogBookLFC(true);
        exportRequest.setDslRequest(queryUnit);
        given(handlerIO.getJsonFromWorkspace(DIP_REQUEST_FILE_NAME)).willReturn(JsonHandler.toJsonNode(exportRequest));

        WorkerParameters wp = WorkerParametersFactory.newWorkerParameters();

        // When
        ItemStatus itemStatus = createManifest.execute(wp, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);

        Map<String, Object> linkBetweenBinaryIdAndFileName =
            JsonHandler.getMapFromInputStream(new FileInputStream(guidToPathFile));

        assertThat(linkBetweenBinaryIdAndFileName)
            .containsKey("aeaaaaaaaabhu53raawyuak7tm2uapqaaaaq")
            .containsKey("aeaaaaaaaabhu53raawyuak7tm2uaqqaaaba")
            .doesNotContainKey("aeaaaaaaaabhu53raawyuak7tm2uaqiaaaaq");

        assertThat(((Map) linkBetweenBinaryIdAndFileName.get("aeaaaaaaaabhu53raawyuak7tm2uapqaaaaq")).get("FILE_NAME"))
            .isEqualTo("Content/aeaaaaaaaabhu53raawyuak7tm2uapqaaaaq.pdf");
        assertThat(((Map) linkBetweenBinaryIdAndFileName.get("aeaaaaaaaabhu53raawyuak7tm2uapqaaaaq")).get("strategyId"))
            .isEqualTo("default-fake");

        assertThat(((Map) linkBetweenBinaryIdAndFileName.get("aeaaaaaaaabhu53raawyuak7tm2uaqiaaaaq"))).isNull();

        assertThat(((Map) linkBetweenBinaryIdAndFileName.get("aeaaaaaaaabhu53raawyuak7tm2uaqqaaaba")).get("FILE_NAME"))
            .isEqualTo("Content/aeaaaaaaaabhu53raawyuak7tm2uaqqaaaba.pdf");
        assertThat(((Map) linkBetweenBinaryIdAndFileName.get("aeaaaaaaaabhu53raawyuak7tm2uaqqaaaba")).get("strategyId"))
            .isEqualTo("default-fake-2");

        ArrayNode fromFile = (ArrayNode) JsonHandler.getFromFile(binaryFile);

        assertThat(fromFile).hasSize(2).extracting(JsonNode::asText)
            .containsExactlyInAnyOrder("aeaaaaaaaabhu53raawyuak7tm2uapqaaaaq", "aeaaaaaaaabhu53raawyuak7tm2uaqqaaaba")
            .doesNotContain("aeaaaaaaaabhu53raawyuak7tm2uaqiaaaaq");

        Assert.assertThat(Input.fromFile(manifestFile), hasXPath(
            "//vitam:ArchiveDeliveryRequestReply/vitam:DataObjectPackage/vitam:DataObjectGroup/vitam:BinaryDataObject/vitam:Uri",
            equalTo("Content/aeaaaaaaaabhu53raawyuak7tm2uapqaaaaq.pdf"))
            .withNamespaceContext(prefix2Uri));
        Assert.assertThat(Input.fromFile(manifestFile), hasXPath(
            "//vitam:ArchiveDeliveryRequestReply/vitam:DataObjectPackage/vitam:ManagementMetadata/vitam:OriginatingAgencyIdentifier",
            equalTo("FRAN_NP_005568"))
            .withNamespaceContext(prefix2Uri));
        Assert.assertThat(Input.fromFile(manifestFile), hasXPath(
            "//vitam:ArchiveDeliveryRequestReply/vitam:DataObjectPackage/vitam:DataObjectGroup/vitam:PhysicalDataObject/vitam:PhysicalId",
            equalTo("1 Num 1/204-4"))
            .withNamespaceContext(prefix2Uri));
        Assert.assertThat(Input.fromFile(manifestFile), hasXPath(
            "//vitam:ArchiveDeliveryRequestReply/vitam:DataObjectPackage/vitam:DescriptiveMetadata/vitam:ArchiveUnit/vitam:Management/vitam:LogBook/vitam:Event/vitam:EventIdentifier",
            equalTo("aedqaaaaacaam7mxaaaamakvhiv4rsqaaaaq")).withNamespaceContext(prefix2Uri));
    }



    @Test
    @RunWithCustomExecutor
    public void testAccesControlManifestCreation() throws Exception {
        // Given
        HandlerIO handlerIO = mock(HandlerIO.class);
        MetaDataClient metaDataClient = mock(MetaDataClient.class);
        given(metaDataClientFactory.getClient()).willReturn(metaDataClient);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        AccessContractModel accessContractModel = new AccessContractModel();
        accessContractModel.setEveryDataObjectVersion(false);
        accessContractModel.setDataObjectVersion(new HashSet<>(Arrays.asList("PhysicalMaster", "BinaryMaster")));
        accessContractModel.setEveryOriginatingAgency(true);

        VitamThreadUtils.getVitamSession().setContract(accessContractModel);

        JsonNode queryUnit =
            JsonHandler.getFromInputStream(getClass().getResourceAsStream("/CreateManifest/query.json"));

        JsonNode queryDataObjectVersion =
            JsonHandler
                .getFromInputStream(getClass().getResourceAsStream("/CreateManifest/dataObjectVersionFilter.json"));

        JsonNode queryUnitWithTree =
            JsonHandler
                .getFromInputStream(getClass().getResourceAsStream("/CreateManifest/queryWithTreeProjection.json"));

        JsonNode queryObjectGroup =
            JsonHandler.getFromInputStream(getClass().getResourceAsStream("/CreateManifest/queryObjectGroup.json"));

        given(handlerIO.getJsonFromWorkspace("query.json")).willReturn(queryUnit);

        given(handlerIO.getJsonFromWorkspace("dataObjectVersionFilter.json")).willReturn(queryDataObjectVersion);

        given(metaDataClient.selectUnits(queryUnit.deepCopy())).willReturn(
            JsonHandler.getFromInputStream(getClass().getResourceAsStream("/CreateManifest/resultMetadata.json")));

        given(metaDataClient.selectUnits(queryUnitWithTree)).willReturn(
            JsonHandler.getFromInputStream(getClass().getResourceAsStream("/CreateManifest/resultMetadataTree.json")));

        given(metaDataClient.selectObjectGroups(queryObjectGroup)).willReturn(
            JsonHandler.getFromInputStream(getClass().getResourceAsStream("/CreateManifest/resultObjectGroup.json")));

        File manifestFile = tempFolder.newFile();
        given(handlerIO.getOutput(MANIFEST_XML_RANK))
            .willReturn(new ProcessingUri(UriPrefix.WORKSPACE, manifestFile.getPath()));
        given(handlerIO.getNewLocalFile(manifestFile.getPath())).willReturn(manifestFile);

        File reportFile = tempFolder.newFile();
        given(handlerIO.getOutput(REPORT)).willReturn(new ProcessingUri(UriPrefix.WORKSPACE, reportFile.getPath()));
        given(handlerIO.getNewLocalFile(reportFile.getPath())).willReturn(reportFile);

        File guidToPathFile = tempFolder.newFile();
        given(handlerIO.getOutput(GUID_TO_INFO_RANK))
            .willReturn(new ProcessingUri(UriPrefix.WORKSPACE, guidToPathFile.getPath()));
        given(handlerIO.getNewLocalFile(guidToPathFile.getPath())).willReturn(guidToPathFile);

        File binaryFile = tempFolder.newFile();
        given(handlerIO.getOutput(BINARIES_RANK))
            .willReturn(new ProcessingUri(UriPrefix.WORKSPACE, binaryFile.getPath()));
        given(handlerIO.getNewLocalFile(binaryFile.getPath())).willReturn(binaryFile);
        ExportRequest exportRequest = new ExportRequest();
        exportRequest.setExportWithLogBookLFC(true);
        exportRequest.setDslRequest(queryUnit);
        ExportRequestParameters exportRequestParameters = new ExportRequestParameters();
        exportRequestParameters.setMessageRequestIdentifier(GUIDFactory.newGUID().getId());
        exportRequestParameters.setArchivalAgencyIdentifier("ArchivalAgency");
        exportRequestParameters.setRequesterIdentifier("Vitam-Bis");

        exportRequest.setExportRequestParameters(exportRequestParameters);

        given(handlerIO.getJsonFromWorkspace(DIP_REQUEST_FILE_NAME)).willReturn(JsonHandler.toJsonNode(exportRequest));

        // When
        ItemStatus itemStatus = createManifest.execute(WorkerParametersFactory.newWorkerParameters(), handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);

        Map<String, Object> linkBetweenBinaryIdAndFileName =
            JsonHandler.getMapFromInputStream(new FileInputStream(guidToPathFile));

        assertThat(linkBetweenBinaryIdAndFileName).hasSize(1);
        assertThat(linkBetweenBinaryIdAndFileName)
            .containsKey("aeaaaaaaaabhu53raawyuak7tm2uapqaaaaq");

        assertThat(((Map) linkBetweenBinaryIdAndFileName.get("aeaaaaaaaabhu53raawyuak7tm2uapqaaaaq")).get("FILE_NAME"))
            .isEqualTo("Content/aeaaaaaaaabhu53raawyuak7tm2uapqaaaaq.pdf");

        ArrayNode fromFile = (ArrayNode) JsonHandler.getFromFile(binaryFile);

        assertThat(fromFile).hasSize(1).extracting(JsonNode::asText)
            .containsExactlyInAnyOrder("aeaaaaaaaabhu53raawyuak7tm2uapqaaaaq");

        Assert.assertThat(Input.fromFile(manifestFile), hasXPath(
            "//vitam:ArchiveDeliveryRequestReply/vitam:DataObjectPackage/vitam:DataObjectGroup/vitam:BinaryDataObject/vitam:Uri",
            equalTo("Content/aeaaaaaaaabhu53raawyuak7tm2uapqaaaaq.pdf"))
            .withNamespaceContext(prefix2Uri));
        Assert.assertThat(Input.fromFile(manifestFile), hasXPath(
            "//vitam:ArchiveDeliveryRequestReply/vitam:DataObjectPackage/vitam:ManagementMetadata/vitam:OriginatingAgencyIdentifier",
            equalTo("FRAN_NP_005568"))
            .withNamespaceContext(prefix2Uri));
    }

    @Test
    @RunWithCustomExecutor
    public void givenSipWhenUnitsLinkedToOneDataObjectGroupThenDipContainOneDataObjectGroupElement() throws Exception {
        // Given
        HandlerIO handlerIO = mock(HandlerIO.class);
        MetaDataClient metaDataClient = mock(MetaDataClient.class);
        given(metaDataClientFactory.getClient()).willReturn(metaDataClient);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        AccessContractModel accessContractModel = new AccessContractModel();
        accessContractModel.setEveryDataObjectVersion(false);
        accessContractModel.setDataObjectVersion(new HashSet<>(Arrays.asList("PhysicalMaster", "BinaryMaster_1")));
        accessContractModel.setEveryOriginatingAgency(true);

        VitamThreadUtils.getVitamSession().setContract(accessContractModel);

        JsonNode queryUnit =
            JsonHandler.getFromInputStream(getClass().getResourceAsStream("/CreateManifest/querybug5160.json"));

        JsonNode queryUnitWithTree =
            JsonHandler.getFromInputStream(
                getClass().getResourceAsStream("/CreateManifest/queryWithTreeProjectionbug5160.json"));

        JsonNode queryObjectGroup =
            JsonHandler
                .getFromInputStream(getClass().getResourceAsStream("/CreateManifest/queryObjectGroupbug5160.json"));

        given(handlerIO.getJsonFromWorkspace("query.json")).willReturn(queryUnit);

        given(metaDataClient.selectUnits(queryUnit.deepCopy())).willReturn(
            JsonHandler
                .getFromInputStream(getClass().getResourceAsStream("/CreateManifest/resultMetadatabug5160.json")));

        given(metaDataClient.selectUnits(queryUnitWithTree)).willReturn(
            JsonHandler
                .getFromInputStream(getClass().getResourceAsStream("/CreateManifest/resultMetadatabug5160.json")));

        given(metaDataClient.selectObjectGroups(queryObjectGroup)).willReturn(
            JsonHandler
                .getFromInputStream(getClass().getResourceAsStream("/CreateManifest/resultObjectGroup5160.json")));

        File manifestFile = tempFolder.newFile();
        given(handlerIO.getOutput(MANIFEST_XML_RANK))
            .willReturn(new ProcessingUri(UriPrefix.WORKSPACE, manifestFile.getPath()));
        given(handlerIO.getNewLocalFile(manifestFile.getPath())).willReturn(manifestFile);

        File reportFile = tempFolder.newFile();
        given(handlerIO.getOutput(REPORT)).willReturn(new ProcessingUri(UriPrefix.WORKSPACE, reportFile.getPath()));
        given(handlerIO.getNewLocalFile(reportFile.getPath())).willReturn(reportFile);

        File guidToPathFile = tempFolder.newFile();
        given(handlerIO.getOutput(GUID_TO_INFO_RANK))
            .willReturn(new ProcessingUri(UriPrefix.WORKSPACE, guidToPathFile.getPath()));
        given(handlerIO.getNewLocalFile(guidToPathFile.getPath())).willReturn(guidToPathFile);

        File binaryFile = tempFolder.newFile();
        given(handlerIO.getOutput(BINARIES_RANK))
            .willReturn(new ProcessingUri(UriPrefix.WORKSPACE, binaryFile.getPath()));
        given(handlerIO.getNewLocalFile(binaryFile.getPath())).willReturn(binaryFile);
        ExportRequest exportRequest = new ExportRequest();
        exportRequest.setExportWithLogBookLFC(true);
        exportRequest.setDslRequest(queryUnit);
        given(handlerIO.getJsonFromWorkspace("export_query.json")).willReturn(JsonHandler.toJsonNode(exportRequest));

        // When
        ItemStatus itemStatus = createManifest.execute(WorkerParametersFactory.newWorkerParameters(), handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);

        Assert.assertThat(StringUtils.countMatches(Files.readAllLines(Paths.get(manifestFile.getPath()),
            Charset.defaultCharset()).get(0), "<DataObjectGroup id=\"aebaaaaaaefjz7wkabvpoalnfgzdwfyaaaaq\">"),
            equalTo(1));

    }

    @Test
    @RunWithCustomExecutor
    public void should_create_report() throws Exception {
        // Given
        HandlerIO handlerIO = mock(HandlerIO.class);
        MetaDataClient metaDataClient = mock(MetaDataClient.class);
        given(metaDataClientFactory.getClient()).willReturn(metaDataClient);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        AccessContractModel accessContractModel = new AccessContractModel();
        accessContractModel.setEveryDataObjectVersion(true);
        accessContractModel.setEveryOriginatingAgency(true);

        VitamThreadUtils.getVitamSession().setContract(accessContractModel);

        JsonNode queryUnit =
            JsonHandler.getFromInputStream(getClass().getResourceAsStream("/CreateManifest/query.json"));

        JsonNode queryUnitWithTree = JsonHandler
            .getFromInputStream(getClass().getResourceAsStream("/CreateManifest/queryWithTreeProjection.json"));

        JsonNode queryObjectGroup =
            JsonHandler.getFromInputStream(getClass().getResourceAsStream("/CreateManifest/queryObjectGroup.json"));

        given(handlerIO.getJsonFromWorkspace("query.json")).willReturn(queryUnit);

        given(metaDataClient.selectUnits(queryUnit.deepCopy())).willReturn(
            JsonHandler.getFromInputStream(getClass().getResourceAsStream("/CreateManifest/resultMetadata.json")));

        given(metaDataClient.selectUnits(queryUnitWithTree)).willReturn(
            JsonHandler.getFromInputStream(getClass().getResourceAsStream("/CreateManifest/resultMetadataTree.json")));

        given(metaDataClient.selectObjectGroups(queryObjectGroup)).willReturn(
            JsonHandler.getFromInputStream(getClass().getResourceAsStream("/CreateManifest/resultObjectGroup.json")));

        File manifestFile = tempFolder.newFile();
        given(handlerIO.getOutput(MANIFEST_XML_RANK))
            .willReturn(new ProcessingUri(UriPrefix.WORKSPACE, manifestFile.getPath()));
        given(handlerIO.getNewLocalFile(manifestFile.getPath())).willReturn(manifestFile);

        File reportFile = tempFolder.newFile();
        given(handlerIO.getOutput(REPORT)).willReturn(new ProcessingUri(UriPrefix.WORKSPACE, reportFile.getPath()));
        given(handlerIO.getNewLocalFile(reportFile.getPath())).willReturn(reportFile);

        File guidToPathFile = tempFolder.newFile();
        given(handlerIO.getOutput(GUID_TO_INFO_RANK))
            .willReturn(new ProcessingUri(UriPrefix.WORKSPACE, guidToPathFile.getPath()));
        given(handlerIO.getNewLocalFile(guidToPathFile.getPath())).willReturn(guidToPathFile);

        File binaryFile = tempFolder.newFile();
        given(handlerIO.getOutput(BINARIES_RANK))
            .willReturn(new ProcessingUri(UriPrefix.WORKSPACE, binaryFile.getPath()));
        given(handlerIO.getNewLocalFile(binaryFile.getPath())).willReturn(binaryFile);

        ExportRequest exportRequest = new ExportRequest();
        exportRequest.setExportWithLogBookLFC(true);
        exportRequest.setDslRequest(queryUnit);
        exportRequest.setExportType(ArchiveTransfer);
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
        given(handlerIO.getJsonFromWorkspace(DIP_REQUEST_FILE_NAME)).willReturn(JsonHandler.toJsonNode(exportRequest));

        given(backupService.backup(any(), any(), anyString())).willReturn(null);

        WorkerParameters wp = WorkerParametersFactory.newWorkerParameters();

        // When
        ItemStatus itemStatus = createManifest.execute(wp, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.OK);
        List<String> lines = Files.lines(reportFile.toPath())
            .map(String::trim)
            .collect(Collectors.toList());
        assertThat(lines.size()).isEqualTo(6); // 4 units + 2 report header
        assertEquals("{\"id\":\"aeaqaaaaaadf6mc4aathcak7tmtgdnaaaaba\",\"status\":\"OK\"}", lines.get(2));
        assertEquals("{\"id\":\"aeaqaaaaaadf6mc4aathcak7tmtgdmyaaaba\",\"status\":\"OK\"}", lines.get(3));
        assertEquals("{\"id\":\"aeaqaaaaaadf6mc4aathcak7tmtgdayaaaca\",\"status\":\"OK\"}", lines.get(4));
        assertEquals("{\"id\":\"aeaqaaaaaadf6mc4aathcak7tmtgdniaaaba\",\"status\":\"OK\"}", lines.get(5));
    }

    @Test
    @RunWithCustomExecutor
    public void should_transfer_with_warning() throws Exception {
        // Given
        HandlerIO handlerIO = mock(HandlerIO.class);
        MetaDataClient metaDataClient = mock(MetaDataClient.class);
        given(metaDataClientFactory.getClient()).willReturn(metaDataClient);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        AccessContractModel accessContractModel = new AccessContractModel();
        accessContractModel.setEveryDataObjectVersion(true);
        accessContractModel.setEveryOriginatingAgency(true);

        VitamThreadUtils.getVitamSession().setContract(accessContractModel);

        JsonNode queryUnit =
            JsonHandler.getFromInputStream(getClass().getResourceAsStream("/CreateManifest/query.json"));

        JsonNode queryUnitWithTree = JsonHandler
            .getFromInputStream(getClass().getResourceAsStream("/CreateManifest/queryWithTreeProjection.json"));

        JsonNode queryObjectGroup =
            JsonHandler.getFromInputStream(getClass().getResourceAsStream("/CreateManifest/queryObjectGroup.json"));

        given(handlerIO.getJsonFromWorkspace("query.json")).willReturn(queryUnit);

        given(metaDataClient.selectUnits(queryUnit.deepCopy())).willReturn(
            JsonHandler
                .getFromInputStream(getClass().getResourceAsStream("/CreateManifest/resultMetadataWithTransfer.json")));

        given(metaDataClient.selectUnits(queryUnitWithTree)).willReturn(
            JsonHandler.getFromInputStream(getClass().getResourceAsStream("/CreateManifest/resultMetadataTree.json")));

        given(metaDataClient.selectObjectGroups(queryObjectGroup)).willReturn(
            JsonHandler.getFromInputStream(getClass().getResourceAsStream("/CreateManifest/resultObjectGroup.json")));

        File manifestFile = tempFolder.newFile();
        given(handlerIO.getOutput(MANIFEST_XML_RANK))
            .willReturn(new ProcessingUri(UriPrefix.WORKSPACE, manifestFile.getPath()));
        given(handlerIO.getNewLocalFile(manifestFile.getPath())).willReturn(manifestFile);

        File reportFile = tempFolder.newFile();
        given(handlerIO.getOutput(REPORT)).willReturn(new ProcessingUri(UriPrefix.WORKSPACE, reportFile.getPath()));
        given(handlerIO.getNewLocalFile(reportFile.getPath())).willReturn(reportFile);

        File guidToPathFile = tempFolder.newFile();
        given(handlerIO.getOutput(GUID_TO_INFO_RANK))
            .willReturn(new ProcessingUri(UriPrefix.WORKSPACE, guidToPathFile.getPath()));
        given(handlerIO.getNewLocalFile(guidToPathFile.getPath())).willReturn(guidToPathFile);

        File binaryFile = tempFolder.newFile();
        given(handlerIO.getOutput(BINARIES_RANK))
            .willReturn(new ProcessingUri(UriPrefix.WORKSPACE, binaryFile.getPath()));
        given(handlerIO.getNewLocalFile(binaryFile.getPath())).willReturn(binaryFile);

        ExportRequest exportRequest = new ExportRequest();
        exportRequest.setExportWithLogBookLFC(true);
        exportRequest.setDslRequest(queryUnit);
        exportRequest.setExportType(ArchiveTransfer);
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
        given(handlerIO.getJsonFromWorkspace(DIP_REQUEST_FILE_NAME)).willReturn(JsonHandler.toJsonNode(exportRequest));

        given(backupService.backup(any(), any(), anyString())).willReturn(null);

        WorkerParameters wp = WorkerParametersFactory.newWorkerParameters();

        // When
        ItemStatus itemStatus = createManifest.execute(wp, handlerIO);

        // Then
        assertThat(itemStatus.getGlobalStatus()).isEqualTo(StatusCode.WARNING);
    }
}
