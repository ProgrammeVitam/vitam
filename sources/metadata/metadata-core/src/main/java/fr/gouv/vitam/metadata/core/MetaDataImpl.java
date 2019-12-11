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
package fr.gouv.vitam.metadata.core;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import com.mongodb.client.MongoCollection;
import fr.gouv.vitam.common.client.OntologyLoader;
import fr.gouv.vitam.common.database.builder.facet.Facet;
import fr.gouv.vitam.common.database.builder.facet.FacetHelper;
import fr.gouv.vitam.common.database.builder.query.BooleanQuery;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.GLOBAL;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTION;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.PROJECTIONARGS;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.RequestMultiple;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.collections.CachedOntologyLoader;
import fr.gouv.vitam.common.database.facet.model.FacetOrder;
import fr.gouv.vitam.common.database.index.model.IndexationResult;
import fr.gouv.vitam.common.database.parameter.IndexParameters;
import fr.gouv.vitam.common.database.parser.request.multiple.InsertParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.RequestParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.UpdateParserMultiple;
import fr.gouv.vitam.common.database.server.elasticsearch.IndexationHelper;
import fr.gouv.vitam.common.database.server.elasticsearch.model.ElasticsearchCollections;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.BadRequestException;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamDBException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.exception.VitamThreadAccessException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.DatabaseCursor;
import fr.gouv.vitam.common.model.DurationData;
import fr.gouv.vitam.common.model.FacetBucket;
import fr.gouv.vitam.common.model.FacetResult;
import fr.gouv.vitam.common.model.MetadataType;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.UnitType;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.common.model.massupdate.RuleActions;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.client.AdminManagementOntologyLoader;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterDetail;
import fr.gouv.vitam.functional.administration.common.server.AccessionRegisterSymbolic;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import fr.gouv.vitam.metadata.api.model.BulkUnitInsertRequest;
import fr.gouv.vitam.metadata.api.model.ObjectGroupPerOriginatingAgency;
import fr.gouv.vitam.metadata.core.database.collections.DbRequest;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.MongoDbAccessMetadataImpl;
import fr.gouv.vitam.metadata.core.database.collections.MongoDbVarNameAdapter;
import fr.gouv.vitam.metadata.core.database.collections.Result;
import fr.gouv.vitam.metadata.core.model.UpdateUnit;
import fr.gouv.vitam.metadata.core.model.UpdateUnitKey;
import fr.gouv.vitam.metadata.core.model.UpdatedDocument;
import fr.gouv.vitam.metadata.core.utils.MetadataJsonResponseUtils;
import fr.gouv.vitam.metadata.core.utils.OriginatingAgencyBucketResult;
import fr.gouv.vitam.metadata.core.validation.CachedArchiveUnitProfileLoader;
import fr.gouv.vitam.metadata.core.validation.CachedSchemaValidatorLoader;
import fr.gouv.vitam.metadata.core.validation.MetadataValidationException;
import fr.gouv.vitam.metadata.core.validation.OntologyValidator;
import fr.gouv.vitam.metadata.core.validation.UnitValidator;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.bson.Document;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.filter.Filter;
import org.elasticsearch.search.aggregations.bucket.nested.Nested;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.cardinality.Cardinality;
import org.elasticsearch.search.aggregations.metrics.sum.Sum;
import org.elasticsearch.search.aggregations.metrics.valuecount.ValueCount;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.and;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.ne;
import static fr.gouv.vitam.common.json.JsonHandler.toArrayList;
import static fr.gouv.vitam.common.model.StatusCode.FATAL;
import static fr.gouv.vitam.common.model.StatusCode.KO;
import static fr.gouv.vitam.metadata.core.database.collections.MetadataCollections.OBJECTGROUP;
import static fr.gouv.vitam.metadata.core.model.UpdateUnitKey.CHECK_UNIT_SCHEMA;
import static fr.gouv.vitam.metadata.core.model.UpdateUnitKey.UNIT_METADATA_NO_CHANGES;
import static fr.gouv.vitam.metadata.core.model.UpdateUnitKey.UNIT_METADATA_UPDATE;
import static fr.gouv.vitam.metadata.core.model.UpdateUnitKey.UNIT_UNKNOWN_OR_FORBIDDEN;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static java.util.Collections.singletonList;

