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
package fr.gouv.vitam.collect.internal.helpers;

import fr.gouv.vitam.common.format.identification.model.FormatIdentifierResponse;
import fr.gouv.vitam.common.format.identification.siegfried.FormatIdentifierSiegfried;
import fr.gouv.vitam.common.model.objectgroup.DbObjectGroupModel;
import fr.gouv.vitam.common.model.objectgroup.DbQualifiersModel;
import fr.gouv.vitam.common.model.objectgroup.DbVersionsModel;

import java.util.Comparator;
import java.util.List;

public class TransactionHelper {
    private TransactionHelper() throws IllegalAccessException {
        throw new IllegalAccessException("Utility class!");
    }

    public static String readMessageDigestReturn(byte[] theDigestResult) {
        StringBuilder sb = new StringBuilder();
        for (byte b : theDigestResult) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString().toLowerCase();
    }

    public static FormatIdentifierResponse getFirstPronomFormat(List<FormatIdentifierResponse> formats) {
        return formats.stream()
            .filter(format -> FormatIdentifierSiegfried.PRONOM_NAMESPACE.equals(format.getMatchedNamespace()))
            .findFirst().orElse(null);
    }

    public static DbVersionsModel getObjectVersionsModel(DbObjectGroupModel dbObjectGroupModel, String qualifier, int version) {

        if (dbObjectGroupModel.getQualifiers() == null) {
            return null;
        }

        final String dataObjectVersion = qualifier + "_" + version;

        return dbObjectGroupModel.getQualifiers().stream()
            .peek(dbQualifiersModel -> {
                if (dbQualifiersModel.getQualifier() != null && dbQualifiersModel.getQualifier().contains("_")) {
                    dbQualifiersModel.setQualifier(dbQualifiersModel.getQualifier().split("_")[0]);
                }
            })
            .filter(dbQualifiersModel -> qualifier.equals(dbQualifiersModel.getQualifier()))
            .flatMap(dbQualifiersModel -> dbQualifiersModel.getVersions().stream())
            .filter(dbVersionsModel -> dataObjectVersion.equals(dbVersionsModel.getDataObjectVersion()))
            .findFirst().orElse(null);
    }

    public static int getLastVersion(DbQualifiersModel qualifierModelToUpdate) {
        return qualifierModelToUpdate.getVersions()
            .stream()
            .map(DbVersionsModel::getDataObjectVersion)
            .map(dataObjectVersion -> dataObjectVersion.split("_")[1])
            .map(Integer::parseInt)
            .max(Comparator.naturalOrder())
            .orElse(0);
    }

    public static DbQualifiersModel findQualifier(List<DbQualifiersModel> qualifiers, String targetQualifier) {
        return qualifiers.stream()
            .filter(qualifier -> qualifier.getQualifier().equals(targetQualifier))
            .findFirst()
            .orElse(null);
    }

    public static void checkVersion(int version, int lastVersion) {
        if (version != lastVersion) {
            throw new IllegalArgumentException("version number not valid " + version);
        }
    }
}
