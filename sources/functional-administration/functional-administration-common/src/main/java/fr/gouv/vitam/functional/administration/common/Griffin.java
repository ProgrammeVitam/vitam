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
package fr.gouv.vitam.functional.administration.common;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import org.bson.Document;

/**
 * Griffin class
 */
public class Griffin extends VitamDocument<Griffin> {

    public Griffin() {}

    public Griffin(JsonNode content) {
        super(content);
    }

    public Griffin(Document document) {
        super(document);
    }

    public Griffin(String content) {
        super(content);
    }

    public static final String IDENTIFIER = "Identifier";

    private static final String NAME = "Name";

    private static final String DESCRIPTION = "Description";

    private static final String EXECUTABLE_VERSION = "ExecutableVersion";

    private static final String EXECUTABLE_NAME = "ExecutableName";

    @Override
    public VitamDocument<Griffin> newInstance(JsonNode content) {
        return new Griffin(content);
    }

    public Griffin setId(String id) {
        append(VitamDocument.ID, id);
        return this;
    }

    public String getIdentifier() {
        return getString(IDENTIFIER);
    }

    public Griffin setIdentifier(String identifier) {
        append(IDENTIFIER, identifier);
        return this;
    }

    public String getName() {
        return getString(NAME);
    }

    public Griffin setName(String name) {
        append(NAME, name);
        return this;
    }

    public String getDescription() {
        return getString(DESCRIPTION);
    }

    public Griffin setDescription(String description) {
        append(DESCRIPTION, description);
        return this;
    }

    public String getExecutableVersion() {
        return getString(EXECUTABLE_VERSION);
    }

    public Griffin setExecutableVersion(String executableVersion) {
        append(EXECUTABLE_VERSION, executableVersion);
        return this;
    }

    public String getExecutableName() {
        return getString(EXECUTABLE_NAME);
    }

    public Griffin setExecutableName(String executableName) {
        append(EXECUTABLE_NAME, executableName);
        return this;
    }
}