public class MetaDataImpl {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MetaDataImpl.class);

    private static final String REQUEST_IS_NULL = "Request select is null or is empty";
    private static final MongoDbVarNameAdapter DEFAULT_VARNAME_ADAPTER = new MongoDbVarNameAdapter();

    private final MongoDbAccessMetadataImpl mongoDbAccess;
    private final IndexationHelper indexationHelper;
    private final DbRequest dbRequest;
    private final UnitValidator unitValidator;
    private final OntologyValidator unitOntologyValidator;
    private final OntologyValidator objectGroupOntologyValidator;
    private final OntologyLoader unitOntologyLoader;
    private final OntologyLoader objectGroupOntologyLoader;

    public MetaDataImpl(MongoDbAccessMetadataImpl mongoDbAccess,
        int ontologyCacheMaxEntries, int ontologyCacheTimeoutInSeconds,
        int archiveUnitProfileCacheMaxEntries, int archiveUnitProfileCacheTimeoutInSeconds,
        int schemaValidatorCacheMaxEntries, int schemaValidatorCacheTimeoutInSeconds) {

        this(mongoDbAccess, AdminManagementClientFactory.getInstance(), IndexationHelper.getInstance(),
            new DbRequest(), ontologyCacheMaxEntries, ontologyCacheTimeoutInSeconds,
            archiveUnitProfileCacheMaxEntries, archiveUnitProfileCacheTimeoutInSeconds,
            schemaValidatorCacheMaxEntries, schemaValidatorCacheTimeoutInSeconds);
    }

    @VisibleForTesting
    public MetaDataImpl(MongoDbAccessMetadataImpl mongoDbAccess,
        AdminManagementClientFactory adminManagementClientFactory,
        IndexationHelper indexationHelper,
        DbRequest dbRequest, int ontologyCacheMaxEntries, int ontologyCacheTimeoutInSeconds,
        int archiveUnitProfileCacheMaxEntries, int archiveUnitProfileCacheTimeoutInSeconds,
        int schemaValidatorCacheMaxEntries, int schemaValidatorCacheTimeoutInSeconds) {
        this.mongoDbAccess = mongoDbAccess;
        this.indexationHelper = indexationHelper;
        this.dbRequest = dbRequest;

        this.unitOntologyLoader = new CachedOntologyLoader(
            ontologyCacheMaxEntries,
            ontologyCacheTimeoutInSeconds,
            new AdminManagementOntologyLoader(adminManagementClientFactory, Optional.of(MetadataType.UNIT.getName()))
        );
        this.objectGroupOntologyLoader = new CachedOntologyLoader(
            ontologyCacheMaxEntries,
            ontologyCacheTimeoutInSeconds,
            new AdminManagementOntologyLoader(adminManagementClientFactory, Optional.of(MetadataType.OBJECTGROUP.getName()))
        );

        this.unitOntologyValidator = new OntologyValidator(this.unitOntologyLoader);
        this.objectGroupOntologyValidator = new OntologyValidator(this.objectGroupOntologyLoader);

        CachedArchiveUnitProfileLoader archiveUnitProfileLoader = new CachedArchiveUnitProfileLoader(
            adminManagementClientFactory,
            archiveUnitProfileCacheMaxEntries,
            archiveUnitProfileCacheTimeoutInSeconds
        );

        CachedSchemaValidatorLoader schemaValidatorLoader = new CachedSchemaValidatorLoader(
            schemaValidatorCacheMaxEntries,
            schemaValidatorCacheTimeoutInSeconds
        );

        this.unitValidator = new UnitValidator(archiveUnitProfileLoader, schemaValidatorLoader);
    }

    /**
     * Get a new MetaDataImpl instance
     *
     * @param mongoDbAccessMetadata
     * @return a new instance of MetaDataImpl
     */
    public static MetaDataImpl newMetadata(MongoDbAccessMetadataImpl mongoDbAccessMetadata,
        int ontologyCacheMaxEntries, int ontologyCacheTimeoutInSeconds,
        int archiveUnitProfileCacheMaxEntries, int archiveUnitProfileCacheTimeoutInSeconds,
        int schemaValidatorCacheMaxEntries, int schemaValidatorCacheTimeoutInSeconds) {

        return new MetaDataImpl(mongoDbAccessMetadata, ontologyCacheMaxEntries, ontologyCacheTimeoutInSeconds,
            archiveUnitProfileCacheMaxEntries, archiveUnitProfileCacheTimeoutInSeconds,
            schemaValidatorCacheMaxEntries, schemaValidatorCacheTimeoutInSeconds);
    }

    /**
     * @return the MongoDbAccessMetadataImpl
     */
    public MongoDbAccessMetadataImpl getMongoDbAccess() {
        return mongoDbAccess;
    }

    public void insertUnits(BulkUnitInsertRequest request)
        throws InvalidParseOperationException, MetaDataExecutionException,
        MetaDataNotFoundException {
        try {

            dbRequest.execInsertUnitRequests(request);

        } catch (VitamRuntimeException e) {
            if (e.getCause() instanceof InvalidParseOperationException) {
                throw (InvalidParseOperationException) e.getCause();
            }
            throw e;
        }
    }

    public void deleteUnits(List<String> idList)
        throws IllegalArgumentException, MetaDataExecutionException {

        dbRequest.deleteUnits(idList);

    }

    public void deleteObjectGroups(List<String> idList)
        throws IllegalArgumentException, MetaDataExecutionException {

        dbRequest.deleteObjectGroups(idList);

    }

    public void insertObjectGroup(JsonNode objectGroupRequest)
        throws InvalidParseOperationException, MetaDataExecutionException {
        final InsertParserMultiple insertParser = new InsertParserMultiple(DEFAULT_VARNAME_ADAPTER);
        insertParser.parse(objectGroupRequest);
        insertParser.getRequest().addHintFilter(BuilderToken.FILTERARGS.OBJECTGROUPS.exactToken());
        dbRequest.execInsertObjectGroupRequests(singletonList(insertParser));

    }

    public void insertObjectGroups(List<JsonNode> objectGroupRequest)
        throws InvalidParseOperationException, MetaDataExecutionException {

        try {
            List<InsertParserMultiple> collect = objectGroupRequest.stream().map(insertRequest -> {
                    InsertParserMultiple insertParser = new InsertParserMultiple(DEFAULT_VARNAME_ADAPTER);
                    try {
                        insertParser.parse(insertRequest);
                    } catch (InvalidParseOperationException e) {
                        throw new VitamRuntimeException(e);
                    }
                    return insertParser;
                }
            ).collect(Collectors.toList());

            dbRequest.execInsertObjectGroupRequests(collect);

        } catch (VitamRuntimeException e) {
            if (e.getCause() instanceof InvalidParseOperationException) {
                throw (InvalidParseOperationException) e.getCause();
            }
            throw e;
        }
    }

    /**
     * @param operationId operation id
     * @return List of FacetBucket
     */
    public List<FacetBucket> selectOwnAccessionRegisterOnUnitByOperationId(String operationId)
        throws MetaDataExecutionException {

        final SelectParserMultiple request = new SelectParserMultiple(DEFAULT_VARNAME_ADAPTER);
        final SelectMultiQuery select = new SelectMultiQuery();
        try {

            BooleanQuery query = and().add(
                eq(PROJECTIONARGS.INITIAL_OPERATION.exactToken(), operationId),
                ne(PROJECTIONARGS.UNITTYPE.exactToken(), UnitType.HOLDING_UNIT.name())
            );

            select.addQueries(query);

            Facet facet = FacetHelper
                .terms(AccessionRegisterDetail.class.getSimpleName(), PROJECTIONARGS.ORIGINATING_AGENCY.exactToken(),
                    Integer.MAX_VALUE,
                    FacetOrder.ASC);
            select.addFacets(facet);

            select.setLimitFilter(0, 1);

            request.parse(select.getFinalSelect());

        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            throw new MetaDataExecutionException(e);
        }

        try {
            List<OntologyModel> ontologies;
            if (request.model() == BuilderToken.FILTERARGS.UNITS) {
                ontologies = this.unitOntologyLoader.loadOntologies();
            } else {
                ontologies = this.objectGroupOntologyLoader.loadOntologies();
            }
            Result result = dbRequest.execRequest(request, ontologies);
            List<FacetResult> facetResults = (result != null) ? result.getFacet() : new ArrayList<>();

            if (!CollectionUtils.isEmpty(facetResults)) {
                FacetResult facetResult = facetResults.iterator().next();
                if (null != facetResult && !CollectionUtils.isEmpty(facetResult.getBuckets())) {
                    return facetResult.getBuckets();
                }
            }

        } catch (InvalidParseOperationException | BadRequestException | VitamDBException e) {
            throw new MetaDataExecutionException(e);
        }

        return new ArrayList<>();
    }

    public List<Document> createAccessionRegisterSymbolic(Integer tenant) {
        Aggregations aUAccessionRegisterInfo = selectArchiveUnitAccessionRegisterInformation(tenant);
        Aggregations oGAccessionRegisterInfo = selectObjectGroupAccessionRegisterInformation(tenant);

        String creationDate = ISO_LOCAL_DATE_TIME.format(LocalDateTime.now());

        return createWithInformations(aUAccessionRegisterInfo, oGAccessionRegisterInfo, creationDate, tenant);
    }

    private List<Document> createWithInformations(Aggregations archiveUnitAccessionRegisterInformation,
        Aggregations objectGroupAccessionRegisterInformation, String creationDate, Integer tenant) {
        Map<String, AccessionRegisterSymbolic> accessionRegisterSymbolicByOriginatingAgency =
            fillWithArchiveUnitInformation(archiveUnitAccessionRegisterInformation, creationDate, tenant);
        updateExistingAccessionRegisterWithObjectGroupInformation(objectGroupAccessionRegisterInformation, creationDate,
            tenant, accessionRegisterSymbolicByOriginatingAgency);

        return new ArrayList<>(accessionRegisterSymbolicByOriginatingAgency.values());
    }

    private void updateExistingAccessionRegisterWithObjectGroupInformation(
        Aggregations objectGroupAccessionRegisterInformation, String creationDate, Integer tenant,
        Map<String, AccessionRegisterSymbolic> accessionRegisterSymbolicByOriginatingAgency) {

        Terms objectGroupOriginatingAgencies = objectGroupAccessionRegisterInformation.get("originatingAgencies");
        Terms objectGroupOriginatingAgency = objectGroupAccessionRegisterInformation.get("originatingAgency");

        Map<String, OriginatingAgencyBucketResult> objectGroupByOriginatingAgency =
            objectGroupOriginatingAgency.getBuckets().stream()
                .map(bucket -> OriginatingAgencyBucketResult
                    .of(bucket.getKeyAsString(),
                        bucket.getDocCount(),
                        bucket.getAggregations().get("nestedVersions")
                    ))
                .collect(Collectors.toMap(e -> e.originatingAgency, e -> e));

        objectGroupOriginatingAgencies.getBuckets()
            .forEach(bucket ->
                updateAccessionsRegister(
                    creationDate,
                    tenant,
                    accessionRegisterSymbolicByOriginatingAgency,
                    objectGroupByOriginatingAgency,
                    OriginatingAgencyBucketResult
                        .of(bucket.getKeyAsString(),
                            bucket.getDocCount(),
                            bucket.getAggregations().get("nestedVersions")
                        )
                )
            );
    }

    private void updateAccessionsRegister(String creationDate, Integer tenant,
        Map<String, AccessionRegisterSymbolic> accessionRegisterSymbolicByOriginatingAgency,
        Map<String, OriginatingAgencyBucketResult> objectGroupByOriginatingAgency,
        OriginatingAgencyBucketResult objectGroup) {

        OriginatingAgencyBucketResult originatingAgencyBucketResult = objectGroupByOriginatingAgency
            .getOrDefault(objectGroup.originatingAgency, OriginatingAgencyBucketResult.empty());

        long groupObjectsCount = objectGroup.docCount - originatingAgencyBucketResult.docCount;
        long objectCount = objectGroup.objectCount - originatingAgencyBucketResult.objectCount;
        double binaryObjectSize = objectGroup.binaryObjectSize - originatingAgencyBucketResult.binaryObjectSize;
        AccessionRegisterSymbolic existingAccessionRegister =
            accessionRegisterSymbolicByOriginatingAgency.get(objectGroup.originatingAgency);

        if (groupObjectsCount > 0 && existingAccessionRegister != null) {
            existingAccessionRegister.setObjectGroup(groupObjectsCount)
                .setBinaryObject(objectCount)
                .setBinaryObjectSize(binaryObjectSize);
            return;
        }

        if (groupObjectsCount <= 0 && existingAccessionRegister != null) {
            existingAccessionRegister.setObjectGroup(0)
                .setBinaryObject(0L)
                .setBinaryObjectSize(0D);
            return;
        }

        if (groupObjectsCount > 0 && existingAccessionRegister == null) {
            accessionRegisterSymbolicByOriginatingAgency
                .put(objectGroup.originatingAgency, new AccessionRegisterSymbolic()
                    .setId(GUIDFactory.newAccessionRegisterSymbolicGUID(tenant).getId())
                    .setCreationDate(creationDate)
                    .setTenant(tenant)
                    .setOriginatingAgency(objectGroup.originatingAgency)
                    .setArchiveUnit(0L)
                    .setObjectGroup(groupObjectsCount)
                    .setBinaryObject(objectCount)
                    .setBinaryObjectSize(binaryObjectSize));
            return;
        }

        if (groupObjectsCount <= 0 && existingAccessionRegister == null) {
            return;
        }

        throw new IllegalStateException("Cannot go there.");
    }

    private Map<String, AccessionRegisterSymbolic> fillWithArchiveUnitInformation(
        Aggregations archiveUnitAccessionRegisterformation, String creationDate, Integer tenant) {
        Terms archiveUnitOriginatingAgencies = archiveUnitAccessionRegisterformation.get("originatingAgencies");
        Terms archiveUnitOriginatingAgency = archiveUnitAccessionRegisterformation.get("originatingAgency");

        Map<String, Long> archiveUnitByOriginatingAgency = archiveUnitOriginatingAgency.getBuckets().stream()
            .collect(Collectors
                .toMap(MultiBucketsAggregation.Bucket::getKeyAsString, MultiBucketsAggregation.Bucket::getDocCount));

        return archiveUnitOriginatingAgencies.getBuckets().stream()
            .map(e -> {
                long archiveUnitCount =
                    e.getDocCount() - archiveUnitByOriginatingAgency.getOrDefault(e.getKeyAsString(), 0L);
                if (archiveUnitCount <= 0) {
                    return null;
                }
                return new AccessionRegisterSymbolic()
                    .setId(GUIDFactory.newAccessionRegisterSymbolicGUID(tenant).getId())
                    .setCreationDate(creationDate)
                    .setTenant(tenant)
                    .setOriginatingAgency(e.getKeyAsString())
                    .setArchiveUnit(archiveUnitCount);
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(AccessionRegisterSymbolic::getOriginatingAgency, e -> e));
    }

    private Aggregations selectObjectGroupAccessionRegisterInformation(Integer tenant) {
        TermsAggregationBuilder ogs = AggregationBuilders.terms("originatingAgencies")
            .field("_sps")
            .subAggregation(AggregationBuilders.nested("nestedVersions", "_qualifiers.versions")
                .subAggregation(AggregationBuilders.sum("binaryObjectSize").field("_qualifiers.versions.Size"))
                .subAggregation(AggregationBuilders.count("binaryObjectCount").field("_qualifiers.versions._id")));

        TermsAggregationBuilder og = AggregationBuilders.terms("originatingAgency")
            .field("_sp")
            .subAggregation(AggregationBuilders.nested("nestedVersions", "_qualifiers.versions")
                .subAggregation(AggregationBuilders.sum("binaryObjectSize").field("_qualifiers.versions.Size"))
                .subAggregation(AggregationBuilders.count("binaryObjectCount").field("_qualifiers.versions._id")));

        return OBJECTGROUP.getEsClient()
            .basicSearch(OBJECTGROUP, tenant, Arrays.asList(og, ogs), QueryBuilders.termQuery("_tenant", tenant))
            .getAggregations();
    }

    private Aggregations selectArchiveUnitAccessionRegisterInformation(Integer tenant) {
        List<AggregationBuilder> aggregations = Arrays.asList(
            AggregationBuilders.terms("originatingAgency").field("_sp"),
            AggregationBuilders.terms("originatingAgencies").field("_sps")
        );
        return MetadataCollections.UNIT.getEsClient()
            .basicSearch(MetadataCollections.UNIT, tenant, aggregations, QueryBuilders.termQuery("_tenant", tenant))
            .getAggregations();
    }

    public List<ObjectGroupPerOriginatingAgency> selectOwnAccessionRegisterOnObjectGroupByOperationId(Integer tenant,
        String operationId) {
        AggregationBuilder originatingAgencyAgg = aggregationForObjectGroupAccessionRegisterByOperationId(
            operationId);

        QueryBuilder query = queryForObjectGroupAccessionRegisterByOperationId(tenant, operationId);

        Aggregations result = OBJECTGROUP.getEsClient()
            .basicSearch(OBJECTGROUP, tenant, Collections.singletonList(originatingAgencyAgg), query).getAggregations();

        List<ObjectGroupPerOriginatingAgency> listOgsPerSps = new ArrayList<>();
        Terms originatingAgencyResult = result.get("originatingAgency");
        for (Bucket originatingAgencyBucket : originatingAgencyResult.getBuckets()) {
            String sp = originatingAgencyBucket.getKeyAsString();
            ObjectGroupPerOriginatingAgency ogPerSp = new ObjectGroupPerOriginatingAgency(operationId, sp, 0l, 0l, 0l);
            Terms operationResult = originatingAgencyBucket.getAggregations().get("operation");
            for (Bucket operationBucket : operationResult.getBuckets()) {
                String opi = operationBucket.getKeyAsString();
                Nested versionResult = operationBucket.getAggregations().get("version");
                Filter versionOperationResult = versionResult.getAggregations().get("versionOperation");
                Cardinality gotCountResult = versionOperationResult.getAggregations().get("gotCount");
                Sum binaryObjectSizeResult = versionOperationResult.getAggregations().get("binaryObjectSize");
                ValueCount binaryObjectCountResult = versionOperationResult.getAggregations().get("binaryObjectCount");

                long gotCount = gotCountResult.getValue();
                long binaryObjectSize = (long) binaryObjectSizeResult.getValue();
                long binaryObjectCount = binaryObjectCountResult.getValue();
                if (opi.equals(operationId)) {
                    ogPerSp.setNumberOfGOT(ogPerSp.getNumberOfGOT() + gotCount);
                }
                ogPerSp.setSize(ogPerSp.getSize() + binaryObjectSize);
                ogPerSp.setNumberOfObject(ogPerSp.getNumberOfObject() + binaryObjectCount);
            }
            listOgsPerSps.add(ogPerSp);
        }

        return listOgsPerSps;
    }

    private QueryBuilder queryForObjectGroupAccessionRegisterByOperationId(Integer tenant, String operationId) {
        QueryBuilder tenantQuery = QueryBuilders.termQuery("_tenant", tenant);
        QueryBuilder operationQuery = QueryBuilders.matchQuery("_ops", operationId);
        QueryBuilder nestedOperationQuery = QueryBuilders.nestedQuery("_qualifiers.versions",
            QueryBuilders.matchQuery("_qualifiers.versions._opi", operationId), ScoreMode.Avg);
        return QueryBuilders.boolQuery().must(tenantQuery).must(operationQuery).must(nestedOperationQuery);
    }

    private AggregationBuilder aggregationForObjectGroupAccessionRegisterByOperationId(String operationId) {
        AggregationBuilder gotCountAgg = AggregationBuilders.cardinality("gotCount")
            .field("_qualifiers.versions.DataObjectGroupId");
        AggregationBuilder binaryObjectSizeAgg = AggregationBuilders.sum("binaryObjectSize")
            .field("_qualifiers.versions.Size");
        AggregationBuilder binaryObjectCountAgg = AggregationBuilders.count("binaryObjectCount")
            .field("_qualifiers.versions._id");
        AggregationBuilder versionOperationAgg = AggregationBuilders
            .filter("versionOperation", QueryBuilders.matchQuery("_qualifiers.versions._opi", operationId))
            .subAggregation(binaryObjectCountAgg).subAggregation(binaryObjectSizeAgg).subAggregation(gotCountAgg);
        AggregationBuilder versionAgg = AggregationBuilders.nested("version", "_qualifiers.versions")
            .subAggregation(versionOperationAgg);
        AggregationBuilder operationAgg = AggregationBuilders.terms("operation").field("_opi")
            .subAggregation(versionAgg);
        AggregationBuilder originatingAgencyAgg = AggregationBuilders.terms("originatingAgency").field("_sp")
            .subAggregation(operationAgg);
        return originatingAgencyAgg;
    }

    public RequestResponse<JsonNode> selectUnitsByQuery(JsonNode selectQuery)
        throws MetaDataExecutionException, InvalidParseOperationException,
        MetaDataDocumentSizeException, MetaDataNotFoundException, BadRequestException, VitamDBException {
        LOGGER.debug("SelectUnitsByQuery/ selectQuery: " + selectQuery);
        return selectMetadataObject(selectQuery, null, singletonList(BuilderToken.FILTERARGS.UNITS));

    }

    public RequestResponse<JsonNode> selectObjectGroupsByQuery(JsonNode selectQuery)
        throws MetaDataExecutionException, InvalidParseOperationException,
        MetaDataDocumentSizeException, MetaDataNotFoundException, BadRequestException, VitamDBException {
        LOGGER.debug("selectObjectGroupsByQuery/ selectQuery: " + selectQuery);
        return selectMetadataObject(selectQuery, null, singletonList(BuilderToken.FILTERARGS.OBJECTGROUPS));

    }

    public RequestResponse<JsonNode> selectUnitsById(JsonNode selectQuery, String unitId)
        throws InvalidParseOperationException, MetaDataExecutionException,
        MetaDataDocumentSizeException, MetaDataNotFoundException, BadRequestException, VitamDBException {
        LOGGER.debug("SelectUnitsById/ selectQuery: " + selectQuery);
        return selectMetadataObject(selectQuery, unitId, singletonList(BuilderToken.FILTERARGS.UNITS));
    }

    public RequestResponse<JsonNode> selectObjectGroupById(JsonNode selectQuery, String objectGroupId)
        throws InvalidParseOperationException, MetaDataDocumentSizeException, MetaDataExecutionException,
        MetaDataNotFoundException, BadRequestException, VitamDBException {
        LOGGER.debug("SelectObjectGroupById - objectGroupId : " + objectGroupId);
        LOGGER.debug("SelectObjectGroupById - selectQuery : " + selectQuery);
        return selectMetadataObject(selectQuery, objectGroupId,
            singletonList(BuilderToken.FILTERARGS.OBJECTGROUPS));
    }

    private RequestResponseOK<JsonNode> selectMetadataObject(JsonNode selectQuery, String unitOrObjectGroupId,
        List<BuilderToken.FILTERARGS> filters)
        throws MetaDataExecutionException, InvalidParseOperationException,
        MetaDataDocumentSizeException, MetaDataNotFoundException, BadRequestException, VitamDBException {

        Result result;
        ArrayNode arrayNodeResponse;
        if (selectQuery.isNull()) {
            throw new InvalidParseOperationException(REQUEST_IS_NULL);
        }

        final JsonNode queryCopy = selectQuery.deepCopy();
        long offset = 0;
        long limit = 0;
        if (selectQuery.get("$filter") != null) {
            if (selectQuery.get("$filter").get("$offset") != null) {
                offset = selectQuery.get("$filter").get("$offset").asLong();
            }
            if (selectQuery.get("$filter").get("$limit") != null) {
                limit = selectQuery.get("$filter").get("$limit").asLong();
            }
        }

        // parse Select request
        final RequestParserMultiple selectRequest = new SelectParserMultiple(DEFAULT_VARNAME_ADAPTER);
        selectRequest.parse(selectQuery);
        // Reset $roots (add or override id on roots)
        if (unitOrObjectGroupId != null && !unitOrObjectGroupId.isEmpty()) {
            final RequestMultiple request = selectRequest.getRequest();
            if (request != null) {
                LOGGER.debug("Reset $roots id with :" + unitOrObjectGroupId);
                request.resetRoots().addRoots(unitOrObjectGroupId);
            }
        }
        if (filters != null && !filters.isEmpty()) {
            final RequestMultiple request = selectRequest.getRequest();
            if (request != null) {
                final String[] hints = filters.stream()
                    .map(BuilderToken.FILTERARGS::exactToken)
                    .toArray(String[]::new);
                LOGGER.debug("Adding given $hint filters: " + Arrays.toString(hints));
                request.addHintFilter(hints);
            }
        }

        boolean shouldComputeUnitRule = false;
        ObjectNode fieldsProjection =
            (ObjectNode) selectRequest.getRequest().getProjection().get(PROJECTION.FIELDS.exactToken());
        if (fieldsProjection != null && fieldsProjection.get(GLOBAL.RULES.exactToken()) != null) {
            shouldComputeUnitRule = true;
            fieldsProjection.removeAll();
        }

        List<OntologyModel> ontologies;
        if (selectRequest.model() == BuilderToken.FILTERARGS.UNITS) {
            ontologies = this.unitOntologyLoader.loadOntologies();
        } else {
            ontologies = this.objectGroupOntologyLoader.loadOntologies();
        }

        result = dbRequest.execRequest(selectRequest, ontologies);
        arrayNodeResponse = MetadataJsonResponseUtils.populateJSONObjectResponse(result, selectRequest);

        // Compute Rule for unit(only with search by Id)
        if (shouldComputeUnitRule && result.hasFinalResult()) {
            computeRuleForUnit(arrayNodeResponse);
        }

        List res = toArrayList(arrayNodeResponse);
        List<FacetResult> facetResults = (result != null) ? result.getFacet() : new ArrayList<>();
        Long total = (result != null) ? result.getTotal() : res.size();
        String scrollId = (result != null) ? result.getScrollId() : null;
        DatabaseCursor hits = (scrollId != null) ? new DatabaseCursor(total, offset, limit, res.size(), scrollId)
            : new DatabaseCursor(total, offset, limit, res.size());
        return new RequestResponseOK<JsonNode>(queryCopy)
            .addAllResults(res).addAllFacetResults(facetResults).setHits(hits);
    }

    public void updateObjectGroupId(JsonNode updateQuery, String objectId)
        throws InvalidParseOperationException, MetaDataExecutionException, MetaDataNotFoundException,
        MetadataValidationException {

        if (updateQuery.isNull()) {
            throw new InvalidParseOperationException(REQUEST_IS_NULL);
        }

        final RequestParserMultiple updateRequest = new UpdateParserMultiple(new MongoDbVarNameAdapter());
        updateRequest.parse(updateQuery);

        // FIXME : Object group ontology to be implemented in INGEST workflows
        // Execute DSL request
        dbRequest.execUpdateRequest(updateRequest, objectId, OBJECTGROUP, this.objectGroupOntologyValidator, null, this.objectGroupOntologyLoader.loadOntologies());

    }

    public RequestResponse<UpdateUnit> updateUnits(JsonNode updateQuery)
        throws InvalidParseOperationException {
        Set<String> unitIds;
        final UpdateParserMultiple updateRequest = new UpdateParserMultiple(DEFAULT_VARNAME_ADAPTER);
        updateRequest.parse(updateQuery);
        final RequestMultiple request = updateRequest.getRequest();
        unitIds = request.getRoots();

        List<UpdateUnit> updatedUnits = unitIds.stream()
            .map(unitId -> updateAndTransformUnit(updateRequest, unitId))
            .collect(Collectors.toList());

        return new RequestResponseOK<UpdateUnit>(updateQuery)
            .addAllResults(updatedUnits)
            .setTotal(updatedUnits.size());
    }

    private UpdateUnit updateAndTransformUnit(UpdateParserMultiple updateRequest, String unitId) {

        try {

            UpdatedDocument updatedDocument = dbRequest
                .execUpdateRequest(updateRequest, unitId, MetadataCollections.UNIT, this.unitOntologyValidator, this.unitValidator, this.unitOntologyLoader.loadOntologies());

            String diffs = String.join("\n", VitamDocument.getConcernedDiffLines(
                VitamDocument.getUnifiedDiff(JsonHandler.prettyPrint(updatedDocument.getBeforeUpdate()),
                    JsonHandler.prettyPrint(updatedDocument.getAfterUpdate()))));

            if (diffs.isEmpty()) {
                LOGGER.warn(String.format("UNKNOWN updates for unit update %s.", unitId));
                return new UpdateUnit(unitId, StatusCode.OK, UNIT_METADATA_NO_CHANGES, "Unit updated with UNKNOWN changes.", "UNKNOWN diff, there are some changes but they cannot be trace.");
            }

            return new UpdateUnit(unitId, StatusCode.OK, UNIT_METADATA_UPDATE, "Update unit OK.", diffs);

        } catch (MetadataValidationException e) {
            LOGGER.error("An error occurred during unit update " + unitId, e);
            return error(unitId, KO, CHECK_UNIT_SCHEMA, e.getMessage());
        } catch (MetaDataNotFoundException e) {
            LOGGER.error("Unit not found during unit update " + unitId, e);
            return error(unitId, KO, UNIT_UNKNOWN_OR_FORBIDDEN, e.getMessage());
        } catch (Exception e) {
            LOGGER.error("An error occurred during unit update " + unitId, e);
            return error(unitId, FATAL, UNIT_METADATA_UPDATE, e.getMessage());
        }
    }

    public RequestResponse<UpdateUnit> updateUnitsRules(List<String> unitIds, RuleActions ruleActions,
        Map<String, DurationData> bindRuleToDuration) {

        List<UpdateUnit> unitRules = unitIds.stream()
            .map(unitId -> updateAndTransformUnitRules(unitId, ruleActions, bindRuleToDuration))
            .collect(Collectors.toList());

        return new RequestResponseOK<UpdateUnit>()
            .addAllResults(unitRules)
            .setTotal(unitRules.size());
    }

    private UpdateUnit updateAndTransformUnitRules(String unitId, RuleActions ruleActions,
        Map<String, DurationData> bindRuleToDuration) {
        try {
            UpdatedDocument updatedDocument =
                dbRequest.execRuleRequest(unitId, ruleActions, bindRuleToDuration, this.unitOntologyValidator,
                    unitValidator, this.unitOntologyLoader.loadOntologies());

            String diffs = String.join("\n", VitamDocument.getConcernedDiffLines(
                VitamDocument.getUnifiedDiff(JsonHandler.prettyPrint(updatedDocument.getBeforeUpdate()),
                    JsonHandler.prettyPrint(updatedDocument.getAfterUpdate()))));

            if (diffs.isEmpty()) {
                LOGGER.warn(String.format("UNKNOWN updates for unit update %s.", unitId));
                return new UpdateUnit(unitId, StatusCode.OK, UNIT_METADATA_NO_CHANGES, "Unit updated with UNKNOWN changes.", "UNKNOWN diff, there are some changes but they cannot be trace.");
            }

            return new UpdateUnit(unitId, StatusCode.OK, UNIT_METADATA_UPDATE, "Update unit rules OK.", diffs);

        } catch (MetadataValidationException e) {
            LOGGER.error("An error occurred during unit update " + unitId, e);
            return error(unitId, KO, CHECK_UNIT_SCHEMA, e.getMessage());
        } catch (MetaDataNotFoundException e) {
            LOGGER.error("Unit not found during unit update " + unitId, e);
            return error(unitId, KO, UNIT_UNKNOWN_OR_FORBIDDEN, e.getMessage());
        } catch (Exception e) {
            LOGGER.error("An error occurred during unit update " + unitId, e);
            return error(unitId, FATAL, UNIT_METADATA_UPDATE, e.getMessage());
        }
    }

    private UpdateUnit error(String unitId, StatusCode status, UpdateUnitKey key, String message) {
        return new UpdateUnit(unitId, status, key,
            StringUtils.defaultIfBlank(message, "Unknown error"), "no diff");
    }

    public UpdateUnit updateUnitById(JsonNode updateQuery, String unitId)
        throws MetaDataNotFoundException, InvalidParseOperationException, MetaDataExecutionException,
        MetadataValidationException {

        // parse Update request
        final RequestParserMultiple updateRequest = new UpdateParserMultiple(DEFAULT_VARNAME_ADAPTER);
        updateRequest.parse(updateQuery);

        UpdatedDocument updatedDocument = dbRequest
            .execUpdateRequest(updateRequest, unitId, MetadataCollections.UNIT, this.unitOntologyValidator, this.unitValidator, this.unitOntologyLoader.loadOntologies());

        String diffs = String.join("\n", VitamDocument.getConcernedDiffLines(
            VitamDocument.getUnifiedDiff(JsonHandler.prettyPrint(updatedDocument.getBeforeUpdate()),
                JsonHandler.prettyPrint(updatedDocument.getAfterUpdate()))));

        return new UpdateUnit(unitId, StatusCode.OK, UNIT_METADATA_UPDATE, "Update unit OK.", diffs);
    }

    private SelectMultiQuery createSearchParentSelect(List<String> unitList) throws InvalidParseOperationException {
        SelectMultiQuery newSelectQuery = new SelectMultiQuery();
        String[] rootList = new String[unitList.size()];
        rootList = unitList.toArray(rootList);
        newSelectQuery.addRoots(rootList);
        newSelectQuery.addProjection(
            JsonHandler.createObjectNode().set(PROJECTION.FIELDS.exactToken(),
                JsonHandler.createObjectNode()
                    .put(PROJECTIONARGS.ID.exactToken(), 1)
                    .put(PROJECTIONARGS.UNITUPS.exactToken(), 1)
                    .put(PROJECTIONARGS.MANAGEMENT.exactToken(), 1)));
        return newSelectQuery;
    }

    /**
     * @deprecated : Use the new api /unitsWithInheritedRules instead. To be removed in future releases.
     */
    private void computeRuleForUnit(ArrayNode arrayNodeResponse)
        throws InvalidParseOperationException, MetaDataExecutionException, MetaDataDocumentSizeException,
        MetaDataNotFoundException, BadRequestException, VitamDBException {
        Map<String, UnitNode> allUnitNode = new HashMap<>();
        Set<String> rootList = new HashSet<>();
        List<String> unitParentIdList = new ArrayList<>();
        String unitId = "";
        for (JsonNode unitNode : arrayNodeResponse) {
            ArrayNode unitParentId = (ArrayNode) unitNode.get(PROJECTIONARGS.ALLUNITUPS.exactToken());
            for (JsonNode parentIdNode : unitParentId) {
                unitParentIdList.add(parentIdNode.asText());
            }
            String currentUnitId = unitNode.get(PROJECTIONARGS.ID.exactToken()).asText();
            if (unitId.isEmpty()) {
                unitId = currentUnitId;
            }
            unitParentIdList.add(currentUnitId);
        }
        SelectMultiQuery newSelectQuery = createSearchParentSelect(unitParentIdList);
        RequestResponseOK unitParents = selectMetadataObject(newSelectQuery.getFinalSelect(), null,
            singletonList(BuilderToken.FILTERARGS.UNITS));

        Map<String, UnitSimplified> unitMap = UnitSimplified.getUnitIdMap(unitParents.getResults());
        UnitRuleCompute unitNode = new UnitRuleCompute(unitMap.get(unitId));
        unitNode.buildAncestors(unitMap, allUnitNode, rootList);
        unitNode.computeRule();
        JsonNode rule = JsonHandler.toJsonNode(unitNode.getHeritedRules().getInheritedRule());
        ((ObjectNode) arrayNodeResponse.get(0)).set(UnitInheritedRule.INHERITED_RULE, rule);
    }

    public void refreshUnit() throws IllegalArgumentException, VitamThreadAccessException {
        final Integer tenantId = ParameterHelper.getTenantParameter();
        mongoDbAccess.getEsClient().refreshIndex(MetadataCollections.UNIT, tenantId);
    }

    public void refreshObjectGroup() throws IllegalArgumentException, VitamThreadAccessException {
        final Integer tenantId = ParameterHelper.getTenantParameter();
        mongoDbAccess.getEsClient().refreshIndex(MetadataCollections.OBJECTGROUP, tenantId);
    }

    public IndexationResult reindex(IndexParameters indexParam) {
        MetadataCollections collection;
        try {
            collection = MetadataCollections.valueOf(indexParam.getCollectionName().toUpperCase());
        } catch (IllegalArgumentException exc) {
            String message = String.format("Try to reindex a non metadata collection '%s' with metadata module",
                indexParam.getCollectionName());
            LOGGER.error(message);
            return indexationHelper.getFullKOResult(indexParam, message);
        }
        // mongo collection
        MongoCollection<Document> mongoCollection = collection.getCollection();
        try (InputStream mappingStream = ElasticsearchCollections.valueOf(indexParam.getCollectionName().toUpperCase())
            .getMappingAsInputStream()) {
            return indexationHelper.reindex(mongoCollection, collection.getName(), mongoDbAccess.getEsClient(),
                indexParam.getTenants(), mappingStream);
        } catch (IOException exc) {
            LOGGER.error("Cannot get '{}' elastic search mapping for tenants {}", collection.name(),
                indexParam.getTenants().stream().map(Object::toString).collect(Collectors.joining(", ")));
            return indexationHelper.getFullKOResult(indexParam, exc.getMessage());
        }
    }

    public void switchIndex(String alias, String newIndexName) throws DatabaseException {
        try {
            indexationHelper.switchIndex(alias, newIndexName, mongoDbAccess.getEsClient());
        } catch (DatabaseException exc) {
            LOGGER.error("Cannot switch alias {} to index {}", alias, newIndexName);
            throw exc;
        }
    }
}
