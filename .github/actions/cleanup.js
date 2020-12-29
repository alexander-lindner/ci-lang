const core = require('@actions/core'),
    exec = require('@actions/exec').exec;

async function remove() {
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
    await exec("sudo rm -f /bin/cish", [], options);
}

remove();