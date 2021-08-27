
// Add the shard to the configuration
        
// Enable sharding on Vitam databases

sh.enableSharding("offer2")

// Add the sharding key for the sharded collections

sh.shardCollection("offer2.OfferLog"                        ,{ _id: "hashed" })
