/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.ihmrecette.appserver;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.client.MongoDatabase;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.collections.CachedOntologyLoader;
import fr.gouv.vitam.common.database.collections.VitamCollection;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchAccess;
import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.database.server.mongodb.MongoDbAccess;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.serverv2.application.CommonBusinessApplication;
import fr.gouv.vitam.functional.administration.common.client.FunctionAdministrationOntologyLoader;
import fr.gouv.vitam.ihmdemo.common.pagination.PaginationHelper;
import fr.gouv.vitam.ihmdemo.core.DslQueryHelper;
import fr.gouv.vitam.ihmdemo.core.UserInterfaceTransactionManager;
import fr.gouv.vitam.ihmrecette.appserver.applicativetest.ApplicativeTestResource;
import fr.gouv.vitam.ihmrecette.appserver.applicativetest.ApplicativeTestService;
import fr.gouv.vitam.ihmrecette.appserver.performance.PerformanceResource;
import fr.gouv.vitam.ihmrecette.appserver.performance.PerformanceService;
import fr.gouv.vitam.ihmrecette.appserver.populate.LogbookRepository;
import fr.gouv.vitam.ihmrecette.appserver.populate.MasterdataRepository;
import fr.gouv.vitam.ihmrecette.appserver.populate.MetadataRepository;
import fr.gouv.vitam.ihmrecette.appserver.populate.MetadataStorageService;
import fr.gouv.vitam.ihmrecette.appserver.populate.PopulateResource;
import fr.gouv.vitam.ihmrecette.appserver.populate.PopulateService;
import fr.gouv.vitam.ihmrecette.appserver.populate.StoragePopulateImpl;
import fr.gouv.vitam.ihmrecette.appserver.populate.UnitGraph;
import fr.gouv.vitam.storage.engine.server.rest.StorageConfiguration;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import javax.servlet.ServletConfig;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static fr.gouv.vitam.common.serverv2.application.ApplicationParameter.CONFIGURATION_FILE_APPLICATION;
import static java.lang.String.format;

/**
 * Business Application for ihm recette declaring resources and filters
 */
public class BusinessApplication extends Application {

    private static final String STORAGE_CONF_FILE = "storage.conf";

    private final CommonBusinessApplication commonBusinessApplication;

    private Set<Object> singletons;

    /**
     * BusinessApplication Constructor
     *
     * @param servletConfig the servlet configuration
     */
    public BusinessApplication(@Context ServletConfig servletConfig) {
        String configurationFile = servletConfig.getInitParameter(CONFIGURATION_FILE_APPLICATION);

        try (final InputStream yamlIS = PropertiesUtils.getConfigAsStream(configurationFile)) {
            final WebApplicationConfig configuration =
                PropertiesUtils.readYaml(yamlIS, WebApplicationConfig.class);

            commonBusinessApplication = new CommonBusinessApplication();
            singletons = new HashSet<>();
            singletons.addAll(commonBusinessApplication.getResources());

            Path sipDirectory = Paths.get(configuration.getSipDirectory());
            Path reportDirectory = Paths.get(configuration.getPerformanceReportDirectory());

            if (!sipDirectory.toFile().exists()) {
                throw new FileNotFoundException(String.format("directory %s does not exist", sipDirectory));
            }

            if (!reportDirectory.toFile().exists()) {
                throw new FileNotFoundException(format("directory %s does not exist", reportDirectory));
            }

            PerformanceService performanceService = new PerformanceService(sipDirectory, reportDirectory);
            singletons.add(new PerformanceResource(performanceService));

            String testSystemSipDirectory = configuration.getTestSystemSipDirectory();
            String testSystemReportDirectory = configuration.getTestSystemReportDirectory();
            ApplicativeTestService applicativeTestService =
                new ApplicativeTestService(Paths.get(testSystemReportDirectory));

            singletons.add(new ApplicativeTestResource(applicativeTestService, testSystemSipDirectory));

            MongoClientOptions mongoClientOptions = VitamCollection.getMongoClientOptions();
            MongoClient mongoClient = MongoDbAccess.createMongoClient(configuration, mongoClientOptions);
            MongoDatabase metadataDb = mongoClient.getDatabase(configuration.getMetadataDbName());
            MongoDatabase masterdataDb = mongoClient.getDatabase(configuration.getMasterdataDbName());
            MongoDatabase logbookDb = mongoClient.getDatabase(configuration.getLogbookDbName());
            List<ElasticsearchNode> elasticsearchNodes = configuration.getElasticsearchNodes();
            Settings settings = ElasticsearchAccess.getSettings(configuration.getClusterName());
            TransportClient esClient = getClient(settings, elasticsearchNodes);

            StoragePopulateImpl storagePopulateService;
            try (final InputStream storageYamlIS = PropertiesUtils.getConfigAsStream(STORAGE_CONF_FILE)) {
                final StorageConfiguration storageConfiguration =
                    PropertiesUtils.readYaml(storageYamlIS, StorageConfiguration.class);
                storagePopulateService = new StoragePopulateImpl(storageConfiguration);
            }


            MetadataRepository metadataRepository = new MetadataRepository(metadataDb, esClient, storagePopulateService);
            MasterdataRepository masterdataRepository = new MasterdataRepository(masterdataDb, esClient);
            LogbookRepository logbookRepository = new LogbookRepository(logbookDb);
            MetadataStorageService metadataStorageService = new MetadataStorageService(metadataRepository, logbookRepository, storagePopulateService);
            UnitGraph unitGraph = new UnitGraph(metadataRepository);
            PopulateService populateService =
                new PopulateService(metadataRepository, masterdataRepository, logbookRepository, unitGraph,
                    configuration.getIngestMaxThread(), metadataStorageService);
            PopulateResource populateResource = new PopulateResource(populateService);

            singletons.add(populateResource);

            CachedOntologyLoader ontologyLoader = new CachedOntologyLoader(
                VitamConfiguration.getOntologyCacheMaxEntries(),
                VitamConfiguration.getOntologyCacheTimeoutInSeconds(),
                new FunctionAdministrationOntologyLoader()
            );

            final WebApplicationResourceDelete deleteResource = new WebApplicationResourceDelete(configuration, ontologyLoader);
            final WebApplicationResource resource =
                    new WebApplicationResource(configuration, UserInterfaceTransactionManager.getInstance(),
                            PaginationHelper.getInstance(), DslQueryHelper.getInstance(), populateService);
            singletons.add(deleteResource);
            singletons.add(resource);

        } catch (IOException | VitamException e) {
            throw new VitamRuntimeException(e);
        }
    }

    private TransportClient getClient(Settings settings, List<ElasticsearchNode> nodes) throws VitamException {
        try (final TransportClient clientNew = new PreBuiltTransportClient(settings)) {
            for (final ElasticsearchNode node : nodes) {
                clientNew.addTransportAddress(
                    new TransportAddress(InetAddress.getByName(node.getHostName()), node.getTcpPort()));
            }
            return clientNew;
        } catch (final UnknownHostException e) {
            throw new VitamException(e);
        }
    }


    @Override
    public Set<Class<?>> getClasses() {
        return commonBusinessApplication.getClasses();
    }

    @Override
    public Set<Object> getSingletons() {
        return singletons;
    }

}
