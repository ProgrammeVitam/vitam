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
 *******************************************************************************/
package fr.gouv.vitam.worker.core.distribution;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;

import java.io.IOException;

/**
 * Helper class for exporting chained files
 */
public abstract class AbstractChainedFileWriter implements AutoCloseable {

    private final String filename;
    private final int batchSize;

    private ChainedFileModel currentChainedFile;
    private String currentFileName;
    private int chainedFileCount;
    private boolean closed;

    public AbstractChainedFileWriter(String filename, int batchSize) {
        this.filename = filename;
        this.batchSize = batchSize;

        this.currentChainedFile = new ChainedFileModel();
        this.currentFileName = filename;
        this.chainedFileCount = 0;
    }

    public void addEntry(String id) throws InvalidParseOperationException, IOException {
        if (closed) {
            throw new IllegalStateException("Closed writer");
        }

        currentChainedFile.getElements().add(id);
        if (currentChainedFile.getElements().size() == batchSize) {
            split();
        }
    }

    public void split() throws InvalidParseOperationException, IOException {

        if (currentChainedFile.getElements().isEmpty()) {
            return;
        }

        chainedFileCount++;
        String nextFileName = filename + "." + chainedFileCount;
        currentChainedFile.setNextFile(nextFileName);

        storeToWorkspace();

        currentFileName = nextFileName;
        currentChainedFile = new ChainedFileModel();
    }

    private void storeToWorkspace() throws InvalidParseOperationException, IOException {
        storeToWorkspace(currentFileName, currentChainedFile);
    }

    protected abstract void storeToWorkspace(String filename, ChainedFileModel chainedFileModel)
        throws IOException, InvalidParseOperationException;

    @Override
    public void close() throws InvalidParseOperationException, IOException {
        if (!closed) {
            storeToWorkspace();
            closed = true;
        }
    }
}
