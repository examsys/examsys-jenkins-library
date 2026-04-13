def call(Map pipelineParams = [:]) {
    // The path to the tgz file containing the ExamSys code to be deployed.
    def String deployFile = pipelineParams.deployFile
    // The servers code will be deployed to, we expect a string containing one server per line,
    // it should include the upgrade and read only servers.
    def String[] servers = pipelineParams.servers.split('\n')
    // The server that the upgrade will be run on.
    def String upgradeServer = pipelineParams.upgradeServer
    // The user that will be used on the remote servers to install ExamSys.
    def String sshUser = pipelineParams.sshUser
    // The credentials that store the username and password to connect to the ExamSys database.
    def String credentialsId = pipelineParams.credentialsId

    // Flags if we should run the upgrade script.
    def Boolean runUpgradeScript = pipelineParams.runUpgradeScript ?: true
    // the location that the code should live when it is live, it must have no trailing slash.
    def String deployLocation = pipelineParams.deployLocation ?: '/var/www/html'
    def String oldCode = deployLocation + '_old'
    def String newCode = deployLocation + '_new'
    // The permissions that the config files will be given.
    def String configPermissions = pipelineParams.configPermissions ?: '444'
    // The user and group that the deployed files will be given.
    def String serverUser =  pipelineParams.serverUser ?: 'www-data'
    def String serverGroup =  pipelineParams.serverGroup ?: 'www-data'

    // Flags for upgrading help.
    def Boolean upgradeStaffHelp = pipelineParams.upgradeStaffHelp ?: true
    def Boolean upgradeStudentHelp = pipelineParams.upgradeStudentHelp ?: true
    // Parameters used in the
    def String staffHelp = upgradeStaffHelp ? '-o1' : ''
    def String studentHelp = upgradeStudentHelp ? '-q1' : ''

    // The name of the file we will use on the servers for the code we are uploading.
    def String packageFilename = 'examsysCode.tar.gz'
    def String configDir = '/config'
    def String configFile = configDir + '/config.php'

    def String configDiff = ''

    // First check that we can deploy to all the servers.
    for(String server : servers) {
        // Fail if the ExamSys live directory does not exist.
        sh """ssh ${sshUser}@${server} -t '[[ ! -d ${deployLocation}]] && exit 1'"""
        // Fail is there is a partial deployment present.
        sh """ssh ${sshUser}@${server} -t '[[ -d ${newCode}]] && exit 1'"""
        // Fail if there is an old deployment file there.
        sh """ssh ${sshUser}@${server} -t '[[ -f ~/${packageFilename}]] && exit 1'"""
    }

    // Put the code on each of the servers.
    // The code will not yet be in the live directory location.
    for(String server : servers) {
        // First delete any old code directory on the server, we are going to make it impossible
        // to roll back to a state previous to the upgrade we are about to do.
        sh """ssh ${sshUser}@${server} -t '[[ -d ${oldCode}]] && rm -rf ${oldCode}'"""

        // Upload the new code onto the servers.
        sh """ssh ${sshUser}@${server} -t 'mkdir ${newCode}'"""
        sh """scp ${deployFile} ${sshUser}@${upgradeServer}:~/${packageFilename}"""
        sh """ssh ${sshUser}@${server} -t 'cd ${newCode}; tar xzf ~/${packageFilename}'"""

        // Copy the existing config file into the new codebase.
        sh """ssh ${sshUser}@${server} -t 'cp ${deployLocation}${configFile} ${newCode}${configFile}'"""

        // Protect the config files.
        sh """ssh ${sshUser}@${server} -t 'chmod ${configPermissions} ${newCode}${configDir}/*'"""

        // Set the file ownership correctly.
        sh """ssh ${sshUser}@${server} -t 'chmod -R ${serverUser}:${serverGroup} ${newCode}'"""

        // Clean up the server after ourselves.
        sh """ssh ${sshUser}@${server} -t 'rm ~/${packageFilename}'"""
    }

    // Move the code to it's live location, we wait until now to minimise the amount of time
    // that each server has different code.
    for(String server : servers) {
        // First store the old code in the old directory, so that if a rollback is needed it can be done manually by
        // a SysAdmin.
        sh """ssh ${sshUser}@${server} -t 'mv ${deployLocation} ${oldCode}'"""
        // Move the new code into it's place.
        sh """ssh ${sshUser}@${server} -t 'mv ${newCode} ${deployLocation}'"""
    }

    if (runUpgradeScript) {
        // Run the upgrade.
        withCredentials([usernamePassword(credentialsId: credentialsId, usernameVariable: 'DBUPGRADEUSER', passwordVariable: 'DBUPGRADEPASS')]) {
            // Get the existing config file from the upgrade server.
            sh """scp ${sshUser}@${upgradeServer}:${deployLocation}${configFile} '$WORKSPACE/config-old.php'"""

            // Allow the config file to be modified during an upgrade.
            sh """ssh ${sshUser}@${server} -t 'chmod 774 ${newCode}${configFile}'"""

            // Do the upgrade.
            sh """ssh ${sshUser}@${upgradeServer} -t 'sudo ${deployLocation}/cli/upd.php  -u${DBUPGRADEUSER} -p${DBUPGRADEPASS} ${staffHelp} ${studentHelp}'"""

            // Make sure that the config file and any backups are readonly.
            sh """ssh ${sshUser}@${server} -t 'chmod ${configPermissions} ${newCode}${configDir}/*'"""

            // Get the new config file from the upgrade server.
            sh """scp ${sshUser}@${upgradeServer}:${deployLocation}${configFile} '$WORKSPACE/config-new.php'"""

            // Store if there have been any configuration changes.
            configDiff = sh(
                script: """diff $WORKSPACE/config-old.php $WORKSPACE/config-new.php"""
                returnStdout: true
            ).trim()

            // Clean up after ourselves.
            sh """rm '$WORKSPACE/config-old.php'"""
            sh """rm '$WORKSPACE/config-new.php'"""
        }
    }

    // Return the diff of configuration changes, so we can let the admin know,
    // they will need to make them on all front ends (except for the one that ran the upgrade).
    return configDiff
}
