package fr.gouv.vitam.functional.administration.common.client;

import com.mongodb.client.FindIterable;
import fr.gouv.vitam.common.client.OntologyLoader;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.json.BsonHelper;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class FunctionAdministrationOntologyLoader implements OntologyLoader {
    @Override
    public List<OntologyModel> loadOntologies() {
        try {
            ArrayList<OntologyModel> ontologyModels = new ArrayList<>();
            FindIterable<Document> documents = FunctionalAdminCollections.ONTOLOGY.getCollection().find();
            for (Document document : documents) {
                ontologyModels.add(JsonHandler.getFromString(BsonHelper.stringify(document), OntologyModel.class));
            }

            return ontologyModels;
        } catch (InvalidParseOperationException e) {
            throw new VitamRuntimeException(e);
        }
    }
}
