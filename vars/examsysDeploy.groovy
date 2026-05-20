def call(Map pipelineParams = [:]) {
    // The path to the tgz file containing the ExamSys code to be deployed.
    String deployFile = pipelineParams.deployFile
    // The servers code will be deployed to, we expect a string containing one server per line,
    // it should include the upgrade and read only servers.
    String[] servers = pipelineParams.servers.split('\n')
    // The server that the upgrade will be run on.
    String upgradeServer = pipelineParams.upgradeServer
    // The user that will be used on the remote servers to install ExamSys.
    String sshUser = pipelineParams.sshUser
    // The credentials that store the username and password to connect to the ExamSys database.
    String credentialsId = pipelineParams.credentialsId

    // Flags if we should run the upgrade script.
    Boolean runUpgradeScript = pipelineParams.runUpgradeScript ?: true
    // the location that the code should live when it is live, it must have no trailing slash.
    String deployLocation = pipelineParams.deployLocation ?: '/var/www/html'
    // The general file permissions for ExamSys.
    String filePermissions = pipelineParams.filePermissions ?: '775'
    // The cli file permissions for ExamSys.
    String cliPermissions = pipelineParams.cliPermissions ?: filePermissions
    // The permissions that the config files will be given.
    String configPermissions = pipelineParams.configPermissions ?: '444'
    // The user and group that the deployed files will be given.
    String serverUser =  pipelineParams.serverUser ?: sshUser
    String serverGroup =  pipelineParams.serverGroup ?: 'www-data'

    // Flags for upgrading help.
    Boolean upgradeStaffHelp = pipelineParams.upgradeStaffHelp ?: true
    Boolean upgradeStudentHelp = pipelineParams.upgradeStudentHelp ?: true

    String configDiff = ''

    // First check that we can deploy to all the servers.
    for (String server : servers) {
        testServer(sshUser, server, deployLocation)
    }

    // Put the code on each of the servers.
    // The code will not yet be in the live directory location.
    for (String server : servers) {
        addCode(
            sshUser,
            server,
            deployFile,
            serverUser,
            configPermissions,
            filePermissions,
            cliPermissions,
            serverGroup,
            deployLocation
        )
    }

    // Move the code to it's live location, we wait until now to minimise the amount of time
    // that each server has different code.
    for (String server : servers) {
        moveCode(sshUser, server, deployLocation)
    }

    if (runUpgradeScript) {
        // Run the upgrade and get back a list of any changes to the config file.
        configDiff = doUpgrade(
            credentialsId,
            sshUser,
            upgradeServer,
            deployLocation,
            upgradeStaffHelp,
            upgradeStudentHelp,
            configPermissions
        )
    }

    // Return the diff of configuration changes, so we can let the admin know,
    // they will need to make them on all front ends (except for the one that ran the upgrade).
    return configDiff
}

private packageFilename() {
    // The name we will use within this code for the deployment file.
    return 'examsysCode.tar.gz'
}

private configDir() {
    // The ExamSys configuration directory, must start with a slash and re relative to the root of ExamSys.
    return '/config'
}

private configFile() {
    // The name of the ExamSys configuration file.
    return configDir() + '/config.inc.php'
}

private newCodeLocation(String deployLocation) {
    // The name of the directory that we will temporarily store the new code.
    return deployLocation + '_new'
}

private oldCodeLocation(String deployLocation) {
    // The name of the directory we will store the old code.
    return deployLocation + '_old'
}

private testServer(
    String sshUser,
    String server,
    String deployLocation
) {
    String newCode = newCodeLocation(deployLocation)
    String packageFilename = packageFilename()

    // Fail if the ExamSys live directory does not exist.
    sh """ssh ${sshUser}@${server} -t 'if [ ! -d ${deployLocation} ]; then exit 1; fi'"""
    // Fail is there is a partial deployment present.
    sh """ssh ${sshUser}@${server} -t 'if [ -d ${newCode} ]; then exit 1; fi'"""
    // Delete the tar.gz file if it is present.
    sh """ssh ${sshUser}@${server} -t 'if [ -f ~/${packageFilename} ]; then rm -f ~/${packageFilename}; fi'"""
}

