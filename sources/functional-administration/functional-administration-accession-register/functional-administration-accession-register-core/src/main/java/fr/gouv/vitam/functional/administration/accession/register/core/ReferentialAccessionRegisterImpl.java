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

import static fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.MongoBulkWriteException;
import com.mongodb.MongoWriteException;
import fr.gouv.vitam.common.database.builder.request.single.Update;
import fr.gouv.vitam.common.database.server.DbRequestResult;
import fr.gouv.vitam.common.database.server.DbRequestSingle;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.SchemaValidationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.VitamAutoCloseable;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterDetail;
import fr.gouv.vitam.functional.administration.common.AccessionRegisterSummary;
import fr.gouv.vitam.functional.administration.common.ReferentialAccessionRegisterSummaryUtil;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;

/**
 * Referential Accession Register Implement
 */
public class ReferentialAccessionRegisterImpl implements VitamAutoCloseable {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ReferentialAccessionRegisterImpl.class);
    private final MongoDbAccessAdminImpl mongoAccess;
    private final ReferentialAccessionRegisterSummaryUtil referentialAccessionRegisterSummaryUtil;

    /**
     * Constructor
     *
     * @param dbConfiguration                         the mongo access configuration
     * @param referentialAccessionRegisterSummaryUtil the accession register summary
     */
    public ReferentialAccessionRegisterImpl(MongoDbAccessAdminImpl dbConfiguration,
        ReferentialAccessionRegisterSummaryUtil referentialAccessionRegisterSummaryUtil) {
        mongoAccess = dbConfiguration;
        this.referentialAccessionRegisterSummaryUtil = referentialAccessionRegisterSummaryUtil;
    }

    /**
     * @param registerDetail to create in Mongodb
     * @throws ReferentialException throws when insert mongodb error
     */
    public void createOrUpdateAccessionRegister(AccessionRegisterDetail registerDetail)
        throws ReferentialException {

        LOGGER.debug("register ID / Originating Agency: {} / {}", registerDetail.getId(),
            registerDetail.getOriginatingAgency());
        // Store accession register detail
        try {
            mongoAccess.insertDocument(JsonHandler.toJsonNode(registerDetail),
                FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL).close();

        } catch (final InvalidParseOperationException | SchemaValidationException e) {
            throw new ReferentialException("Create register detail error", e);
        }

        // store accession register summary
        try {
            final AccessionRegisterSummary accessionRegister = referentialAccessionRegisterSummaryUtil
                .initAccessionRegisterSummary(registerDetail.getOriginatingAgency(),
                    GUIDFactory.newAccessionRegisterSummaryGUID(ParameterHelper.getTenantParameter()).getId());

            LOGGER.debug("register ID / Originating Agency: {} / {}", registerDetail.getId(),
                registerDetail.getOriginatingAgency());

            mongoAccess.insertDocument(JsonHandler.toJsonNode(accessionRegister),
                ACCESSION_REGISTER_SUMMARY);
        } catch (ReferentialException e) {
            if (!DbRequestSingle.checkInsertOrUpdate(e)) {
                throw e;
            }
        } catch (final InvalidParseOperationException | SchemaValidationException e) {
            throw new ReferentialException(e);
        } catch (final MongoWriteException | MongoBulkWriteException e) {
            LOGGER.info("Document existed, updating ...");
        }

        try {
            Update update = referentialAccessionRegisterSummaryUtil.generateUpdateQuery(registerDetail);

            mongoAccess.updateData(update.getFinalUpdate(), ACCESSION_REGISTER_SUMMARY);
        } catch (final Exception e) {
            throw new ReferentialException("Create register detail Error", e);
        }

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
        try (DbRequestResult result = mongoAccess.findDocuments(select, ACCESSION_REGISTER_SUMMARY)) {
            return result.getRequestResponseOK(select, AccessionRegisterSummary.class);
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
            return result.getRequestResponseOK(select, AccessionRegisterDetail.class);
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
