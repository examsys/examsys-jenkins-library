def call(Map pipelineParams = [:]) {
    // The name of the plugin (it will also be the name of the directory the plugin is installed into)
    def String name = pipelineParams.name
    // The details of the repository for the plugin.
    def Map source = pipelineParams.source

    def String buildDir = name + 'Build'
    def Map scmVars

    dir('texteditor') {
        dir('plugin_tinymce_texteditor') {
            dir('js') {
                dir('plugins') {
                    // Build the plugin.
                    dir(buildDir) {
                        scmVars = git(source)
                        sh '''npm install'''
                        sh '''yarn build'''
                    }
                    // Remove the existing built plugin if it is present.
                    sh """if [ -d ${name} ]; then rm -rf ${name}; fi"""
                    // Move the built code to the correct location.
                    sh """mv ${buildDir}/dist/${name} ."""
                    // Delete the build directory.
                    sh """rm -rf ${buildDir}"""
                }
            }
        }
    }

    return scmVars.GIT_COMMIT
}
