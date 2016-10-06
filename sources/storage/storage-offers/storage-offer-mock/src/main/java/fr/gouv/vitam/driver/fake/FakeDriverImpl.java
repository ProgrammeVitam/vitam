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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

import org.apache.commons.io.IOUtils;

import fr.gouv.vitam.common.BaseXx;
import fr.gouv.vitam.storage.driver.Connection;
import fr.gouv.vitam.storage.driver.Driver;
import fr.gouv.vitam.storage.driver.exception.StorageDriverException;
import fr.gouv.vitam.storage.driver.model.GetObjectRequest;
import fr.gouv.vitam.storage.driver.model.GetObjectResult;
import fr.gouv.vitam.storage.driver.model.PutObjectRequest;
import fr.gouv.vitam.storage.driver.model.PutObjectResult;
import fr.gouv.vitam.storage.driver.model.RemoveObjectRequest;
import fr.gouv.vitam.storage.driver.model.RemoveObjectResult;
import fr.gouv.vitam.storage.driver.model.StorageCapacityResult;

/**
 * Driver implementation for test only <br>
 * !!! WARNING !!! : in case of modification of class fr.gouv.vitam.driver.fake.FakeDriverImpl, you need to recompile the
 * storage-offer-mock.jar from the this module and copy it in the src/test/resources folder of the storage-integration-test
 * module in place of the previous one.
 */
public class FakeDriverImpl implements Driver {

    @Override
    public Connection connect(String s, Properties properties) throws StorageDriverException {
        if (properties.contains("fail")) {
            throw new StorageDriverException(getName(), StorageDriverException.ErrorCode.INTERNAL_SERVER_ERROR,
                "Intentionaly thrown");
        }
        return new ConnectionImpl();
    }

    @Override
    public boolean isStorageOfferAvailable(String s, Properties properties) throws StorageDriverException {
        if (properties.contains("fail")) {
            throw new StorageDriverException(getName(), StorageDriverException.ErrorCode.INTERNAL_SERVER_ERROR,
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

    class ConnectionImpl implements Connection {

        @Override
        public StorageCapacityResult getStorageCapacity(String tenantId)
            throws StorageDriverException {
            if ("daFakeTenant".equals(tenantId)) {
                throw new StorageDriverException("driverInfo", StorageDriverException.ErrorCode.INTERNAL_SERVER_ERROR,
                    "ExceptionTest");
            }
            StorageCapacityResult result = new StorageCapacityResult();
            result.setUsableSpace(1000000);
            result.setUsedSpace(99999);
            return result;
        }

        @Override
        public GetObjectResult getObject(GetObjectRequest objectRequest) throws StorageDriverException {
            GetObjectResult result = new GetObjectResult("0", new ByteArrayInputStream("fakefile".getBytes()));
            return result;
        }

        @Override
        public PutObjectResult putObject(PutObjectRequest objectRequest) throws StorageDriverException {
            if ("digest_bad_test".equals(objectRequest.getGuid())) {
                return new PutObjectResult(objectRequest.getGuid(), "different_digest_hash", "0");
            } else {
                try {
                    byte[] bytes = IOUtils.toByteArray(objectRequest.getDataStream());
                    MessageDigest messageDigest = MessageDigest.getInstance(objectRequest.getDigestAlgorithm());
                    return new PutObjectResult(objectRequest.getGuid(), BaseXx.getBase16(messageDigest.digest(bytes)),
                        "0");
                } catch (NoSuchAlgorithmException | IOException e) {
                    throw new StorageDriverException(getName(), StorageDriverException.ErrorCode.INTERNAL_SERVER_ERROR,
                        e);
                }
            }
        }


        @Override
        public RemoveObjectResult removeObject(RemoveObjectRequest objectRequest) throws StorageDriverException {
            return new RemoveObjectResult();
        }

        @Override
        public Boolean objectExistsInOffer(GetObjectRequest request) throws StorageDriverException {
            return "already_in_offer".equals(request.getGuid());
        }

        @Override
        public void close() throws StorageDriverException {
            // Empty
        }
    }
}
