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
package fr.gouv.vitam.collect.internal.core.helpers;

import fr.gouv.vitam.collect.internal.core.common.TransactionModel;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.export.ExportRequest;
import fr.gouv.vitam.common.model.export.ExportRequestParameters;
import fr.gouv.vitam.common.model.export.ExportType;

import static fr.gouv.vitam.common.database.parser.request.GlobalDatasParser.DEFAULT_SCROLL_TIMEOUT;
import static fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchAccess.SCROLL_ACTIVATE_KEYWORD;

public class SipHelper {

    public static final String COLLECT_REQUESTER_IDENTIFIER = "Vitam-Collect";

    private SipHelper() throws IllegalAccessException {
        throw new IllegalAccessException("Utility class!");
    }

    public static ExportRequestParameters buildExportRequestParameters(TransactionModel transactionModel) {
        ExportRequestParameters exportRequestParameters = new ExportRequestParameters();
        exportRequestParameters.setMessageRequestIdentifier(GUIDFactory.newGUID().getId());
        exportRequestParameters.setArchivalAgencyIdentifier(
            transactionModel.getManifestContext().getArchivalAgencyIdentifier()
        );
        exportRequestParameters.setRequesterIdentifier(COLLECT_REQUESTER_IDENTIFIER);
        exportRequestParameters.setComment(transactionModel.getManifestContext().getComment());
        exportRequestParameters.setArchivalAgreement(
            (transactionModel.getManifestContext() != null
                    ? transactionModel.getManifestContext().getArchivalAgreement()
                    : null)
        );
        exportRequestParameters.setTransferringAgency(
            transactionModel.getManifestContext().getTransferringAgencyIdentifier()
        );
        return exportRequestParameters;
    }

    public static ExportRequest buildExportRequest(
        TransactionModel transactionModel,
        ExportRequestParameters exportRequestParameters
    ) throws InvalidCreateOperationException {
        SelectMultiQuery exportSelect = new SelectMultiQuery();
        exportSelect.setQuery(QueryHelper.eq(VitamFieldsHelper.initialOperation(), transactionModel.getId()));
        exportSelect.setScrollFilter(SCROLL_ACTIVATE_KEYWORD, DEFAULT_SCROLL_TIMEOUT, 10000);

        ExportRequest exportRequest = new ExportRequest();
        exportRequest.setDslRequest(exportSelect.getFinalSelect());

        exportRequest.setExportWithLogBookLFC(false);
        exportRequest.setExportType(ExportType.MinimalArchiveDeliveryRequestReply);

        exportRequest.setExportRequestParameters(exportRequestParameters);
        return exportRequest;
    }
}
