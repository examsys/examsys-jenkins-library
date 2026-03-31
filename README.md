# ExamSys global trusted pipeline library for Jenkins

This is used to provide functionality to our pipeline jobs as a
[shared library in Jenkins](https://www.jenkins.io/doc/book/pipeline/shared-libraries/)

## Adding to Jenkins

To use this repository we must also include it as a global trusted pipeline Library in Jenkins.

It should be added with the following settings:

* Name: examsys-library
* Default version: production
* Load implicitly: Unchecked
* Allow default version to be overridden: Checked
* Include @Library changes in job recent changes: Checked
* Cache fetched versions on controller for quick retrieval: Unchecked 
        (assuming you do not want to take manual action to get updates)

## How to use in a pipeline

Include the library with the following command:

```groovy
@Library('examsys-library')
```

To use the development branch use:

```groovy
@Library('examsys-library@development')
```

### Available functions

#### examsysBuild

Has several parameters, where they are optional a default is specified:

* source: The details of the plugins repository, see [git()](https://www.jenkins.io/doc/pipeline/steps/git/)
* production: Flags if the build should only contain production directories (default: true)
* maintenance: Flag to include a `.htaccess-restricted` and `.htaccess-online` file which can be
               used to put ExamSys into and out of maintenance mode (default: false)
* maintenanceIPs: A list of IP addresses (open per line) that should be included in the
                  maintenance `.htaccess-restricted` file
* clean: Flags if we should clean the contents of the directory before we start (default: false)

```groovy
examsysBuild([
    source: [
        url: 'git@github.org:examsys/examsys.git',
        branch: '7.6.1',
        credentialsId: 'ab97fb96-726d-4b05-a3a1-e60db0ff0ae2'
    ],
    production: false,
    maintenance: true,
    maintenanceIPs: '127.0.0.1',
    clean: false,
]) {
    // Add additional build steps here.
}
```

It will create an artefact named: `ExamSys-<commit number>.tar.gz`, this will allow us to
easily associate a build file with a specific commit in the repository if we need to verify
what code it contains.

This step requires that the build node has the following installed:

* grunt
* npm

#### examsysAddPlugin

Has three parameters:

* type: The type of ExamSys plugin
* name: The name of the ExamSys plugin
* source: The details of the plugins repository, see [git()](https://www.jenkins.io/doc/pipeline/steps/git/)

```groovy
examsysAddPlugin([
    type: 'SMS',
    name: 'plugin_cs_sms',
    source: [
        url: 'git@github.com:examsys/examsys-plugin_cs_sms.git',
        branch: '1.4.0',
        credentialsId: 'ab97fb96-726d-4b05-a3a1-e60db0ff0ae2'
    ]
])
```

#### examsysAddTinymcePlugin

Has two parameters:

* name: The name of the TinMCE plugin
* source: The details of the plugins repository, see [git()](https://www.jenkins.io/doc/pipeline/steps/git/)

```groovy
examsysAddTinymcePlugin([
    name: 'maths-equation-editor',
    source: [
        url: 'git@github.com:examsys/examsys-tinymce-plugin-maths-equation-editor.git',
        branch: 'main',
        credentialsId: 'ab97fb96-726d-4b05-a3a1-e60db0ff0ae2'
    ]
])
```

This step requires that the node running it has the following installed:

* npm
* sed
* tar
* wget
* yarn

#### examsysInstallLanguagePacks

Installs all the language packs.

```groovy
examsysAddTinymcePlugin([version: '7.6.0'])
```

## Required Jenkins plugins

* [Git](https://plugins.jenkins.io/git/)

## Development of the library

The library has three branches:

* **production** - This branch should be stable and tested, it will be used for jobs that are used in production.
* **testing** - This branch should be used during testing to ensure that future releases of the code work.
* **development** - New changes should go into this branch
