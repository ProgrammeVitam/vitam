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
package fr.gouv.vitam.storage.offers.tape.pool;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.storage.offers.tape.spec.TapeDriveService;
import fr.gouv.vitam.storage.offers.tape.spec.TapeLibraryPool;
import fr.gouv.vitam.storage.offers.tape.spec.TapeRobotService;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class TapeLibraryPoolImpl implements TapeLibraryPool {

    private final String libraryIdentifier;
    private final BlockingQueue<TapeRobotService> tapeRobotServicePool;
    private final ConcurrentHashMap<Integer, TapeDriveService> tapeDriveServicePool;

    public TapeLibraryPoolImpl(
        String libraryIdentifier,
        BlockingQueue<TapeRobotService> tapeRobotServicePool,
        ConcurrentHashMap<Integer, TapeDriveService> tapeDriveServicePool
    ) {
        this.libraryIdentifier = libraryIdentifier;
        this.tapeRobotServicePool = tapeRobotServicePool;
        this.tapeDriveServicePool = tapeDriveServicePool;
    }

    @Override
    public TapeRobotService checkoutRobotService() throws InterruptedException {
        return this.tapeRobotServicePool.take();
    }

    @Override
    public TapeRobotService checkoutRobotService(long timeout, TimeUnit unit) throws InterruptedException {
        return this.tapeRobotServicePool.poll(timeout, unit);
    }

    @Override
    public TapeDriveService checkoutDriveService(Integer driveIndex) {
        ParametersChecker.checkParameter("driveIndex is required", driveIndex);
        return this.tapeDriveServicePool.remove(driveIndex);
    }

    @Override
    public void pushRobotService(TapeRobotService tapeRobotService) throws InterruptedException {
        ParametersChecker.checkParameter("TapeRobotService is required", tapeRobotService);
        this.tapeRobotServicePool.put(tapeRobotService);
    }

    @Override
    public void pushDriveService(TapeDriveService tapeDriveService) {
        ParametersChecker.checkParameter("TapeDriveService is required", tapeDriveService);
        this.tapeDriveServicePool.put(tapeDriveService.getTapeDriveConf().getIndex(), tapeDriveService);
    }

    @Override
    public Set<Map.Entry<Integer, TapeDriveService>> drives() {
        return tapeDriveServicePool.entrySet();
    }

    @Override
    public String getLibraryIdentifier() {
        return libraryIdentifier;
    }
}
