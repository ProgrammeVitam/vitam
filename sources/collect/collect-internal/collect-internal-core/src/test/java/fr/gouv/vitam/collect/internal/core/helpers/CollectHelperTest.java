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

import fr.gouv.vitam.collect.common.dto.ProjectDto;
import fr.gouv.vitam.collect.common.dto.TransactionDto;
import fr.gouv.vitam.collect.common.enums.TransactionStatus;
import fr.gouv.vitam.collect.internal.core.common.ManifestContext;
import fr.gouv.vitam.common.LocalDateUtil;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

public class CollectHelperTest {

    String creationDate = LocalDateUtil.nowFormatted();
    ProjectDto projectDto;

    @Before
    public void init() {
        projectDto = new ProjectDto(
            "XXXX00000111111",
            "name",
            "acquisitionInformation",
            "legalStatus",
            creationDate,
            creationDate,
            "status",
            "archivalAgreement",
            "messageIdentifier",
            "archivalAgencyIdentifier",
            "transferringAgencyIdentifier",
            "originatingAgencyIdentifier",
            "submissionAgencyIdentifier",
            "archivalProfile",
            "comment",
            "unitUp",
            1
        );
    }

    @Test
    public void mapTransactionDtoToManifestContextTest_should_take_transaction_value() {
        // GIVEN
        TransactionDto transactionDto = new TransactionDto(
            "XXXX00000111111",
            "Tr_archivalAgreement",
            "Tr_messageIdentifier",
            "Tr_archivalAgencyIdentifier",
            "Tr_transferringAgencyIdentifier",
            "Tr_originatingAgencyIdentifier",
            "Tr_submissionAgencyIdentifier",
            "Tr_archivalProfile",
            "Tr_comment",
            1,
            "Tr_acquisitionInformation",
            "Tr_legalStatus",
            creationDate,
            creationDate,
            TransactionStatus.OPEN.toString()
        );

        // WHEN
        ManifestContext manifestContext = CollectHelper.mapTransactionDtoToManifestContext(transactionDto, projectDto);

        // THEN
        Assertions.assertThat(manifestContext).isNotNull();
        Assertions.assertThat(manifestContext.getAcquisitionInformation()).isEqualTo(
            transactionDto.getAcquisitionInformation()
        );
        Assertions.assertThat(manifestContext.getArchivalAgreement()).isEqualTo(transactionDto.getArchivalAgreement());
        Assertions.assertThat(manifestContext.getComment()).isEqualTo(transactionDto.getComment());
    }

    @Test
    public void mapTransactionDtoToManifestContextTest_should_take_project_value() {
        // GIVEN
        final String creationDate = LocalDateUtil.nowFormatted();
        TransactionDto transactionDto = new TransactionDto(
            "XXXX00000111111",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            1,
            null,
            null,
            creationDate,
            creationDate,
            TransactionStatus.OPEN.toString()
        );
        // WHEN
        ManifestContext manifestContext = CollectHelper.mapTransactionDtoToManifestContext(transactionDto, projectDto);

        // THEN
        Assertions.assertThat(manifestContext).isNotNull();
        Assertions.assertThat(manifestContext.getAcquisitionInformation()).isEqualTo(
            projectDto.getAcquisitionInformation()
        );
        Assertions.assertThat(manifestContext.getArchivalAgreement()).isEqualTo(projectDto.getArchivalAgreement());
        Assertions.assertThat(manifestContext.getComment()).isEqualTo(projectDto.getComment());
    }

    @Test
    public void mapTransactionDtoToManifestContextTest_should_take_mixed_value() {
        // GIVEN
        final String creationDate = LocalDateUtil.nowFormatted();
        TransactionDto transactionDto = new TransactionDto(
            "XXXX00000111111",
            "Tr_archivalAgreement",
            "Tr_messageIdentifier",
            "Tr_archivalAgencyIdentifier",
            "Tr_transferringAgencyIdentifier",
            "Tr_originatingAgencyIdentifier",
            "Tr_submissionAgencyIdentifier",
            "Tr_archivalProfile",
            "Tr_comment",
            1,
            null,
            "Tr_legalStatus",
            creationDate,
            creationDate,
            TransactionStatus.OPEN.toString()
        );
        // WHEN
        ManifestContext manifestContext = CollectHelper.mapTransactionDtoToManifestContext(transactionDto, projectDto);

        // THEN
        Assertions.assertThat(manifestContext).isNotNull();
        Assertions.assertThat(manifestContext.getAcquisitionInformation()).isEqualTo(
            projectDto.getAcquisitionInformation()
        );
        Assertions.assertThat(manifestContext.getArchivalAgreement()).isEqualTo(transactionDto.getArchivalAgreement());
        Assertions.assertThat(manifestContext.getComment()).isEqualTo(transactionDto.getComment());
    }
}
