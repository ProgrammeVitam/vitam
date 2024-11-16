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

package fr.gouv.vitam.common.xml;

import fr.gouv.vitam.common.PropertiesUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Test;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

import javax.xml.transform.TransformerException;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class XmlNamespaceUtilsTest {

    private static final String XML_FILE_OK = "xml/xml_document_ok.xml";
    private static final String XML_DOCUMENT_OK_WITH_DEFAULT_NAMESPACE =
        "xml/xml_document_ok_with_default_namespace.xml";
    private static final String XML_DOCUMENT_OK_WITH_DEFAULT_NAMESPACE_TRANSFORMED =
        "xml/xml_document_ok_with_default_namespace_transformed.xml";
    private static final String XML_DOCUMENT_OK_WITH_PREFIXED_NAMESPACE =
        "xml/xml_document_ok_with_prefixed_namespace.xml";
    private static final String XML_DOCUMENT_OK_WITH_PREFIXED_NAMESPACE_TRANSFORMED =
        "xml/xml_document_ok_with_prefixed_namespace_transformed.xml";
    private static final String XML_DOCUMENT_OK_WITH_DEFAULT_NAMESPACE_AND_XXE =
        "xml/xml_document_ok_with_default_namespace_and_xxe.xml";

    @Test
    public final void shouldParseXmlNamespaceSucceedForEmptyNamespace() throws Exception {
        InputStream xmlInputStream = PropertiesUtils.getResourceAsStream(XML_FILE_OK);
        assertThat(XmlNamespaceUtils.parseXmlNamespace(xmlInputStream)).isNull();
    }

    @Test
    public final void shouldParseXmlNamespaceSucceedForDefaultNamespace() throws Exception {
        InputStream xmlInputStream = PropertiesUtils.getResourceAsStream(XML_DOCUMENT_OK_WITH_DEFAULT_NAMESPACE);
        assertThat(XmlNamespaceUtils.parseXmlNamespace(xmlInputStream)).isEqualTo("my:namespace");
    }

    @Test
    public final void shouldParseXmlNamespaceSucceedForPrefixedNamespace() throws Exception {
        InputStream xmlInputStream = PropertiesUtils.getResourceAsStream(XML_DOCUMENT_OK_WITH_PREFIXED_NAMESPACE);
        assertThat(XmlNamespaceUtils.parseXmlNamespace(xmlInputStream)).isEqualTo("my:namespace");
    }

    @Test
    public final void shouldTransformXmlNamespaceSucceedForEmptySourceNamespace() throws Exception {
        try (
            InputStream xmlInputStream = PropertiesUtils.getResourceAsStream(XML_DOCUMENT_OK_WITH_DEFAULT_NAMESPACE);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()
        ) {
            assertThatThrownBy(
                () ->
                    XmlNamespaceUtils.transformXMLNamespace(
                        xmlInputStream,
                        byteArrayOutputStream,
                        "",
                        "my:new:namespace"
                    )
            )
                .isInstanceOf(TransformerException.class)
                .hasMessageContaining("Source and target namespaces are required");
        }
    }

    @Test
    public final void shouldTransformXmlNamespaceSucceedForEmptyTargetNamespace() throws Exception {
        try (
            InputStream xmlInputStream = PropertiesUtils.getResourceAsStream(XML_DOCUMENT_OK_WITH_DEFAULT_NAMESPACE);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()
        ) {
            assertThatThrownBy(
                () ->
                    XmlNamespaceUtils.transformXMLNamespace(
                        xmlInputStream,
                        byteArrayOutputStream,
                        "my:new:namespace",
                        ""
                    )
            )
                .isInstanceOf(TransformerException.class)
                .hasMessageContaining("Source and target namespaces are required");
        }
    }

    @Test
    public final void shouldTransformXmlNamespaceSucceedForDefaultNamespace() throws Exception {
        try (
            InputStream xmlInputStream = PropertiesUtils.getResourceAsStream(XML_DOCUMENT_OK_WITH_DEFAULT_NAMESPACE);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()
        ) {
            XmlNamespaceUtils.transformXMLNamespace(
                xmlInputStream,
                byteArrayOutputStream,
                "my:namespace",
                "my:new:namespace"
            );
            String transformedXml = byteArrayOutputStream.toString(StandardCharsets.UTF_8);
            assertXmlEquals(
                transformedXml,
                PropertiesUtils.getResourceFile(XML_DOCUMENT_OK_WITH_DEFAULT_NAMESPACE_TRANSFORMED)
            );
        }
    }

    @Test
    public final void shouldTransformXmlNamespaceSucceedForPrefixNamespace() throws Exception {
        try (
            InputStream xmlInputStream = PropertiesUtils.getResourceAsStream(XML_DOCUMENT_OK_WITH_PREFIXED_NAMESPACE);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()
        ) {
            XmlNamespaceUtils.transformXMLNamespace(
                xmlInputStream,
                byteArrayOutputStream,
                "my:namespace",
                "my:new:namespace"
            );
            String transformedXml = byteArrayOutputStream.toString(StandardCharsets.UTF_8);
            assertXmlEquals(
                transformedXml,
                PropertiesUtils.getResourceFile(XML_DOCUMENT_OK_WITH_PREFIXED_NAMESPACE_TRANSFORMED)
            );
        }
    }

    @Test
    public final void shouldTransformXmlNamespaceIgnoringXXEInjections() throws Exception {
        try (
            InputStream xmlInputStream = PropertiesUtils.getResourceAsStream(
                XML_DOCUMENT_OK_WITH_DEFAULT_NAMESPACE_AND_XXE
            );
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()
        ) {
            assertThatThrownBy(
                () ->
                    XmlNamespaceUtils.transformXMLNamespace(
                        xmlInputStream,
                        byteArrayOutputStream,
                        "my:namespace",
                        "my:new:namespace"
                    )
            )
                .isInstanceOf(TransformerException.class)
                .hasMessageContaining("\"xxe\"");
        }
    }

    private static void assertXmlEquals(String transformedXml, File expectedFileContent) {
        Diff xmlDiff = DiffBuilder.compare(transformedXml).withTest(expectedFileContent).checkForIdentical().build();
        assertThat(xmlDiff.hasDifferences()).withFailMessage("Expected no differences " + xmlDiff).isFalse();
    }
}
