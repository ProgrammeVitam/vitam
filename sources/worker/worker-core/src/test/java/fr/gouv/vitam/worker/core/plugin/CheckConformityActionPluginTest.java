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
package fr.gouv.vitam.worker.core.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.IOParameter;
import fr.gouv.vitam.common.model.processing.ProcessingUri;
import fr.gouv.vitam.common.model.processing.UriPrefix;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClient;
import fr.gouv.vitam.logbook.lifecycles.client.LogbookLifeCyclesClientFactory;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import fr.gouv.vitam.processing.common.parameter.DefaultWorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.InputStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class CheckConformityActionPluginTest {

    private static final String CALC_CHECK = "CALC_CHECK";

    static ObjectNode jsonNode_1 = JsonHandler.createObjectNode()
        .put(
            "MessageDigest",
            "e726e114f302c871b64569a00acb3a19badb7ee8ce4aef72cc2a043ace4905b8e8fca6f4771f8d6f67e221a53a4bbe170501af318c8f2c026cc8ea60f66fa804"
        )
        .put("Algorithm", "SHA-512")
        .put(
            "SystemMessageDigest",
            "e726e114f302c871b64569a00acb3a19badb7ee8ce4aef72cc2a043ace4905b8e8fca6f4771f8d6f67e221a53a4bbe170501af318c8f2c026cc8ea60f66fa804"
        )
        .put("SystemAlgorithm", "SHA-512");

    static ObjectNode jsonNode_2 = JsonHandler.createObjectNode()
        .put("MessageDigest", "f332ca3fd108067eb3500df34283485a1c35e36bdf8f4bd3db3fd9064efdb954")
        .put("Algorithm", "SHA-256")
        .put(
            "SystemMessageDigest",
            "abead17e841c937187270cb95b0656bf3f7a9e71c8ca95e7fc8efa38cfffcab9889f353a95136fa3073a422d825175bf1bef24dc355bfa081f7e48b106070fd5"
        )
        .put("SystemAlgorithm", "SHA-512");

    static ObjectNode jsonNode_3 = JsonHandler.createObjectNode()
        .put(
            "MessageDigest",
            "fe2b0664fc66afd85f839be6ee4b6433b60a06b9a4481e0743c9965394fa0b8aa51b30df11f3281fef3d7f6c86a35cd2925351076da7abc064ad89369edf44f0"
        )
        .put("Algorithm", "SHA-512")
        .put(
            "SystemMessageDigest",
            "fe2b0664fc66afd85f839be6ee4b6433b60a06b9a4481e0743c9965394fa0b8aa51b30df11f3281fef3d7f6c86a35cd2925351076da7abc064ad89369edf44f0"
        )
        .put("SystemAlgorithm", "SHA-512");

    static ObjectNode jsonNode_4 = JsonHandler.createObjectNode()
        .put("MessageDigest", "3273aa2ccb0cf4d5d37cef899d1774b9")
        .put("Algorithm", "MD5")
        .put(
            "SystemMessageDigest",
            "d156f4a4cc725cc6eaaafdcb7936c9441d25bdf033e4e2f1852cf540d39713446cfcd42f2ba087eb66f3f9dbfeca338180ca64bdde645706ec14499311d557f4"
        )
        .put("SystemAlgorithm", "SHA-512");

    private static final Map<String, ObjectNode> EV_DETAIL_DATA = Collections.unmodifiableMap(
        Stream.of(
            new AbstractMap.SimpleEntry<>("aeaaaaaaaaaaaaababaumakxynrf3rqaaaaq", jsonNode_1),
            new AbstractMap.SimpleEntry<>("aeaaaaaaaaaaaaababaumakxynrf3tqaaaaq", jsonNode_2),
            new AbstractMap.SimpleEntry<>("aeaaaaaaaaaaaaababaumakxynrf3uaaaaaq", jsonNode_3),
            new AbstractMap.SimpleEntry<>("aeaaaaaaaaaaaaababaumakxynrf3uyaaaaq", jsonNode_4)
        ).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()))
    );

    private static final JsonNode EV_DETAIL_DATA_BDO_AND_PDO = JsonHandler.createObjectNode()
        .put(
            "MessageDigest",
            "942bb63cc16bf5ca3ba7fabf40ce9be19c3185a36cd87ad17c63d6fad1aa29d4312d73f2d6a1ba1266c3a71fc4119dd476d2d776cf2ad2acd7a9a3dfa1f80dc7"
        )
        .put("Algorithm", "SHA-512")
        .put(
            "SystemMessageDigest",
            "942bb63cc16bf5ca3ba7fabf40ce9be19c3185a36cd87ad17c63d6fad1aa29d4312d73f2d6a1ba1266c3a71fc4119dd476d2d776cf2ad2acd7a9a3dfa1f80dc7"
        )
        .put("SystemAlgorithm", "SHA-512");

    private static final String OBJECT_GROUP = "storeObjectGroupHandler/aeaaaaaaaaaaaaababaumakxynrf3sqaaaaq.json";
    private static final String bdo1 =
        "e726e114f302c871b64569a00acb3a19badb7ee8ce4aef72cc2a043ace4905b8e8fca6f4771f8d6f67e221a53a4bbe170501af318c8f2c026cc8ea60f66fa804.odp";
    private static final String bdo2 =
        "d156f4a4cc725cc6eaaafdcb7936c9441d25bdf033e4e2f1852cf540d39713446cfcd42f2ba087eb66f3f9dbfeca338180ca64bdde645706ec14499311d557f4.txt";
    private static final String bdo3 =
        "fe2b0664fc66afd85f839be6ee4b6433b60a06b9a4481e0743c9965394fa0b8aa51b30df11f3281fef3d7f6c86a35cd2925351076da7abc064ad89369edf44f0.png";
    private static final String bdo4 = "f332ca3fd108067eb3500df34283485a1c35e36bdf8f4bd3db3fd9064efdb954.pdf";

    private static final String OBJECT_GROUP_BDO_AND_PDO =
        "checkConformityActionPlugin/aebaaaaaaaakwtamaai7cak32lvlyoyaaaba.json";
    private static final String OBJECT_GROUP_DIGEST_EMPTY =
        "checkConformityActionPlugin/aebaaaaaaaakwtamaai7cak32lvlyoyaaabb.json";
    private static final String OBJECT_GROUP_DIGEST_INVALID =
        "checkConformityActionPlugin/aebaaaaaaaakwtamaai7cak32lvlyoyaaabc.json";

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Mock
    private WorkspaceClient workspaceClient;

    @Mock
    private WorkspaceClientFactory workspaceClientFactory;

    @Mock
    private LogbookLifeCyclesClientFactory logbookLifeCyclesClientFactory;

    @Mock
    private LogbookLifeCyclesClient logbookLifeCyclesClient;

    @Mock
    private LogbookOperationsClientFactory logbookOperationsClientFactory;

    @Mock
    private LogbookOperationsClient logbookOperationsClient;

    @Before
    public void setUp() throws Exception {
        Mockito.reset(workspaceClient, logbookOperationsClient, logbookLifeCyclesClient);

        when(workspaceClientFactory.getClient()).thenReturn(workspaceClient);
        when(logbookOperationsClientFactory.getClient()).thenReturn(logbookOperationsClient);
        when(logbookLifeCyclesClientFactory.getClient()).thenReturn(logbookLifeCyclesClient);
    }

    @Test
    public void getNonStandardDigestUpdate() throws Exception {
        InputStream objectGroup = PropertiesUtils.getResourceAsStream(OBJECT_GROUP);
        when(workspaceClient.getObject(any(), eq("ObjectGroup/objName2"))).thenReturn(
            Response.status(Status.OK).entity(objectGroup).build()
        );
        when(workspaceClient.getObject(any(), eq("SIP/content/" + bdo1))).thenReturn(
            Response.status(Status.OK).entity(PropertiesUtils.getResourceAsStream("BinaryObject/" + bdo1)).build()
        );

        when(workspaceClient.getObject(any(), eq("SIP/content/" + bdo2))).thenReturn(
            Response.status(Status.OK).entity(PropertiesUtils.getResourceAsStream("BinaryObject/" + bdo2)).build()
        );
        when(workspaceClient.getObject(any(), eq("SIP/content/" + bdo3))).thenReturn(
            Response.status(Status.OK).entity(PropertiesUtils.getResourceAsStream("BinaryObject/" + bdo3)).build()
        );
        when(workspaceClient.getObject(any(), eq("SIP/content/" + bdo4))).thenReturn(
            Response.status(Status.OK).entity(PropertiesUtils.getResourceAsStream("BinaryObject/" + bdo4)).build()
        );

        // assertNotNull(objectGroup);
        CheckConformityActionPlugin plugin = new CheckConformityActionPlugin();
        final WorkerParameters params = getDefaultWorkerParameters();
        params.setObjectName("objName2");
        String objectId = "objectId";
        final HandlerIOImpl handlerIO = new HandlerIOImpl(
            workspaceClientFactory,
            logbookLifeCyclesClientFactory,
            "CheckConformityActionHandlerTest",
            "workerId",
            Lists.newArrayList(objectId)
        );
        handlerIO.setCurrentObjectId(objectId);

        final List<IOParameter> out = new ArrayList<>();
        out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "objectGroupId.json")));

        final List<IOParameter> in = new ArrayList<>();
        in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.VALUE, "SHA-512")));
        handlerIO.addInIOParameters(in);
        handlerIO.addOutIOParameters(out);
        final ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
        Integer count = response.getStatusMeter().get(StatusCode.OK.ordinal());
        Assertions.assertThat(count).isEqualTo(4);

        // check all subtasks
        response
            .getItemsStatus()
            .get(CALC_CHECK)
            .getSubTaskStatus()
            .forEach((k, v) -> {
                assertEquals(v.getEvDetailData(), JsonHandler.unprettyPrint(EV_DETAIL_DATA.get(k)));
            });
        handlerIO.close();
    }

    @Test
    public void checkBinaryAndPhysicalObject() throws Exception {
        InputStream objectGroup = PropertiesUtils.getResourceAsStream(OBJECT_GROUP_BDO_AND_PDO);
        when(workspaceClient.getObject(any(), eq("ObjectGroup/objName1"))).thenReturn(
            Response.status(Status.OK).entity(objectGroup).build()
        );
        when(workspaceClient.getObject(any(), eq("SIP/Content/5zC1uD6CvaYDipUhETOyUWVEbxHmE1.pdf"))).thenReturn(
            Response.status(Status.OK)
                .entity(
                    PropertiesUtils.getResourceAsStream(
                        "checkConformityActionPlugin/binaryObject/5zC1uD6CvaYDipUhETOyUWVEbxHmE1.pdf"
                    )
                )
                .build()
        );

        CheckConformityActionPlugin plugin = new CheckConformityActionPlugin();
        final WorkerParameters params = getDefaultWorkerParameters();
        params.setObjectName("objName1");
        String objectId = "objectId";
        final HandlerIOImpl handlerIO = new HandlerIOImpl(
            workspaceClientFactory,
            logbookLifeCyclesClientFactory,
            "CheckConformityActionHandlerTest",
            "workerId",
            Lists.newArrayList(objectId)
        );
        handlerIO.setCurrentObjectId(objectId);
        final List<IOParameter> out = new ArrayList<>();
        out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "objectGroupId.json")));
        final List<IOParameter> in = new ArrayList<>();
        in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.VALUE, "SHA-512")));
        handlerIO.addInIOParameters(in);
        handlerIO.addOutIOParameters(out);
        final ItemStatus response = plugin.execute(params, handlerIO);
        Integer count = response.getStatusMeter().get(StatusCode.OK.ordinal());
        Assertions.assertThat(count).isEqualTo(1);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
        assertEquals(
            response.getItemsStatus().get(CALC_CHECK).getSubTaskStatus().values().iterator().next().getEvDetailData(),
            JsonHandler.unprettyPrint(EV_DETAIL_DATA_BDO_AND_PDO)
        );
        handlerIO.close();
    }

    @Test
    public void checkEmptyDigestMessage() throws Exception {
        InputStream objectGroupEmptyDigest = PropertiesUtils.getResourceAsStream(OBJECT_GROUP_DIGEST_EMPTY);
        when(workspaceClient.getObject(any(), eq("ObjectGroup/objectName2"))).thenReturn(
            Response.status(Status.OK).entity(objectGroupEmptyDigest).build()
        );
        when(workspaceClient.getObject(any(), eq("SIP/Content/5zC1uD6CvaYDipUhETOyUWVEbxHmE1.pdf"))).thenReturn(
            Response.status(Status.OK)
                .entity(
                    PropertiesUtils.getResourceAsStream(
                        "checkConformityActionPlugin/binaryObject/5zC1uD6CvaYDipUhETOyUWVEbxHmE1.pdf"
                    )
                )
                .build()
        );

        CheckConformityActionPlugin plugin = new CheckConformityActionPlugin();
        final WorkerParameters params = getDefaultWorkerParameters();
        params.setObjectName("objectName2");

        String objectId = "objectId";
        final HandlerIOImpl handlerIO = new HandlerIOImpl(
            workspaceClientFactory,
            logbookLifeCyclesClientFactory,
            "CheckConformityActionHandlerTest",
            "workerId",
            Lists.newArrayList(objectId)
        );
        handlerIO.setCurrentObjectId(objectId);
        final List<IOParameter> out = new ArrayList<>();
        out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "objectGroupId.json")));
        final List<IOParameter> in = new ArrayList<>();
        in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.VALUE, "SHA-512")));
        handlerIO.addInIOParameters(in);
        handlerIO.addOutIOParameters(out);
        final ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(StatusCode.KO, response.getGlobalStatus());
        LinkedHashMap<String, ItemStatus> subTasks = response.getItemsStatus().get(CALC_CHECK).getSubTaskStatus();
        assertEquals(1, subTasks.size());
        ItemStatus subtask = subTasks.entrySet().iterator().next().getValue();
        assertEquals("EMPTY", subtask.getGlobalOutcomeDetailSubcode());

        handlerIO.close();
    }

    @Test
    public void checkInvalidDigestMessage() throws Exception {
        InputStream objectGroupInvalideDigest = PropertiesUtils.getResourceAsStream(OBJECT_GROUP_DIGEST_INVALID);
        when(workspaceClient.getObject(any(), eq("ObjectGroup/objectName3"))).thenReturn(
            Response.status(Status.OK).entity(objectGroupInvalideDigest).build()
        );
        when(workspaceClient.getObject(any(), eq("SIP/Content/5zC1uD6CvaYDipUhETOyUWVEbxHmE1.pdf"))).thenReturn(
            Response.status(Status.OK)
                .entity(
                    PropertiesUtils.getResourceAsStream(
                        "checkConformityActionPlugin/binaryObject/5zC1uD6CvaYDipUhETOyUWVEbxHmE1.pdf"
                    )
                )
                .build()
        );

        CheckConformityActionPlugin plugin = new CheckConformityActionPlugin();
        final WorkerParameters params = getDefaultWorkerParameters();
        params.setObjectName("objectName3");

        String objectId = "objectId";
        final HandlerIOImpl handlerIO = new HandlerIOImpl(
            workspaceClientFactory,
            logbookLifeCyclesClientFactory,
            "CheckConformityActionHandlerTest",
            "workerId",
            Lists.newArrayList(objectId)
        );
        handlerIO.setCurrentObjectId(objectId);
        final List<IOParameter> out = new ArrayList<>();
        out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "objectGroupId.json")));
        final List<IOParameter> in = new ArrayList<>();
        in.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.VALUE, "SHA-512")));
        handlerIO.addInIOParameters(in);
        handlerIO.addOutIOParameters(out);
        final ItemStatus response = plugin.execute(params, handlerIO);
        assertEquals(StatusCode.KO, response.getGlobalStatus());
        LinkedHashMap<String, ItemStatus> subTasks = response.getItemsStatus().get(CALC_CHECK).getSubTaskStatus();
        assertEquals(1, subTasks.size());
        ItemStatus subtask = subTasks.entrySet().iterator().next().getValue();
        assertEquals("INVALID", subtask.getGlobalOutcomeDetailSubcode());

        handlerIO.close();
    }

    private DefaultWorkerParameters getDefaultWorkerParameters() {
        DefaultWorkerParameters workerParam = WorkerParametersFactory.newWorkerParameters(
            "pId",
            "stepId",
            "CheckConformityActionHandlerTest",
            "currentStep",
            Lists.newArrayList("objName"),
            "metadataURL",
            "workspaceURL"
        );
        return workerParam;
    }
}