// groovylint-disable-next-line ParameterCount
private addCode(
    String sshUser,
    String server,
    String deployFile,
    String serverUser,
    String configPermissions,
    String filePermissions,
    String cliPermissions,
    String serverGroup,
    String deployLocation
) {
    String oldCode = oldCodeLocation(deployLocation)
    String newCode = newCodeLocation(deployLocation)
    String packageFilename = packageFilename()
    String configDir = configDir()
    String configFile = configFile()

    // First delete any old code directory on the server, we are going to make it impossible
    // to roll back to a state previous to the upgrade we are about to do.
    sh """ssh ${sshUser}@${server} -t 'if [ -d ${oldCode} ]; then rm -rf ${oldCode}; fi'"""

    // Upload the new code onto the servers.
    sh """ssh ${sshUser}@${server} -t 'mkdir ${newCode}'"""
    sh """scp ${deployFile} ${sshUser}@${upgradeServer}:~/${packageFilename}"""
    sh """ssh ${sshUser}@${server} -t 'cd ${newCode}; tar xzf ~/${packageFilename}'"""

    // Copy the existing config file into the new codebase.
    sh """ssh ${sshUser}@${server} -t 'cp ${deployLocation}${configFile} ${newCode}${configFile}'"""

    // Set the general file permissions.
    sh """ssh ${sshUser}@${server} -t 'chmod -R ${filePermissions} ${newCode}'"""

    // Set the command line file permissions.
    sh """ssh ${sshUser}@${server} -t 'chmod -R ${cliPermissions} ${newCode}/cli/*'"""

    // Protect the config files.
    sh """ssh ${sshUser}@${server} -t 'chmod ${configPermissions} ${newCode}${configDir}/*'"""

    // Set the file ownership correctly.
    sh """ssh ${sshUser}@${server} -t 'chown -R ${serverUser}:${serverGroup} ${newCode}'"""

    // Clean up the server after ourselves.
    sh """ssh ${sshUser}@${server} -t 'rm ~/${packageFilename}'"""
}

private moveCode(
    String sshUser,
    String server,
    String deployLocation
) {
    String oldCode = oldCodeLocation(deployLocation)
    String newCode = newCodeLocation(deployLocation)

    // First store the old code in the old directory, so that if a rollback is needed it can be done manually by
    // a SysAdmin.
    sh """ssh ${sshUser}@${server} -t 'mv ${deployLocation} ${oldCode}'"""
    // Move the new code into it's place.
    sh """ssh ${sshUser}@${server} -t 'mv ${newCode} ${deployLocation}'"""
}

// groovylint-disable-next-line ParameterCount
private doUpgrade(
    String credentialsId,
    String sshUser,
    String upgradeServer,
    String deployLocation,
    Boolean upgradeStaffHelp,
    Boolean upgradeStudentHelp,
    String configPermissions
) {
    String configDiff = ''
    String configDir = configDir()
    String configFile = configFile()

    // Parameters used in the upgrade.
    String staffHelp = upgradeStaffHelp ? '-o1' : ''
    String studentHelp = upgradeStudentHelp ? '-q1' : ''

    // Run the upgrade.
    withCredentials([usernamePassword(
        credentialsId: credentialsId,
        usernameVariable: 'DBUPGRADEUSER',
        passwordVariable: 'DBUPGRADEPASS'
    )]) {
        String oldFile = "$WORKSPACE/config-old.php"
        String newFile = "$WORKSPACE/config-new.php"

        // Clean up config files, if they have been left there by a failed deployment.
        sh """if [ -f ${oldFile} ]; then rm -f ${oldFile}; fi;"""
        sh """if [ -f ${newFile} ]; then rm -f ${newFile}; fi;"""

        // Get the existing config file from the upgrade server.
        sh """scp ${sshUser}@${upgradeServer}:${deployLocation}${configFile} '${oldFile}'"""

        // Allow the config file to be modified during an upgrade.
        sh """ssh ${sshUser}@${upgradeServer} -t 'chmod 774 ${deployLocation}${configFile}'"""

        // Do the upgrade.
        withEnv([
            "DBUPGRADEUSER=${DBUPGRADEUSER}",
            "DBUPGRADEPASS=${DBUPGRADEPASS}"
        ]) {
            String localCommand = """ssh ${sshUser}@${upgradeServer} -t"""
            String parameters = '-u${DBUPGRADEUSER} -p${DBUPGRADEPASS}'
            // There is a bug with the upgrade script that means it only works correctly
            // when run from the root of ExamSys.
            sh """${localCommand} \"cd ${deployLocation}; php cli/upd.php ${parameters} ${staffHelp} ${studentHelp}\""""
        }

        // Make sure that the config file and any backups are readonly.
        sh """ssh ${sshUser}@${upgradeServer} -t 'chmod ${configPermissions} ${deployLocation}${configDir}/*'"""

        // Get the new config file from the upgrade server.
        sh """scp ${sshUser}@${upgradeServer}:${deployLocation}${configFile} '${newFile}'"""

        // Store if there have been any configuration changes.
        configDiff = sh(
            script: """diff ${oldFile} ${newFile}""",
            returnStdout: true
        ).trim()

        // Clean up after ourselves.
        sh """rm ${oldFile}"""
        sh """rm ${newFile}"""
    }

    return configDiff
}
