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

Returns: The commit number of ExamSys that was checked out

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
* sed
* tar
* wget

#### examsysAddPlugin

Has three parameters:

* type: The type of ExamSys plugin
* name: The name of the ExamSys plugin
* source: The details of the plugins repository, see [git()](https://www.jenkins.io/doc/pipeline/steps/git/)

Returns: The commit number of plugin that was checked out

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

Returns: The commit number of TinyMCE plugin that was checked out

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
* yarn

#### examsysInstallLanguagePacks

Installs all the language packs.

Has 2 parameters:

* version: The version of ExamSys the strings are for
* location: The base URL of the language packs (default: https://examsys-oss.nottingham.ac.uk/langpacks/)

```groovy
examsysAddTinymcePlugin([version: '7.6.0'])
```

#### examsysMaintenanceMode

Has four parameters:

* enable: If maintenance mode is turned on or off (default: true)
* username: The user that will be used to turn maintenance mode on or off
* path: The base path that ExamSys is installed in on the servers
* servers: List of servers that should have maintenance mode changed on them. One server per line.

```groovy
examsysMaintenanceMode([
    enable: true,
    username: 'jenkins',
    path: '/var/www/html',
    servers: 'examsys'
])
```

#### examsysDeploy

Has five required parameters:

* deployFile: The name of a tar.gz file that contains the ExamSys code to be deployed
* servers: List of servers that should have code deployed on them. One server per line.
* upgradeServer: The name of the server that the database upgrade will be performed on
* sshUser: The username that will be used to connect to the servers, we assume
           that we have automatic ssh login for them.
* credentialsId: The id of the the credentials provider that provides
                 the username and password that will be passed to the upgrade script

Has seven optional parameters:

* runUpgradeScript: Flag if a database upgrade should be performed (default: true)
* deployLocation: The location that the code should be deployed to (default: /var/www/html)
* configPermissions: The permissions that the config files should have (default: 444)
* filePermissions: The permissions given to the files in ExamSys (default: 775)
* cliPermissions: The permissions of the files in the cli directory of ExamSys (default: the value of filePermissions)
* serverUser: The user that the ExamSys code should be owned by (default: the sshUser)
* serverGroup: The group that the ExamSys code should be owned by (default: www-data)
* upgradeStaffHelp: Flags if staff help should be updated (default: true)
* upgradeStudentHelp: Flags if the student help should be updated (default: true)

```groovy
examsysDeploy([
    deployFile: '/path/to/file.tar.gz',
    servers: 'examsys',
    upgradeServer: 'examsys',
    sshUser: 'jenkins',
    credentialsId: 'ab97fb96-726d-4b05-a3a1-e60db0ff0ae2',
    runUpgradeScript: true,
    deployLocation: '/var/www/html',
    configPermissions: '444',
    filePermissions: '775',
    cliPermissions: '775',
    serverUser: 'jenkins',
    serverGroup: 'www-data',
    upgradeStaffHelp: true,
    upgradeStudentHelp: true
])
```

This step requires that the user logging into the servers can `sudo` without further
authentication

## Required Jenkins plugins

* [Git](https://plugins.jenkins.io/git/)
* [SSH Agent](https://plugins.jenkins.io/ssh-agent/)

## Development of the library

The library has three branches:

* **production** - This branch should be stable and tested, it will be used for jobs that are used in production.
* **testing** - This branch should be used during testing to ensure that future releases of the code work.
* **development** - New changes should go into this branch

The project can be linted on Linux using:

```bash
bin/lint
```
