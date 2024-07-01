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
package fr.gouv.vitam.ihmdemo.common.pagination;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.json.JsonHandler;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.config.Ini;
import org.apache.shiro.config.IniSecurityManagerFactory;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.Factory;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PaginationHelperTest {

    static String sessionId;

    private static final String RESULT =
        "{\"$query\":{}," + "\"$hits\":{\"total\":100,\"offset\":0,\"limit\":25}," + "\"$results\":";

    private static final String OPERATION =
        "    \"evId\": \"aedqaaaaacaam7mxaaaamakvhiv4rsqaaaaq\"," +
        "    \"evType\": \"Process_SIP_unitary\"," +
        "    \"evDateTime\": \"2016-06-10T11:56:35.914\"," +
        "    \"evIdProc\": \"aedqaaaaacaam7mxaaaamakvhiv4rsiaaaaq\"," +
        "    \"evTypeProc\": \"INGEST\"," +
        "    \"outcome\": \"STARTED\"," +
        "    \"outDetail\": null," +
        "    \"outMessg\": \"SIP entry : SIP.zip\"," +
        "    \"agId\": {\"name\":\"ingest_1\",\"role\":\"ingest\",\"pid\":425367}," +
        "    \"agIdApp\": null," +
        "    \"agIdPers\": null," +
        "    \"evIdAppSession\": null," +
        "    \"evIdReq\": \"aedqaaaaacaam7mxaaaamakvhiv4rsiaaaaq\"," +
        "    \"agIdExt\": null," +
        "    \"obId\": null," +
        "    \"obIdReq\": null," +
        "    \"obIdIn\": null," +
        "    \"events\": []}";

    @BeforeClass
    public static void setup() {
        final Ini ini = new Ini();
        ini.loadFromPath("src/test/resources/shiro.ini");
        final Factory<SecurityManager> factory = new IniSecurityManagerFactory(ini);
        final SecurityManager securityManager = factory.getInstance();
        SecurityUtils.setSecurityManager(securityManager);

        final UsernamePasswordToken token = new UsernamePasswordToken("user", "user", true);

        final Subject currentUser = new Subject.Builder(securityManager).buildSubject();
        currentUser.getSession().stop();
        currentUser.login(token);
        sessionId = currentUser.getSession(true).getId().toString();
    }

    @Test
    public void givenSessionAlreadyExistsWhenPaginateResultThenReturnJsonNode() throws Exception {
        PaginationHelper.getInstance().setResult(sessionId, createResult());
        JsonNode result = PaginationHelper.getInstance().getResult(sessionId, new OffsetBasedPagination());
        assertEquals(result.get("$results").size(), 100);
        result = PaginationHelper.getInstance().getResult(createResult(), new OffsetBasedPagination());
        assertEquals(result.get("$results").size(), 100);
    }

    @Test(expected = VitamException.class)
    public void givenSessionNotFoundWhenSetResultThenRaiseAnException() throws Exception {
        PaginationHelper.getInstance().setResult("SessionNotFound", createResult());
    }

    private JsonNode createResult() throws InvalidParseOperationException {
        String result = RESULT + "[";
        for (int i = 0; i < 100; i++) {
            String s_i = "{\"#id\": \"aedqaaaaacaam7mxaaaamakvhiv4rsiaaa" + i + "\",";
            s_i += OPERATION;
            result += s_i;
            if (i < 99) {
                result += ",";
            }
        }
        result += "]}";
        return JsonHandler.getFromString(result);
    }
}
