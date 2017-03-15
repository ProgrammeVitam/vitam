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
package fr.gouv.vitam.common.junit;

/**
 * Factory to implements to get a VitamApplication started
 *
 * @param <A> Application class
 */
public interface VitamApplicationTestFactory<A> {

    /**
     * Start Response
     *
     * @param <A> Application class
     */
    public static final class StartApplicationResponse<A> {
        private int serverPort;
        private int serverAdminPort;
        private A application;

        /**
         *
         * @return the server port
         */
        public int getServerPort() {
            return serverPort;
        }


        /**
         *
         * @return The server admin port
         */
        public int getServerAdminPort() {
            return serverAdminPort;
        }

        /**
         *
         * @param serverPort the newly assigned port
         * @return this
         */
        public StartApplicationResponse<A> setServerPort(int serverPort) {
            this.serverPort = serverPort;
            return this;
        }

        /**
         *
         * @param serverAdminPort the newly assigned admin port
         * @return this
         */
        public StartApplicationResponse<A> setServerAdminPort(int serverAdminPort) {
            this.serverAdminPort = serverAdminPort;
            return this;
        }

        /**
         *
         * @return the application
         */
        public A getApplication() {
            return application;
        }

        /**
         *
         * @param application the newly created application
         * @return this
         */
        public StartApplicationResponse<A> setApplication(A application) {
            this.application = application;
            return this;
        }
    }

    /**
     * Should build the VitamApplication and start it (not startAndJoin)
     *
     * @param reservedPort the port reserved for the application and already set through XML property
     * @return return StartApplicationResponse with the port as vitamApplication.getVitamServer().getPort(); and the
     *         started application
     * @throws IllegalStateException if the start is incorrect
     */
    StartApplicationResponse<A> startVitamApplication(int reservedPort) throws IllegalStateException;
}
