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
package fr.gouv.vitam.common.server.application.configuration;

import fr.gouv.vitam.common.ParametersChecker;

import java.util.List;

/**
 * Implementation of DbConfiguraton Interface
 */
public class DbConfigurationImpl extends DefaultVitamApplicationConfiguration implements DbConfiguration {

    private static final String PORT_MUST_BE_POSITIVE = "Port must be positive";
    private static final String CONFIGURATION_PARAMETERS = "DbConfiguration parameters";
    private List<MongoDbNode> mongoDbNodes;
    private String dbName;
    private boolean dbAuthentication = false;
    private String dbUserName;
    private String dbPassword;

    /**
     * DbConfiguration empty constructor for YAMLFactory
     */
    public DbConfigurationImpl() {
        // empty
    }

    /**
     * DbConfiguration constructor with authentication
     *
     * @param mongoDbNodes database server IP address and port
     * @param dbName database name
     * @param dbAuthentication
     * @param dbUserName
     * @param dbPassword
     * @throws IllegalArgumentException if host or dbName null or empty, or if port &lt;= 0
     */
    public DbConfigurationImpl(
        List<MongoDbNode> mongoDbNodes,
        String dbName,
        boolean dbAuthentication,
        String dbUserName,
        String dbPassword
    ) {
        for (final MongoDbNode node : mongoDbNodes) {
            ParametersChecker.checkParameter(CONFIGURATION_PARAMETERS, node.getDbHost(), dbName);
            if (node.getDbPort() <= 0) {
                throw new IllegalArgumentException(PORT_MUST_BE_POSITIVE);
            }
        }
        this.mongoDbNodes = mongoDbNodes;
        this.dbName = dbName;
        this.dbAuthentication = dbAuthentication;
        this.dbUserName = dbUserName;
        this.dbPassword = dbPassword;
    }

    /**
     * DbConfiguration constructor
     *
     * @param mongoDbNodes database server IP address and port
     * @param dbName database name
     * @throws IllegalArgumentException if host or dbName null or empty, or if port &lt;= 0
     */
    public DbConfigurationImpl(List<MongoDbNode> mongoDbNodes, String dbName) {
        for (final MongoDbNode node : mongoDbNodes) {
            ParametersChecker.checkParameter(CONFIGURATION_PARAMETERS, node.getDbHost(), dbName);
            if (node.getDbPort() <= 0) {
                throw new IllegalArgumentException(PORT_MUST_BE_POSITIVE);
            }
        }
        this.mongoDbNodes = mongoDbNodes;
        this.dbName = dbName;
        dbAuthentication = false;
    }

    @Override
    public List<MongoDbNode> getMongoDbNodes() {
        return mongoDbNodes;
    }

    @Override
    public String getDbName() {
        return dbName;
    }

    @Override
    public String getDbUserName() {
        return dbUserName;
    }

    @Override
    public String getDbPassword() {
        return dbPassword;
    }

    @Override
    public boolean isDbAuthentication() {
        return dbAuthentication;
    }

    /**
     * @param mongoDbNodes to set
     * @return this
     * @throws IllegalArgumentException if dbHost is null or empty
     */
    public DbConfigurationImpl setMongoDbNodes(List<MongoDbNode> mongoDbNodes) {
        for (final MongoDbNode node : mongoDbNodes) {
            ParametersChecker.checkParameter(CONFIGURATION_PARAMETERS, node.getDbHost());
            if (node.getDbPort() <= 0) {
                throw new IllegalArgumentException(PORT_MUST_BE_POSITIVE);
            }
        }
        this.mongoDbNodes = mongoDbNodes;
        return this;
    }

    /**
     * @param dbName the Db Name to set
     * @return this
     * @throws IllegalArgumentException if dbName is null or empty
     */
    public DbConfigurationImpl setDbName(String dbName) {
        ParametersChecker.checkParameter(CONFIGURATION_PARAMETERS, dbName);
        this.dbName = dbName;
        return this;
    }

    /**
     * @param userName
     * @return MetaDataConfiguration
     */
    public DbConfigurationImpl setDbUserName(String userName) {
        dbUserName = userName;
        return this;
    }

    /**
     * @param password
     * @return MetaDataConfiguration
     */
    public DbConfigurationImpl setDbPassword(String password) {
        dbPassword = password;
        return this;
    }

    /**
     * @param authentication
     * @return MetaDataConfiguration
     */
    public DbConfigurationImpl setDbAuthentication(boolean authentication) {
        dbAuthentication = authentication;
        return this;
    }
}
