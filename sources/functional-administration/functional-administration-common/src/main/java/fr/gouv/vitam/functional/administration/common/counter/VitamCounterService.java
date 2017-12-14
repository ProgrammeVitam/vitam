/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.functional.administration.common.counter;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Sorts.descending;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.BasicDBObject;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.parser.request.adapter.SingleVarNameAdapter;
import fr.gouv.vitam.common.database.parser.request.single.SelectParserSingle;
import fr.gouv.vitam.common.database.server.DbRequestResult;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.HeaderIdHelper;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.VitamSequence;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import org.bson.conversions.Bson;



/**
 * Vitam functionnal counter service
 */
public class VitamCounterService {
    private static final String ARGUMENT_MUST_NOT_BE_NULL = "Argument must not be null";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(VitamCounterService.class);
    public static final String BACK_UP_SEQUENCE = "BackUp";
    private final MongoDbAccessAdminImpl mongoAccess;
    private final Set<Integer> tenants;
    private final Map<SequenceType, FunctionalAdminCollections> collections = new HashMap<>();
    private final Map<Integer, List<FunctionalAdminCollections>> externalIdentifiers;

    /**
     * Constructor
     *
     * @param dbConfiguration
     * @param tenants
     * @param externalIdentifiers
     * @throws VitamException
     */
    public VitamCounterService(MongoDbAccessAdminImpl dbConfiguration, List<Integer> tenants,
        Map<Integer, List<String>> externalIdentifiers)
        throws VitamException {
        ParametersChecker.checkParameter(ARGUMENT_MUST_NOT_BE_NULL, tenants);
        ArrayList<SequenceType> sequences = new ArrayList<>();
        this.externalIdentifiers = new HashMap<>();
        mongoAccess = dbConfiguration;

        Collections.addAll(sequences, SequenceType.values());
        sequences.forEach(i -> collections.put(i, i.getCollection()));
        this.tenants = new HashSet<>(tenants);


        initSequences();
        initExternalIds(externalIdentifiers);
    }

    private void initExternalIds(Map<Integer, List<String>> externalIdentifiers) {
        if (externalIdentifiers != null)
            for (Map.Entry<Integer, List<String>> identifiers : externalIdentifiers.entrySet()) {
                List<FunctionalAdminCollections> functionalAdminCollections = new ArrayList<>();
                for (String collection : identifiers.getValue()) {
                    functionalAdminCollections.add(FunctionalAdminCollections.valueOf(collection));
                }
                this.externalIdentifiers.put(identifiers.getKey(), functionalAdminCollections);
            }
    }

    /**
     * @throws VitamException
     */
    private void initSequences() throws VitamException {
        try {
            for (Integer tenantId : this.tenants) {
                runInVitamThread(() -> {
                    VitamThreadUtils.getVitamSession().setTenantId(tenantId);
                    try {
                        for (Map.Entry<SequenceType, FunctionalAdminCollections> entry : collections.entrySet()) {
                            JsonNode query = generateQuery(entry.getKey());
                            try (DbRequestResult result =
                                mongoAccess.findDocuments(query, FunctionalAdminCollections.VITAM_SEQUENCE)) {
                                if (!result.hasResult()) {
                                    createSequence(tenantId, entry.getKey().getName());
                                }
                            }
                            createSequence(tenantId, BACK_UP_SEQUENCE);
                        }
                    } catch (VitamException | InvalidCreateOperationException e) {
                        throw new RuntimeException(e);
                    }
                });
            }

        } catch (Exception e) {
            throw new VitamException(e);
        }
    }

    private JsonNode generateQuery(SequenceType sequenceType)
        throws InvalidParseOperationException, InvalidCreateOperationException {
        final SelectParserSingle parser = new SelectParserSingle(new SingleVarNameAdapter());
        parser.parse(new Select().getFinalSelect());
        parser.addCondition(QueryHelper.eq(VitamSequence.NAME, sequenceType.getName()));
        if (sequenceType.getCollection().isMultitenant()) {
            parser.addCondition(QueryHelper.eq(VitamFieldsHelper.tenant(), HeaderIdHelper.getTenantId()));
        }
        return parser.getRequest().getFinalSelect();
    }

    /**
     * @param tenant
     * @param sequenceNAme
     * @throws VitamException
     */
    private void createSequence(Integer tenant, String sequenceNAme)
        throws VitamException {
        ObjectNode node = JsonHandler.createObjectNode();
        node.put(VitamSequence.NAME, sequenceNAme);
        node.put(VitamSequence.COUNTER, 0);
        node.put(VitamSequence.TENANT_ID, tenant);
        JsonNode firstSequence = JsonHandler.getFromString(node.toString());
        //  sequenceCollection.
        mongoAccess.insertDocument(firstSequence, FunctionalAdminCollections.VITAM_SEQUENCE).close();
    }


