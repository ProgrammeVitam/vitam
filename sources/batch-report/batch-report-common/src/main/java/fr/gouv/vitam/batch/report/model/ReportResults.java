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
package fr.gouv.vitam.batch.report.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(value = { "total" }, allowGetters = true)
public class ReportResults {

    @JsonProperty("OK")
    private Integer nbOk;

    @JsonProperty("KO")
    private Integer nbKo;

    @JsonProperty("WARNING")
    private Integer nbWarning;

    public ReportResults() {
        this(0, 0, 0);
    }

    public ReportResults(Integer nbOk, Integer nbKo, Integer nbWarning) {
        this.nbOk = nbOk;
        this.nbKo = nbKo;
        this.nbWarning = nbWarning;
    }

    public Integer getNbOk() {
        return nbOk;
    }

    public void setNbOk(Integer nbOk) {
        this.nbOk = nbOk;
    }

    public Integer getNbKo() {
        return nbKo;
    }

    public void setNbKo(Integer nbKo) {
        this.nbKo = nbKo;
    }

    public Integer getNbWarning() {
        return nbWarning;
    }

    public void setNbWarning(Integer nbWarning) {
        this.nbWarning = nbWarning;
    }

    @JsonGetter("total")
    public Integer getTotal() {
        return this.nbOk + this.nbWarning + this.nbKo;
    }

    public void addOneStatus(String status, Integer count) {
        switch (status) {
            case "OK":
                this.nbOk = count;
                break;
            case "WARNING":
                this.nbWarning = count;
                break;
            case "KO":
                this.nbKo = count;
                break;
            default:
                throw new IllegalArgumentException("Status invalid " + status);
        }
    }

    public void incrementStatus(String status, Integer count) {
        switch (status) {
            case "OK":
                this.nbOk = this.nbOk + count;
                break;
            case "WARNING":
                this.nbWarning = this.nbWarning + count;
                break;
            case "KO":
                this.nbKo = this.nbKo + count;
                break;
            default:
                throw new IllegalArgumentException("Status invalid " + status);
        }
    }
}
