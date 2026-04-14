def call(Map pipelineParams = [:]) {
    // The type of plugin in ExamSys (it should also match the name of the plugin directory inside ExamSys)
    String type = pipelineParams.type
    // The name of the plugin (this will also be the name of the directory the plugin is installed into)
    String name = pipelineParams.name
    // The details of the repository for the plugin.
    Map source = pipelineParams.source

    Map scmVars

    dir('plugins') {
        dir(type) {
            dir(name) {
                scmVars = git(source)
            }
        }
    }

    return scmVars.GIT_COMMIT
}
