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
package fr.gouv.vitam.storage.engine.common.model;

import org.bson.Document;

/**
 * Offer sequence.
 */
public class OfferSequence {

    public static final String COUNTER_FIELD = "Counter";
    public static final String ID_FIELD = "_id";

    private String id;

    private long counter;

    /**
     * Constructor, jackson usage only
     */
    public OfferSequence() {
    }

    /**
     * Constructor
     *
     * @param id id
     */
    public OfferSequence(String id) {
        this.id = id;
    }

    /**
     * Gets the id
     *
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the id
     *
     * @param id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the counter
     *
     * @return counter
     */
    public long getCounter() {
        return counter;
    }

    /**
     * Sets the counter
     *
     * @param counter counter
     */
    public void setCounter(long counter) {
        this.counter = counter;
    }

    /**
     * Ugly workaround to keep object type when convert JSON to Mongo Document
     *
     * TODO: switch to mongo-java-driver &gt;= 3.5.0 and use PojoCodecProvider
     *
     * @return Document
     * @see <a href=
     * "http://mongodb.github.io/mongo-java-driver/3.5/driver/getting-started/quick-start-pojo/#creating-a-custom-codecregistry">PojoCodecProvider</a>
     */
    public Document toDocument() {
        Document document = new Document();
        document.put(ID_FIELD, id);
        document.put(COUNTER_FIELD, counter);
        return document;
    }
}
