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

## Development of the library

The library has three branches:

* **production** - This branch should be stable and tested, it will be used for jobs that are used in production.
* **testing** - This branch should be used during testing to ensure that future releases of the code work.
* **development** - New changes should go into this branch
