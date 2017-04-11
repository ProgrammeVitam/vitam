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
package fr.gouv.vitam.driver.fake;

import com.fasterxml.jackson.databind.node.ObjectNode;
import fr.gouv.vitam.common.BaseXx;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.client.AbstractMockClient;
import fr.gouv.vitam.common.client.DefaultClient;
import fr.gouv.vitam.common.client.TestVitamClientFactory;
import fr.gouv.vitam.common.client.VitamRequestIterator;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.storage.driver.AbstractConnection;
import fr.gouv.vitam.storage.driver.AbstractDriver;
import fr.gouv.vitam.storage.driver.Connection;
import fr.gouv.vitam.storage.driver.exception.StorageDriverException;
import fr.gouv.vitam.storage.driver.model.*;
import fr.gouv.vitam.storage.engine.common.referential.model.StorageOffer;
import org.apache.commons.io.IOUtils;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Driver implementation for test only
 */
public class FakeDriverImpl extends AbstractDriver {

    @Override
    public Connection connect(StorageOffer offer, Properties properties) throws StorageDriverException {
        if (properties.contains("fail")) {
            throw new StorageDriverException(getName(),
                "Intentionaly thrown");
        }
        return new ConnectionImpl();
    }

    @Override
    public boolean isStorageOfferAvailable(StorageOffer offer) throws StorageDriverException {
        if (offer.getParameters().containsKey("fail")) {
            throw new StorageDriverException(getName(),
                    "Intentionaly thrown");
        }
        return true;
    }

    @Override
    public String getName() {
        return "Fake driver";
    }

    @Override
    public int getMajorVersion() {
        return 1;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    class ConnectionImpl extends AbstractConnection {

        ConnectionImpl() {
            super("FakeDriverName", new TestVitamClientFactory<AbstractConnection>(1324, "/chemin/"));
        }

        @Override
        public StorageCapacityResult getStorageCapacity(Integer tenantId) throws StorageDriverException {
            Integer fakeTenant = -1;
            if (fakeTenant.equals(tenantId)) {
                throw new StorageDriverException("driverInfo",
                    "ExceptionTest");
            }

            final StorageCapacityResult result = new StorageCapacityResult(tenantId, 1000000, 99999);
            return result;
        }

        @Override
        public StorageCountResult countObjects(StorageRequest request) throws StorageDriverException {
            return new StorageCountResult(request.getTenantId(), request.getType(), 1);
        }

        @Override
        public StorageGetResult getObject(StorageObjectRequest objectRequest) throws StorageDriverException {

            return new StorageGetResult(objectRequest.getTenantId(), objectRequest.getType(), objectRequest.getGuid(),
                new AbstractMockClient.FakeInboundResponse(Status.OK, new ByteArrayInputStream("test".getBytes()),
                    MediaType.APPLICATION_OCTET_STREAM_TYPE, null));
        }

        @Override
        public StoragePutResult putObject(StoragePutRequest objectRequest) throws StorageDriverException {
            if ("digest_bad_test".equals(objectRequest.getGuid())) {
                return new StoragePutResult(objectRequest.getTenantId(), objectRequest.getType(), objectRequest.getGuid(),
                    objectRequest.getGuid(), "different_digest_hash", 0);
            }
            if ("retry_test".equals(objectRequest.getGuid())) {
                throw new StorageDriverException(getName(), "retry_test");
            } else {
                try {
                    final byte[] bytes = IOUtils.toByteArray(objectRequest.getDataStream());
                    final MessageDigest messageDigest = MessageDigest.getInstance(objectRequest.getDigestAlgorithm());
                    return new StoragePutResult(objectRequest.getTenantId(), objectRequest.getType(), objectRequest.getGuid(),
                        objectRequest.getGuid(), BaseXx.getBase16(messageDigest.digest(bytes)), bytes.length);
                } catch (NoSuchAlgorithmException | IOException e) {
                    throw new StorageDriverException(getName(), "Digest or Storage Put in error", e);
                }
            }
        }

        @Override
        public StorageRemoveResult removeObject(StorageRemoveRequest objectRequest) throws StorageDriverException {
            if ("digest_bad_test".equals(objectRequest.getGuid())) {
                throw new StorageDriverException("removeObject",
                    "ExceptionTest");

            } else {
                return new StorageRemoveResult(objectRequest.getTenantId(), objectRequest.getType(), objectRequest.getGuid(),
                    objectRequest.getDigestAlgorithm(), objectRequest.getDigestHashBase16(), true);
            }
        }

        @Override
        public Boolean objectExistsInOffer(StorageObjectRequest request) throws StorageDriverException {
            return "already_in_offer".equals(request.getGuid());
        }

        @Override
        public StorageCheckResult checkObject(StorageCheckRequest request) throws StorageDriverException {
            if ("digest_bad_test".equals(request.getGuid())) {
                throw new StorageDriverException("checkObject",
                    "ExceptionTest");
            }
            return new StorageCheckResult(request.getTenantId(), request.getType(), request.getGuid(),
                request.getDigestAlgorithm(), request.getDigestHashBase16(), true);
        }

        @Override
        public StorageMetadatasResult getMetadatas(StorageObjectRequest request) throws StorageDriverException {
            return new StorageMetadatasResult(null);
        }

        @Override
        public Response listObjects(StorageListRequest request) throws StorageDriverException {
            MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
            headers.add(GlobalDataRest.X_TENANT_ID, request.getTenantId());
            try (VitamRequestIterator<ObjectNode> iterator = new VitamRequestIterator<>(this, HttpMethod.GET, "/iterator",
                ObjectNode.class, null, null)) {
                final RequestResponseOK response = new RequestResponseOK();
                final ObjectNode node1 = JsonHandler.createObjectNode().put("val", 1);
                final ObjectNode node2 = JsonHandler.createObjectNode().put("val", 2);
                final ObjectNode node3 = JsonHandler.createObjectNode().put("val", 3);
                response.addResult(node1);
                final List<ObjectNode> list = new ArrayList<>();
                list.add(node2);
                list.add(node3);
                response.addAllResults(list);
                response.setQuery(JsonHandler.createObjectNode());
                response.setHits(response.getResults().size(), 0, response.getResults().size());
                Response.ResponseBuilder builder = Response.status(Status.OK);

                return VitamRequestIterator.setHeaders(builder, false, "newcursor").entity(response).build();
            }
        }
    }

}
