/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 * 
 * This software is a computer program whose purpose is to implement a digital 
 * archiving back-office system managing high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL 2.1
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL 2.1 license and that you accept its terms.
 */
package fr.gouv.vitam.ihmdemo.core.auth.shiro;

import java.io.File;
import java.io.IOException;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;


/**
 * Authentication Service Factory
 */
public class AuthenticationServiceFactory {
    
    private static AuthenticationServiceType defaultAuthenticationServiceType;
    private static final String CONFIGURATION_FILENAME = "shiro.ini";
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(AuthenticationServiceFactory.class);
    private static final AuthenticationServiceFactory AUTHENTICATION_SERVICE_FACTORY = new AuthenticationServiceFactory();

    private String iniFullPath;

    private AuthenticationServiceFactory() {
        changeConfigurationFile(CONFIGURATION_FILENAME);
    }

    /**
     * Set the AuthenticationServiceFactory configuration
     *
     * @param type
     * @param iniFullPath
     * @throws IllegalArgumentException
     *             if type null or if type is PRODUCTION and iniFullPath is null or
     *             empty
     */
    static final void setConfiguration(AuthenticationServiceType type, String iniFullPath) {

        ParametersChecker.checkParameter("AuthenticationServiceType cannot be null or empty", type);
        if (type == AuthenticationServiceType.PRODUCTION) {
            ParametersChecker.checkParameter("ini path cannot be null or empty", iniFullPath);
            AUTHENTICATION_SERVICE_FACTORY.iniFullPath=iniFullPath;
        }
        
        changeDefaultAuthenticationServiceType(type);
      }

    /**
     * Get the AuthenticationServiceFactory instance
     *
     * @return the instance
     */
    public static final AuthenticationServiceFactory getInstance() {
        return AUTHENTICATION_SERVICE_FACTORY;
    }

    /**
     * Get the default Authentication Service
     *
     * @return the default Authentication Service
     */
    public AuthenticationService getAuthenticationService() {
        AuthenticationService service;
        switch (defaultAuthenticationServiceType) {
        case MOCK:
            service = new AuthenticationServiceMock();
            break;
        case PRODUCTION:
            service = new AuthenticationServicePassword(iniFullPath);
            break;
        default:
            throw new IllegalArgumentException("Authentication Service type unknown");
        }
        return service;
    }

    /**
     * Modify the default Authentication Service Type
     *
     * @param type
     *            the Authentication Service type to set
     * @throws IllegalArgumentException
     *             if type null
     */
    static void changeDefaultAuthenticationServiceType(AuthenticationServiceType type) {
        defaultAuthenticationServiceType = type;
    }

    /**
     * get Absolute Path of configuration file
     *
     * @param configurationPath
     *            the path to the configuration file
     */
    public final void changeConfigurationFile(String configurationPath) {
        changeDefaultAuthenticationServiceType(AuthenticationServiceType.MOCK);
        File iniFile = null;
        try {
            iniFile= PropertiesUtils.findFile(configurationPath);
        } catch (final IOException fnf) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Error when retrieving configuration file %s, using mock",
                        CONFIGURATION_FILENAME), fnf);
            }
        }
        if (iniFile == null) {
            LOGGER.debug(
                    String.format("Error when retrieving configuration file %s, using mock", CONFIGURATION_FILENAME));
        } else {
            iniFullPath=iniFile.getAbsolutePath();
            changeDefaultAuthenticationServiceType(AuthenticationServiceType.PRODUCTION);
        }
    }

    /**
     * enum to define Authentication Service Type
     */
    public enum AuthenticationServiceType {
        /**
         * To use only in MOCK ACCESS
         */
        MOCK,
        /**
         * Use real service
         */
        PRODUCTION
    }

}
