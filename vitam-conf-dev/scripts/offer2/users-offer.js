
// Switch to identity database
db = db.getSiblingDB('offer2')

// Create offer user

if (! db.getUser("offer")) {
    db.createUser(
        {
            user: "offer",
            pwd: "azerty5",
            roles: [
                { role: "readWrite", db: "offer2" }
            ]
        }
    )
}
else {
    db.updateUser(
        "offer",
        {
            pwd: "azerty5",
            roles: [
                { role: "readWrite", db: "offer2" }
            ]
        }
    )
}

// Create admin user

if (! db.getUser("vitamdb-admin")) {
    db.createUser(
        {
            user: "vitamdb-admin",
            pwd: "azerty",
            roles: [
                { role: "readWrite", db: "offer2" },
                { role: "dbAdmin", db: "offer2" }
            ]
        }
    )
}
else {
    db.updateUser(
        "vitamdb-admin",
        {
            pwd: "azerty",
            roles: [
                { role: "readWrite", db: "offer" },
                { role: "dbAdmin", db: "offer" },
                { role: "readWrite", db: "offer2" },
                { role: "dbAdmin", db: "offer2" }
            ]
        }
    )
}
