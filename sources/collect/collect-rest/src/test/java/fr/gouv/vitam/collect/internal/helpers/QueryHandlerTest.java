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
package fr.gouv.vitam.collect.internal.helpers;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.collect.internal.dto.FileInfoDto;
import fr.gouv.vitam.collect.internal.dto.ObjectGroupDto;
import fr.gouv.vitam.collect.internal.helpers.builders.DbObjectGroupModelBuilder;
import fr.gouv.vitam.collect.internal.helpers.builders.DbQualifiersModelBuilder;
import fr.gouv.vitam.collect.internal.helpers.handlers.QueryHandler;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.administration.DataObjectVersionType;
import fr.gouv.vitam.common.model.objectgroup.DbObjectGroupModel;
import fr.gouv.vitam.common.model.objectgroup.DbQualifiersModel;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static fr.gouv.vitam.collect.internal.helpers.QueryHandlerTest.TestDummyData.FILE_NAME;
import static fr.gouv.vitam.collect.internal.helpers.QueryHandlerTest.TestDummyData.OBJECT_GROUP_ID;
import static fr.gouv.vitam.collect.internal.helpers.QueryHandlerTest.TestDummyData.OPI;
import static fr.gouv.vitam.collect.internal.helpers.QueryHandlerTest.TestDummyData.QUALIFIER_VERSION_1;
import static fr.gouv.vitam.collect.internal.helpers.QueryHandlerTest.TestDummyData.USAGE;
import static fr.gouv.vitam.collect.internal.helpers.QueryHandlerTest.TestDummyData.VERSION_ID;
import static fr.gouv.vitam.collect.internal.helpers.QueryHandlerTest.TestDummyData.qualifiersAddMultiQuery;
import static org.assertj.core.api.Assertions.assertThat;


public class QueryHandlerTest {

    @Test
    public void should_create_qualifiers_add_multiple_query()
        throws InvalidCreateOperationException, InvalidParseOperationException {
        // GIVEN
        DbQualifiersModel qualifiersModel = new DbQualifiersModelBuilder()
            .withUsage(USAGE)
            .withVersion(VERSION_ID, FILE_NAME, USAGE, QUALIFIER_VERSION_1)
            .withNbc(1)
            .build();
        List<DbQualifiersModel > qualifiers = new ArrayList<>();
        qualifiers.add(qualifiersModel);

        DbObjectGroupModel objectGroup = new DbObjectGroupModelBuilder()
            .withQualifiers(VERSION_ID, FILE_NAME, USAGE, QUALIFIER_VERSION_1)
            .withOpi(OPI)
            .withId(OBJECT_GROUP_ID)
            .withFileInfoModel(FILE_NAME)
            .build();
        ObjectGroupDto objectGroupDto = new ObjectGroupDto();
        FileInfoDto fileInfo = new FileInfoDto();
        fileInfo.setFileName(FILE_NAME);
        objectGroupDto.setFileInfo(fileInfo);
        objectGroupDto.setId("aeeaaaaaacaltpovaewckalf3ukh4myaaaeq");

        // WHEN
        UpdateMultiQuery qualifiersAddMultiQuery =
            QueryHandler.getQualifiersAddMultiQuery(USAGE, QUALIFIER_VERSION_1, qualifiers, objectGroupDto, VERSION_ID, objectGroup);

        // THEN
        JsonNode expectedJsonNode = qualifiersAddMultiQuery();
        assertThat(qualifiersAddMultiQuery).isNotNull();
        assertThat(qualifiersAddMultiQuery.getFinalUpdate().toPrettyString()).hasToString(expectedJsonNode.toPrettyString());
    }

    static class TestDummyData {
        static DataObjectVersionType USAGE = DataObjectVersionType.BINARY_MASTER;
        static String FILE_NAME = "memoire_nationale.txt";
        static String VERSION_ID = "aebbaaaaacaltpovaewckal62ukh4ml5a67q";
        static String OPI = "aeeaaaaaacaltpovaewckal62ukh4myaaaaq";
        static String OBJECT_GROUP_ID = "aeedaaaaacaltpovaewckal62ukh4myaa67q";
        static int QUALIFIER_VERSION_1 = 1;

        static JsonNode qualifiersAddMultiQuery() throws InvalidParseOperationException {
            String qualifiersAddMultiQuery = "{\n" +
                "  \"$roots\": [],\n" +
                "  \"$query\": [],\n" +
                "  \"$filter\": {\n" +
                "    \"$hint\": [\n" +
                "      \"objectgroups\"\n" +
                "    ]\n" +
                "  },\n" +
                "  \"$action\": [\n" +
                "    {\n" +
                "      \"$set\": {\n" +
                "        \"#qualifiers\": [\n" +
                "          {\n" +
                "            \"qualifier\": \""+USAGE+"\",\n" +
                "            \"_nbc\": 1,\n" +
                "            \"versions\": [\n" +
                "              {\n" +
                "                \"_id\": \""+VERSION_ID+"\",\n" +
                "                \"DataObjectVersion\": \""+USAGE+"_"+QUALIFIER_VERSION_1+"\",\n" +
                "                \"FileInfo\": {\n" +
                "                  \"Filename\": \""+FILE_NAME+"\"\n" +
                "                },\n" +
                "                \"Size\": 0\n" +
                "              }\n" +
                "            ]\n" +
                "          },\n" +
                "          {\n" +
                "            \"qualifier\": \""+USAGE+"\",\n" +
                "            \"_nbc\": 1,\n" +
                "            \"versions\": [\n" +
                "              {\n" +
                "                \"_id\": \""+VERSION_ID+"\",\n" +
                "                \"DataObjectVersion\": \""+USAGE+"_"+QUALIFIER_VERSION_1+"\",\n" +
                "                \"FileInfo\": {\n" +
                "                  \"Filename\": \""+FILE_NAME+"\"\n" +
                "                },\n" +
                "                \"Size\": 0\n" +
                "              }\n" +
                "            ]\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"$set\": {\n" +
                "        \"#nbobjects\": 2\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}";
            return JsonHandler.getFromString(qualifiersAddMultiQuery);
        }
    }



}