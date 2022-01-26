/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.collect.internal.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import fr.gouv.vitam.collect.internal.exception.CollectException;
import fr.gouv.vitam.collect.internal.model.CollectModel;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.InQuery;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.parser.query.ParserTokens;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.utils.ScrollSpliterator;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.manifest.ExportException;
import fr.gouv.vitam.common.manifest.ManifestBuilder;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.export.ExportRequest;
import fr.gouv.vitam.common.model.export.ExportRequestParameters;
import fr.gouv.vitam.common.model.export.ExportType;
import fr.gouv.vitam.common.model.unit.ArchiveUnitModel;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.AccessContract;
import fr.gouv.vitam.functional.administration.common.exception.AdminManagementClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.client.MetadataType;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.client.WorkspaceType;
import fr.gouv.vitam.workspace.common.CompressInformation;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.google.common.collect.Iterables.partition;
import static fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper.id;
import static fr.gouv.vitam.common.mapping.dip.UnitMapper.buildObjectMapper;
import static fr.gouv.vitam.common.model.IngestWorkflowConstants.SEDA_FILE;
import static fr.gouv.vitam.common.thread.VitamThreadUtils.getVitamSession;

public class GenerateSipService {

    private static final int MAX_ELEMENT_IN_QUERY = 1000;
    static final String CONTENT = "Content";
    private final MetaDataClientFactory metaDataClientFactory = MetaDataClientFactory.getInstance(MetadataType.COLLECT);
    private final WorkspaceClientFactory workspaceClientFactory = WorkspaceClientFactory.getInstance(WorkspaceType.COLLECT);
    private final AdminManagementClientFactory adminManagementClientFactory = AdminManagementClientFactory.getInstance();

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(GenerateSipService.class);
    private static final String MANIFEST_FILE_NAME = "manifest.xml";
    ObjectMapper objectMapper;

    public GenerateSipService() {
        objectMapper = buildObjectMapper();
    }

    public String generateSip(CollectModel collectModel)
        throws InvalidParseOperationException, JAXBException, XMLStreamException, CollectException {

        File localDirectory = PropertiesUtils.fileFromTmpFolder(collectModel.getId());

        File manifestFile = new File(localDirectory.getAbsolutePath().concat("/").concat(MANIFEST_FILE_NAME));
        manifestFile.getParentFile().mkdirs();

        try (MetaDataClient client = metaDataClientFactory.getClient();
             OutputStream outputStream = new FileOutputStream(manifestFile);
             ManifestBuilder manifestBuilder = new ManifestBuilder(outputStream)) {
            // got
            ExportRequest exportRequest = new ExportRequest();
            exportRequest.setDslRequest(JsonHandler.getFromString("{  \"$roots\": [],  \"$query\": [ { \"$eq\": { \"#opi\": \"" + collectModel.getId() + "\" } } ],\"filter\": {\"$scrollId\":\"START\",\"$scrollTimeout\":300000,\"$limit\":10000} , \"$projection\": {}}"));
            exportRequest.setExportWithLogBookLFC(false);
            exportRequest.setExportType(ExportType.MinimalArchiveDeliveryRequestReply);
            ExportRequestParameters exportRequestParameters = new ExportRequestParameters();
            exportRequestParameters.setMessageRequestIdentifier(GUIDFactory.newGUID().getId());
            exportRequestParameters.setArchivalAgencyIdentifier("Identifier4");
            exportRequestParameters.setRequesterIdentifier("Vitam-Bis");
            exportRequestParameters.setComment("Comments");
            exportRequestParameters.setArchivalAgreement("ArchivalAgreement0");
            exportRequestParameters.setRelatedTransferReference(List.of("RelatedTransferReference"));
            exportRequestParameters.setTransferRequestReplyIdentifier("TransferRequestReplyIdentifier");
            exportRequestParameters.setTransferringAgency("Identifier5");


            exportRequest.setExportRequestParameters(exportRequestParameters);
            manifestBuilder.startDocument("containerName", ExportType.ArchiveTransfer, exportRequestParameters);

            ListMultimap<String, String> multimap = ArrayListMultimap.create();
            Set<String> originatingAgencies = new HashSet<>();
            Map<String, String> ogs = new HashMap<>();

            SelectParserMultiple parser = new SelectParserMultiple();

            parser.parse(exportRequest.getDslRequest());
            SelectMultiQuery request = parser.getRequest();

            ScrollSpliterator<JsonNode> scrollRequest = ScrollSpliteratorHelper
                    .createUnitScrollSplitIterator(client, request);

            StreamSupport.stream(scrollRequest, false)
                    .forEach(item -> createGraph(multimap, originatingAgencies, ogs, item));

            manifestBuilder.startDataObjectPackage();
            Select select = new Select();
            Map<String, JsonNode> idBinaryWithFileName = new HashMap<>();
            Iterable<List<Map.Entry<String, String>>> partitions = partition(ogs.entrySet(), MAX_ELEMENT_IN_QUERY);
            for (List<Map.Entry<String, String>> partition : partitions) {

                ListMultimap<String, String> unitsForObjectGroupId = partition.stream()
                        .collect(
                                ArrayListMultimap::create,
                                (map, entry) -> map.put(entry.getValue(), entry.getKey()),
                                (list1, list2) -> list1.putAll(list2)
                        );
                InQuery in = QueryHelper.in(id(), partition.stream().map(Map.Entry::getValue).toArray(String[]::new));

                select.setQuery(in);

                AccessContractModel accessContractModel = getAccessContractModel();

                JsonNode response = client.selectObjectGroups(select.getFinalSelect());
                ArrayNode objects = (ArrayNode) response.get("$results");
                for (JsonNode object : objects) {
                    List<String> linkedUnits = unitsForObjectGroupId.get(
                            object.get(ParserTokens.PROJECTIONARGS.ID.exactToken()).textValue());
                    idBinaryWithFileName.putAll(manifestBuilder.writeGOT(
                        object, linkedUnits.get(linkedUnits.size() - 1), Collections.emptySet(),
                        Stream.empty(), accessContractModel
                    ));
                }
            }
            manifestBuilder.startDescriptiveMetadata();
            SelectParserMultiple initialQueryParser = new SelectParserMultiple();
            initialQueryParser.parse(exportRequest.getDslRequest());

            scrollRequest = new ScrollSpliterator<>(initialQueryParser.getRequest(),
                    query -> {
                        try {
                            JsonNode node = client.selectUnits(query.getFinalSelect());
                            return RequestResponseOK.getFromJsonNode(node);
                        } catch (MetaDataExecutionException | MetaDataDocumentSizeException | MetaDataClientServerException | InvalidParseOperationException e) {
                            throw new IllegalStateException(e);
                        }
                    }, VitamConfiguration.getElasticSearchScrollTimeoutInMilliseconds(), VitamConfiguration.getElasticSearchScrollLimit());


            StreamSupport.stream(scrollRequest, false)
                    .forEach(result -> {
                        try {
                            ArchiveUnitModel archiveUnitModel = objectMapper.treeToValue(result, ArchiveUnitModel.class);
                            manifestBuilder.writeArchiveUnit(archiveUnitModel, multimap, ogs);
                        } catch (JsonProcessingException | JAXBException | DatatypeConfigurationException e) {
                            e.printStackTrace();
                        }
                    });
            manifestBuilder.endDescriptiveMetadata();

            manifestBuilder
                    .writeManagementMetadata("FRAN_NP_009913", "FRAN_NP_009913");
            manifestBuilder.endDataObjectPackage();

            manifestBuilder
                    .writeFooter(ExportType.ArchiveTransfer, exportRequest.getExportRequestParameters());
            manifestBuilder.closeManifest();
            return saveManifestInWorkspace(collectModel, new FileInputStream(manifestFile));
        } catch (IOException | MetaDataExecutionException | MetaDataDocumentSizeException |
            MetaDataClientServerException | InvalidCreateOperationException
            | ContentAddressableStorageServerException | ExportException | InternalServerException e) {
            LOGGER.error(e.getMessage());
            throw new CollectException(e.getMessage());
        }
    }

