def call(Map pipelineParams = [:]) {
    // If maintenance mode should be enabled or disabled.
    Boolean enable = pipelineParams.enable ?: true
    // The username that will be used to connect to the servers.
    String username = pipelineParams.username
    // The ExamSys installation directory
    String examsysPath = pipelineParams.path ?: '/var/www/html'
    // A list of servers one per line.
    String serversString = pipelineParams.servers ?: ''
    // An array of server names.
    String[] servers = serversString.split('\n')

    String sourceFile = enable ? '.htaccess-restricted' : '.htaccess-open'

    for (String server : servers) {
        // Change the .htacess file to the version that we want. If the files does not exist there will be no change.
        String command = """cd ${examsysPath}; if [ -f ${sourceFile} ]; then cp -f ${sourceFile} .htaccess; fi"""
        sh """ssh ${username}@${server} -t '${command}'"""
    }
}
