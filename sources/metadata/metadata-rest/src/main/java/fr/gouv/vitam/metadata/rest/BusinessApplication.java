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
package fr.gouv.vitam.metadata.rest;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.api.VitamRepositoryFactory;
import fr.gouv.vitam.common.database.offset.OffsetRepository;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.security.waf.SanityDynamicFeature;
import fr.gouv.vitam.common.server.application.GenericBusinessApplication;
import fr.gouv.vitam.common.serverv2.application.CommonBusinessApplication;
import fr.gouv.vitam.metadata.core.MetaDataImpl;
import fr.gouv.vitam.metadata.core.MongoDbAccessMetadataFactory;
import fr.gouv.vitam.metadata.core.config.ElasticsearchMetadataIndexManager;
import fr.gouv.vitam.metadata.core.config.MetaDataConfiguration;
import fr.gouv.vitam.metadata.core.config.MetaDataConfigurationValidator;
import fr.gouv.vitam.metadata.core.database.collections.MongoDbAccessMetadataImpl;
import fr.gouv.vitam.metadata.core.database.collections.PersistentIdentifierRepositoryImpl;
import fr.gouv.vitam.metadata.core.reconstruction.domain.OffsetManager;
import fr.gouv.vitam.metadata.core.reconstruction.domain.PersistentIdentifierReconstructionManager;
import fr.gouv.vitam.metadata.core.reconstruction.repository.OperationReportRepository;
import fr.gouv.vitam.metadata.core.reconstruction.repository.PersistentIdentifierRepository;
import fr.gouv.vitam.metadata.core.reconstruction.repository.ReconstructionOperationRepository;
import fr.gouv.vitam.metadata.core.reconstruction.repository.impl.OperationReportRepositoryImpl;
import fr.gouv.vitam.metadata.core.reconstruction.repository.impl.ReconstructionOperationRepositoryImpl;
import fr.gouv.vitam.metadata.core.rules.MetadataRuleService;
import jakarta.servlet.ServletConfig;
import jakarta.ws.rs.core.Context;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import static fr.gouv.vitam.common.serverv2.application.ApplicationParameter.CONFIGURATION_FILE_APPLICATION;

/**
 * Metadata resources and filter
 */
public class BusinessApplication extends GenericBusinessApplication {

    private final CommonBusinessApplication commonBusinessApplication;

    private final Set<Object> singletons;

    /**
     * Constructor
     *
     * @param servletConfig the servlet configuration
     */
    public BusinessApplication(@Context ServletConfig servletConfig) {
        String configurationFile = servletConfig.getInitParameter(CONFIGURATION_FILE_APPLICATION);

        try (final InputStream yamlIS = PropertiesUtils.getConfigAsStream(configurationFile)) {
            MetaDataConfiguration metaDataConfiguration = PropertiesUtils.readYaml(yamlIS, MetaDataConfiguration.class);
            commonBusinessApplication = new CommonBusinessApplication();

            // Validate configuration
            MetaDataConfigurationValidator.validateConfiguration(metaDataConfiguration);

            // Elasticsearch configuration
            ElasticsearchMetadataIndexManager indexManager = new ElasticsearchMetadataIndexManager(
                metaDataConfiguration,
                VitamConfiguration.getTenants()
            );

            MongoDbAccessMetadataImpl mongoAccessMetadata = MongoDbAccessMetadataFactory.create(
                metaDataConfiguration,
                indexManager
            );

            OffsetRepository offsetRepository = new OffsetRepository(mongoAccessMetadata);

            VitamRepositoryFactory vitamRepositoryProvider = VitamRepositoryFactory.get();

            MetaDataImpl metadata = MetaDataImpl.newMetadata(
                mongoAccessMetadata,
                VitamConfiguration.getOntologyCacheMaxEntries(),
                VitamConfiguration.getOntologyCacheTimeoutInSeconds(),
                indexManager,
                metaDataConfiguration
            );

            MetadataRuleService metadataRuleService = new MetadataRuleService(metadata);
            MetadataResource metaDataResource = new MetadataResource(
                metadata,
                metadataRuleService,
                metaDataConfiguration
            );
            MetadataRawResource metadataRawResource = new MetadataRawResource(vitamRepositoryProvider);
            MetadataManagementResource metadataManagementResource = new MetadataManagementResource(
                vitamRepositoryProvider,
                metadata,
                metaDataConfiguration,
                indexManager
            );
            MetadataReconstructionResource metadataReconstructionResource = new MetadataReconstructionResource(
                vitamRepositoryProvider,
                offsetRepository,
                metaDataConfiguration,
                indexManager
            );
            final MetadataAuditResource metadataAuditResource = new MetadataAuditResource(metaDataConfiguration);

            final PersistentIdentifierRepository persistentIdentifierRepository =
                new PersistentIdentifierRepositoryImpl(mongoAccessMetadata);
            final OperationReportRepository operationReportRepository = new OperationReportRepositoryImpl();
            final ReconstructionOperationRepository reconstructionOperationRepository =
                new ReconstructionOperationRepositoryImpl();
            final PersistentIdentifierReconstructionManager persistentIdentifierReconstructionManager =
                new PersistentIdentifierReconstructionManager(
                    operationReportRepository,
                    reconstructionOperationRepository,
                    metaDataConfiguration,
                    persistentIdentifierRepository
                );
            final OffsetManager offsetManager = new OffsetManager(offsetRepository);
            final PersistentIdentifierResource persistentIdentifierResource = new PersistentIdentifierResource(
                persistentIdentifierReconstructionManager,
                offsetManager,
                metaDataConfiguration,
                persistentIdentifierRepository
            );

            singletons = new HashSet<>();
            singletons.addAll(commonBusinessApplication.getResources());
            singletons.add(metaDataResource);
            singletons.add(metadataRawResource);
            singletons.add(metadataManagementResource);
            singletons.add(metadataReconstructionResource);
            singletons.add(metadataAuditResource);
            singletons.add(persistentIdentifierResource);
            singletons.add(new SanityDynamicFeature());
        } catch (IOException e) {
            throw new VitamRuntimeException(e);
        }
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }
}
