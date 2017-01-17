package fr.gouv.vitam.common.server.application.resources;

import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

/**
 * Created by kw on 31/12/2016.
 */
public class VersionHelper {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(VersionHelper.class);

    private final static List<Map<String, String>> cachedComponents = new ArrayList<>();

    private final static String MANIFEST_VITAM_FLAG = "Vitam-component";

    /**
     * Tag used to group the summary by
     */
    public final static String MANIFEST_SUMMARY_TAG = "Scm-commit-id";

    /**
     * Read-only list of attributes searched into the main section of the jar manifests.
     */
    public final static List<String> MANIFEST_FIELDS = Collections.unmodifiableList(Arrays.asList("Maven-groupId", "Maven-artefactId",
            "Maven-version", "Scm-branch", "Scm-tags", MANIFEST_SUMMARY_TAG, "Scm-commit-id-abbrev", "Scm-dirty",
            "Scm-commit-time", "Maven-build-timestamp", "Build-Jdk"));

    static {
        List<URL> resources;
        try {
            resources = Collections.list(Thread.currentThread().getContextClassLoader().getResources(JarFile.MANIFEST_NAME));
        } catch (IOException e) {
            LOGGER.warn("Unable to open any MANIFEST.MF during the search of component versions... No component version will be available in status API.", e);
            resources = new LinkedList<>();
        }
        if (resources.isEmpty()) {
            LOGGER.warn("No MANIFEST.MF file was found ! No component version will be available in status API");
        } else {
            LOGGER.info("Found {} components in classpath.", resources.size());
        }
        for (URL resource : resources) {
            try {
                Manifest manifest = new Manifest(resource.openStream());
                if (manifest.getMainAttributes().getValue(MANIFEST_VITAM_FLAG) != null) {
                    cachedComponents.add(extractManifestInfo(manifest));
                }
            } catch (Exception e) {
                LOGGER.warn("Unable to open MANIFEST.MF (from " + resource + ") to search for the component version... Skipping component.", e);
            }
        }
    }

    /**
     * Get detailed information on all internal vitam components versions
     * @return A read-only list of informations ; each entry stands for a component.
     */
    public static List<Map<String, String>> getVersionDetailedInfo() {
        return Collections.unmodifiableList(cachedComponents);
    }

    /**
     * Get the summary version information for vitam components found in the current classpath.
     * @return A map with keys being the git commit id (long hash) and the value the number of components having this version.
     */
    public static Map<String, Long> getVersionSummary() {
        // No need to return a read-only collection here, as this map is created just there and doesn't contain mutable elements.
        return cachedComponents.stream().collect(Collectors.groupingBy(n -> n.getOrDefault(MANIFEST_SUMMARY_TAG, "<no commit id found>"), Collectors.counting()));
    }

    /**
     * Extract manifest attributes into a map
     * @param mf
     * @return A read-only map of attributes.
     */
    private static Map<String, String> extractManifestInfo(Manifest mf) {
        Map<String, String> result = new HashMap<>();
        Attributes attr = mf.getMainAttributes();
        for (String field : MANIFEST_FIELDS) {
            result.put(field, attr.getValue(field));
        }
        LOGGER.info("Found component : {}", result);
        return Collections.unmodifiableMap(result);
    }

}
