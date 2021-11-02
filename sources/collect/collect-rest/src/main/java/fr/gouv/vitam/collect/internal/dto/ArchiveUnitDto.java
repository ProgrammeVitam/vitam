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
package fr.gouv.vitam.collect.internal.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ArchiveUnitDto implements Serializable {

    @JsonProperty("id")
    private String id;

    @JsonProperty("content")
    private ArchiveUnitContent content;

    @JsonProperty("up")
    private String parentUnit;

    @JsonProperty("_opi")
    private String transactionId;

    @JsonProperty("got")
    private String objectGroupDto;

    public ArchiveUnitDto() {
        //Empty constructor for serialization
    }

    public ArchiveUnitDto(String id, ArchiveUnitContent content, String parentUnit, String objectGroupDto, String transactionId) {
        this.id = id;
        this.content = content;
        this.parentUnit = parentUnit;
        this.objectGroupDto = objectGroupDto;
        this.transactionId = transactionId;
    }

    public ArchiveUnitContent getContent() {
        return content;
    }

    public void setContent(ArchiveUnitContent content) {
        this.content = content;
    }

    public String getParentUnit() {
        return parentUnit;
    }

    public void setParentUnit(String parentUnit) {
        this.parentUnit = parentUnit;
    }

    public ArchiveUnitDto(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getObjectGroupDto() {
        return objectGroupDto;
    }

    public void setObjectGroupDto(String objectGroupDto) {
        this.objectGroupDto = objectGroupDto;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ArchiveUnitDto that = (ArchiveUnitDto) o;
        return Objects.equals(id, that.id) && Objects.equals(content, that.content) &&
            Objects.equals(parentUnit, that.parentUnit) &&
            Objects.equals(transactionId, that.transactionId) &&
            Objects.equals(objectGroupDto, that.objectGroupDto);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, content, parentUnit, transactionId, objectGroupDto);
    }

    @Override
    public String toString() {
        return "ArchiveUnitDto{" +
            "id='" + id + '\'' +
            ", content=" + content +
            ", parentUnit='" + parentUnit + '\'' +
            ", transactionId='" + transactionId + '\'' +
            ", objectGroupDto='" + objectGroupDto + '\'' +
            '}';
    }
}
