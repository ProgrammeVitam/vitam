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
package fr.gouv.vitam.metadata.core.reconstruction.repository.impl;

import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.metadata.core.reconstruction.exception.ReconstructionException;
import fr.gouv.vitam.metadata.core.reconstruction.repository.OperationReportRepository;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.client.exception.StorageUnavailableDataFromAsyncOfferClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import jakarta.ws.rs.core.Response;

import java.io.InputStream;

public class OperationReportRepositoryImpl implements OperationReportRepository {

    public static final String JSONL = ".jsonl";
    private final StorageClientFactory storageClientFactory;

    public OperationReportRepositoryImpl() {
        this(StorageClientFactory.getInstance());
    }

    public OperationReportRepositoryImpl(StorageClientFactory storageClientFactory) {
        this.storageClientFactory = storageClientFactory;
    }

    @Override
    public InputStream retrieveJsonReportForOperation(String operationId) throws ReconstructionException {
        try (
            StorageClient storageClient = storageClientFactory.getClient();
            Response reportResponse = storageClient.getContainerAsync(
                VitamConfiguration.getDefaultStrategy(),
                operationId + JSONL,
                DataCategory.REPORT,
                AccessLogUtils.getNoLogAccessLog()
            )
        ) {
            return reportResponse.readEntity(InputStream.class);
        } catch (
            StorageNotFoundException
            | StorageUnavailableDataFromAsyncOfferClientException
            | StorageServerClientException e
        ) {
            throw new ReconstructionException("Error retrieving JsonReport for operation" + operationId, e);
        }
    }
}
