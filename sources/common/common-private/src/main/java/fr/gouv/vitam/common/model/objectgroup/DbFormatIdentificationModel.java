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
package fr.gouv.vitam.common.model.objectgroup;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.json.Difference;

import java.util.Objects;

/**
 * DbFormatIdentificationModel
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DbFormatIdentificationModel {

    @JsonProperty("FormatLitteral")
    private String formatLitteral;

    @JsonProperty("MimeType")
    private String mimeType;

    @JsonProperty("FormatId")
    private String formatId;

    @JsonProperty("Encoding")
    private String encoding;

    public DbFormatIdentificationModel(String formatLitteral, String mimeType, String formatId) {
        this.formatLitteral = formatLitteral;
        this.mimeType = mimeType;
        this.formatId = formatId;
    }

    public DbFormatIdentificationModel() {}

    public String getFormatLitteral() {
        return formatLitteral;
    }

    public void setFormatLitteral(String formatLitteral) {
        this.formatLitteral = formatLitteral;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getFormatId() {
        return formatId;
    }

    public void setFormatId(String formatId) {
        this.formatId = formatId;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DbFormatIdentificationModel that = (DbFormatIdentificationModel) o;
        return (
            Objects.equals(formatLitteral, that.formatLitteral) &&
            Objects.equals(mimeType, that.mimeType) &&
            Objects.equals(formatId, that.formatId) &&
            Objects.equals(encoding, that.encoding)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(formatLitteral, mimeType, formatId, encoding);
    }

    public Difference difference(Object o) {
        if (this == o) {
            return Difference.empty();
        }
        if (o == null || getClass() != o.getClass()) {
            return Difference.empty();
        }
        DbFormatIdentificationModel that = (DbFormatIdentificationModel) o;
        Difference<String> difference = new Difference<>(FormatIdentificationModel.class.getSimpleName());
        if (!Objects.equals(formatLitteral, that.formatLitteral)) {
            difference.add("formatLitteral", formatLitteral, that.formatLitteral);
        }
        if (!Objects.equals(mimeType, that.mimeType)) {
            difference.add("mimeType", mimeType, that.mimeType);
        }
        if (!Objects.equals(formatId, that.formatId)) {
            difference.add("formatId", formatId, that.formatId);
        }
        if (!Objects.equals(encoding, that.encoding)) {
            difference.add("encoding", encoding, that.encoding);
        }
        return difference;
    }
}
