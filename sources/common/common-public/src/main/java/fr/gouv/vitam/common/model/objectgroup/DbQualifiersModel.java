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
package fr.gouv.vitam.common.model.objectgroup;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DbQualifiersModel {

    @JsonProperty("qualifier")
    private String qualifier;

    @JsonProperty("_nbc")
    private int nbc;

    @JsonProperty("versions")
    private List<DbVersionsModel> versions;

    public DbQualifiersModel(String qualifier, int nbc, List<DbVersionsModel> versions) {
        this.qualifier = qualifier;
        this.nbc = nbc;
        this.versions = versions;
    }

    public DbQualifiersModel() {
        // empty constructor for deserialization
    }

    public String getQualifier() {
        return qualifier;
    }

    public void setQualifier(String qualifier) {
        this.qualifier = qualifier;
    }

    public int getNbc() {
        return nbc;
    }

    public void setNbc(int nbc) {
        this.nbc = nbc;
    }

    public List<DbVersionsModel> getVersions() {
        return versions;
    }

    public void setVersions(List<DbVersionsModel> versions) {
        this.versions = versions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        DbQualifiersModel that = (DbQualifiersModel) o;
        return nbc == that.nbc
            && qualifier.equals(that.qualifier)
            && CollectionUtils.isEqualCollection(versions, that.versions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(qualifier, nbc, versions);
    }
}
