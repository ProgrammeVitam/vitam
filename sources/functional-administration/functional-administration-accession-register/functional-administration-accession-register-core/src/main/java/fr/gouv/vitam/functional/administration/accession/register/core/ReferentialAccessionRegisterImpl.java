/*******************************************************************************
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
package fr.gouv.vitam.functional.administration.accession.register.core;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.MongoBulkWriteException;
import com.mongodb.MongoWriteException;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.UPDATEACTION;
import fr.gouv.vitam.common.database.server.DbRequestResult;
import fr.gouv.vitam.common.database.server.DbRequestSingle;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.VitamAutoCloseable;
import fr.gouv.vitam.common.model.administration.RegisterValueDetailModel;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterDetail;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterSummary;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialNotFoundException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;

/**
 * Referential Accession Register Implement
 */
public class ReferentialAccessionRegisterImpl implements VitamAutoCloseable {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ReferentialAccessionRegisterImpl.class);
    private final MongoDbAccessAdminImpl mongoAccess;

    /**
     * Constructor
     *
     * @param dbConfiguration the mongo access configuration
     */
    public ReferentialAccessionRegisterImpl(MongoDbAccessAdminImpl dbConfiguration) {
        mongoAccess = dbConfiguration;
    }

    /**
     * @param registerDetail to create in Mongodb
     * @throws ReferentialException throws when insert mongodb error
     */
    public void createOrUpdateAccessionRegister(AccessionRegisterDetail registerDetail)
        throws ReferentialException {

        LOGGER.debug("register ID / Originating Agency: {} / {}", registerDetail.getId(),
            registerDetail.getOriginatingAgency());
        // store accession register detail
        try {
            mongoAccess.insertDocument(JsonHandler.toJsonNode(registerDetail),
                FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL).close();
        } catch (final InvalidParseOperationException e) {
            LOGGER.info("Create register detail Error", e);
            throw new ReferentialException(e);
        }

        // store accession register summary
        final RegisterValueDetailModel initialValue = new RegisterValueDetailModel(0, 0, 0);
        try {
            final AccessionRegisterSummary accessionRegister = new AccessionRegisterSummary();
            accessionRegister
                .setId(GUIDFactory.newAccessionRegisterSummaryGUID(ParameterHelper.getTenantParameter()).getId())
                .setOriginatingAgency(registerDetail.getOriginatingAgency())
                .setTotalObjects(initialValue)
                .setTotalObjectGroups(initialValue)
                .setTotalUnits(initialValue)
                .setObjectSize(initialValue)
                .setCreationDate(LocalDateUtil.now().toString());


            LOGGER.debug("register ID / Originating Agency: {} / {}", registerDetail.getId(),
                registerDetail.getOriginatingAgency());

            mongoAccess.insertDocument(JsonHandler.toJsonNode(accessionRegister),
                FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY);
        } catch (ReferentialException e) {
            if (!DbRequestSingle.checkInsertOrUpdate(e)) {
                throw e;
            }
        } catch (final InvalidParseOperationException e) {
            throw new ReferentialException(e);
        } catch (final MongoWriteException | MongoBulkWriteException e) {
            LOGGER.info("Document existed, updating ...");
        }

        try {
            final Map<String, Object> updateMap = createMaptoUpdate(registerDetail);
            mongoAccess.updateAccessionRegisterByMap(
                updateMap,
                JsonHandler.toJsonNode(registerDetail),
                FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY,
                UPDATEACTION.INC);
        } catch (final InvalidParseOperationException e) {
            LOGGER.info("Update error", e);
            throw new ReferentialException(e);
        } catch (final Exception e) {
            LOGGER.info("Unknown error", e);
            throw new ReferentialException(e);
        }

    }

    private Map<String, Object> createMaptoUpdate(AccessionRegisterDetail registerDetail) {
        final Map<String, Object> updateMap = new HashMap<>();

        updateMap.put(AccessionRegisterSummary.TOTAL_OBJECTGROUPS + "." + AccessionRegisterSummary.INGESTED,
            registerDetail.getTotalObjectGroups().getIngested());
        updateMap.put(AccessionRegisterSummary.TOTAL_OBJECTGROUPS + "." + AccessionRegisterSummary.DELETED,
            registerDetail.getTotalObjectGroups().getDeleted());
        updateMap.put(AccessionRegisterSummary.TOTAL_OBJECTGROUPS + "." + AccessionRegisterSummary.REMAINED,
            registerDetail.getTotalObjectGroups().getRemained());
        updateMap.put(AccessionRegisterSummary.TOTAL_OBJECTGROUPS + "." + AccessionRegisterSummary.DETACHED,
            registerDetail.getTotalObjectGroups().getDetached());
        updateMap.put(AccessionRegisterSummary.TOTAL_OBJECTGROUPS + "." + AccessionRegisterSummary.ATTACHED,
            registerDetail.getTotalObjectGroups().getAttached());
        updateMap.put(AccessionRegisterSummary.TOTAL_OBJECTGROUPS + "." + AccessionRegisterSummary.SYMBOLIC_REMAINED,
            registerDetail.getTotalObjectGroups().getSymbolicRemained());

        updateMap.put(AccessionRegisterSummary.TOTAL_OBJECTS + "." + AccessionRegisterSummary.INGESTED,
            registerDetail.getTotalObjects().getIngested());
        updateMap.put(AccessionRegisterSummary.TOTAL_OBJECTS + "." + AccessionRegisterSummary.DELETED,
            registerDetail.getTotalObjects().getDeleted());
        updateMap.put(AccessionRegisterSummary.TOTAL_OBJECTS + "." + AccessionRegisterSummary.REMAINED,
            registerDetail.getTotalObjects().getRemained());
        updateMap.put(AccessionRegisterSummary.TOTAL_OBJECTS + "." + AccessionRegisterSummary.DETACHED,
            registerDetail.getTotalObjects().getDetached());
        updateMap.put(AccessionRegisterSummary.TOTAL_OBJECTS + "." + AccessionRegisterSummary.ATTACHED,
            registerDetail.getTotalObjects().getAttached());
        updateMap.put(AccessionRegisterSummary.TOTAL_OBJECTS + "." + AccessionRegisterSummary.SYMBOLIC_REMAINED,
            registerDetail.getTotalObjects().getSymbolicRemained());

        updateMap.put(AccessionRegisterSummary.TOTAL_UNITS + "." + AccessionRegisterSummary.INGESTED,
            registerDetail.getTotalUnits().getIngested());
        updateMap.put(AccessionRegisterSummary.TOTAL_UNITS + "." + AccessionRegisterSummary.DELETED,
            registerDetail.getTotalUnits().getDeleted());
        updateMap.put(AccessionRegisterSummary.TOTAL_UNITS + "." + AccessionRegisterSummary.REMAINED,
            registerDetail.getTotalUnits().getRemained());
        updateMap.put(AccessionRegisterSummary.TOTAL_UNITS + "." + AccessionRegisterSummary.DETACHED,
            registerDetail.getTotalUnits().getDetached());
        updateMap.put(AccessionRegisterSummary.TOTAL_UNITS + "." + AccessionRegisterSummary.ATTACHED,
            registerDetail.getTotalUnits().getAttached());
        updateMap.put(AccessionRegisterSummary.TOTAL_UNITS + "." + AccessionRegisterSummary.SYMBOLIC_REMAINED,
            registerDetail.getTotalUnits().getSymbolicRemained());

        updateMap.put(AccessionRegisterSummary.OBJECT_SIZE + "." + AccessionRegisterSummary.INGESTED,
            registerDetail.getTotalObjectSize().getIngested());
        updateMap.put(AccessionRegisterSummary.OBJECT_SIZE + "." + AccessionRegisterSummary.DELETED,
            registerDetail.getTotalObjectSize().getDeleted());
        updateMap.put(AccessionRegisterSummary.OBJECT_SIZE + "." + AccessionRegisterSummary.REMAINED,
            registerDetail.getTotalObjectSize().getRemained());
        updateMap.put(AccessionRegisterSummary.OBJECT_SIZE + "." + AccessionRegisterSummary.DETACHED,
            registerDetail.getTotalObjectSize().getDetached());
        updateMap.put(AccessionRegisterSummary.OBJECT_SIZE + "." + AccessionRegisterSummary.ATTACHED,
            registerDetail.getTotalObjectSize().getAttached());
        updateMap.put(AccessionRegisterSummary.OBJECT_SIZE + "." + AccessionRegisterSummary.SYMBOLIC_REMAINED,
            registerDetail.getTotalObjectSize().getSymbolicRemained());
        return updateMap;
    }

    @Override
    public void close() {
        // Empty
    }

    /**
     * search for an accession register's summary
     *
     * @param select the search criteria for the select operation
     * @return A list of AccressionRegisterSummaries matching the 'select' criteria.
     * @throws ReferentialException If the search's result is null or empty, or if the mongo search throw error
     */
    public RequestResponseOK<AccessionRegisterSummary> findDocuments(JsonNode select) throws ReferentialException {
        try (DbRequestResult result =
            mongoAccess.findDocuments(select, FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY)) {
            final RequestResponseOK<AccessionRegisterSummary> list =
                result.getRequestResponseOK(select, AccessionRegisterSummary.class);
            if (list.isEmpty()) {
                throw new ReferentialNotFoundException("Register Summary not found");
            }
            return list;
        } catch (final ReferentialException e) {
            LOGGER.error(e.getMessage());
            throw e;
        }
    }

    /**
     * search for an accession register's operation detail
     *
     * @param select the search criteria for the select operation
     * @return A list of AccressionRegisterDetails matching the 'select' criteria.
     * @throws ReferentialException If the search's result is null or empty, or if the mongo search throw error
     */
    public RequestResponseOK<AccessionRegisterDetail> findDetail(JsonNode select) throws ReferentialException {
        try (DbRequestResult result =
            mongoAccess.findDocuments(select, FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL)) {
            final RequestResponseOK<AccessionRegisterDetail> list =
                result.getRequestResponseOK(select, AccessionRegisterDetail.class);
            if (list.isEmpty()) {
                throw new ReferentialNotFoundException("Register Detail not found");
            }
            return list;
        } catch (final ReferentialException e) {
            LOGGER.error(e.getMessage());
            throw e;
        }
    }

    /**
     * Reset MongoDB Index (import optimization?)
     */
    public static void resetIndexAfterImport() {
        LOGGER.info("Rebuild indexes");
        AccessionRegisterSummary.addIndexes();
    }
}
