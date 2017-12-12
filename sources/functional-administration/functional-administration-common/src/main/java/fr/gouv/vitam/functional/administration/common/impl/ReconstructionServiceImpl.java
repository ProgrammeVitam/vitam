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
package fr.gouv.vitam.functional.administration.common.impl;

import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;

import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.database.api.impl.VitamElasticsearchRepository;
import fr.gouv.vitam.common.database.api.impl.VitamMongoRepository;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.functional.administration.common.CollectionBackupModel;
import fr.gouv.vitam.functional.administration.common.ReconstructionFactory;
import fr.gouv.vitam.functional.administration.common.api.ReconstructionService;
import fr.gouv.vitam.functional.administration.common.api.RestoreBackupService;
import fr.gouv.vitam.functional.administration.common.server.AdminManagementConfiguration;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;

/**
 * Reconstrution of Vitam Collections.<br>
 */
public class ReconstructionServiceImpl implements ReconstructionService {

    /**
     * Vitam Logger.
     */
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ReconstructionServiceImpl.class);

    private static final String STRATEGY_ID = "default";

    private AdminManagementConfiguration adminManagementConfig;

    private VitamMongoRepository mongoRepository;
    private VitamElasticsearchRepository elasticsearchRepository;
    private RestoreBackupService recoverBuckupService;

    private ReconstructionFactory repositoryFactory;

    public ReconstructionServiceImpl(AdminManagementConfiguration adminManagementConfig, ReconstructionFactory repositoryFactory) {
        this.adminManagementConfig = adminManagementConfig;
        this.repositoryFactory = repositoryFactory;
    }

    @VisibleForTesting
    public ReconstructionServiceImpl(AdminManagementConfiguration adminManagementConfig, VitamMongoRepository mongoRepository, VitamElasticsearchRepository elasticsearchRepository,
        RestoreBackupService recoverBuckupService, ReconstructionFactory reconstructionFactory) {
        this(adminManagementConfig, reconstructionFactory);
        this.mongoRepository = mongoRepository;
        this.elasticsearchRepository = elasticsearchRepository;
        this.recoverBuckupService = recoverBuckupService;
    }

    @PostConstruct
    public void init(){
        if (recoverBuckupService == null) {
            recoverBuckupService = new RestoreBackupServiceImpl();
        }
    }

    /**
     * purge collection content and reconstruct the content.
     *
     * @param collection the collection to reconstruct.
     * @param tenants    the given tenant.
     */
    @Override
    public void reconstruct(FunctionalAdminCollections collection, Integer... tenants) throws DatabaseException {

        ParametersChecker.checkParameter("All parameters [%s, %s] are required.", collection, tenants);
        LOGGER.debug(String
            .format("Start reconstruction of the %s collection on the Vitam tenant %s.", collection.getType(),
                tenants));

        mongoRepository = repositoryFactory.getVitamMongoRepository(collection);
        elasticsearchRepository = repositoryFactory.getVitamESRepository(collection);

        // purge collection content
        for (Integer tenant : tenants) {
            mongoRepository.purge(tenant);
            elasticsearchRepository.purge(tenant);

            // get the last version of the backup copies.
            Optional<CollectionBackupModel> collectionBackup =
                recoverBuckupService.readLatestSavedFile(STRATEGY_ID, collection, tenant);

            // reconstruct Vitam collection from the backup copy.
            if (collectionBackup.isPresent()) {
                LOGGER.debug(String.format("Last backup copy version : %s", collectionBackup));

                // saving the backup collection on mongoDB and elasticSearch.
                mongoRepository.save(collectionBackup.get().getCollections());
                elasticsearchRepository.save(collectionBackup.get().getCollections());

                // saving the backup sequence on mongoDB and elasticSearch.
                mongoRepository.save(collectionBackup.get().getSequence());
                elasticsearchRepository.save(collectionBackup.get().getSequence());

                // log the recontruction of Vitam collection.
                LOGGER.debug(String
                    .format("[Reconstruction]: the collection {%s} has been reconstructed on the tenants {%s} at %s",
                        collectionBackup, tenants, LocalDateUtil.now()));
            }
        }
    }

    @Override
    public void reconstruct(FunctionalAdminCollections collection) throws DatabaseException {

        ParametersChecker.checkParameter("The collection parameter is required.", collection);
        LOGGER.debug(String
            .format("Start reconstruction of the %s collection on all of the Vitam tenants.", collection.getType()));

        // get the list of vitam tenants from the configuration.
        List<Integer> tenants = adminManagementConfig.getTenants();

        // reconstruct all the Vitam tenants from the backup copy.
        if (null != tenants && !tenants.isEmpty()) {
            LOGGER.debug(String.format("Reconstruction of %s Vitam tenants", tenants.size()));
            // reconstruction of the list of tenants
            reconstruct(collection, tenants.stream().toArray(Integer[]::new));
        }
    }

}
