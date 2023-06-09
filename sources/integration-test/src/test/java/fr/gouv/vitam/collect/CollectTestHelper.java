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
package fr.gouv.vitam.collect;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.culture.archivesdefrance.seda.v2.LegalStatusType;
import fr.gouv.vitam.collect.common.dto.ProjectDto;
import fr.gouv.vitam.collect.common.dto.TransactionDto;
import fr.gouv.vitam.collect.common.enums.TransactionStatus;
import fr.gouv.vitam.collect.external.client.CollectExternalClient;
import fr.gouv.vitam.collect.external.client.CollectExternalClientFactory;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class CollectTestHelper {
    public static Optional<ProjectDto> createProject(VitamContext vitamContext) {
        try (final CollectExternalClient client = CollectExternalClientFactory.getInstance().getClient()) {
            final ProjectDto createProjectDto = initProjectData();
            final RequestResponse<JsonNode> response = client.initProject(vitamContext, createProjectDto);

            if (response.isOk()) {
                final JsonNode payload = ((RequestResponseOK<JsonNode>) response).getFirstResult();

                return Optional.of(JsonHandler.getFromJsonNode(payload, ProjectDto.class));
            }

            return Optional.empty();
        } catch (VitamClientException | InvalidParseOperationException e) {
            return Optional.empty();
        }
    }

    static ProjectDto initProjectData() {
        final ProjectDto projectDto = new ProjectDto();

        projectDto.setTransferringAgencyIdentifier("Identifier5");
        projectDto.setOriginatingAgencyIdentifier("Service_producteur");
        projectDto.setSubmissionAgencyIdentifier("Service_versant");
        projectDto.setMessageIdentifier("20220302-000005");
        projectDto.setArchivalAgencyIdentifier("Identifier4");
        projectDto.setLegalStatus(LegalStatusType.PRIVATE_ARCHIVE.value());
        projectDto.setComment("Versement du service producteur : Cabinet de Michel Mercier");
        projectDto.setName("Projet de versement");
        projectDto.setArchivalAgreement("ArchivalAgreement0");

        return projectDto;
    }

    public static Optional<TransactionDto> createTransaction(final VitamContext vitamContext, final String projectId) {
        try (final CollectExternalClient client = CollectExternalClientFactory.getInstance().getClient()) {
            final TransactionDto createTransactionDto = initTransaction();
            final RequestResponse<JsonNode> response = client.initTransaction(vitamContext, createTransactionDto, projectId);

            if (response.isOk()) {
                final JsonNode payload = ((RequestResponseOK<JsonNode>) response).getFirstResult();

                return Optional.of(JsonHandler.getFromJsonNode(payload, TransactionDto.class));
            }

            return Optional.empty();
        } catch (VitamClientException | InvalidParseOperationException e) {
            return Optional.empty();
        }
    }

    public static void uploadZipTransaction(final VitamContext vitamContext, final String transactionId, final String zipPath) {
        try (
                final CollectExternalClient client = CollectExternalClientFactory.getInstance().getClient();
                final InputStream is = PropertiesUtils.getResourceAsStream(zipPath)
        ) {
            client.uploadProjectZip(vitamContext, transactionId, is);
        } catch (IOException | VitamClientException e) {
            throw new RuntimeException(e);
        }
    }

    public static void closeTransaction(final VitamContext vitamContext, final String transactionId) {
        try (final CollectExternalClient client = CollectExternalClientFactory.getInstance().getClient()) {
            RequestResponse response = client.closeTransaction(vitamContext, transactionId);

            if (response.getStatus() != 200) {
                throw new RuntimeException("Transaction close action is not OK, but hasn't raised any exception");
            }
        } catch (VitamClientException e) {
            throw new RuntimeException(e);
        }
    }

    static TransactionDto initTransaction() {
        final TransactionDto transactionDto = new TransactionDto();

        transactionDto.setName("My Transaction");
        transactionDto.setArchivalAgreement("ArchivalAgreement0");
        transactionDto.setAcquisitionInformation("AcquisitionInformation");
        transactionDto.setArchivalAgencyIdentifier("Identifier4");
        transactionDto.setTransferringAgencyIdentifier("Identifier5");
        transactionDto.setOriginatingAgencyIdentifier("Service_producteur");
        transactionDto.setSubmissionAgencyIdentifier("Service_versant");
        transactionDto.setMessageIdentifier("20220302-000005");
        transactionDto.setLegalStatus(LegalStatusType.PRIVATE_ARCHIVE.value());
        transactionDto.setComment("Versement du service producteur : Cabinet de Michel Mercier");

        return transactionDto;
    }

    public static void uploadUnit(VitamContext vitamContext, final String transactionId, final String resourcePath) {
        try (
                final CollectExternalClient client = CollectExternalClientFactory.getInstance().getClient();
                final InputStream is = PropertiesUtils.getResourceAsStream(resourcePath)
        ) {
            final JsonNode jsonNode = JsonHandler.getFromInputStream(is);
            final RequestResponse<JsonNode> response = client.uploadArchiveUnit(vitamContext, jsonNode, transactionId);

            if (!response.isOk()) {
                throw new RuntimeException("Something wrong with archive upload");
            }
        } catch (InvalidParseOperationException | VitamClientException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void updateUnit(VitamContext vitamContext, final String transactionId, final String resourcePath) throws IOException, VitamClientException {
        try (
                final CollectExternalClient client = CollectExternalClientFactory.getInstance().getClient();
                final InputStream is = PropertiesUtils.getResourceAsStream(resourcePath)
        ) {
            final String data = PropertiesUtils.getResourceAsString(resourcePath);
            assertThat(data.length()).isGreaterThan(0);

            final RequestResponse<JsonNode> response = client.updateUnits(vitamContext, transactionId, is);

            if (!response.isOk()) {
                throw new RuntimeException("Something wrong with archive upload");
            }
        }
    }
}
