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

package fr.gouv.vitam.scheduler.server.util;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.digest.Digest;
import fr.gouv.vitam.common.digest.DigestType;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DatabaseMigrationsTest {

    private static final String ROOT_MIGRATION_SCRIPTS = "migration-scripts";

    // ######################################################################################
    // /!\ IMPORTANT : Do NOT UPDATE or REFORMAT migration files. Add new versions if needed.
    // ######################################################################################

    private static final Map<String, String> MIGRATION_SCRIPT_DIGEST_UPDATE_GUARD = Map.of(
        "h2/V1__init_tables_h2.sql",
        "d6006910f96c85681859794ca925d1c65cc85bf5b80115fbdc514928e57bd0e8",
        "postgres/V1__init_tables_postgres.sql",
        "53c1cd51b0bdc59dc66ed3f73ae629dcc3a20cfdc531d1315d3187fb5a622288"
    );

    @Test
    public void testScriptIntegrity() throws Exception {
        Map<String, String> actualChecksums = loadActualChecksums();

        // Check missing files
        for (String expectedPath : MIGRATION_SCRIPT_DIGEST_UPDATE_GUARD.keySet()) {
            assertTrue("Missing resource: " + expectedPath, actualChecksums.containsKey(expectedPath));
        }

        // Check unexpected files
        for (String actualPath : actualChecksums.keySet()) {
            assertTrue(
                "Unexpected resource: " + actualPath,
                MIGRATION_SCRIPT_DIGEST_UPDATE_GUARD.containsKey(actualPath)
            );
        }

        // Check digests
        for (Map.Entry<String, String> entry : MIGRATION_SCRIPT_DIGEST_UPDATE_GUARD.entrySet()) {
            String path = entry.getKey();
            String expectedDigest = entry.getValue();
            String actualDigest = actualChecksums.get(path);

            assertEquals("Checksum mismatch for: " + path, expectedDigest, actualDigest);
        }
    }

    private Map<String, String> loadActualChecksums() throws Exception {
        Path rootPath = PropertiesUtils.getResourceFile(ROOT_MIGRATION_SCRIPTS).toPath();
        assertNotNull("Resource folder not found: " + ROOT_MIGRATION_SCRIPTS, rootPath);

        try (Stream<Path> walk = Files.walk(rootPath)) {
            return walk
                .filter(Files::isRegularFile)
                .collect(Collectors.toMap(path -> rootPath.relativize(path).toString(), this::computeSha256));
        }
    }

    private String computeSha256(Path path) {
        try {
            Digest digest = new Digest(DigestType.SHA256);
            digest.update(path.toFile());
            return digest.digestHex();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
