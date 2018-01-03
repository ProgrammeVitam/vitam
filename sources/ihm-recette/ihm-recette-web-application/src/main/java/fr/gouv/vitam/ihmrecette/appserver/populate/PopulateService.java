/**
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
package fr.gouv.vitam.ihmrecette.appserver.populate;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

import java.util.concurrent.atomic.AtomicBoolean;

public class PopulateService {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(PopulateService.class);

    private AtomicBoolean populateInProgress = new AtomicBoolean(false);

    private final MetadataRepository metadataRepository;

    private UnitGraph unitGraph;

    public PopulateService(MetadataRepository metadataRepository, DescriptiveMetadataGenerator descriptiveMetadataGenerator,
        UnitGraph unitGraph) {
        this.metadataRepository = metadataRepository;
        this.unitGraph = unitGraph;
    }

    /**
     * Populate vitam with data using populateMode
     * 
     * @param populateModel config to use
     */
    public void populateVitam(PopulateModel populateModel) {

        if (populateInProgress.get()) {
            return;
        }

        populateInProgress.set(true);

        Observable.range(0, populateModel.getNumberOfUnit())
            .observeOn(Schedulers.computation())
            .map(i -> unitGraph
                .createGraph(i, populateModel.getRootId(), populateModel.getTenant(), populateModel.getSp(), 
                        populateModel.isWithGots()))
            .buffer(populateModel.getBulkSize())
            .subscribe(unitGotList -> metadataRepository.store(populateModel.getTenant(), unitGotList), t -> {
                LOGGER.error(t);
                populateInProgress.set(false);
            }, () -> populateInProgress.set(false));
    }

    /**
     * Check if a populating task is in progress
     * 
     * @return true if there is a populating task in progress
     */
    public boolean inProgress() {
        return populateInProgress.get();
    }

}
