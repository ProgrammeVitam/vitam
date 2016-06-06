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
package fr.gouv.vitam.api.config;

/**
 * MetaDataConfiguration contains database access informations 
 */
// TODO REVIEW should be final
public class MetaDataConfiguration {

	private String host;
	private int port;
	private String dbName;
	// TODO REVIEW this is incorrect since we will have multiple collection: this does not used the logic of collections managed by MongoDbAccess (checking index and correct CODECs)
	private String collectionName;
	/**
	 * MetaDataConfiguration constructor
	 * @param host
	 * 			database server IP address
	 * @param port
	 * 			database server port 
	 * @param dbName
	 * 			database name
	 * @param collectionName
	 * 			database collection name
	 */
	public MetaDataConfiguration(String host, int port, String dbName, String collectionName) {
		// TODO REVIEW do not allow null values and throw IllegalArgumentException
		this.host = host;
		this.port = port;
		this.dbName = dbName;
		this.collectionName = collectionName;
	}
	
	/**
	 * MetaDataConfiguration empty constructor for YAMLFactory
	 */
	// TODO: Could protected be working?
	public MetaDataConfiguration() {}
	
	/**
	 * @return the database address as String
	 */
	public String getHost() {
		return host;
	}

	/**
	 * @param host the address of database server as String
	 *  the MetaDataConfiguration with database server address is setted
	 */
	public MetaDataConfiguration setHost(String host) {
		this.host = host;
		return this;
	}

	/**
     * @return the database port server 
     */
	public int getPort() {
		return port;
	}

	/**
	 * @param port the port of database server as integer
	 * @return the MetaDataConfiguration with database server port is setted
	 */
	public MetaDataConfiguration setPort(int port) {
		this.port = port;
		return this;
	}

    /**
     * @return the database name 
     */	
	public String getDbName() {
		return dbName;
	}

    /**
     * @param dbName the name of database as String
     * @return the MetaDataConfiguration with database name is setted
     */
	public MetaDataConfiguration setDbName(String dbName) {
		this.dbName = dbName;
		return this;
	}

    /**
     * @return the database collection 
     */ 
	public String getCollectionName() {
		return collectionName;
	}

	/**
     * @param collectionName the name collection in database as String
     * @return the MetaDataConfiguration with collection name is setted
     */	
	public MetaDataConfiguration setCollectionName(String collectionName) {
		this.collectionName = collectionName;
		return this;
	}
}
