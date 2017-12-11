package fr.gouv.vitam.functionaltest.cucumber.step;

import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.AutoCloseableSoftAssertions;

import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.external.client.IngestCollection;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.stream.StreamUtils;

/**
 * Class for ATR tests.
 */
public class IngestATRStep {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestATRStep.class);

    private World world;

    private String atr;

    public IngestATRStep(World world) {
        this.world = world;
    }

    /**
     * Download the ATR file
     * 
     * @throws VitamClientException
     * @throws IOException
     */
    @When("je télécharge son fichier ATR")
    public void download_atr()
        throws VitamClientException, IOException {
        Response response = world.getIngestClient()
            .downloadObjectAsync(
                new VitamContext(world.getTenantId()).setApplicationSessionId(world.getApplicationSessionId()),
                world.getOperationId(), IngestCollection.ARCHIVETRANSFERREPLY);
        if (response.getStatus() == Status.OK.getStatusCode()) {
            InputStream inputStream = response.readEntity(InputStream.class);
            atr = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            StreamUtils.closeSilently(inputStream);
            world.getIngestClient().consumeAnyEntityAndClose(response);
        } else {
            LOGGER.error(String.format("could not download ATR for operationId: %s", world.getOperationId()));
            fail(String.format("could not download ATR for operationId: %s", world.getOperationId()));
        }
    }

    /**
     * Check the replyCode value in the ATR
     * 
     * @param replyCode reply code
     */
    @Then("^l'état final du fichier ATR est (.*)$")
    public void final_status_atr(String replyCode) {
        if (!StringUtils.contains(atr, "<ReplyCode>" + replyCode + "</ReplyCode>")) {
            LOGGER.error(String.format("replyCode %s was not found in ATR", replyCode));
            fail(String.format("replyCode %s was not found in ATR", replyCode));
        }
    }

    /**
     * Check if the atr contains the String values
     * 
     * @param values values
     */
    @Then("^le fichier ATR contient (?:la|les) valeur[s]? (.*)$")
    public void atr_contains_value(List<String> values) {
        try (AutoCloseableSoftAssertions softly = new AutoCloseableSoftAssertions()) {
            for (String value : values) {
                if (!StringUtils.contains(atr, value)) {
                    LOGGER.error(String.format("%s value was not found in ATR", value));
                    fail(String.format("%s value was not found in ATR", value));
                }
            }
        }
    }

    /**
     * Check if the ATR contains a complex string with special chars (like , " . { })
     * 
     * @param value value
     */
    @Then("^le fichier ATR contient la  chaîne de caractères$")
    public void atr_contains_complex_value(String value) {
        if (!StringUtils.contains(atr, value)) {
            LOGGER.error(String.format("%s value was not found in ATR", value));
            fail(String.format("%s value was not found in ATR", value));
        }
    }

    /**
     * Check if the ATR contains the value exactly the number of times given
     * 
     * @param count number of times
     * @param value value
     */
    @Then("^le fichier ATR contient (.*) fois la valeur (.*)$")
    public void atr_contains_n_times_the_value(int count, String value) {
        int realCount = StringUtils.countMatches(atr, value);
        if (realCount != count) {
            LOGGER.error(String.format("expected %d% times the value %s but was %d times", count, value, realCount));
            fail(String.format("expected %d% times the value %s but was %d times", count, value, realCount));
        }
    }

    /**
     * Check if the ATR contains the tag exactly the number of time given
     * 
     * @param count nomber of times
     * @param tag tag
     */
    @Then("^le fichier ATR contient (.*) balise[s]? de type (.*)$")
    public void atr_contains_n_times_the_tag(int count, String tag) {
        // count ending tag to ensure there is no attribute in the checked tag
        int realCount = StringUtils.countMatches(atr, "</" + tag + ">");
        if (realCount != count) {
            LOGGER.error(String.format("expected %d% tags %s but was %d", count, tag, realCount));
            fail(String.format("expected %d% tags %s but was %d", count, tag, realCount));

        }
    }

    /**
     * The atr contains the tag </ArchiveUnit> exactly the number given
     * 
     * @param nbUnits number of units
     */
    @Then("^le fichier ATR contient (.*) unité[s]? archivistique[s]?$")
    public void atr_contains_units(int nbUnits) {
        atr_contains_n_times_the_tag(nbUnits, "ArchiveUnit");
    }

    /**
     * The atr contains the tag </BinaryDataObject> exactly the number given
     * 
     * @param nbBinaryObjects number of binary objects
     */
    @Then("^le fichier ATR contient (.*) objet[s]? binaire[s]?$")
    public void atr_contains_binary_objects(int nbBinaryObjects) {
        atr_contains_n_times_the_tag(nbBinaryObjects, "BinaryDataObject");
    }

    /**
     * The atr contains the tag </PhysicalDataObject> exactly the number given
     * 
     * @param nbPhysicalObjects number of physical objects
     */
    @Then("^le fichier ATR contient (.*) objet[s]? physique[s]?$")
    public void atr_contains_physical_objects(int nbPhysicalObjects) {
        atr_contains_n_times_the_tag(nbPhysicalObjects, "PhysicalDataObject");
    }

}
