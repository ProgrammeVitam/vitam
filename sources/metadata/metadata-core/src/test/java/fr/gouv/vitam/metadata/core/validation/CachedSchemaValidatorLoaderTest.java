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
package fr.gouv.vitam.metadata.core.validation;

import com.google.common.collect.Sets;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.json.JsonSchemaValidationException;
import fr.gouv.vitam.common.json.JsonSchemaValidator;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

public class CachedSchemaValidatorLoaderTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Rule
    public RunWithCustomExecutorRule runInThread = new RunWithCustomExecutorRule(
        VitamThreadPoolExecutor.getDefaultExecutor()
    );

    @Mock
    AdminManagementClientFactory adminManagementClientFactory;

    @Mock
    AdminManagementClient adminManagementClient;

    @Before
    public void before() {
        doReturn(adminManagementClient).when(adminManagementClientFactory).getClient();
    }

    @Test
    @RunWithCustomExecutor
    public void testLoading() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(3);
        GUID guid = GUIDFactory.newRequestIdGUID(3);
        VitamThreadUtils.getVitamSession().setRequestId(guid);

        String schema =
            "{\"$schema\":\"http://json-schema.org/draft-04/schema#\",\"id\":\"http://example.com/root.json\",\"type\":\"object\",\"additionalProperties\":true,\"anyOf\":[{\"required\":[\"_id\",\"Title\"]}],\"properties\":{\"_id\":{\"type\":\"string\"},\"Title\":{\"type\":\"string\"}}}";

        // When
        CachedSchemaValidatorLoader schemaValidatorLoader = new CachedSchemaValidatorLoader(10, 60);
        JsonSchemaValidator result = schemaValidatorLoader.loadSchemaValidator(schema);

        // Then
        result.validateJson(JsonHandler.createObjectNode().put("_id", "MyId").put("Title", "MyTitle"));

        assertThatThrownBy(
            () -> result.validateJson(JsonHandler.createObjectNode().put("Title", "MyTitle"))
        ).isInstanceOf(JsonSchemaValidationException.class);
    }

    @Test
    @RunWithCustomExecutor
    public void testReLoadingFromCache() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(3);
        GUID guid = GUIDFactory.newRequestIdGUID(3);
        VitamThreadUtils.getVitamSession().setRequestId(guid);

        String schema =
            "{\"$schema\":\"http://json-schema.org/draft-04/schema#\",\"id\":\"http://example.com/root.json\",\"type\":\"object\",\"additionalProperties\":true,\"anyOf\":[{\"required\":[\"_id\",\"Title\"]}],\"properties\":{\"_id\":{\"type\":\"string\"},\"Title\":{\"type\":\"string\"}}}";

        // When
        CachedSchemaValidatorLoader schemaValidatorLoader = new CachedSchemaValidatorLoader(10, 60);
        Set<JsonSchemaValidator> results = Sets.newIdentityHashSet();
        for (int i = 0; i < 10; i++) {
            results.add(schemaValidatorLoader.loadSchemaValidator(schema));
        }

        // Then
        assertThat(results).hasSize(1);
    }

    @Test
    @RunWithCustomExecutor
    public void testCacheTimeout() throws Exception {
        // Given
        VitamThreadUtils.getVitamSession().setTenantId(3);
        GUID guid = GUIDFactory.newRequestIdGUID(3);
        VitamThreadUtils.getVitamSession().setRequestId(guid);

        String schema =
            "{\"$schema\":\"http://json-schema.org/draft-04/schema#\",\"id\":\"http://example.com/root.json\",\"type\":\"object\",\"additionalProperties\":true,\"anyOf\":[{\"required\":[\"_id\",\"Title\"]}],\"properties\":{\"_id\":{\"type\":\"string\"},\"Title\":{\"type\":\"string\"}}}";

        // When
        CachedSchemaValidatorLoader schemaValidatorLoader = new CachedSchemaValidatorLoader(10, 1);
        JsonSchemaValidator result1 = schemaValidatorLoader.loadSchemaValidator(schema);
        TimeUnit.SECONDS.sleep(2);
        JsonSchemaValidator result2 = schemaValidatorLoader.loadSchemaValidator(schema);

        // Then
        assertThat(result1).isNotNull();
        assertThat(result2).isNotNull();
        assertThat(result2).isNotSameAs(result1);
    }
}
