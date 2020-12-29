const core = require('@actions/core');
const github = require('@actions/github');
const httpc = require('@actions/http-client');

const fs = require('fs');

function copy(callback) {
    let oldPath = "cish",
        newPath = "/bin/cish",
        readStream = fs.createReadStream(oldPath),
        writeStream = fs.createWriteStream(newPath);
    readStream.on('error', callback);
    writeStream.on('error', callback);

    readStream.on('close', () => fs.unlink(oldPath, callback));

    readStream.pipe(writeStream);
}

async function downloadFile(version, callback) {
    const file = fs.createWriteStream("cish");

    const http = new httpc.HttpClient('actions-cish', undefined, {
        allowRetries: true,
        maxRetries: 3
    });
    const url = `https://github.com/alexander-lindner/cish/releases/download/${version}/cish`;
    core.debug(`Downloading from ${url}`);
    const response = await http.get(url);
    const statusCode = response.message.statusCode || 0;

    if (statusCode < 200 || statusCode > 299) {
        const message = `Unexpected HTTP status code '${response.message.statusCode}' when retrieving versions from '${url}'. ${body}`.trim();
        throw new Error(message);
    } else {
        response.message.pipe(file).on('close', () => {
            file.close();
            fs.rename("cish", "/bin/cish", (err) => {
                if (err) {
                    if (err.code === 'EXDEV') {
                        copy(callback);
                    } else {
                        callback(err);
                    }
                    return;
                }
                callback();
            });
        })
    }
}

try {
    const version = core.getInput('version');
    core.debug(`Download Version ${version}!`);

    const payload = JSON.stringify(github.context.payload, undefined, 2)
    core.debug(`The event payload: ${payload}`);

    let callback = (err) => {
        if (err) {
            throw  err;
        }
        core.debug("Cish successfully installed");
    };
    downloadFile("v0.3.0", callback);
} catch (error) {
    core.setFailed(error.message);
}