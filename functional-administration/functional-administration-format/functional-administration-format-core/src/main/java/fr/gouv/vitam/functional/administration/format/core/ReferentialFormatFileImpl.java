/**
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
 */
package fr.gouv.vitam.functional.administration.format.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.mongodb.client.MongoCursor;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.application.configuration.DbConfiguration;
import fr.gouv.vitam.functional.administration.common.FileFormat;
import fr.gouv.vitam.functional.administration.common.ReferentialFile;
import fr.gouv.vitam.functional.administration.common.exception.FileFormatException;
import fr.gouv.vitam.functional.administration.common.exception.FileFormatNotFoundException;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminFactory;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessAdminImpl;

/**
 * ReferentialFormatFileImpl implementing the ReferentialFormatFile interface
 */
public class ReferentialFormatFileImpl implements ReferentialFile {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(PronomParser.class);
    private final MongoDbAccessAdminImpl mongoAccess;
    
    /**
     * Constructor
     * @param dbConfiguration
     */
    public ReferentialFormatFileImpl(DbConfiguration dbConfiguration) {
        this.mongoAccess = MongoDbAccessAdminFactory.create(dbConfiguration);
    }

    @Override
    public void importFile(File xmlPronom)
        throws ReferentialException {
        ParametersChecker.checkParameter("Pronom file is a mandatory parameter", xmlPronom);
        try {
            ArrayNode pronomList = PronomParser.getPronom(xmlPronom);
            this.mongoAccess.insertDocuments(pronomList, FunctionalAdminCollections.FORMATS);
        } catch (ReferentialException e) {
            LOGGER.error(e.getMessage());
            throw new FileFormatException(e);
        }

    }

    @Override
    public void deleteCollection() {
        this.mongoAccess.deleteCollection(FunctionalAdminCollections.FORMATS);
    }

    // TODO Check with xsd file  
    @Override
    public boolean checkFile(File xmlPronom) throws FileFormatException {
        ParametersChecker.checkParameter("Pronom file is a mandatory parameter", xmlPronom);
        try {
            InputStream xmlPronomStream = new FileInputStream(xmlPronom);
            final XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
            XMLEventReader eventReader = xmlInputFactory.createXMLEventReader(xmlPronomStream);
            while (eventReader.hasNext()) {
                eventReader.nextEvent();
            }
        } catch (FileNotFoundException e) {
            LOGGER.error(e.getMessage());
            throw new FileFormatNotFoundException("Not found referential format file");
        } catch (XMLStreamException e) {
            LOGGER.error(e.getMessage());
            return false;
        }

        return true;
    }

    @Override
    public FileFormat findDocumentById(String id) throws ReferentialException {
        try {
            return (FileFormat) this.mongoAccess.getDocumentById(id, FunctionalAdminCollections.FORMATS);
        } catch (ReferentialException e) {
            LOGGER.error(e.getMessage());
            throw new FileFormatException(e);
        }
    }

    @Override
    public List<FileFormat> findDocuments(JsonNode select) throws ReferentialException {
        try {
            MongoCursor<FileFormat> formats = (MongoCursor<FileFormat>) this.mongoAccess.select(select, FunctionalAdminCollections.FORMATS);
            final List<FileFormat> result = new ArrayList<>();
            if (formats == null || !formats.hasNext()) {
                throw new FileFormatNotFoundException("Format not found");
            }
            while (formats.hasNext()) {
                result.add(formats.next());
            }
            return result;
        } catch (ReferentialException e) {
            LOGGER.error(e.getMessage());
            throw new FileFormatException(e);
        }
    }

}