    private AccessContractModel getAccessContractModel() throws CollectException {
        String identifier = "ContratTNR";
        try (final AdminManagementClient client = adminManagementClientFactory.getClient()) {
            Select select = new Select();
            Query query = QueryHelper.eq(AccessContract.IDENTIFIER, identifier);
            select.setQuery(query);
            return ((RequestResponseOK<AccessContractModel>) client.findAccessContracts(
                select.getFinalSelect())
            ).getResults().get(0);
        } catch (InvalidCreateOperationException | AdminManagementClientServerException | InvalidParseOperationException e) {
            throw new CollectException(e.getMessage(), e.getCause());
        }
    }


    private String saveManifestInWorkspace(CollectModel collectModel, InputStream inputStream) throws ContentAddressableStorageServerException {
        LOGGER.debug("Try to push manifest to workspace...");
        String digest = null;
        try (WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {
            if (workspaceClient.isExistingContainer(collectModel.getId())) {
                MessageDigest messageDigest = MessageDigest.getInstance("SHA-512");
                inputStream = new DigestInputStream(inputStream, messageDigest);
                workspaceClient.putObject(collectModel.getId(), MANIFEST_FILE_NAME, inputStream);
                digest = readMessageDigestReturn(messageDigest.digest());
                // compress
                CompressInformation compressInformation = new CompressInformation();
                compressInformation.getFiles().add(SEDA_FILE);
                compressInformation.getFiles().add(CONTENT);
                compressInformation.setOutputFile(collectModel.getId() + ".zip");
                compressInformation.setOutputContainer(collectModel.getId());
                workspaceClient.compress(collectModel.getId(), compressInformation);
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        LOGGER.debug(" -> push manifest to workspace finished");
        return digest;
    }

    private void createGraph(ListMultimap<String, String> multimap, Set<String> originatingAgencies,
                             Map<String, String> ogs, JsonNode result) {
        String archiveUnitId = result.get(id()).asText();
        ArrayNode nodes = (ArrayNode) result.get(VitamFieldsHelper.unitups());
        for (JsonNode node : nodes) {
            multimap.put(node.asText(), archiveUnitId);
        }
        Optional<JsonNode> originatingAgency = Optional.ofNullable(result.get(VitamFieldsHelper.originatingAgency()));
        originatingAgency.ifPresent(jsonNode -> originatingAgencies.add(jsonNode.asText()));
        JsonNode objectIdNode = result.get(VitamFieldsHelper.object());
        if (objectIdNode != null) {
            ogs.put(archiveUnitId, objectIdNode.asText());
        }
    }


    public String readMessageDigestReturn(byte[] theDigestResult) {
        StringBuilder sb = new StringBuilder();
        for (byte b : theDigestResult) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString().toLowerCase();
    }
}
