/*******************************************************************************
 * This file is part of Vitam Project.
 * 
 * Copyright Vitam (2012, 2015)
 *
 * This software is governed by the CeCILL 2.1 license under French law and
 * abiding by the rules of distribution of free software. You can use, modify
 * and/ or redistribute the software under the terms of the CeCILL license as
 * circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify
 * and redistribute granted by the license, users are provided only with a
 * limited warranty and the software's author, the holder of the economic
 * rights, and the successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with
 * loading, using, modifying and/or developing or reproducing the software by
 * the user in light of its specific status of free software, that may mean that
 * it is complicated to manipulate, and that also therefore means that it is
 * reserved for developers and experienced professionals having in-depth
 * computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling
 * the security of their systems and/or data to be ensured and, more generally,
 * to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.common;

import static org.junit.Assert.*;

import java.io.StringReader;
import java.io.StringWriter;

import org.json.JSONObject;
import org.json.XML;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.copy.HierarchicalStreamCopier;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
import com.thoughtworks.xstream.io.xml.XppReader;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.json.XmlHandler;

/**
 *
 */
public class Xml2JsonTest {
    static final String XML = "<ArchiveUnit>\n"+
"<Management>\n"+
"    <StorageRule>\n"+
"        <Rule id='ID012'>Rule0</Rule>\n"+
"        <FinalAction>RestrictAccess</FinalAction>\n"+
"    </StorageRule>\n"+
"    <AppraisalRule>\n"+
"        <Rule >Rule2</Rule>\n"+
"        <FinalAction>Keep</FinalAction>\n"+
"    </AppraisalRule>\n"+
"    <AccessRule>\n"+
"        <Rule >Rule4</Rule>\n"+
"    </AccessRule>\n"+
"    <DisseminationRule>\n"+
"        <Rule >Rule6</Rule>\n"+
"    </DisseminationRule>\n"+
"    <ReuseRule>\n"+
"        <Rule >Rule8</Rule>\n"+
"        <Rule >Rule9</Rule>\n"+
"    </ReuseRule>\n"+
"    <ClassificationRule>\n"+
"        <Rule >Rule10</Rule>\n"+
"        <Rule >Rule11</Rule>\n"+
"        <ClassificationLevel>ClassificationLevel0</ClassificationLevel>\n"+
"        <ClassificationOwner>ClassificationOwner0</ClassificationOwner>\n"+
"    </ClassificationRule>\n"+
"    <NeedAuthorization>false</NeedAuthorization>\n"+
"</Management>\n"+
"<Content xml:id='ID026'>\n"+
"    <DescriptionLevel>Fonds</DescriptionLevel>\n"+
"    <Title >Title0</Title>\n"+
"    <FilePlanPosition>FilePlanPosition0</FilePlanPosition>\n"+
"    <OriginatingSystemId>OriginatingSystemId0</OriginatingSystemId>\n"+
"    <ArchivalAgencyArchiveUnitIdentifier>ArchivalAgencyArchiveUnitIdentifier0</ArchivalAgencyArchiveUnitIdentifier>\n"+
"    <OriginatingAgencyArchiveUnitIdentifier>OriginatingAgencyArchiveUnitIdentifier0</OriginatingAgencyArchiveUnitIdentifier>\n"+
"    <TransferringAgencyArchiveUnitIdentifier>TransferringAgencyArchiveUnitIdentifier0</TransferringAgencyArchiveUnitIdentifier>\n"+
"    <Description xml:lang='fr-FR'>Description0</Description>\n"+
"    <Description xml:lang='en-EN'>Description1</Description>\n"+
"    <DocumentType >DocumentType0</DocumentType>\n"+
"    <Language>fr-FR</Language>\n"+
"    <Tag>Tag0</Tag>\n"+
"    <Tag>Tag1</Tag>\n"+
"    <OriginatingAgency>\n"+
"        <Identifier>Identifier0</Identifier>\n"+
"    </OriginatingAgency>\n"+
"    <SubmissionAgency>\n"+
"        <Identifier>Identifier1</Identifier>\n"+
"    </SubmissionAgency>\n"+
"    <AuthorizedAgent>\n"+
"        <Corpname>Corpname0</Corpname>\n"+
"    </AuthorizedAgent>\n"+
"    <Writer>\n"+
"        <FirstName>FirstName0</FirstName>\n"+
"        <BirthName>BirthName0</BirthName>\n"+
"    </Writer>\n"+
"    <Writer>\n"+
"        <FirstName>FirstName1</FirstName>\n"+
"        <BirthName>BirthName1</BirthName>\n"+
"    </Writer>\n"+
"    <Addressee>\n"+
"        <FirstName>FirstName2</FirstName>\n"+
"        <BirthName>BirthName2</BirthName>\n"+
"    </Addressee>\n"+
"    <Addressee>\n"+
"        <FirstName>FirstName3</FirstName>\n"+
"        <BirthName>BirthName3</BirthName>\n"+
"    </Addressee>\n"+
"    <Recipient>\n"+
"        <Corpname>Corpname1</Corpname>\n"+
"    </Recipient>\n"+
"    <Recipient>\n"+
"        <Corpname>Corpname2</Corpname>\n"+
"    </Recipient>\n"+
"    <CreatedDate>2006-05-04</CreatedDate>\n"+
"    <TransactedDate>2006-05-04</TransactedDate>\n"+
"    <AcquiredDate>2006-05-04</AcquiredDate>\n"+
"    <SentDate>2006-05-04</SentDate>\n"+
"    <ReceivedDate>2006-05-04</ReceivedDate>\n"+
"    <RegisteredDate>2006-05-04</RegisteredDate>\n"+
"    <StartDate>2006-05-04</StartDate>\n"+
"    <EndDate>2006-05-04</EndDate>\n"+
"</Content>\n"+
"</ArchiveUnit>";
    
    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void testUsingOrgJson() {
        System.out.println("org.json.JSON");
        String xml = XML;
        System.out.println(xml);
        JSONObject json = org.json.XML.toJSONObject(xml);
        System.out.println(json.toString(2));
        try {
            System.out.println(JsonHandler.writeAsString(JsonHandler.getFromString(json.toString())));
        } catch (InvalidParseOperationException e) {
            fail(e.getMessage());
        }
        try {
            System.out.println(XmlHandler.writeAsString(JsonHandler.getFromString(json.toString())));
        } catch (InvalidParseOperationException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testUsingXstream() {
        System.out.println("Xstream and Jettison");
        String xml = XML;
        System.out.println(xml);
        HierarchicalStreamReader sourceReader = new XppReader(new StringReader(xml));
        StringWriter buffer = new StringWriter();
        JettisonMappedXmlDriver jettisonDriver = new JettisonMappedXmlDriver();
        jettisonDriver.createWriter(buffer);
        HierarchicalStreamWriter destinationWriter = jettisonDriver.createWriter(buffer);
        HierarchicalStreamCopier copier = new HierarchicalStreamCopier();
        copier.copy(sourceReader, destinationWriter);
        String json = buffer.toString();
        System.out.println(json);
        try {
            System.out.println(JsonHandler.writeAsString(JsonHandler.getFromString(json)));
        } catch (InvalidParseOperationException e) {
            fail(e.getMessage());
        }
    }

}
