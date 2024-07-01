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
package fr.gouv.vitam.worker.core.plugin.migration;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.utils.ScrollSpliterator;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.metadata.core.database.configuration.GlobalDatasDb;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.worker.core.plugin.ScrollSpliteratorHelper;

import static fr.gouv.vitam.worker.core.plugin.migration.MigrationHelper.exportToReportAndDistributionFile;
import static fr.gouv.vitam.worker.core.plugin.migration.MigrationHelper.getSelectMultiQuery;

/**
 * MigrationUnitPrepare class
 */
public class MigrationUnitPrepare extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(MigrationUnitPrepare.class);

    private static final String MIGRATION_UNITS_LIST = "MIGRATION_UNITS_LIST";
    static final String MIGRATION_UNITS_LIST_IDS = "migrationUnitsListIds";
    private final MetaDataClientFactory metaDataClientFactory;
    private final int bachSize;
    private static final String REPORTS = "reports";

    @VisibleForTesting
    public MigrationUnitPrepare(MetaDataClientFactory metaDataClientFactory, int bachSize) {
        this.metaDataClientFactory = metaDataClientFactory;
        this.bachSize = bachSize;
    }

    public MigrationUnitPrepare() {
        this(MetaDataClientFactory.getInstance(), GlobalDatasDb.LIMIT_LOAD);
    }

    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler) {
        ItemStatus itemStatus = new ItemStatus(MIGRATION_UNITS_LIST);

        try (MetaDataClient client = metaDataClientFactory.getClient()) {
            SelectMultiQuery selectMultiQuery = getSelectMultiQuery();

            ScrollSpliterator<JsonNode> scrollRequest = ScrollSpliteratorHelper.createUnitScrollSplitIterator(
                client,
                selectMultiQuery,
                bachSize
            );

            exportToReportAndDistributionFile(
                scrollRequest,
                handler,
                "Units.jsonl",
                REPORTS + "/" + MIGRATION_UNITS_LIST_IDS + ".json"
            );
        } catch (InvalidParseOperationException | InvalidCreateOperationException | ProcessingException e) {
            LOGGER.error(e);
            return itemStatus.increment(StatusCode.FATAL);
        }

        itemStatus.increment(StatusCode.OK);
        return new ItemStatus(MIGRATION_UNITS_LIST).setItemsStatus(MIGRATION_UNITS_LIST, itemStatus);
    }

    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // nothing
    }
}
