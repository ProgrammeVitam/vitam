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

package fr.gouv.vitam.functionaltest.cucumber.service;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.fail;

public class XmlTestHelper {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(XmlTestHelper.class);

    public static void checkTagCountInXmlFile(Path file, int count, String tag) throws IOException {
        try (InputStream inputStream = Files.newInputStream(file)) {
            checkTagCountInXmlFile(inputStream, count, tag);
        }
    }

    public static void checkTagCountInXmlFile(InputStream is, int count, String tag) throws IOException {
        String xml = IOUtils.toString(is, StandardCharsets.UTF_8);

        // count ending tag and empty tag to ensure there is no attribute in the checked tag
        int realCount =
            StringUtils.countMatches(xml, "</" + tag + ">") + StringUtils.countMatches(xml, "<" + tag + "/>");

        if (realCount != count) {
            LOGGER.error(String.format("expected %d tags %s but was %d", count, tag, realCount));
            fail(String.format("expected %d tags %s but was %d", count, tag, realCount));
        }
    }
}
