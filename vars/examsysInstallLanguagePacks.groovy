def call(Map pipelineParams = [:]) {
    // The version branch of ExamSys we are building.
    String version = pipelineParams.version ?: 'develop'
    String langServer = pipelineParams.langServer ?: 'https://examsys-oss.nottingham.ac.uk'
    String langBasePath = pipelineParams.langBasePath ?: 'langpacks/'

    String langPacksURL = langServer + '/' + langBasePath + version + '/'

    // Download and install all language packs for ExamSys.
    sh '''if [ -f rogo.zip ]; then rm rogo.zip; fi'''
    sh """wget ${langPacksURL}/rogo.zip"""
    sh '''unzip -o rogo.zip'''
    sh '''rm rogo.zip'''

    // Download and install the TinyMCE translations.
    dir('plugins') {
        dir('texteditor') {
            dir('plugin_tinymce_texteditor') {
                dir('js') {
                    sh '''if [ -f tinymce_languages.zip ]; then rm tinymce_languages.zip; fi'''
                    sh """wget ${langPacksURL}/tinymce_languages.zip"""
                    sh '''unzip -o tinymce_languages.zip'''
                    sh '''rm tinymce_languages.zip'''
                }
            }
        }
    }
}
