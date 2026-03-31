def call(Map pipelineParams = [:]) {
    def String name = pipelineParams.name
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
