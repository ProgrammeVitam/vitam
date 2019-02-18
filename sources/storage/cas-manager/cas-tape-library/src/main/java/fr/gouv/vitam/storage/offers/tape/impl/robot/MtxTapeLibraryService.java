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
package fr.gouv.vitam.storage.offers.tape.impl.robot;

import com.google.common.collect.Lists;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.storage.tapelibrary.TapeRebotConf;
import fr.gouv.vitam.storage.offers.tape.dto.CommandResponse;
import fr.gouv.vitam.storage.offers.tape.dto.TapeLibraryState;
import fr.gouv.vitam.storage.offers.tape.process.Output;
import fr.gouv.vitam.storage.offers.tape.process.ProcessExecutor;
import fr.gouv.vitam.storage.offers.tape.spec.TapeLoadUnloadService;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MtxTapeLibraryService implements TapeLoadUnloadService {
    public static final String F = "-f";
    public static final String UNLOAD = "unload";
    public static final String LOAD = "load";
    public static final String STATUS = "status";
    private final Lock lock = new ReentrantLock();

    private final TapeRebotConf tapeRebotConf;
    private final ProcessExecutor processExecutor;

    public MtxTapeLibraryService(TapeRebotConf tapeRebotConf, ProcessExecutor processExecutor) {
        this.tapeRebotConf = tapeRebotConf;
        this.processExecutor = processExecutor;
    }

    @Override
    public TapeLibraryState status(long timeoutInMillisecondes) {
        List<String> args = Lists.newArrayList(F, tapeRebotConf.getDevice(), STATUS);
        Output output = getExecutor().execute(tapeRebotConf.getMtxPath(), timeoutInMillisecondes, args);
        return parse(output, TapeLibraryState.class);
    }

    @Override
    public CommandResponse loadTape(long timeoutInMillisecondes, String tapeIndex, String driveIndex) {
        ParametersChecker.checkParameter("Arguments tapeIndex and deriveIndex are required", tapeIndex, driveIndex);

        List<String> args = Lists.newArrayList(F, tapeRebotConf.getDevice(), LOAD, tapeIndex, driveIndex);
        Output output = getExecutor().execute(tapeRebotConf.getMtxPath(), timeoutInMillisecondes, args);

        return parse(output, CommandResponse.class);
    }

    @Override
    public CommandResponse unloadTape(long timeoutInMillisecondes, String tapeIndex, String driveIndex) {
        ParametersChecker.checkParameter("Arguments tapeIndex and deriveIndex are required", tapeIndex, driveIndex);

        List<String> args = Lists.newArrayList(F, tapeRebotConf.getDevice(), UNLOAD, tapeIndex, driveIndex);
        Output output = getExecutor().execute(tapeRebotConf.getMtxPath(), timeoutInMillisecondes, args);

        return parse(output, CommandResponse.class);
    }


    @Override
    public ProcessExecutor getExecutor() {
        return processExecutor;
    }

    @Override
    public boolean begin() {
        return lock.tryLock();
    }

    @Override
    public void end() {
        lock.unlock();
    }

    @Override
    public <T> T parse(Output output, Class<T> clazz) {
        return null;
    }
}
