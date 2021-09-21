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
package fr.gouv.vitam.security.internal.rest.repository;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.security.internal.common.model.CertificateBaseModel;
import fr.gouv.vitam.security.internal.common.model.CertificateStatus;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.List;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;

/**
 * Certificate state updater helper class
 */
public class CertificateCRLCheckRepositoryHelper {

    private final MongoCollection<Document> certificateCollection;

    public CertificateCRLCheckRepositoryHelper(MongoCollection<Document> certificateCollection) {
        this.certificateCollection = certificateCollection;
    }

    public FindIterable<Document> findCertificate(String issuerDN, CertificateStatus certificateStatus) {

        Bson crlCAFilter = and(eq(CertificateBaseModel.ISSUER_DN_TAG, issuerDN),
            eq(CertificateBaseModel.STATUS_TAG, certificateStatus.name()));

        return certificateCollection.find(crlCAFilter);
    }

    public void updateCertificateState(List<String> certificatesToUpdate, CertificateStatus certificateStatus) {

        Bson fieldsToUpdateBson = set(CertificateBaseModel.STATUS_TAG, certificateStatus.name());

        if (certificateStatus.equals(CertificateStatus.REVOKED)) {
            fieldsToUpdateBson = combine(fieldsToUpdateBson,
                set(CertificateBaseModel.REVOCATION_DATE_TAG,
                    LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()))
            );
        }
        certificateCollection
            .updateMany(in(VitamDocument.ID, certificatesToUpdate),
                fieldsToUpdateBson);
    }
}
