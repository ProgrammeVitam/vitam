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
package fr.gouv.vitam.functional.administration.counter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.mongodb.BasicDBObject;
import com.mongodb.client.model.Filters;

import static com.mongodb.client.model.Filters.and;

import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
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
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.thread.VitamThreadFactory;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.VitamSequence;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;
import org.bson.conversions.Bson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/***
 *Vitam functionnal counter service
 */
public class VitamCounterService {
    private static final String ARGUMENT_MUST_NOT_BE_NULL = "Argument must not be null";

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(VitamCounterService.class);
    private final MongoDbAccessAdminImpl mongoAccess;
    private final Map<Integer, Integer> tenants;
    private final Map<String, FunctionalAdminCollections> collections =
            ImmutableMap.<String, FunctionalAdminCollections>builder()
                    .put("AC", FunctionalAdminCollections.ACCESS_CONTRACT)
                    .put("IC", FunctionalAdminCollections.INGEST_CONTRACT)
                    .put("PR", FunctionalAdminCollections.PROFILE)
                    .put("CT", FunctionalAdminCollections.CONTEXT)
                    .build();

    /**
     * Constructor
     *
     * @param dbConfiguration
     */
    public VitamCounterService(MongoDbAccessAdminImpl dbConfiguration, List<Integer> tenants)
            throws VitamException {
        ParametersChecker.checkParameter(ARGUMENT_MUST_NOT_BE_NULL, tenants);

        mongoAccess = dbConfiguration;
        this.tenants = new HashMap<Integer, Integer>();
        tenants.stream().forEach((i) -> {
            this.tenants.put(i, new Integer(i));
        });
        initSequences();
    }

    /**
     * @throws VitamException
     */
    private void initSequences() throws VitamException {
        try {
            for (Integer tenantId : this.tenants.values()) {
                runInVitamThread(() -> {
                    VitamThreadUtils.getVitamSession().setTenantId(tenantId);
                    try {
                        for (Map.Entry<String, FunctionalAdminCollections> entry : collections.entrySet()) {
                            JsonNode query = generateQuery(entry.getKey());
                            try (DbRequestResult result =
                                mongoAccess.findDocuments(query, FunctionalAdminCollections.VITAM_SEQUENCE)) {
                                if (!result.getCursor().hasNext()) {
                                    createSequence(tenantId, entry);
                                }
                            }
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

    private JsonNode generateQuery(String code) throws InvalidParseOperationException, InvalidCreateOperationException {
        final SelectParserSingle parser = new SelectParserSingle(new SingleVarNameAdapter());
        parser.parse(new Select().getFinalSelect());
        parser.addCondition(QueryHelper.eq(VitamSequence.NAME, code));
        return parser.getRequest().getFinalSelect();
    }

    /**
     * @param tenant
     * @param entry
     * @throws VitamException
     */
    private void createSequence(Integer tenant, Map.Entry<String, FunctionalAdminCollections> entry) throws VitamException {
        ObjectNode node = JsonHandler.createObjectNode();
        node.put(VitamSequence.NAME, entry.getKey());
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
     * @param code
     * @return String
     * @throws InvalidCreateOperationException
     * @throws InvalidParseOperationException
     * @throws ReferentialException
     */
    public String getNextSequence(Integer tenant, String code) throws InvalidCreateOperationException,
            InvalidParseOperationException, ReferentialException {
        Integer sequence;

        final BasicDBObject incQuery = new BasicDBObject();
        incQuery.append("$inc", new BasicDBObject(VitamSequence.COUNTER, 1));
        Bson query = and(
                Filters.eq(VitamSequence.NAME, code),
                Filters.eq(VitamDocument.TENANT_ID, ParameterHelper.getTenantParameter()));
        FindOneAndUpdateOptions findOneAndUpdateOptions = new FindOneAndUpdateOptions();
        findOneAndUpdateOptions.returnDocument(ReturnDocument.AFTER);
        try {
            final Object result = FunctionalAdminCollections.VITAM_SEQUENCE.getCollection()
                .findOneAndUpdate(query, incQuery, findOneAndUpdateOptions);
            sequence = ((VitamSequence) result).getCounter();
        } catch (final Exception e) {
            LOGGER.error("find Document Exception", e);
            throw new ReferentialException(e);
        }
        return code + "-" + String.format("%06d", sequence);
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
}
