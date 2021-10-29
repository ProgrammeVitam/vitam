// Switch to identity database
db = db.getSiblingDB('offer')

// Create indexes
// A single collection can have no more than 64 indexes.
db.TapeAccessRequestReferential.createIndex( { "unavailableArchiveIds" : 1 } )
db.TapeAccessRequestReferential.createIndex( { "objectIds" : 1 } )
db.TapeAccessRequestReferential.createIndex( { "expirationDate" : 1 } )
db.TapeAccessRequestReferential.createIndex( { "purgeDate" : 1 } )

// TODO Tape collections : TapeArchiveReferential, TapeCatalog, TapeObjectReferential, TapeQueueMessage