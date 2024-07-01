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
package fr.gouv.vitam.functionaltest.cucumber.step;

import com.fasterxml.jackson.databind.JsonNode;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import fr.gouv.vitam.access.external.common.exception.AccessExternalClientException;
import fr.gouv.vitam.common.FileUtil;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.SecurityProfileModel;
import fr.gouv.vitam.functional.administration.common.SecurityProfile;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import static fr.gouv.vitam.common.database.builder.query.QueryHelper.eq;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Security Profile Step
 */
public class SecurityProfileStep extends CommonStep {

    private String fileName;
    private String securityProfileName;

    public SecurityProfileStep(World world) {
        super(world);
    }

    /**
     * define a security profile
     *
     * @param fileName name of a security profile
     */
    @Given("^un profile de sécurité nommé (.*)$")
    public void a_security_profile_with_file_name(String fileName) {
        this.fileName = fileName;
    }

    @Then("^j'importe ce profile de sécurité en succès")
    public void success_upload_security_profile()
        throws IOException, VitamClientException, AccessExternalClientException, InvalidParseOperationException {
        Path securityProfile = Paths.get(world.getBaseDirectory(), fileName);
        RequestResponse response = world
            .getAdminClient()
            .createSecurityProfiles(
                new VitamContext(world.getTenantId()).setApplicationSessionId(world.getApplicationSessionId()),
                Files.newInputStream(securityProfile, StandardOpenOption.READ),
                fileName
            );

        final String operationId = response.getHeaderString(GlobalDataRest.X_REQUEST_ID);
        world.setOperationId(operationId);
        assertThat(response.getHttpCode()).isEqualTo(Response.Status.CREATED.getStatusCode());
    }

    @Then("^j'importe ce profile de sécurité en échec")
    public void fail_upload_security_profile()
        throws VitamClientException, IOException, AccessExternalClientException, InvalidParseOperationException {
        Path securityProfile = Paths.get(world.getBaseDirectory(), fileName);
        RequestResponse response = world
            .getAdminClient()
            .createSecurityProfiles(
                new VitamContext(world.getTenantId()).setApplicationSessionId(world.getApplicationSessionId()),
                Files.newInputStream(securityProfile, StandardOpenOption.READ),
                fileName
            );
        final String operationId = response.getHeaderString(GlobalDataRest.X_REQUEST_ID);
        world.setOperationId(operationId);
        assertThat(response.getHttpCode()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }

    /**
     * define a security profile
     *
     * @param securityOperationName name of a security profile
     */
    @Given("^un profile de sécurité ayant pour nom (.*)$")
    public void a_security_profile_name(String securityOperationName) {
        this.securityProfileName = securityOperationName;
    }

    @When(
        "^je modifie le profile de sécurité avec le fichier de requête suivant (.*) le statut de la requête est (.*)$"
    )
    public void update_security_profile_by_query(String queryFilename, Integer statusCode)
        throws VitamClientException, IOException, InvalidParseOperationException, InvalidCreateOperationException {
        String securityProfileIdentifier = getSecurityProfileByName().getIdentifier();

        Path queryFile = Paths.get(world.getBaseDirectory(), queryFilename);
        String query = FileUtil.readFile(queryFile.toFile());
        JsonNode queryDsl = JsonHandler.getFromString(query);
        RequestResponse<SecurityProfileModel> requestResponse = world
            .getAdminClient()
            .updateSecurityProfile(
                new VitamContext(world.getTenantId()).setApplicationSessionId(world.getApplicationSessionId()),
                securityProfileIdentifier,
                queryDsl
            );

        final String operationId = requestResponse.getHeaderString(GlobalDataRest.X_REQUEST_ID);
        world.setOperationId(operationId);
        assertThat(statusCode).isEqualTo(requestResponse.getStatus());
    }

    @Then("^le profile de sécurité contient la permission (.*)$")
    public void has_permission(String permission)
        throws AccessExternalClientException, InvalidParseOperationException, VitamClientException, InvalidCreateOperationException {
        SecurityProfileModel securityProfileModel = getSecurityProfileByName();
        assertThat(securityProfileModel.getPermissions()).contains(permission);
    }

    @Then("^le profile de sécurité ne contient pas la permission (.*)$")
    public void has_not_permission(String permission)
        throws AccessExternalClientException, InvalidParseOperationException, VitamClientException, InvalidCreateOperationException {
        SecurityProfileModel securityProfileModel = getSecurityProfileByName();
        assertThat(securityProfileModel.getPermissions()).doesNotContain(permission);
    }

    @Then("^le profile de sécurité a toutes les permissions$")
    public void has_full_access()
        throws InvalidParseOperationException, VitamClientException, InvalidCreateOperationException {
        SecurityProfileModel securityProfileModel = getSecurityProfileByName();
        assertThat(securityProfileModel.getFullAccess()).isTrue();
    }

    private SecurityProfileModel getSecurityProfileByName()
        throws InvalidCreateOperationException, VitamClientException {
        final fr.gouv.vitam.common.database.builder.request.single.Select select =
            new fr.gouv.vitam.common.database.builder.request.single.Select();
        select.setQuery(eq(SecurityProfile.NAME, securityProfileName));
        final JsonNode query = select.getFinalSelect();

        RequestResponse<SecurityProfileModel> requestResponse = world
            .getAdminClient()
            .findSecurityProfiles(
                new VitamContext(world.getTenantId())
                    .setAccessContract(null)
                    .setApplicationSessionId(world.getApplicationSessionId()),
                query
            );

        assertThat(requestResponse.isOk()).isTrue();
        assertThat(((RequestResponseOK<SecurityProfileModel>) requestResponse).getResults().size()).isEqualTo(1);
        SecurityProfileModel securityProfileModel =
            ((RequestResponseOK<SecurityProfileModel>) requestResponse).getFirstResult();
        assertThat(securityProfileModel.getName()).isEqualTo(securityProfileName);
        return securityProfileModel;
    }
}
