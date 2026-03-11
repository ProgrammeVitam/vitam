
// Switch to ina-tape-proxy database
db = db.getSiblingDB('ina-tape-proxy')

// Create ina-proxy-db user

if (! db.getUser("ina-tape-proxy")) {
    db.createUser(
        {
            user: "ina-tape-proxy",
            pwd: "azerty10",
            roles: [
                { role: "readWrite", db: "ina-tape-proxy" }
            ]
        }
    )
}
else {
    db.updateUser(
        "ina-tape-proxy",
        {
            pwd: "azerty10",
            roles: [
                { role: "readWrite", db: "ina-tape-proxy" }
            ]
        }
    )
}
