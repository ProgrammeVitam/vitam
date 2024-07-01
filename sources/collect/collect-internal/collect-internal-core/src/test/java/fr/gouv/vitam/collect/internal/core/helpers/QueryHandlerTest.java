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
package fr.gouv.vitam.collect.internal.core.helpers;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.collect.common.dto.FileInfoDto;
import fr.gouv.vitam.collect.common.dto.ObjectDto;
import fr.gouv.vitam.collect.internal.core.helpers.builders.DbObjectGroupModelBuilder;
import fr.gouv.vitam.collect.internal.core.helpers.handlers.QueryHandler;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.administration.DataObjectVersionType;
import fr.gouv.vitam.common.model.objectgroup.DbObjectGroupModel;
import org.junit.Test;

import java.util.ArrayList;

import static fr.gouv.vitam.collect.internal.core.helpers.QueryHandlerTest.TestDummyData.FILE_NAME;
import static fr.gouv.vitam.collect.internal.core.helpers.QueryHandlerTest.TestDummyData.OBJECT_GROUP_ID;
import static fr.gouv.vitam.collect.internal.core.helpers.QueryHandlerTest.TestDummyData.OPI;
import static fr.gouv.vitam.collect.internal.core.helpers.QueryHandlerTest.TestDummyData.QUALIFIER_VERSION;
import static fr.gouv.vitam.collect.internal.core.helpers.QueryHandlerTest.TestDummyData.USAGE;
import static fr.gouv.vitam.collect.internal.core.helpers.QueryHandlerTest.TestDummyData.VERSION_ID;
import static fr.gouv.vitam.collect.internal.core.helpers.QueryHandlerTest.TestDummyData.qualifiersAddMultiQuery;
import static org.assertj.core.api.Assertions.assertThat;

public class QueryHandlerTest {

    @Test
    public void should_create_qualifiers_add_multiple_query()
        throws InvalidCreateOperationException, InvalidParseOperationException {
        // GIVEN
        DbObjectGroupModel objectGroup = new DbObjectGroupModelBuilder()
            .withQualifiers(new ArrayList<>())
            .withOpi(OPI)
            .withId(OBJECT_GROUP_ID)
            .withFileInfoModel(FILE_NAME)
            .build();

        ObjectDto objectDto = new ObjectDto();
        FileInfoDto fileInfo = new FileInfoDto();
        fileInfo.setFileName(FILE_NAME);
        objectDto.setFileInfo(fileInfo);
        objectDto.setId(VERSION_ID);

        // WHEN
        UpdateMultiQuery qualifiersAddMultiQuery = QueryHandler.getQualifiersAddMultiQuery(
            objectGroup,
            USAGE,
            QUALIFIER_VERSION,
            objectDto
        );

        // THEN
        JsonNode expectedJsonNode = qualifiersAddMultiQuery();
        assertThat(qualifiersAddMultiQuery).isNotNull();
        assertThat(qualifiersAddMultiQuery.getFinalUpdate().toPrettyString()).hasToString(
            expectedJsonNode.toPrettyString()
        );
    }

    static class TestDummyData {

        static DataObjectVersionType USAGE = DataObjectVersionType.BINARY_MASTER;
        static String FILE_NAME = "memoire_nationale.txt";
        static String VERSION_ID = "OBJECT_ID";
        static String OPI = "OPI";
        static String OBJECT_GROUP_ID = "OBJECT_GROUP_ID";
        static int QUALIFIER_VERSION = 1;

        static JsonNode qualifiersAddMultiQuery() throws InvalidParseOperationException {
            String qualifiersAddMultiQuery =
                "{\n" +
                "  \"$roots\" : [ ],\n" +
                "  \"$query\" : [ ],\n" +
                "  \"$filter\" : {\n" +
                "    \"$hint\" : [ \"objectgroups\" ]\n" +
                "  },\n" +
                "  \"$action\" : [ {\n" +
                "    \"$set\" : {\n" +
                "      \"#qualifiers\" : [ {\n" +
                "        \"qualifier\" : \"" +
                USAGE.getName() +
                "\",\n" +
                "        \"_nbc\" : 1,\n" +
                "        \"versions\" : [ {\n" +
                "          \"_id\" : \"" +
                VERSION_ID +
                "\",\n" +
                "          \"DataObjectVersion\" : \"" +
                USAGE.getName() +
                "_" +
                QUALIFIER_VERSION +
                "\",\n" +
                "          \"FileInfo\" : {\n" +
                "            \"Filename\" : \"" +
                FILE_NAME +
                "\"\n" +
                "          },\n" +
                "          \"Size\" : 0\n" +
                "        } ]\n" +
                "      } ]\n" +
                "    }\n" +
                "  }, {\n" +
                "    \"$set\" : {\n" +
                "      \"#nbobjects\" : 1\n" +
                "    }\n" +
                "  } ]\n" +
                "}";
            return JsonHandler.getFromString(qualifiersAddMultiQuery);
        }
    }
}
