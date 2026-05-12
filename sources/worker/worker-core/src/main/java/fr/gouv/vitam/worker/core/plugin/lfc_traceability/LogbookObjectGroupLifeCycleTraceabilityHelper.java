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
package fr.gouv.vitam.worker.core.plugin.lfc_traceability;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.security.merkletree.MerkleTreeAlgo;
import fr.gouv.vitam.logbook.common.exception.TraceabilityException;
import fr.gouv.vitam.logbook.common.model.TraceabilityFile;
import fr.gouv.vitam.logbook.common.model.TraceabilityType;
import fr.gouv.vitam.logbook.common.parameters.Contexts;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.worker.common.HandlerIO;

import java.io.IOException;

public class LogbookObjectGroupLifeCycleTraceabilityHelper extends LogbookLifeCycleTraceabilityHelper {

    private static final String ZIP_NAME = "LogbookObjectGroupLifecycles";

    private final CloseableIterator<JsonNode> traceabilityDataIterator;

    /**
     * @param handlerIO Workflow Input/Output of the traceability event
     * @param logbookOperationsClient used to search the operation to secure
     * @param itemStatus used by workflow, event must be updated here
     * @param operationID of the current traceability process
     * @param traceabilityDataIterator
     * @param traceabilityEventFileName
     * @param traceabilityZipFileName
     */
    public LogbookObjectGroupLifeCycleTraceabilityHelper(
        HandlerIO handlerIO,
        LogbookOperationsClient logbookOperationsClient,
        ItemStatus itemStatus,
        String operationID,
        CloseableIterator<JsonNode> traceabilityDataIterator,
        String traceabilityEventFileName,
        String traceabilityZipFileName,
        String securisationVersion
    ) {
        super(
            handlerIO,
            logbookOperationsClient,
            itemStatus,
            operationID,
            traceabilityEventFileName,
            traceabilityZipFileName,
            securisationVersion
        );
        this.traceabilityDataIterator = traceabilityDataIterator;
    }

    @Override
    public void saveDataInZip(MerkleTreeAlgo algo, TraceabilityFile file) throws IOException, TraceabilityException {
        file.initStoreLog();
        try {
            extractAppendToFinalFile(traceabilityDataIterator, file, algo);
        } finally {
            file.closeStoreLog();
        }
    }

    @Override
    public String getZipName() {
        return ZIP_NAME;
    }

    @Override
    public TraceabilityType getTraceabilityType() {
        return TraceabilityType.OBJECTGROUP_LIFECYCLE;
    }

    @Override
    protected String getEventType() {
        return Contexts.OBJECTGROUP_LFC_TRACEABILITY.getEventType();
    }
}
