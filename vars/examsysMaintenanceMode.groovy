def call(Map pipelineParams = [:]) {
    // If maintenance mode should be enabled or disabled.
    def Boolean enable = pipelineParams.enable ?: true;
    // The username that will be used to connect to the servers.
    def String username = pipelineParams.username
    // The id of the credentials we will use to SSH into the servers.
    def String credentials = pipelineParams.credentialId
    // The ExamSys installation directory
    def String examsysPath = pipelineParams.path ?: '/var/www/html'
    // A list of servers one per line.
    def String serversString = pipelineParams.servers ?: ''
    // An array of server names.
    def String[] servers = serversString.split('\n')

    def String sourceFile = enable ? '.htaccess-restricted' : '.htaccess-online'

    for(String server : servers) {
        sshagent([credentials]) {
            // Change the .htacess file to the version that we want. If the files does not exist there will be no change.
            sh """ssh $deployuser@$server -t 'cd ${examsysPath}; if [ -f ${sourceFile} ]; then cp -f ${sourceFile} .htaccess; fi'"""
        }
    }
}
