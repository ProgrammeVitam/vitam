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
package fr.gouv.vitam.common.server.application.configuration;

import fr.gouv.vitam.common.ParametersChecker;

/**
 * Implementation of ServerIdentityConfiguration Interface
 */
public class ServerIdentityConfigurationImpl implements ServerIdentityConfiguration {

    private static final String CONFIGURATION_PARAMETERS = "ServerIdentityConfiguration parameters";
    private String identityName;
    private int identityPlatformId;
    private String identityRole;


    /**
     * ServerIdentityConfiguration empty constructor for YAMLFactory
     */
    public ServerIdentityConfigurationImpl() {
        // empty
    }

    /**
     * ServerIdentityConfiguration constructor
     * 
     * @param identityName database identity name
     * @param identityPlatformId identity platform id
     * @param identityRole identity role
     * @throws IllegalArgumentException if identityName or identityRole
     */
    public ServerIdentityConfigurationImpl(String identityName, int identityPlatformId, String identityRole) {
        ParametersChecker.checkParameter(CONFIGURATION_PARAMETERS,
            identityName, identityRole);
        if (identityPlatformId < 0) {
            throw new IllegalArgumentException("Platform id must be positive");
        }
        this.identityName = identityName;
        this.identityPlatformId = identityPlatformId;
        this.identityRole = identityRole;
    }

    @Override
    public String getIdentityName() {
        return identityName;
    }

    @Override
    public int getIdentityPlatformId() {
        return identityPlatformId;
    }

    @Override
    public String getIdentityRole() {
        return identityRole;
    }

    /**
     * @param identityName the identity Name to set
     * @return this
     * @throws IllegalArgumentException if identityName is null or empty
     */
    public ServerIdentityConfigurationImpl setIdentityName(String identityName) {
        ParametersChecker.checkParameter(CONFIGURATION_PARAMETERS,
            identityName);
        this.identityName = identityName;
        return this;
    }

    /**
     * @param identityName the identity Platform id to set
     * @return this
     * @throws IllegalArgumentException if identityPlatformId < 0
     */
    public ServerIdentityConfigurationImpl setIdentityPlatformId(int identityPlatformId) {
        if (identityPlatformId < 0) {
            throw new IllegalArgumentException("Platform id must be positive");
        }
        this.identityPlatformId = identityPlatformId;
        return this;
    }

    /**
     * @param identityName the identity Role to set
     * @return this
     * @throws IllegalArgumentException if identityRole is null or empty
     */
    public ServerIdentityConfigurationImpl setIdentityRole(String identityRole) {
        ParametersChecker.checkParameter(CONFIGURATION_PARAMETERS,
            identityRole);
        this.identityRole = identityRole;
        return this;
    }
}
