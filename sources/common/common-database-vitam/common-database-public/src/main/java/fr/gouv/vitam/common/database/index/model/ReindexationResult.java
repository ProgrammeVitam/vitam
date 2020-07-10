/*
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2020)
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
package fr.gouv.vitam.common.database.index.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * IndexationResult pojo
 */
public class ReindexationResult {

    @JsonProperty("collectionName")
    private String collectionName;

    @JsonProperty("OK")
    private List<ReindexationOK> OK;

    @JsonProperty("KO")
    private List<ReindexationKO> KO;

    public String getCollectionName() {
        return collectionName;
    }

    public ReindexationResult setCollectionName(String collectionName) {
        this.collectionName = collectionName;
        return this;
    }

    @JsonGetter("OK")
    public List<ReindexationOK> getIndexOK() {
        return OK;
    }

    public void setIndexOK(List<ReindexationOK> indexesOK) {
        this.OK = indexesOK;
    }

    @JsonGetter("KO")
    public List<ReindexationKO> getIndexKO() {
        return KO;
    }

    public void setIndexKO(List<ReindexationKO> indexesKO) {
        this.KO = indexesKO;
    }

    public ReindexationResult addIndexOK(ReindexationOK reindexationOK) {
        if(this.OK == null) {
            this.OK = new ArrayList<>();
        }
        this.OK.add(reindexationOK);
        return this;
    }

    public ReindexationResult addIndexKO(ReindexationKO reindexationKO) {
        if(this.KO == null) {
            this.KO = new ArrayList<>();
        }
        this.KO.add(reindexationKO);
        return this;
    }
}
