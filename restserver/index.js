const express = require("express");
const bodyParser = require('body-parser')

const app = express();

app.use(bodyParser.json());

// All servers have the same username and password for simplicity.
// If different credentials are required, make a new object that maps a server's type, host, and port to a username and password
const username = "user";
const password = "password";

const serverToFile = {}

function headersToServerName(headers) {
    return `${headers["ftp-type"]} ${headers["ftp-host"]}:${headers["port"]}`;
}

function serverExists(server) {
    return !!serverToFile[server];
}

function checkHeaders(req, res) {
    if (!req.headers["content-type"] ||
        !req.headers["username"] ||
        !req.headers["password"] ||
        !req.headers["port"] ||
        !req.headers["ftp-host"] ||
        !req.headers["ftp-type"]) {
        res.status(400).json({
            "Status": "ERROR",
            "Message": "Missing headers",
            "Code": 400
        });

        return true;
    }

    if (req.headers["content-type"] != "application/json") {
        res.status(400).json({
            "Status": "ERROR",
            "Message": "Invalid content type",
            "Code": 400
        });

        return true;
    }

    if (req.headers["username"] != username || req.headers["password"] != password) {
        res.status(401).json({
            "Status": "ERROR",
            "Message": "Invalid credentials",
            "Code": 401
        });
        return true;
    }

    return false;
}

app.get("/checkconnection", (req, res) => {
    if (checkHeaders(req, res)) {
        return;
    }

    if (!serverExists(headersToServerName(req.headers))) {
        serverToFile[headersToServerName(req.headers)] = {};
    }

    console.log(serverToFile)

    return res.status(200).json({
        "Status": "SUCCESS",
        "Message": "FTP Server Connected Successfully!",
        "Code": 200
    });
});

app.post("/upload", (req, res) => { 
    if (checkHeaders(req, res)) {
        return;
    }

    if (!serverExists(headersToServerName(req.headers))) {
        return res.status(400).json({
            "Status": "ERROR",
            "Message": "You are not connected to this server",
            "Code": 400
        });
    }

    const files = req.body;

    for (const file of files) {
        serverToFile[headersToServerName(req.headers)][`${file.path}/${file.fileName}`] = file.body;
    }

    console.log(serverToFile)

    res.status(200).json({
        "Status": "SUCCESS",
        "Message": "File upload successfully",
        "Code": 200
    });
});

app.delete("/delete", (req, res) => {
    if (checkHeaders(req, res)) {
        return;
    }

    if (!serverExists(headersToServerName(req.headers))) {
        return res.status(400).json({
            "Status": "ERROR",
            "Message": "You are not connected to this server",
            "Code": 400
        });
    }

    const files = req.body;

    for (const file of files) {
        delete serverToFile[headersToServerName(req.headers)][`${file.path}/${file.fileName}`];
    }

    console.log(serverToFile);

    res.status(200).json({
        "Status": "SUCCESS",
        "Message": "Files deleted successfully",
        "Code": 200
    });
});

app.get("/download", (req, res) => {
    if (checkHeaders(req, res)) {
        return;
    }

    if (!serverExists(headersToServerName(req.headers))) {
        return res.status(400).json({
            "Status": "ERROR",
            "Message": "You are not connected to this server",
            "Code": 400
        });
    }

    if (req.query["path"]) {
        var files = [{"file": req.query["path"], "content": serverToFile[headersToServerName(req.headers)][req.query["path"]]}]
    } else {
        var files = [];
        for (const key in serverToFile[headersToServerName(req.headers)]) {
            files.push({
                "file": key,
                "content": serverToFile[headersToServerName(req.headers)][key]
            });
        }
    }

    console.log(serverToFile)

    res.status(200).json({
        "Status": "SUCCESS",
        "Message": "Files downloaded successfully",
        "Code": 200,
        "Files": files || []
    });
});

app.post("/transfer", (req, res) => {
    if (!req.headers["content-type"] ||
        !req.headers["source-ftp-host"] ||
        !req.headers["source-username"] ||
        !req.headers["source-password"] ||
        !req.headers["source-port"] ||
        !req.headers["source-ftp-type"] ||
        !req.headers["target-ftp-host"] ||
        !req.headers["target-username"] ||
        !req.headers["target-password"] ||
        !req.headers["target-port"] ||
        !req.headers["target-ftp-type"]) {
        return res.status(400).json({
            "Status": "ERROR",
            "Message": "Missing headers",
            "Code": 400
        });
    }

    if (req.headers["content-type"] != "application/json") {
        return res.status(400).json({
            "Status": "ERROR",
            "Message": "Invalid content type",
            "Code": 400
        });
    }

    const sourceServerName = `${req.headers["source-ftp-type"]} ${req.headers["source-ftp-host"]}:${req.headers["source-port"]}`;

    if (!serverExists(sourceServerName)) {
        return res.status(400).json({
            "Status": "ERROR",
            "Message": "Source server is not connected",
            "Code": 400
        });
    }

    const targetServerName = `${req.headers["target-ftp-type"]} ${req.headers["target-ftp-host"]}:${req.headers["target-port"]}`;

    if (!serverExists(targetServerName)) {
        return res.status(400).json({
            "Status": "ERROR",
            "Message": "Target server is not connected",
            "Code": 400
        });
    }

    if (req.headers["source-username"] != username || req.headers["source-password"] != password) {
        return res.status(401).json({
            "Status": "ERROR",
            "Message": "Invalid credentials for source server",
            "Code": 401
        });
    }

    if (req.headers["target-username"] != username || req.headers["target-password"] != password) {
        return res.status(401).json({
            "Status": "ERROR",
            "Message": "Invalid credentials for target server",
            "Code": 401
        });
    }

    const file = req.body;

    if (!serverToFile[sourceServerName][`${file.sourcePath}/${file.sourceFileName}`]) {
        return res.status(400).json({
            "Status": "ERROR",
            "Message": "Source file not found",
            "Code": 400
        });
    }

    serverToFile[targetServerName][`${file.targetPath}/${file.targetFileName}`] = serverToFile[sourceServerName][`${file.sourcePath}/${file.sourceFileName}`];

    console.log(serverToFile);

    return res.status(200).json({
        "Status": "SUCCESS",
        "Message": "Files transfer completed!",
        "Code": 200
    });
})

const port = 8080;
app.listen(port, () => {
    console.log(`Test server listening on port ${port}`);
});