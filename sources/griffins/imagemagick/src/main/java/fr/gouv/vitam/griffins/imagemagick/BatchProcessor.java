/*******************************************************************************
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 *******************************************************************************/

package fr.gouv.vitam.griffins.imagemagick;

import static fr.gouv.vitam.griffins.imagemagick.status.ActionType.ANALYSE;
import static fr.gouv.vitam.griffins.imagemagick.status.ActionType.EXTRACT;
import static fr.gouv.vitam.griffins.imagemagick.status.ActionType.GENERATE;
import static fr.gouv.vitam.griffins.imagemagick.status.ActionType.IDENTIFY;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import fr.gouv.vitam.griffins.imagemagick.pojo.Action;
import fr.gouv.vitam.griffins.imagemagick.pojo.BatchStatus;
import fr.gouv.vitam.griffins.imagemagick.pojo.Input;
import fr.gouv.vitam.griffins.imagemagick.pojo.Output;
import fr.gouv.vitam.griffins.imagemagick.pojo.Outputs;
import fr.gouv.vitam.griffins.imagemagick.pojo.Parameters;
import fr.gouv.vitam.griffins.imagemagick.pojo.RawOutput;
import fr.gouv.vitam.griffins.imagemagick.status.AnalyseResult;
import fr.gouv.vitam.griffins.imagemagick.status.GriffinStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BatchProcessor {
    private static final Logger logger = LoggerFactory.getLogger(BatchProcessor.class);

    private final ObjectMapper mapper = new ObjectMapper();
    private final Pattern fileMatch = Pattern.compile("[a-zA-Z0-9_.\\-]+");

    private final Path batchDirectory;

    public static final String outputFilesDirName = "output-files";
    public static final String parametersFileName = "parameters.json";
    public static final String resultFileName = "result.json";
    public static final String inputFilesDirName = "input-files";

    public BatchProcessor(Path batchDirectory) {
        this.batchDirectory = batchDirectory;
    }

    public BatchStatus execute() {
        long startTime = System.currentTimeMillis();
        String batchProcessingId = batchDirectory.getFileName().toString();
        try {
            File file = batchDirectory.resolve(parametersFileName).toFile();
            Parameters parameters = mapper.readValue(file, Parameters.class);

            if (!batchProcessingId.equals(parameters.getId())) {
                throw new Exception("Batch id must be same as in " + parametersFileName);
            }

            Files.createDirectory(batchDirectory.resolve(outputFilesDirName));

            List<Output> outputs = parameters.getInputs()
                .stream()
                .flatMap(input -> executeActions(input, parameters))
                .collect(toList());

            addToFile(outputs, parameters.getRequestId(), parameters.getId());

            boolean isOutputsContainingError = outputs.stream().anyMatch(o -> o.getStatus() != GriffinStatus.OK);
            if (isOutputsContainingError || outputs.isEmpty()) {
                return BatchStatus
                    .warning(batchProcessingId, startTime, "Batch result contains error or is empty.");
            }
            return BatchStatus.ok(batchProcessingId, startTime);
        } catch (Exception e) {
            logger.error("{}", e);
            return BatchStatus.error(batchProcessingId, startTime, e);
        }
    }

    private void addToFile(List< Output> outputs, String requestId, String id) throws IOException {
        Map<String, List< Output>> outputsMap = outputs.stream()
            .collect(toMap(o -> o.getInput().getName(), Collections::singletonList, (o, o2) -> Stream.concat(o.stream(), o2.stream()).collect(Collectors.toList())));
        mapper.writer().writeValue(batchDirectory.resolve(resultFileName).toFile(), Outputs
            .of(requestId, id, outputsMap));
    }

    private Stream< Output> executeActions(
        Input input, Parameters parameters) {
        return parameters.getActions()
            .stream()
            .map(action -> apply(action, input))
            .map(raw -> postProcess(raw, parameters.isDebug()));
    }

    private  RawOutput apply(
        Action action, Input input) {
        ProcessBuilder processBuilder = new ProcessBuilder(getMagickParams(input, action));
        try {
            Process magick = processBuilder.start();
            magick.waitFor();
            return new  RawOutput(magick, processBuilder, input, getOutputname(input.getName(), action), action);
        } catch (Exception e) {
            logger.error("{}", e);
            return new  RawOutput(e, processBuilder, input, getOutputname(input.getName(), action), action);
        }
    }

    private String getOutputname(String inputname, Action actionType) {
        if (actionType.getType().equals(ANALYSE)) {
            return null;
        }
        if (actionType.getType().equals(EXTRACT)) {
            return String.format("%s-%s.%s", actionType.getType().name(), inputname, "json");
        }
        if (actionType.getType().equals(GENERATE)) {
            return String.format("%s-%s.%s", actionType.getType().name(), inputname, actionType.getValues().getExtension());
        }
        throw new IllegalStateException("Unreachable");
    }

    private List<String> getMagickParams(
        Input input, Action actionType) {
        if (actionType.getType() == null) {
            throw new RuntimeException("Action type cannot be null nor empty");
        }
        if (actionType.getType() == IDENTIFY) {
            throw new RuntimeException("Cannot IDENTIFY with imagemagick");
        }
        if (!fileMatch.matcher(input.getName()).matches()) {
            throw new RuntimeException("filename must match " + fileMatch.pattern());
        }

        List<String> actionCommand = new ArrayList<>(actionType.getType().action);

        actionCommand.replaceAll(c -> c.equals("%inputname%") ? getInputPath(input) : c);
        actionCommand.replaceAll(c -> c.equals("%outputname%") ? getOutputPath(input, actionType) : c);
        actionCommand.replaceAll(c -> c.equals("%format%:%inputname%") ? String.format("%s:%s", PuidImageType.formatTypes.get(input.getFormatId()), getInputPath(input)) : c);

        int indexOf = actionCommand.indexOf("%args%");
        if (indexOf != -1) {
            actionCommand.remove(indexOf);
            actionCommand.addAll(indexOf, actionType.getValues().getArgs());
        }

        return actionCommand;
    }

    private String getInputPath(Input input) {
        return batchDirectory.resolve(inputFilesDirName).resolve(input.getName()).toString();
    }

    private String getOutputPath(
        Input input, Action actionType) {
        return batchDirectory.resolve(outputFilesDirName).resolve(getOutputname(input.getName(), actionType)).toString();
    }

    private Output postProcess(
        RawOutput rawOutput, boolean debug) throws RuntimeException {
        switch (rawOutput.action.getType()) {
            case GENERATE:
                return generate(rawOutput, debug);
            case ANALYSE:
                return analyze(rawOutput, debug);
            case EXTRACT:
                return extract(rawOutput, debug);
            default:
                throw new IllegalStateException("Unreachable");
        }
    }

    private  Output analyze(
        RawOutput rawOutput, boolean debug) {
        if (rawOutput.exception != null) {
            return rawOutput.toError(debug);
        }
        Output output = rawOutput.toOk(debug);
        if (rawOutput.exitCode > 0) {
            output.setAnalyseResult(AnalyseResult.WRONG_FORMAT);
            return output;
        }
        String warning = GriffinStatus.WARNING.name().toLowerCase();
        boolean outputContainsWarnings = rawOutput.stdout.toLowerCase().contains(warning) || rawOutput.stderr.toLowerCase().contains(warning);
        if (outputContainsWarnings) {
            output.setAnalyseResult(AnalyseResult.NOT_VALID);
            return output;
        }
        output.setAnalyseResult(AnalyseResult.VALID_ALL);
        return output;
    }

    private Output generate(
        RawOutput rawOutput, boolean debug) {
        if (rawOutput.exception != null || rawOutput.exitCode > 0) {
            return rawOutput.toError(debug);
        }
        String warning = GriffinStatus.WARNING.name().toLowerCase();
        boolean outputContainsWarnings = rawOutput.stdout.toLowerCase().contains(warning) || rawOutput.stderr.toLowerCase().contains(warning);
        if (outputContainsWarnings) {
            return rawOutput.toWarning(debug);
        }
        return rawOutput.toOk(debug);
    }

    private  Output extract(
         RawOutput rawOutput, boolean debug) {
        if (rawOutput.exception != null || rawOutput.exitCode > 0) {
            return rawOutput.toError(debug);
        }
        try {
            final ArrayNode metadata = (ArrayNode) this.mapper.readTree(rawOutput.stdout);
            Set<Entry<String, String>> dataToExtract = rawOutput.action.getValues()
                .getDataToExtract()
                .entrySet();
            Map<String, JsonNode> dataToExtractByMetadata = dataToExtract.stream()
                .map(e -> new SimpleImmutableEntry<>(e.getKey(), metadata.get(0).at(e.getValue())))
                .collect(toMap(SimpleImmutableEntry::getKey, SimpleImmutableEntry::getValue));

            ObjectWriter writer = this.mapper.writer();
            Path outputPath = batchDirectory.resolve(outputFilesDirName);

            File file = outputPath.resolve(rawOutput.outputName).toFile();
            writer.writeValue(file, dataToExtractByMetadata);

            String warning = GriffinStatus.WARNING.name().toLowerCase();
            boolean outputContainsWarnings = rawOutput.stdout.toLowerCase().contains(warning) || rawOutput.stderr.toLowerCase().contains(warning);
            if (outputContainsWarnings) {
                return rawOutput.toWarning(debug);
            }
            return rawOutput.toOk(debug);
        } catch (IOException e) {
            logger.error("{}", e);
            return rawOutput.toError(debug, e.getMessage());
        }
    }
}
