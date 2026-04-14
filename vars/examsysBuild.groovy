def call(Map pipelineParams = [:], Closure body) {
    // The details of the ExamSys repository.
    Map source = pipelineParams.source
    // Flag if this is a build for a production like server.
    Boolean production = pipelineParams.production ?: true
    // Flag if we want to add the .htaccess files that can be used to turn on maintenance mode easily.
    Boolean maintenance = pipelineParams.maintenance ?: false
    // List of IP addresses that should be allowed to access ExamSys.
    String maintenanceIPs = pipelineParams.maintenanceIPs ?: ''
    // Flags if we should delete the contents of the directory this call was made from.
    Boolean clean = pipelineParams.clean ?: false

    if (clean) {
        // Remove all files in the directory before ww start.
        sh '''rm -rf *'''
    }

    // Download the ExamSys repository.
    def scmVars = git(source)

    String exclude = ''
    String include = '* .htaccess'
    String buildFile = "ExamSys-${scmVars.GIT_COMMIT}.tar.gz"

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

    sh '''if [ -f .htaccess-open ]; then rm -f .htaccess-open; fi'''
    sh '''if [ -f .htaccess-restricted ]; then rm -f .htaccess-restricted; fi'''
    if (maintenance) {
        sh '''cp .htaccess .htaccess-open'''
        sh '''cp .htaccess .htaccess-restricted'''

        // Add in any IP addressed that have been listed.
        buildMaintenanceHTAccess(maintenanceIPs, '.htaccess-restricted')

        include = include + ' .htaccess-restricted'
    }

    if (production) {
        exclude = getExcludedProductionFiles()
    }

    // Clean up any artifact files.
    sh '''rm -f *.tar.gz'''

    // Tar the files we want to copy over excluding the git files
    sh """tar -czf ${buildFile} ${exclude} ${include}"""

    // Save artifact
    archiveArtifacts artifacts: buildFile

    return scmVars.GIT_COMMIT
}

/**
 * Directories and files that we should not keep in production.
 */
private getExcludedProductionFiles() {
    String exclude = '--exclude-vcs ' +
        '--exclude-vcs-ignores ' +
        // CSS source directories.
        '--exclude="css/source" ' +
        "--exclude='component/**/css' " +
        // Files used to create developer environments.
        '--exclude="Vagrantfile" ' +
        // Files used to get 3rd party libraries.
        '--exclude="crowdin.yml.example" ' +
        '--exclude="package-lock.json" ' +
        '--exclude="package.json" ' +
        '--exclude="composer.*" ' +
        // Files used to build ExamSys.
        '--exclude="Gruntfile.js" ' +
        // Automatic tests.
        '--exclude="config/behat.example.xml" ' +
        '--exclude="config/phpunit.example.xml" ' +
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

    return exclude
}

/**
 * Modifies an ExamSys .htaccess file so that it will turn on maintenance mode
 */
private buildMaintenanceHTAccess(String maintenanceIPs, String filename) {
    // Uncomment the rewrite rules.
    sh """sed -i 's/#Options -MultiViews +FollowSymLinks/Options -MultiViews +FollowSymLinks/' ${filename}"""
    sh """sed -i 's/#RewriteEngine On/RewriteEngine On/' ${filename}"""
    sh """sed -i 's/#RewriteRule \\!(maintenance/RewriteRule \\!(maintenance/' ${filename}"""

    if (maintenanceIPs.equals('')) {
        // We will
        return
    }

    String[] ipAddresses = maintenanceIPs.split('\n')
    for (String address : ipAddresses) {
        if (!address.equals('')) {
            // groovylint-disable-next-line LineLength
            sh """sed -i '/#RewriteCond \\%{REMOTE_ADDR} \\!<ip address> \\[NC\\]/a RewriteCond \\%{REMOTE_ADDR} \\!${address} \\[NC\\]' ${filename}"""
        }
    }
}