    /**
     * Atomically find a sequence  and update it.
     *
     * @param tenant
     * @param sequenceType
     * @return the sequence concatened with it name the name
     * @throws InvalidCreateOperationException
     * @throws InvalidParseOperationException
     * @throws ReferentialException
     */
    public String getNextSequenceAsString(Integer tenant, SequenceType sequenceType)
        throws InvalidCreateOperationException,
        InvalidParseOperationException, ReferentialException {
        Integer sequence = getNextSequence(tenant, sequenceType);
        return sequenceType.getName() + "-" + String.format("%06d", sequence);
    }


    /**
     * Atomically find a sequence  and update it.
     *
     * @param tenant
     * @param sequenceType
     * @return the sequence
     * @throws InvalidCreateOperationException
     * @throws InvalidParseOperationException
     * @throws ReferentialException
     */
    public Integer getNextSequence(Integer tenant, SequenceType sequenceType) throws ReferentialException {
        final BasicDBObject incQuery = new BasicDBObject();
        incQuery.append("$inc", new BasicDBObject(VitamSequence.COUNTER, 1));
        Bson query;
        if (sequenceType.getCollection().isMultitenant()) {
            query = and(
                eq(VitamSequence.NAME, sequenceType.getName()),
                eq(VitamDocument.TENANT_ID, tenant));
        } else {
            query = eq(VitamSequence.NAME, sequenceType.getName());
        }

        FindOneAndUpdateOptions findOneAndUpdateOptions = new FindOneAndUpdateOptions();
        findOneAndUpdateOptions.returnDocument(ReturnDocument.AFTER);
        try {
            final Object result = FunctionalAdminCollections.VITAM_SEQUENCE.getCollection()
                .findOneAndUpdate(query, incQuery, findOneAndUpdateOptions);
            return ((VitamSequence) result).getCounter();
        } catch (final Exception e) {
            LOGGER.error("find Document Exception", e);
            throw new ReferentialException(e);
        }
    }

    /**
     * @param tenant
     * @return
     * @throws ReferentialException
     */
    public Integer getNextBackUpSequence(Integer tenant) throws ReferentialException {

        final BasicDBObject incQuery = new BasicDBObject();
        incQuery.append("$inc", new BasicDBObject(VitamSequence.COUNTER, 1));
        Bson query;

        query = and(
            eq(VitamSequence.NAME, BACK_UP_SEQUENCE),
            eq(VitamDocument.TENANT_ID, tenant));

        FindOneAndUpdateOptions findOneAndUpdateOptions = new FindOneAndUpdateOptions();

        findOneAndUpdateOptions.returnDocument(ReturnDocument.AFTER);
        try {
            final Object result = FunctionalAdminCollections.VITAM_SEQUENCE.getCollection()
                .findOneAndUpdate(query, incQuery, findOneAndUpdateOptions);
            return ((VitamSequence) result).getCounter();
        } catch (final Exception e) {
            LOGGER.error("find Document Exception", e);
            throw new ReferentialException(e);
        }
    }
    /**
     * @param r runnable
     */
    private void runInVitamThread(Runnable r) {
        Thread thread = VitamThreadFactory.getInstance().newThread(r);
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the last sequence Functiontionnal collection
     *
     * @param tenant
     * @param sequenceType
     * @return
     * @throws ReferentialException
     * @throws InvalidParseOperationException
     */
    public Integer getSequence(Integer tenant, SequenceType sequenceType) throws ReferentialException,
        InvalidParseOperationException {
        Integer sequence;
        Bson query;
        if (sequenceType.getCollection().isMultitenant()) {
            query = and(
                eq(VitamSequence.NAME, sequenceType.getName()),
                eq(VitamDocument.TENANT_ID, tenant));
        } else {
            query = eq(VitamSequence.NAME, sequenceType.getName());
        }

        try {
            final Collection<FunctionalAdminCollections>
                result =
                FunctionalAdminCollections.VITAM_SEQUENCE.getCollection().find(query).sort(descending("Counter"))
                    .limit(1).into(new ArrayList<FunctionalAdminCollections>());
            if (result.isEmpty()) {
                throw new ReferentialException("Document not found");
            }
            sequence = ((VitamSequence) ((Object) result.iterator().next())).getCounter();
        } catch (final Exception e) {
            LOGGER.error("find Document Exception", e);
            throw new ReferentialException(e);
        }
        return sequence;
    }

    public boolean isSlaveFunctionnalCollectionOnTenant(FunctionalAdminCollections collection, Integer tenant) {
        return externalIdentifiers.containsKey(tenant) && externalIdentifiers.get(tenant).contains(collection);
    }


}
