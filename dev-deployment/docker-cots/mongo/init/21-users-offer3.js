
// Switch to identity database
db = db.getSiblingDB('offer3')

// Create offer3 user

if (! db.getUser("offer3")) {
    db.createUser(
        {
            user: "offer3",
            pwd: "azerty5",
            roles: [
                { role: "readWrite", db: "offer3" }
            ]
        }
    )
}
else {
    db.updateUser(
        "offer3",
        {
            pwd: "azerty5",
            roles: [
                { role: "readWrite", db: "offer3" }
            ]
        }
    )
}
