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
package fr.gouv.vitam.common.model.administration.preservation;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;

public class ValuesPreservation {

    @JsonProperty("Extension")
    private String extension;

    @JsonProperty("Args")
    private List<String> args;

    @JsonProperty("FilteredExtractedObjectGroupData")
    private List<String> filteredExtractedObjectGroupData;

    @JsonProperty("FilteredExtractedUnitData")
    private List<String> filteredExtractedUnitData;

    public ValuesPreservation() {
        // Empty for deserialization
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public List<String> getArgs() {
        if (args == null) {
            return Collections.emptyList();
        }
        return args;
    }

    public void setArgs(List<String> args) {
        this.args = args;
    }

    public List<String> getFilteredExtractedObjectGroupData() {
        return filteredExtractedObjectGroupData;
    }

    public void setFilteredExtractedObjectGroupData(List<String> filteredExtractedObjectGroupData) {
        this.filteredExtractedObjectGroupData = filteredExtractedObjectGroupData;
    }

    public List<String> getFilteredExtractedUnitData() {
        return filteredExtractedUnitData;
    }

    public void setFilteredExtractedUnitData(List<String> filteredExtractedUnitData) {
        this.filteredExtractedUnitData = filteredExtractedUnitData;
    }

    @Override
    public String toString() {
        return (
            "ValuesPreservation{" +
            "extension='" +
            extension +
            '\'' +
            ", args=" +
            args +
            ", FilteredExtractedDataObjectGroup=" +
            filteredExtractedObjectGroupData +
            ", FilteredExtractedDataUnit=" +
            filteredExtractedUnitData +
            '}'
        );
    }
}
