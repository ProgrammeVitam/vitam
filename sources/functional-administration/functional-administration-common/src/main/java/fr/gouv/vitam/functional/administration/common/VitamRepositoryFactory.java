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
package fr.gouv.vitam.functional.administration.common;

import java.util.HashMap;
import java.util.Map;

import com.mongodb.client.MongoCollection;
import fr.gouv.vitam.common.database.api.impl.VitamElasticsearchRepository;
import fr.gouv.vitam.common.database.api.impl.VitamMongoRepository;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import org.bson.Document;

/**
 * Reconstruction instance for instanciating mongoDB and elasticsearch repository.
 */
public class VitamRepositoryFactory implements ReconstructionFactory {

    private static VitamRepositoryFactory instance;

    private Map<FunctionalAdminCollections, VitamMongoRepository> mongoRepository;
    private Map<FunctionalAdminCollections, VitamElasticsearchRepository> esRepository;

    private void initialize() {
        VitamCollection formats = FunctionalAdminCollections.FORMATS.getVitamCollection();
        VitamCollection rules = FunctionalAdminCollections.RULES.getVitamCollection();
        VitamCollection agencies = FunctionalAdminCollections.AGENCIES.getVitamCollection();
        VitamCollection profile = FunctionalAdminCollections.PROFILE.getVitamCollection();
        VitamCollection securityProfile = FunctionalAdminCollections.SECURITY_PROFILE.getVitamCollection();
        VitamCollection ingestContract = FunctionalAdminCollections.INGEST_CONTRACT.getVitamCollection();
        VitamCollection accessContract = FunctionalAdminCollections.ACCESS_CONTRACT.getVitamCollection();
        VitamCollection accessionRegisterSummary =
            FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getVitamCollection();
        VitamCollection accessionRegisterDetail =
            FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getVitamCollection();
        VitamCollection context = FunctionalAdminCollections.CONTEXT.getVitamCollection();

        mongoRepository.put(FunctionalAdminCollections.FORMATS,
            new VitamMongoRepository((MongoCollection<Document>) formats.getCollection()));
        mongoRepository.put(FunctionalAdminCollections.RULES,
            new VitamMongoRepository((MongoCollection<Document>) rules.getCollection()));
        mongoRepository.put(FunctionalAdminCollections.PROFILE,
            new VitamMongoRepository((MongoCollection<Document>) profile.getCollection()));
        mongoRepository.put(FunctionalAdminCollections.AGENCIES,
            new VitamMongoRepository((MongoCollection<Document>) agencies.getCollection()));
        mongoRepository.put(FunctionalAdminCollections.ACCESS_CONTRACT,
            new VitamMongoRepository((MongoCollection<Document>) accessContract.getCollection()));
        mongoRepository.put(FunctionalAdminCollections.SECURITY_PROFILE,
            new VitamMongoRepository((MongoCollection<Document>) securityProfile.getCollection()));
        mongoRepository.put(FunctionalAdminCollections.INGEST_CONTRACT,
            new VitamMongoRepository((MongoCollection<Document>) ingestContract.getCollection()));
        mongoRepository.put(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY,
            new VitamMongoRepository((MongoCollection<Document>) accessionRegisterSummary.getCollection()));
        mongoRepository.put(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL,
            new VitamMongoRepository((MongoCollection<Document>) accessionRegisterDetail.getCollection()));
        mongoRepository.put(FunctionalAdminCollections.CONTEXT,
            new VitamMongoRepository((MongoCollection<Document>) context.getCollection()));

        esRepository.put(FunctionalAdminCollections.RULES,
            new VitamElasticsearchRepository(rules.getEsClient().getClient(), rules.getName(),
                rules.isCreateIndexByTenant()));
        esRepository.put(FunctionalAdminCollections.FORMATS,
            new VitamElasticsearchRepository(formats.getEsClient().getClient(), formats.getName(),
                formats.isCreateIndexByTenant()));
        esRepository.put(FunctionalAdminCollections.PROFILE,
            new VitamElasticsearchRepository(profile.getEsClient().getClient(), profile.getName(),
                profile.isCreateIndexByTenant()));
        esRepository.put(FunctionalAdminCollections.AGENCIES,
            new VitamElasticsearchRepository(agencies.getEsClient().getClient(), agencies.getName(),
                agencies.isCreateIndexByTenant()));
        esRepository.put(FunctionalAdminCollections.ACCESS_CONTRACT,
            new VitamElasticsearchRepository(accessContract.getEsClient().getClient(), accessContract.getName(),
                accessContract.isCreateIndexByTenant()));
        esRepository.put(FunctionalAdminCollections.SECURITY_PROFILE,
            new VitamElasticsearchRepository(securityProfile.getEsClient().getClient(), securityProfile.getName(),
                securityProfile.isCreateIndexByTenant()));
        esRepository.put(FunctionalAdminCollections.INGEST_CONTRACT,
            new VitamElasticsearchRepository(ingestContract.getEsClient().getClient(), ingestContract.getName(),
                ingestContract.isCreateIndexByTenant()));
        esRepository.put(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY,
            new VitamElasticsearchRepository(accessionRegisterSummary.getEsClient().getClient(),
                accessionRegisterSummary.getName(), accessionRegisterSummary.isCreateIndexByTenant()));
        esRepository.put(FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL,
            new VitamElasticsearchRepository(accessionRegisterDetail.getEsClient().getClient(),
                accessionRegisterDetail.getName(), accessionRegisterDetail.isCreateIndexByTenant()));
        esRepository.put(FunctionalAdminCollections.CONTEXT,
            new VitamElasticsearchRepository(context.getEsClient().getClient(), context.getName(),
                context.isCreateIndexByTenant()));
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
     * @return
     */
    public static synchronized VitamRepositoryFactory getInstance() {
        if (instance == null) {
            instance = new VitamRepositoryFactory();
        }
        return instance;
    }

    public VitamMongoRepository getVitamMongoRepository(FunctionalAdminCollections collection) {
        return mongoRepository.get(collection);
    }

    public VitamElasticsearchRepository getVitamESRepository(FunctionalAdminCollections collection) {
        return esRepository.get(collection);
    }
}
