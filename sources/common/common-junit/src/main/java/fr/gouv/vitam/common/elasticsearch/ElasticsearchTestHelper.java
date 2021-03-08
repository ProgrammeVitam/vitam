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
package fr.gouv.vitam.common.elasticsearch;

import org.apache.commons.lang3.StringUtils;

import java.io.FileNotFoundException;
import java.nio.file.Paths;

public class ElasticsearchTestHelper {

    // The relative path to unit and objectgroup elasticsearch mapping
    public static final String UNIT_MAPPING_ES_FROM_ANSIBLE =
            "deployment/ansible-vitam/roles/elasticsearch-mapping/files/unit-es-mapping.json";

    public static final String OG_MAPPING_ES_FROM_ANSIBLE =
            "deployment/ansible-vitam/roles/elasticsearch-mapping/files/og-es-mapping.json";

    public static String loadMapping(String collection) throws Exception {
        String dir = Paths.get("").toAbsolutePath().toString();
        String userDir = StringUtils.substringBeforeLast(dir, "sources/");

        if (collection != null && collection.endsWith("Unit")) {
            return userDir + UNIT_MAPPING_ES_FROM_ANSIBLE;
        } else if (collection != null && collection.endsWith("ObjectGroup")) {
            return userDir + OG_MAPPING_ES_FROM_ANSIBLE;
        } else {
            throw new FileNotFoundException("mapping Files not found");
        }
    }
}
