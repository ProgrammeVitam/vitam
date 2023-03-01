// https://www.mongodb.com/docs/v4.2/reference/command/isMaster/#output
let i = 0;

while (rs.isMaster().ismaster == true) {

    if (i++ === 60) {
        printjson(rsStatus);
        throw "Cannot step down the master.";
    }

    print("Info: Still master...");
    sleep(1000);

}

print("OK: node is not master anymore.")
