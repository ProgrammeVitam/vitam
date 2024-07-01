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
package fr.gouv.vitam.worker.core.utils;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.tmp.TempFolderRule;
import fr.gouv.vitam.worker.common.HandlerIO;
import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

public class JsonLineDataBaseTest {

    @ClassRule
    public static TempFolderRule tempFolderRule = new TempFolderRule();

    public static JsonLineDataBase dataBase;

    @BeforeClass
    public static void setUpClass() {
        HandlerIO handlerIO = mock(HandlerIO.class);
        doAnswer(a -> {
            String name = a.getArgument(0);
            Path path = Path.of(tempFolderRule.newFile().getPath()).getParent().resolve(name);
            FileUtils.createParentDirectories(path.toFile());
            if (Files.exists(path)) {
                return path.toFile();
            } else {
                return tempFolderRule.newFile(name);
            }
        })
            .when(handlerIO)
            .getNewLocalFile(anyString());
        dataBase = new JsonLineDataBase(handlerIO, "dir");
    }

    @Test
    public void should_write_then_read() {
        dataBase.write("A1", JsonHandler.createObjectNode().put("_id", "A1"));
        dataBase.write("A2", JsonHandler.createObjectNode().put("_id", "A2"));
        dataBase.write("B1", JsonHandler.createObjectNode().put("_id", "B1"));
        dataBase.write("A3", JsonHandler.createObjectNode().put("_id", "A3"));

        JsonNode read = dataBase.read("B1");
        assertEquals("B1", read.get("_id").asText());
    }
}
