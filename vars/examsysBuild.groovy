def call(Map pipelineParams = [:], Closure body) {
    // Parse the parameters for the call.
    def Map source = pipelineParams.source
    def Boolean production = pipelineParams.production ?: true
    def Boolean maintenance = pipelineParams.maintenance ?: false
    def String maintenanceIPs = pipelineParams.maintenanceIPs ?: ''
    def Boolean clean = pipelineParams.clean ?: false

    if (clean) {
        // Remove all files in the directory before ww start.
        sh '''rm -rf *'''
    }

    // Download the ExamSys repository.
    def scmVars = git(source)

    def version = source.branch
    def String exclude = ''

    // Remove any existing composer files.
    sh '''if [ -f composer.phar ]; then rm -f composer.phar; fi'''
    // Install composer libraries.
    sh '''wget https://getcomposer.org/composer.phar'''
    sh '''php composer.phar install --no-dev'''

    // install npm libraries.
    sh '''npm install --production'''
    sh '''npm install --production --prefix plugins/texteditor/plugin_tinymce_texteditor'''
    sh '''grunt'''

    // Add build number to rogo.xml
    sh """sed -i "4i\\    <build>${scmVars.GIT_COMMIT}</build>" config/rogo.xml"""

    body()

    if (production) {
        // Delete directories and files that we should not keep in production.
        exclude = '--exclude-vcs ' +
            '--exclude-vcs-ignores ' +
            // CSS source directories.
            '--exclude="css/source" ' +
            "--exclude='component/**/css' " +
            // Files used to create developer environments.
            '--exclude="Vagrantfile" ' +
            // Files used to get 3rd party libraries.
            '--exclude="crowdin.yml.example" ' +
            '--exclude="package-lock.json" '+
            '--exclude="package.json" ' +
            '--exclude="composer.*" ' +
            // Files used to build ExamSys.
            '--exclude="Gruntfile.js" ' +
            // Automatic tests.
            '--exclude="config/behat.example.xml" ' +
            '--exclude="config/phpunit.example.xml" '+
            '--exclude="testing/behat" ' +
            '--exclude="testing/datagenerator" ' +
            '--exclude="testing/eslint" ' +
            '--exclude="testing/javascript" ' +
            '--exclude="testing/unittest" ' +
            '--exclude="rector.php" ' +
            // Directories created by Jenkins when a repository is downloaded.
            '--exclude="**/*@tmp" ' +
            // Exclude any left over build files
            '--exclude="*.tar.gz" '
    }

    // Tar the files we want to copy over excluding the git files
    sh """tar -czf ${version}.tar.gz ${exclude} * .htaccess"""

    // Save artifact
    archiveArtifacts artifacts: '*.gz'

    // Clean up any artifact files.
    sh '''rm -f *.tar.gz'''

    return scmVars
}
