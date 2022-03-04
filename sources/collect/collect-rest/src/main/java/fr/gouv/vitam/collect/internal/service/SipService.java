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
import fr.gouv.vitam.collect.internal.helpers.CollectHelper;
import fr.gouv.vitam.collect.internal.helpers.SipHelper;
import fr.gouv.vitam.collect.internal.model.CollectModel;
import fr.gouv.vitam.collect.internal.server.CollectConfiguration;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.database.builder.query.InQuery;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.parser.query.ParserTokens;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.utils.ScrollSpliterator;
import fr.gouv.vitam.common.exception.InternalServerException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.manifest.ExportException;
import fr.gouv.vitam.common.manifest.ManifestBuilder;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.export.ExportRequest;
import fr.gouv.vitam.common.model.export.ExportRequestParameters;
import fr.gouv.vitam.common.model.export.ExportType;
import fr.gouv.vitam.common.model.unit.ArchiveUnitModel;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.ingest.external.api.exception.IngestExternalException;
import fr.gouv.vitam.ingest.external.client.IngestExternalClient;
import fr.gouv.vitam.ingest.external.client.IngestExternalClientFactory;
import fr.gouv.vitam.ingest.external.client.IngestRequestParameters;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.client.MetadataType;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import fr.gouv.vitam.workspace.client.WorkspaceType;
import fr.gouv.vitam.workspace.common.CompressInformation;
import org.apache.commons.io.FileUtils;

import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.nio.file.Files;
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
import static fr.gouv.vitam.logbook.common.parameters.Contexts.DEFAULT_WORKFLOW;

