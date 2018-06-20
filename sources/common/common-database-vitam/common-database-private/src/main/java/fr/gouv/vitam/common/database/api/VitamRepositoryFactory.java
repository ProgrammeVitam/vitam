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
package fr.gouv.vitam.common.database.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.api.impl.VitamElasticsearchRepository;
import fr.gouv.vitam.common.database.api.impl.VitamMongoRepository;
import fr.gouv.vitam.common.database.collections.VitamCollection;

/**
 * Reconstruction instance for instanciating mongoDB and elasticsearch repository.
 */
public class VitamRepositoryFactory implements VitamRepositoryProvider {

    private static VitamRepositoryFactory instance;

    private static Map<String, VitamMongoRepository> mongoRepository;
    private static Map<String, VitamElasticsearchRepository> esRepository;

    public static synchronized VitamRepositoryFactory initialize(List<VitamCollection> collections) {
        if (instance == null) {
            instance = new VitamRepositoryFactory();
        }

        for (VitamCollection collection : collections) {
            String collectionName = collection.getName();
            if (mongoRepository.containsKey(collectionName)) {
                continue;
            }

            mongoRepository.put(collectionName, new VitamMongoRepository(collection.getTypedCollection()));

            if (null != collection.getEsClient()) {
                esRepository.put(collectionName,
                    new VitamElasticsearchRepository(collection.getEsClient().getClient(),
                        collectionName.toLowerCase(), collection.isCreateIndexByTenant()));
            }
        }

        return instance;
    }

    /**
     * private constructor for instance initialization. <br />
     */
    private VitamRepositoryFactory() {
        mongoRepository = new HashMap<>();
        esRepository = new HashMap<>();
    }


    /**
     * get Thread-Safe instance instance. <br/>
     *
     * @return current instance of VitamRepositoryFactory, create if null
     */
    public static synchronized VitamRepositoryFactory get() {
        ParametersChecker.checkParameter("VitamRepositoryFactory should be instantiated at startup", instance);
        return instance;
    }

    /**
     * Used only for tests
     *
     * @param mongoRepository
     * @param esRepository
     * @return
     */
    @VisibleForTesting
    public static synchronized VitamRepositoryFactory get(
        Map<String, VitamMongoRepository> mongoRepository,
        Map<String, VitamElasticsearchRepository> esRepository) {
        instance = new VitamRepositoryFactory();
        instance.mongoRepository = mongoRepository;
        instance.esRepository = esRepository;
        return instance;
    }

    @Override
    public VitamMongoRepository getVitamMongoRepository(String collectionName) {
        return mongoRepository.get(collectionName);
    }

    @Override
    public VitamElasticsearchRepository getVitamESRepository(String collectionName) {
        return esRepository.get(collectionName);
    }

}
