package fr.gouv.vitam.metadata.core.graph;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.metadata.core.database.collections.Unit;
import org.apache.commons.collections4.CollectionUtils;
import org.bson.Document;
import org.junit.Test;

public class GraphBuilderServiceTest {



    @Test
    @RunWithCustomExecutor
    public void testStreamDocumentsWithoutUp() {
        List<String> uu = Lists
            .newArrayList(new Document(Unit.UP, Lists.newArrayList("1", "2")), new Document("noUP", "noUp"))
            .stream()
            .map(o -> (List<String>) o.get(Unit.UP, List.class))
            .filter(CollectionUtils::isNotEmpty)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
        assertThat(uu).hasSize(2);
    }
}
