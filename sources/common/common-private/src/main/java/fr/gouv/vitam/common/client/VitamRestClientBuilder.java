/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */

package fr.gouv.vitam.common.client;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.SSLConfiguration;


/**
 *  SSL Client Builder
 */
public class VitamRestClientBuilder {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(VitamRestClientBuilder.class); 
    private static final String PARAMETERS = "VitamRestClientBuilder parameters";
    private SSLConfiguration sslConfiguration = new SSLConfiguration();
    private boolean hostnameVerification = true; 
    
    
    /**
     * build a new Client
     * @return Client
     * @throws VitamException
     */
    public  Client build() throws VitamException {
        
        final SSLContext sslContext= sslConfiguration.createSSLContext();
            ClientBuilder clientBuilder = ClientBuilder.newBuilder();
            ClientConfig config = new ClientConfig()
                    .register(JacksonJsonProvider.class)
                    .register(JacksonFeature.class)
                    .register(MultiPartFeature.class);
            
            ClientBuilder cb = clientBuilder.sslContext(sslContext).withConfig(config);
            
            if(!hostnameVerification){
                    cb.hostnameVerifier(getAllowAllHostnameVerifier());
            }
            return cb.build();
    }
    
    /**
     * @param sslConfiguration the sslConfiguration to set
     * @return this
     * @throws IllegalArgumentException if serverHost is null or empty
     */
    public VitamRestClientBuilder setSslConfiguration(SSLConfiguration sslConfiguration) {
        ParametersChecker.checkParameter(PARAMETERS, sslConfiguration);
        this.sslConfiguration = sslConfiguration;
        return this;
    }



    /**
     * @param hostnameVerification the hostnameVerification to set
     * @return this
     * @throws IllegalArgumentException if serverHost is null or empty
     */
    public VitamRestClientBuilder setHostnameVerification(boolean hostnameVerification) {
        ParametersChecker.checkParameter(PARAMETERS, hostnameVerification);
        this.hostnameVerification = hostnameVerification;
        return this;
    }
    

    /**
     * @param verification
     * @return HostnameVerifier : An Allow All HostNameVerifier
     */
    private HostnameVerifier getAllowAllHostnameVerifier() {
        return new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, javax.net.ssl.SSLSession sslSession) {
                return true;
            }
        };
    }
    
}
