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

package fr.gouv.vitam.collect.internal.core.csv;

import com.fasterxml.jackson.core.type.TypeReference;
import fr.gouv.vitam.collect.common.exception.CollectInternalException;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.schema.SchemaResponse;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

public class SedaSchemaInfoResolverTest {

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private AdminManagementClientFactory adminManagementClientFactory;

    @Mock
    private AdminManagementClient adminManagementClient;

    @Before
    public void setUp() throws Exception {
        doReturn(adminManagementClient).when(adminManagementClientFactory).getClient();
        doReturn(loadUnitSchema()).when(adminManagementClient).getUnitSchema();
    }

    @Test
    public void testResolverForContentFields() throws CollectInternalException {
        SedaSchemaInfoResolver instance = new SedaSchemaInfoResolver(adminManagementClientFactory);

        assertThat(instance.getContentSchemaInfo("Content")).isEqualTo(
            new SedaSchemaInfo("Content", null, null, true, false, false, true)
        );

        assertThat(instance.getContentSchemaInfo("Content.DescriptionLevel")).isEqualTo(
            new SedaSchemaInfo(
                "Content.DescriptionLevel",
                "DescriptionLevel",
                "DescriptionLevel",
                false,
                false,
                false,
                false
            )
        );

        assertThat(instance.getContentSchemaInfo("Content.Event")).isEqualTo(
            new SedaSchemaInfo("Content.Event", "Event", "Event", true, true, false, false)
        );

        assertThat(instance.getContentSchemaInfo("Content.Event.EventDateTime")).isEqualTo(
            new SedaSchemaInfo(
                "Content.Event.EventDateTime",
                "Event.evDateTime",
                "evDateTime",
                false,
                false,
                false,
                false
            )
        );

        assertThat(instance.getContentSchemaInfo("Content.Signature.Signer.Function")).isEqualTo(
            new SedaSchemaInfo(
                "Content.Signature.Signer.Function",
                "Signature.Signer.Function",
                "Function",
                false,
                true,
                false,
                false
            )
        );

        assertThat(instance.getContentSchemaInfo("Content.SigningInformation.Extended")).isEqualTo(
            new SedaSchemaInfo(
                "Content.SigningInformation.Extended",
                "SigningInformation.Extended",
                "Extended",
                true,
                false,
                false,
                true
            )
        );

        assertThat(instance.getContentSchemaInfo("Content.Invoice")).isEqualTo(
            new SedaSchemaInfo("Content.Invoice", "Invoice", "Invoice", true, true, true, true)
        );

        assertThat(instance.getContentSchemaInfo("Content.Invoice.Provider.MyKeyword")).isEqualTo(
            new SedaSchemaInfo(
                "Content.Invoice.Provider.MyKeyword",
                "Invoice.Provider.MyKeyword",
                "MyKeyword",
                false,
                true,
                true,
                false
            )
        );

        // No management fields
        assertThat(instance.getContentSchemaInfo("Management.AppraisalRule.Rule")).isNull();

        // No system field (#version, Title_ & Description_)
        assertThat(instance.getContentSchemaInfo("#version")).isNull();
        assertThat(instance.getContentSchemaInfo("Content.Title_")).isNull();
        assertThat(instance.getContentSchemaInfo("Content.Description_")).isNull();

        // Unknown fields
        assertThat(instance.getContentSchemaInfo("Unknown")).isNull();
    }

    public RequestResponse<SchemaResponse> loadUnitSchema() throws InvalidParseOperationException, IOException {
        List<SchemaResponse> unitSchemaModels = JsonHandler.getFromInputStreamAsTypeReference(
            PropertiesUtils.getResourceAsStream("unit-schema-with-custom-fields.json"),
            new TypeReference<>() {}
        );
        return new RequestResponseOK<SchemaResponse>().addAllResults(unitSchemaModels);
    }
}
