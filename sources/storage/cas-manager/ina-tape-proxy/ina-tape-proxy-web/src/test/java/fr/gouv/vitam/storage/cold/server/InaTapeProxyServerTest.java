/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.storage.cold.server;

import fr.gouv.vitam.common.serverv2.application.AdminApplication;
import fr.gouv.vitam.storage.cold.InaTapeProxyConfiguration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class InaTapeProxyServerTest {

    @Test
    void testServerStartWithValidConfiguration() throws Exception {
        String testConfigPath = getClass().getClassLoader().getResource("ina-tape-proxy-web.conf").getFile();
        InaTapeProxyServer server = new InaTapeProxyServer(
            InaTapeProxyConfiguration.class,
            testConfigPath,
            InaTapeProxyApplication.class,
            AdminApplication.class
        );

        assertNotNull(server, "Le serveur ne doit pas être null");

        // Démarrage du serveur (peut bloquer, donc attention)
        // Ici on ne fait pas un run complet pour ne pas bloquer le test
        server.start(); // Initialisation sans bloquer
        server.stop(); // Arrêt rapide
    }

    @Test
    void testServerFailsWithMissingConfiguration() {
        String invalidConfigPath = "nonexistent.yaml";

        assertThatThrownBy(
            () ->
                new InaTapeProxyServer(
                    InaTapeProxyConfiguration.class,
                    invalidConfigPath,
                    InaTapeProxyApplication.class,
                    AdminApplication.class
                )
        )
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot start the INA Tape Proxy Test Application Server");
    }
}
