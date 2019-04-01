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
package fr.gouv.vitam.storage.offers.tape;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.storage.tapelibrary.TapeLibraryConfiguration;
import fr.gouv.vitam.storage.engine.common.collection.OfferCollections;
import fr.gouv.vitam.storage.offers.tape.cas.BasicFileStorage;
import fr.gouv.vitam.storage.offers.tape.cas.BucketTopologyHelper;
import fr.gouv.vitam.storage.offers.tape.cas.InputFileToTarBuilder;
import fr.gouv.vitam.storage.offers.tape.cas.ObjectReferentialRepository;
import fr.gouv.vitam.storage.offers.tape.cas.TapeLibraryContentAddressableStorage;
import fr.gouv.vitam.storage.offers.tape.cas.TarReferentialRepository;
import fr.gouv.vitam.storage.offers.tape.cas.WriteOrderCreator;
import fr.gouv.vitam.storage.offers.tape.spec.QueueRepository;

import java.io.IOException;
import java.io.InputStream;

public class TapeStorageFactory {

    public static final String OFFER_TAPE_CONF = "offer-tape.conf";

    public TapeLibraryContentAddressableStorage initialize(MongoDbAccess mongoDbAccess) {
        TapeLibraryConfiguration configuration = loadConfiguration();
        BucketTopologyHelper bucketTopologyHelper = new BucketTopologyHelper(configuration.getTopology());

        ObjectReferentialRepository objectReferentialRepository =
            new ObjectReferentialRepository(mongoDbAccess.getMongoDatabase()
                .getCollection(OfferCollections.OFFER_OBJECT_REFERENTIAL.getName()));
        TarReferentialRepository tarReferentialRepository =
            new TarReferentialRepository(mongoDbAccess.getMongoDatabase()
                .getCollection(OfferCollections.OFFER_TAR_REFERENTIAL.getName()));

        TapeLibraryFactory tapeLibraryFactory = TapeLibraryFactory.getInstance();
        tapeLibraryFactory.initialize(configuration, mongoDbAccess);

        QueueRepository readWriteQueue = tapeLibraryFactory.getReadWriteQueue();

        // Change all running orders to ready state
        readWriteQueue.initializeOnBootstrap();

        WriteOrderCreator writeOrderCreator = new WriteOrderCreator(configuration, objectReferentialRepository,
            tarReferentialRepository, bucketTopologyHelper, readWriteQueue);
        writeOrderCreator.initializeOnBootstrap();

        BasicFileStorage basicFileStorage =
            new BasicFileStorage(configuration.getInputFileStorageFolder());
        InputFileToTarBuilder inputFileToTarBuilder =
            new InputFileToTarBuilder(configuration, basicFileStorage, bucketTopologyHelper,
                objectReferentialRepository, tarReferentialRepository, writeOrderCreator);
        inputFileToTarBuilder.initializeOnBootstrap();

        TapeLibraryContentAddressableStorage tapeLibraryContentAddressableStorage = new
            TapeLibraryContentAddressableStorage(basicFileStorage, objectReferentialRepository, inputFileToTarBuilder);

        // Everything's alright. Start listeners
        writeOrderCreator.startListener();
        inputFileToTarBuilder.startListeners();

        return tapeLibraryContentAddressableStorage;
    }

    private TapeLibraryConfiguration loadConfiguration() {
        try (final InputStream yamlIS = PropertiesUtils.getConfigAsStream(OFFER_TAPE_CONF)) {
            return PropertiesUtils.readYaml(yamlIS, TapeLibraryConfiguration.class);
        } catch (IOException ex) {
            throw new RuntimeException("Could not load offer tape configuration file " + OFFER_TAPE_CONF, ex);
        }
    }
}
