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
package fr.gouv.vitam.metadata.core.validation;

import fr.gouv.vitam.common.model.administration.OntologyModel;
import fr.gouv.vitam.common.model.administration.OntologyType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class MapDateXmlToMongoDateTest {

    private OntologyValidator ontologyValidator;

    private final String input;
    private final String expected;

    public MapDateXmlToMongoDateTest(String input, String expected) {
        this.input = input;
        this.expected = expected;
    }

    @Before
    public void initialize() {
        List<OntologyModel> ontologyModels = Arrays.asList(
            new OntologyModel().setIdentifier("text").setType(OntologyType.TEXT),
            new OntologyModel().setIdentifier("keyword").setType(OntologyType.KEYWORD),
            new OntologyModel().setIdentifier("date").setType(OntologyType.DATE),
            new OntologyModel().setIdentifier("long").setType(OntologyType.LONG),
            new OntologyModel().setIdentifier("double").setType(OntologyType.DOUBLE),
            new OntologyModel().setIdentifier("boolean").setType(OntologyType.BOOLEAN),
            new OntologyModel().setIdentifier("geopoint").setType(OntologyType.GEO_POINT),
            new OntologyModel().setIdentifier("enum").setType(OntologyType.ENUM)
        );

        this.ontologyValidator = new OntologyValidator(() -> ontologyModels);
    }

    @Parameterized.Parameters(name = "{index}: mapDateToOntology({0}) = {1}")
    public static Collection<Object[]> datesExpected() {
        return Arrays.asList(
            new Object[][] {
                { "2024-03-26", "2024-03-26" },
                { "2024-03-26Z", "2024-03-26" },
                { "2024-03-26+01:00", "2024-03-26T00:00:00+01:00" },
                { "2024-03-26-05:00", "2024-03-26T00:00:00-05:00" },
                { "2024/03/26", "2024-03-26" },
                { "2024-03-26T08:00:00", "2024-03-26T08:00:00" },
                { "2024-03-26T08:00:00Z", "2024-03-26T08:00:00" },
                { "2024-03-26T08:00:00+01:00", "2024-03-26T08:00:00+01:00" },
                { "2024-03-26T08:00:00-07:00", "2024-03-26T08:00:00-07:00" },
                { "2024-03-26T08:00:00.123", "2024-03-26T08:00:00.123" },
                { "2024-03-26T08:00:00.123456", "2024-03-26T08:00:00.123" },
                { "2024-03-26T08:00:00.123Z", "2024-03-26T08:00:00.123" },
                { "2024-03-26T08:00:00.123+01:00", "2024-03-26T08:00:00.123+01:00" },
                { "2024-03-26T08:00:00.123 PST", "2024-03-26T08:00:00.123-07:00" },
                { "2024-01-26T08:00:00.123 PST", "2024-01-26T08:00:00.123-08:00" },
                { "+2458-10-12", "2458-10-12" },
                { "-2458-10-12", "-2458-10-12" },
                { "+2458-10-12+01:00", "2458-10-12T00:00:00+01:00" },
                { "-24581-10-12+01:00", "-24581-10-12T00:00:00+01:00" },
                { "2019-03-27T09:36:10.93094Z", "2019-03-27T09:36:10.930" },
                { "2016-09-26Z", "2016-09-26" },
            }
        );
    }

    @Test
    public void testMapDateToOntology()
        throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = OntologyValidator.class.getDeclaredMethod("mapDateToOntology", String.class);
        method.setAccessible(true); // force l'accès

        String actual = (String) method.invoke(ontologyValidator, input);
        assertEquals("Failed for input: " + input, expected, actual);
    }
}
