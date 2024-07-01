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
package fr.gouv.vitam.storage.offers.tape.worker.tasks;

import fr.gouv.vitam.storage.engine.common.model.ReadOrder;
import fr.gouv.vitam.storage.engine.common.model.ReadWriteOrder;
import fr.gouv.vitam.storage.engine.common.model.TapeCatalog;
import fr.gouv.vitam.storage.engine.common.model.WriteOrder;
import fr.gouv.vitam.storage.offers.tape.cas.AccessRequestManager;
import fr.gouv.vitam.storage.offers.tape.cas.ArchiveCacheStorage;
import fr.gouv.vitam.storage.offers.tape.cas.ArchiveReferentialRepository;
import fr.gouv.vitam.storage.offers.tape.spec.TapeCatalogService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeLibraryService;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ReadWriteTask implements Future<ReadWriteResult> {

    private final Future<ReadWriteResult> readWriteTask;

    public ReadWriteTask(
        ReadWriteOrder order,
        TapeCatalog workerCurrentTape,
        TapeLibraryService tapeLibraryService,
        TapeCatalogService tapeCatalogService,
        ArchiveReferentialRepository archiveReferentialRepository,
        AccessRequestManager accessRequestManager,
        String inputTarPath,
        boolean forceOverrideNonEmptyCartridges,
        ArchiveCacheStorage archiveCacheStorage
    ) {
        if (order.isWriteOrder()) {
            readWriteTask = new WriteTask(
                (WriteOrder) order,
                workerCurrentTape,
                tapeLibraryService,
                tapeCatalogService,
                archiveReferentialRepository,
                archiveCacheStorage,
                inputTarPath,
                forceOverrideNonEmptyCartridges
            );
        } else {
            readWriteTask = new ReadTask(
                (ReadOrder) order,
                workerCurrentTape,
                tapeLibraryService,
                tapeCatalogService,
                accessRequestManager,
                archiveCacheStorage
            );
        }
    }

    @Override
    public ReadWriteResult get() throws InterruptedException, ExecutionException {
        return readWriteTask.get();
    }

    @Override
    public ReadWriteResult get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException {
        return readWriteTask.get(timeout, unit);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return readWriteTask.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return readWriteTask.isCancelled();
    }

    @Override
    public boolean isDone() {
        return readWriteTask.isDone();
    }
}
