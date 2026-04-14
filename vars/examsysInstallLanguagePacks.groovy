def call(Map pipelineParams = [:]) {
    // The version of language files we are building for..
    String version = pipelineParams.version ?: 'develop'
    // The name of the server we get the language files for, we will allow it to be overridden from the normal place.
    String location = pipelineParams.location ?: 'https://examsys-oss.nottingham.ac.uk/langpacks/'

    // Since ExamSys 7.1 we have not allowed language string changes in minor version, which means we
    // should always get strings for the x.y.0 version.
    String versionSeparator = '.'
    String[] splitVersion = version.split(versionSeparator)
    if (splitVersion.length > 2) {
        // groovylint-disable-next-line DuplicateNumberLiteral
        version = splitVersion[0] + versionSeparator + splitVersion[1] + versionSeparator + '0'
    }

    String langPacksURL = location + version

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
