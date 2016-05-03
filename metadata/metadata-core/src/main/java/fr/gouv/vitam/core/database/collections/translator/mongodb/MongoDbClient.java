package fr.gouv.vitam.core.database.collections.translator.mongodb;


import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.core.database.collections.MongoDbHelper;

public class MongoDbClient {
	private static final String DEFAULT_DB = "Vitam";
	private static final String DEFAULT_COLLECTION = "Units";
	private final MongoClient mongoClient;
	private final MongoDatabase mongoDatabase;
	private final MongoCollection<Document> mongoCollection;

	public MongoDbClient(final String host, final int port, final String dbName, final String collectionName) {
		mongoClient = new MongoClient(host, port);
		this.mongoDatabase = mongoClient.getDatabase(dbName);
		this.mongoCollection = this.mongoDatabase.getCollection(collectionName);
	}
	
	public MongoDbClient(MongoClient mongoClient, final String dbName, final String collectionName) {
		this.mongoClient = mongoClient;
		this.mongoDatabase = mongoClient.getDatabase(dbName);
		this.mongoCollection = this.mongoDatabase.getCollection(collectionName);
	}

	public MongoDbClient(MongoClient mongoClient) {
		this(mongoClient, DEFAULT_DB, DEFAULT_COLLECTION);    
	}

	public MongoDbClient() {
		this(new MongoClient(), DEFAULT_DB, DEFAULT_COLLECTION);    
	}

	public MongoClient getMongoClient() {
		return mongoClient;
	}

	public MongoDatabase getMongoDatabase() {
		return mongoDatabase;
	}

	public MongoCollection<Document> getMongoCollection() {
		return mongoCollection;
	}

	public FindIterable<Document> findDocument(Bson searchQuery) {
		return mongoCollection.find(searchQuery);
	}

	public FindIterable<Document> findDocumentById(String id) {
		return mongoCollection.find(new BasicDBObject("_id", id));
	}

	public void insertDocument(Document document) {
		mongoCollection.insertOne(document);
	}

	public boolean insertDocument(Document document, Bson filter) {
		FindIterable<Document> iterator = mongoCollection.find(filter);

		if (iterator.first() != null) {
			BasicDBList parentListId = new BasicDBList();
			for (Document doc : iterator) {
				parentListId.add(doc.getString("_id"));
			}
			document.put("_up", parentListId);
			mongoCollection.insertOne(document);
			return true;
		} else {
			return false;
		}	
	}

	public boolean createDocument(InsertToMongodb insertToMongodb) throws IllegalAccessError, IllegalAccessException, InvalidParseOperationException {
		int NbQueries = insertToMongodb.getNbQueries();
		Document document = Document.parse(MongoDbHelper.bsonToString(insertToMongodb.getFinalData(), false));
		if (NbQueries == 0) {
			insertDocument(document);
			return true;
		} else {
			// 1 query
			Bson filter = insertToMongodb.getNthQueries(0);
			return insertDocument(document, filter);	
		}
	}

}
