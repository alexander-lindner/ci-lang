const fs = require('fs'),
    core = require('@actions/core'),
    github = require('@actions/github'),
    httpc = require('@actions/http-client'),
    exec = require('@actions/exec').exec;


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
        response.message.pipe(file).on('close', async () => {
            file.close();
            let options = {
                listeners: {
                    stdout: (data) => {
                        core.info(data.toString().trim());
                    },
                    stderr: (data) => {
                        core.error(data.toString().trim());
                    }
                }
            };
            await exec("sudo mv cish /bin/", [], options);
            await exec("sudo chmod +x /bin/cish", [], options);
            callback();
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