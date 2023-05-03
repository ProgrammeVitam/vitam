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

import fr.gouv.culture.archivesdefrance.seda.v2.LegalStatusType;
import fr.gouv.vitam.collect.common.dto.ProjectDto;
import fr.gouv.vitam.collect.common.dto.TransactionDto;
import fr.gouv.vitam.collect.common.enums.TransactionStatus;

public class CollectTestHelper {

    private static final String SUBMISSION_AGENCY_IDENTIFIER = "Service_versant";
    private static final String MESSAGE_IDENTIFIER = "20220302-000005";

    static ProjectDto initProjectData() {
        ProjectDto projectDto = new ProjectDto();
        projectDto.setTransferringAgencyIdentifier("Identifier5");
        projectDto.setOriginatingAgencyIdentifier("Service_producteur");
        projectDto.setSubmissionAgencyIdentifier(SUBMISSION_AGENCY_IDENTIFIER);
        projectDto.setMessageIdentifier(MESSAGE_IDENTIFIER);
        projectDto.setArchivalAgencyIdentifier("Identifier4");
        projectDto.setArchivalProfile("ArchiveProfile");
        projectDto.setLegalStatus(TransactionStatus.OPEN.name());
        projectDto.setComment("Versement du service producteur : Cabinet de Michel Mercier");
        projectDto.setName("Projet de versement");
        projectDto.setArchivalAgreement("ArchivalAgreement0");
        return projectDto;
    }



    static TransactionDto initTransaction() {
        TransactionDto transaction = new TransactionDto();
        transaction.setName("My Transaction");
        transaction.setArchivalAgreement("ArchivalAgreement0");
        transaction.setAcquisitionInformation("AcquisitionInformation");
        transaction.setArchivalAgencyIdentifier("Identifier4");
        transaction.setTransferringAgencyIdentifier("Identifier5");
        transaction.setOriginatingAgencyIdentifier("Service_producteur");
        transaction.setSubmissionAgencyIdentifier(SUBMISSION_AGENCY_IDENTIFIER);
        transaction.setMessageIdentifier(MESSAGE_IDENTIFIER);
        transaction.setArchivalProfile("ArchiveProfile");
        transaction.setLegalStatus(LegalStatusType.PRIVATE_ARCHIVE.value());
        transaction.setComment("Versement du service producteur : Cabinet de Michel Mercier");
        return transaction;
    }
}
