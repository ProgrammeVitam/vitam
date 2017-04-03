package fr.gouv.vitam.common.database.server.elasticsearch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

/**
 * Elasticsearch Util
 */
public class ElasticsearchUtil {
    
    /**
     * @param is InputStream of the file json 
     * @return String mapping
     * @throws IOException
     */
    public static String transferJsonToMapping(InputStream is) throws IOException{
        final String mapping;
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(is))) {
            mapping = buffer.lines().collect(Collectors.joining("\n"));
        }
        return mapping;
    }
}
