def call(Map pipelineParams = [:]) {
    def String type = pipelineParams.type
    def String name = pipelineParams.name
    def Map source = pipelineParams.source

    def Map scmVars

    dir('plugins') {
        dir(type) {
            dir(name) {
                scmVars = git(source)
            }
        }
    }

    return scmVars.GIT_COMMIT
}
