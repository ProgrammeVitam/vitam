/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019) <p> contact.vitam@culture.gouv.fr <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently. <p> This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software. You can use, modify and/ or redistribute the software under
 * the terms of the CeCILL 2.1 license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". <p> As a counterpart to the access to the source code and rights to copy, modify and
 * redistribute granted by the license, users are provided only with a limited warranty and the software's author, the
 * holder of the economic rights, and the successive licensors have only limited liability. <p> In this respect, the
 * user's attention is drawn to the risks associated with loading, using, modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software, that may mean that it is complicated to
 * manipulate, and that also therefore means that it is reserved for developers and experienced professionals having
 * in-depth computer knowledge. Users are therefore encouraged to load and test the software's suitability as regards
 * their requirements in conditions enabling the security of their systems and/or data to be ensured and, more
 * generally, to use and operate it in the same conditions as regards security. <p> The fact that you are presently
 * reading this means that you have had knowledge of the CeCILL 2.1 license and that you accept its terms.
 */
package fr.gouv.vitam.logbook.common.server.database.collections;

import java.util.HashMap;
import java.util.Map;

import org.bson.Document;

import com.mongodb.client.MongoCollection;

import fr.gouv.vitam.common.database.api.impl.VitamElasticsearchRepository;
import fr.gouv.vitam.common.database.api.impl.VitamMongoRepository;
import fr.gouv.vitam.common.database.collections.VitamCollection;

/**
 * Reconstruction instance for instanciating mongoDB and elasticsearch repository.
 */
public class VitamRepositoryFactory implements VitamRepositoryProvider {

    private static VitamRepositoryFactory instance;

    private Map<LogbookCollections, VitamMongoRepository> mongoRepository;
    private Map<LogbookCollections, VitamElasticsearchRepository> esRepository;

    /**
     * Initialize mongo and es repositories from collections
     */
    private void initialize() {
        VitamCollection lifecycleObjectgroup = LogbookCollections.LIFECYCLE_OBJECTGROUP.getVitamCollection();
        VitamCollection lifecycleUnit = LogbookCollections.LIFECYCLE_UNIT.getVitamCollection();
        VitamCollection operation = LogbookCollections.OPERATION.getVitamCollection();

        mongoRepository.put(LogbookCollections.LIFECYCLE_OBJECTGROUP,
            new VitamMongoRepository((MongoCollection<Document>) lifecycleObjectgroup.getCollection()));
        mongoRepository.put(LogbookCollections.LIFECYCLE_UNIT,
            new VitamMongoRepository((MongoCollection<Document>) lifecycleUnit.getCollection()));
        mongoRepository.put(LogbookCollections.OPERATION,
            new VitamMongoRepository((MongoCollection<Document>) operation.getCollection()));

        esRepository.put(LogbookCollections.OPERATION,
            new VitamElasticsearchRepository(operation.getEsClient().getClient(), operation.getName().toLowerCase(),
                operation.isCreateIndexByTenant()));

    }

    /**
     * private constructor for instance initialization. <br />
     */
    private VitamRepositoryFactory() {
        mongoRepository = new HashMap<>();
        esRepository = new HashMap<>();
        initialize();
    }

    /**
     * get Thread-Safe instance instance. <br/>
     *
     * @return current instance of VitamRepositoryFactory, create if null
     */
    public static synchronized VitamRepositoryFactory getInstance() {
        if (instance == null) {
            instance = new VitamRepositoryFactory();
        }
        return instance;
    }

    @Override
    public VitamMongoRepository getVitamMongoRepository(LogbookCollections collection) {
        return mongoRepository.get(collection);
    }

    @Override
    public VitamElasticsearchRepository getVitamESRepository(LogbookCollections collection) {
        return esRepository.get(collection);
    }
}
