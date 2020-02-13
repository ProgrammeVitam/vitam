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
package fr.gouv.vitam.functional.administration.agencies.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.functional.administration.common.ReportConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Data Transfer Object Model of Reporting
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class AgenciesReport {


    @JsonProperty("AgenciesToImport")
    private List<String> agenciesToImport = new ArrayList<>();
    @JsonProperty("InsertAgencies")
    private List<String> insertAgencies = new ArrayList<>();

    @JsonProperty("UpdatedAgencies")
    private List<String> updatedAgencies = new ArrayList<>();
    @JsonProperty("UsedAgencies By Contract")
    private List<String> usedAgenciesByContracts = new ArrayList<>();
    @JsonProperty("UsedAgencies By AU")
    private List<String> usedAgenciesByAu = new ArrayList<>();
    @JsonProperty("UsedAgencies to Delete")
    private List<String> usedAgenciesToDelete = new ArrayList<>();

    @JsonProperty("Operation")
    private HashMap<String, String> jdo = new HashMap<>();
    @JsonProperty("error")
    private HashMap<String, Object> errors;

    /**
     *
     * @return
     */
    public List<String> getAgenciesToImport() {
        return agenciesToImport;
    }

    /**
     *
     * @param agenciesToImport
     */
    public void setAgenciesToImport(List<String> agenciesToImport) {
        this.agenciesToImport = agenciesToImport;
    }

    /**
     *
     * @return
     */
    public List<String> getInsertAgencies() {
        return insertAgencies;
    }

    /**
     *
     * @param insertAgencies
     */
    public void setInsertAgencies(List<String> insertAgencies) {
        this.insertAgencies = insertAgencies;
    }

    /**
     *
     * @return
     */
    public List<String> getUpdatedAgencies() {
        return updatedAgencies;
    }

    /**
     *
     * @param updatedAgencies
     */
    public void setUpdatedAgencies(List<String> updatedAgencies) {
        this.updatedAgencies = updatedAgencies;
    }

    /**
     *
     * @return
     */
    public List<String> getUsedAgenciesByContracts() {
        return usedAgenciesByContracts;
    }

    /**
     *
     * @param usedAgenciesByContracts
     */
    public void setUsedAgenciesByContracts(List<String> usedAgenciesByContracts) {
        this.usedAgenciesByContracts = usedAgenciesByContracts;
    }

    /**
     *
     * @return
     */
    public List<String> getUsedAgenciesByAu() {
        return usedAgenciesByAu;
    }

    /**
     *
     * @param usedAgenciesByAu
     */
    public void setUsedAgenciesByAu(List<String> usedAgenciesByAu) {
        this.usedAgenciesByAu = usedAgenciesByAu;
    }

    /**
     *
     * @return
     */
    public List<String> getUsedAgenciesToDelete() {
        return usedAgenciesToDelete;
    }

    /**
     *
     * @param usedAgenciesToDelete
     */
    public void setUsedAgenciesToDelete(List<String> usedAgenciesToDelete) {
        this.usedAgenciesToDelete = usedAgenciesToDelete;
    }

    /**
     *
     * @return
     */
    public HashMap<String, String> getJdo() {
        return jdo;
    }

    /**
     *
     * @param jdo
     */
    public void setJdo(HashMap<String, String> jdo) {
        this.jdo = jdo;
    }

    /**
     *
     * @return
     */
    public HashMap<String, Object> getErrors() {
        return errors;
    }

    /**
     *
     * @param errors
     */
    public void setError(HashMap<String, Object> errors) {
        this.errors = errors;
    }

}