public class SipService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(SipService.class);
    public static final String RESULTS = "$results";
    private static final String SHA_512 = "SHA-512";
    private static final String SIP_EXTENSION = ".zip";
    private static final String MANIFEST_FILE_NAME = "manifest.xml";
    private static final String CONTENT = "Content";
    private static final int MAX_ELEMENT_IN_QUERY = 1000;

    private final IngestExternalClientFactory ingestExternalClientFactory;
    private final WorkspaceClientFactory workspaceClientFactory;
    private final MetaDataClientFactory metaDataClientFactory;
    private final ObjectMapper objectMapper;

    public SipService(CollectConfiguration collectConfiguration) {
        WorkspaceClientFactory.changeMode(collectConfiguration.getWorkspaceUrl(), WorkspaceType.COLLECT);
        this.workspaceClientFactory = WorkspaceClientFactory.getInstance(WorkspaceType.COLLECT);
        this.metaDataClientFactory = MetaDataClientFactory.getInstance(MetadataType.COLLECT);
        this.ingestExternalClientFactory = IngestExternalClientFactory.getInstance();
        this.objectMapper = buildObjectMapper();
    }

    public String generateSip(CollectModel collectModel) throws CollectException {

        File localDirectory = PropertiesUtils.fileFromTmpFolder(collectModel.getId());

        File manifestFile = new File(localDirectory.getAbsolutePath().concat("/").concat(MANIFEST_FILE_NAME));

        boolean isCreated = manifestFile.getParentFile().mkdir();
        if (!isCreated) {
            LOGGER.debug("An error occurs when trying to create manifest parent directory");
            throw new CollectException("An error occurs when trying to create manifest parent directory");
        }

        try (MetaDataClient client = metaDataClientFactory.getClient();
            OutputStream outputStream = new FileOutputStream(manifestFile);
            ManifestBuilder manifestBuilder = new ManifestBuilder(outputStream)) {

            ExportRequestParameters exportRequestParameters = SipHelper.buildExportRequestParameters(collectModel);
            ExportRequest exportRequest = SipHelper.buildExportRequest(collectModel, exportRequestParameters);

            manifestBuilder.startDocument(collectModel.getMessageIdentifier(), ExportType.ArchiveTransfer,
                exportRequestParameters);

            ListMultimap<String, String> multimap = ArrayListMultimap.create();
            Set<String> originatingAgencies = new HashSet<>();
            Map<String, String> ogs = new HashMap<>();

            SelectParserMultiple parser = new SelectParserMultiple();

            parser.parse(exportRequest.getDslRequest());
            SelectMultiQuery request = parser.getRequest();

            ScrollSpliterator<JsonNode> scrollRequest =
                ScrollSpliteratorHelper.createUnitScrollSplitIterator(client, request);

            StreamSupport.stream(scrollRequest, false)
                .forEach(item -> CollectHelper.createGraph(multimap, originatingAgencies, ogs, item));

            manifestBuilder.startDataObjectPackage();
            Select select = new Select();
            Iterable<List<Map.Entry<String, String>>> partitions = partition(ogs.entrySet(), MAX_ELEMENT_IN_QUERY);
            for (List<Map.Entry<String, String>> partition : partitions) {

                ListMultimap<String, String> unitsForObjectGroupId = partition.stream()
                    .collect(ArrayListMultimap::create, (map, entry) -> map.put(entry.getValue(), entry.getKey()),
                        (list1, list2) -> list1.putAll(list2)
                    );
                InQuery in = QueryHelper.in(id(), partition.stream().map(Map.Entry::getValue).toArray(String[]::new));

                select.setQuery(in);

                JsonNode response = client.selectObjectGroups(select.getFinalSelect());
                ArrayNode objects = (ArrayNode) response.get(RESULTS);
                for (JsonNode object : objects) {
                    List<String> linkedUnits =
                        unitsForObjectGroupId.get(object.get(ParserTokens.PROJECTIONARGS.ID.exactToken()).textValue());
                    manifestBuilder.writeGOT(object, linkedUnits.get(linkedUnits.size() - 1), Stream.empty());
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
                    } catch (MetaDataExecutionException | MetaDataDocumentSizeException | MetaDataClientServerException
                        | InvalidParseOperationException e) {
                        throw new IllegalStateException(e);
                    }
                }, VitamConfiguration.getElasticSearchScrollTimeoutInMilliseconds(),
                VitamConfiguration.getElasticSearchScrollLimit());


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

            String submissionAgencyIdentifier = collectModel.getSubmissionAgencyIdentifier();
            if (submissionAgencyIdentifier == null) {
                submissionAgencyIdentifier = collectModel.getOriginatingAgencyIdentifier();
            }

            manifestBuilder.writeManagementMetadata(collectModel.getOriginatingAgencyIdentifier(),
                submissionAgencyIdentifier);
            manifestBuilder.endDataObjectPackage();

            manifestBuilder.writeFooter(ExportType.ArchiveTransfer, exportRequest.getExportRequestParameters());
            manifestBuilder.closeManifest();

        } catch (IOException | InvalidCreateOperationException | ExportException | MetaDataExecutionException
            | InternalServerException | MetaDataClientServerException | MetaDataDocumentSizeException
            | InvalidParseOperationException | JAXBException | XMLStreamException e) {
            LOGGER.error(e.getLocalizedMessage());
            throw new CollectException(e);
        }

        try (InputStream inputStream = Files.newInputStream(manifestFile.toPath())) {
            FileUtils.deleteDirectory(manifestFile.getParentFile());
            return saveManifestInWorkspace(collectModel, inputStream);
        } catch (IOException e) {
            LOGGER.error(e.getLocalizedMessage());
            throw new CollectException(e);
        }

    }

    private String saveManifestInWorkspace(CollectModel collectModel, InputStream inputStream) throws CollectException {
        LOGGER.debug("Try to push manifest to workspace...");
        String digest = null;
        try (WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {
            if (workspaceClient.isExistingContainer(collectModel.getId())) {
                MessageDigest messageDigest = MessageDigest.getInstance(SHA_512);
                inputStream = new DigestInputStream(inputStream, messageDigest);
                workspaceClient.putObject(collectModel.getId(), MANIFEST_FILE_NAME, inputStream);
                digest = CollectHelper.readMessageDigestReturn(messageDigest.digest());
                // compress
                CompressInformation compressInformation = new CompressInformation();
                compressInformation.getFiles().add(SEDA_FILE);
                compressInformation.getFiles().add(CONTENT);
                compressInformation.setOutputFile(collectModel.getId() + SIP_EXTENSION);
                compressInformation.setOutputContainer(collectModel.getId());
                workspaceClient.compress(collectModel.getId(), compressInformation);
            }
        } catch (NoSuchAlgorithmException | ContentAddressableStorageServerException e) {
            LOGGER.error(e.getLocalizedMessage());
            throw new CollectException(e);
        }
        LOGGER.debug(" -> push manifest to workspace finished");
        return digest;
    }

    public String ingest(CollectModel collectModel, String digest) throws CollectException {

        try (IngestExternalClient client = ingestExternalClientFactory.getClient()) {
            Integer tenantId = ParameterHelper.getTenantParameter();
            InputStream sipInputStream = getFileFromWorkspace(collectModel);
            if (sipInputStream == null) {
                throw new CollectException("Can't fetch SIP file from Collect workspace!");
            }
            IngestRequestParameters ingestRequestParameters =
                new IngestRequestParameters(DEFAULT_WORKFLOW.name(), ProcessAction.RESUME.name())
                    .setManifestDigestAlgo(SHA_512)
                    .setManifestDigestValue(digest);

            RequestResponse<Void> response =
                client.ingest(new VitamContext(tenantId), sipInputStream, ingestRequestParameters);
            if (!response.isOk()) {
                LOGGER.error("Error from ingest client: {}", response.toString());
                throw new CollectException("Error from ingest client: " + response);
            }
            return response.getHeaderString(GlobalDataRest.X_REQUEST_ID);
        } catch (IngestExternalException e) {
            LOGGER.error("Error when processing ingest: {}", e);
            throw new CollectException(e);
        }
    }

    private InputStream getFileFromWorkspace(CollectModel collectModel) throws CollectException {
        LOGGER.debug("Try to get Zip from workspace...");
        InputStream sipInputStream = null;
        try (WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {
            if (!workspaceClient.isExistingContainer(collectModel.getId())) {
                return null;
            }
            Response response = workspaceClient.getObject(collectModel.getId(), collectModel.getId() + SIP_EXTENSION);
            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                sipInputStream = (InputStream) response.getEntity();
            }
        } catch (ContentAddressableStorageServerException | ContentAddressableStorageNotFoundException e) {
            LOGGER.error("Error when processing ingest: {}", e);
            throw new CollectException(e);
        }

        LOGGER.debug(" zip from workspace finished");
        return sipInputStream;
    }

}
