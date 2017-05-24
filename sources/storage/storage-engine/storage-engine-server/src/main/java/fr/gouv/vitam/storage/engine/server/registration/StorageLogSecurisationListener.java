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
package fr.gouv.vitam.storage.engine.server.registration;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.storage.engine.server.rest.StorageConfiguration;
import fr.gouv.vitam.storage.engine.server.rest.StorageResource;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Listener used for regitration
 */
public class StorageLogSecurisationListener implements ServletContextListener {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(StorageLogSecurisationListener.class);
    private final StorageConfiguration configuration;
    private final StorageResource storageResource ;
    public StorageLogSecurisationListener(
        StorageResource storageResource, StorageConfiguration configuration) {
        this.configuration = configuration;
        this.storageResource = storageResource ;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOGGER.debug("ServletContextListener started");
        //TODO RETRY TO Secure LOG When Server not shutdown correctly Or securisation fail
    }
    //FIME  secure logs after rebooting server
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LOGGER.debug("ServletContextListener destroyed");
        try {
            for ( Integer tenant : configuration.getTenants()) {
                storageResource.getStorageLogbookService().stopAppenderLoggerAndSecureLastLogs(tenant);
            }
        } catch (final Exception e) {
            LOGGER.error("Fail to Backup Storage log " , e);
        }
    }

}
