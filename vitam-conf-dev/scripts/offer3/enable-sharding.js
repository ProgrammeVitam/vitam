
// Add the shard to the configuration
        
// Enable sharding on Vitam databases

sh.enableSharding("offer3")

// Add the sharding key for the sharded collections

sh.shardCollection("offer3.OfferLog"                        ,{ _id: "hashed" })
