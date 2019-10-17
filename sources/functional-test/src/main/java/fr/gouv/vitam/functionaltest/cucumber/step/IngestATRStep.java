package fr.gouv.vitam.functionaltest.cucumber.step;

import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import fr.gouv.vitam.common.client.VitamContext;
import fr.gouv.vitam.common.exception.VitamClientException;
import fr.gouv.vitam.common.external.client.IngestCollection;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static org.assertj.core.api.Assertions.fail;

/**
 * Class for ATR tests.
 */
public class IngestATRStep {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(IngestATRStep.class);

    private World world;

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

        removeTemporaryAtrFile();

        Response response = world.getIngestClient()
            .downloadObjectAsync(
                new VitamContext(world.getTenantId()).setApplicationSessionId(world.getApplicationSessionId()),
                world.getOperationId(), IngestCollection.ARCHIVETRANSFERREPLY);
        if (response.getStatus() == Status.OK.getStatusCode()) {
            File tempFile = Files.createTempFile("ATR-" + world.getOperationId(), ".xml").toFile();
            try (InputStream inputStream = response.readEntity(InputStream.class)) {
                Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            world.setAtrFile(tempFile.toPath());

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
    public void final_status_atr(String replyCode) throws IOException {
        String atr = FileUtils.readFileToString(world.getAtrFile().toFile(), StandardCharsets.UTF_8);
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
    public void atr_contains_value(List<String> values) throws IOException {
        String atr = FileUtils.readFileToString(world.getAtrFile().toFile(), StandardCharsets.UTF_8);
        for (String value : values) {
            if (!StringUtils.contains(atr, value)) {
                LOGGER.error(String.format("%s value was not found in ATR", value));
                fail(String.format("%s value was not found in ATR", value));
            }
        }
    }

    /**
     * Check if the ATR contains a complex string with special chars (like , " . { })
     *
     * @param value value
     */
    @Then("^le fichier ATR contient la  chaîne de caractères$")
    public void atr_contains_complex_value(String value) throws IOException {
        String atr = FileUtils.readFileToString(world.getAtrFile().toFile(), StandardCharsets.UTF_8);
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
    public void atr_contains_n_times_the_value(int count, String value) throws IOException {
        String atr = FileUtils.readFileToString(world.getAtrFile().toFile(), StandardCharsets.UTF_8);
        int realCount = StringUtils.countMatches(atr, value);
        if (realCount != count) {
            LOGGER.error(String.format("expected %d times the value %s but was %d times", count, value, realCount));
            fail(String.format("expected %d times the value %s but was %d times", count, value, realCount));
        }
    }

    /**
     * Check if the ATR contains the tag exactly the number of time given
     *
     * @param count nomber of times
     * @param tag tag
     */
    @Then("^le fichier ATR contient (.*) balise[s]? de type (.*)$")
    public void atr_contains_n_times_the_tag(int count, String tag) throws IOException {
        String atr = FileUtils.readFileToString(world.getAtrFile().toFile(), StandardCharsets.UTF_8);

        // count ending tag and empty tag to ensure there is no attribute in the checked tag
        int realCount = StringUtils.countMatches(atr, "</" + tag + ">")
            + StringUtils.countMatches(atr, "<" + tag + "/>");

        if (realCount != count) {
            LOGGER.error(String.format("expected %d tags %s but was %d", count, tag, realCount));
            fail(String.format("expected %d tags %s but was %d", count, tag, realCount));

        }
    }

    /**
     * The atr contains the tag </ArchiveUnit> exactly the number given
     *
     * @param nbUnits number of units
     */
    @Then("^le fichier ATR contient (.*) unité[s]? archivistique[s]?$")
    public void atr_contains_units(int nbUnits) throws IOException {
        atr_contains_n_times_the_tag(nbUnits, "ArchiveUnit");
    }

    /**
     * The atr contains the tag </BinaryDataObject> exactly the number given
     *
     * @param nbBinaryObjects number of binary objects
     */
    @Then("^le fichier ATR contient (.*) objet[s]? binaire[s]?$")
    public void atr_contains_binary_objects(int nbBinaryObjects) throws IOException {
        atr_contains_n_times_the_tag(nbBinaryObjects, "BinaryDataObject");
    }

    /**
     * The atr contains the tag </PhysicalDataObject> exactly the number given
     *
     * @param nbPhysicalObjects number of physical objects
     */
    @Then("^le fichier ATR contient (.*) objet[s]? physique[s]?$")
    public void atr_contains_physical_objects(int nbPhysicalObjects) throws IOException {
        atr_contains_n_times_the_tag(nbPhysicalObjects, "PhysicalDataObject");
    }


    private void removeTemporaryAtrFile() {
        if (world.getAtrFile() != null) {
            FileUtils.deleteQuietly(world.getAtrFile().toFile());
            world.setAtrFile(null);
        }
    }
}
