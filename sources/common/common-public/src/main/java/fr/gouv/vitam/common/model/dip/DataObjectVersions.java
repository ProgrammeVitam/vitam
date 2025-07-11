/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2022)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL-C license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL-C license as
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL-C license and that you
 * accept its terms.
 */
package fr.gouv.vitam.common.model.dip;

import fr.gouv.vitam.common.model.administration.DataObjectVersionType;
import jakarta.annotation.Nullable;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class DataObjectVersions {

    private Set<String> dataObjectVersions;
    private Map<DataObjectVersionType, Set<QualifierVersion>> dataObjectVersionsPatterns;

    public DataObjectVersions() {}

    public DataObjectVersions(Set<String> dataObjectVersionToExport) {
        this.dataObjectVersions = dataObjectVersionToExport;
    }

    public DataObjectVersions(Map<DataObjectVersionType, Set<QualifierVersion>> dataObjectVersionsPatterns) {
        this.dataObjectVersionsPatterns = dataObjectVersionsPatterns;
    }

    public Set<String> getDataObjectVersions() {
        if (dataObjectVersions == null) {
            dataObjectVersions = Collections.emptySet();
        }
        return dataObjectVersions;
    }

    public void setDataObjectVersions(Set<String> dataObjectVersions) {
        this.dataObjectVersions = dataObjectVersions;
    }

    public @Nullable Map<DataObjectVersionType, Set<QualifierVersion>> getDataObjectVersionsPatterns() {
        return dataObjectVersionsPatterns;
    }

    public void setDataObjectVersionsPatterns(
        Map<DataObjectVersionType, Set<QualifierVersion>> dataObjectVersionsPatterns
    ) {
        this.dataObjectVersionsPatterns = dataObjectVersionsPatterns;
    }

    public boolean dataHasBeenSetTwice() {
        return CollectionUtils.isNotEmpty(this.dataObjectVersions) && this.dataObjectVersionsPatterns != null;
    }
}
