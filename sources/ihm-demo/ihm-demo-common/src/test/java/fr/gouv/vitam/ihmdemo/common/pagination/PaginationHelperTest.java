package fr.gouv.vitam.ihmdemo.common.pagination;

import static org.junit.Assert.assertEquals;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.config.Ini;
import org.apache.shiro.config.IniSecurityManagerFactory;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.Factory;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.json.JsonHandler;

public class PaginationHelperTest {

    static String sessionId;

    private static final String RESULT =
        "{\"$query\":{}," +
            "\"$hits\":{\"total\":100,\"offset\":0,\"limit\":25}," +
            "\"$results\":";

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
