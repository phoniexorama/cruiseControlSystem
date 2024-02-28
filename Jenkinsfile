pipeline {
    agent any //future use

    environment {
        LOGS_PATH = "./Code"
    }

    stages {
        stage('Child pipelines') {
            steps {
                script {
                    // Define child pipeline for driverSwRequest if specific files are changed
                    buildJobIfFilesChanged('driverSwRequest', ['Design/DriverSwRequest/**/*', 'driverSwRequest.groovy', 'Jenkinsfile', 'tools/**/*'])

                    // Define child pipeline for cruiseControlMode if specific files are changed
                    buildJobIfFilesChanged('cruiseControlMode', ['Design/CruiseControlMode/**/*', 'cruiseControlMode.groovy', 'Jenkinsfile', 'tools/**/*'])

                    // Define child pipeline for targetSpeedThrottle if specific files are changed
                    buildJobIfFilesChanged('targetSpeedThrottle', ['Design/TargetSpeedThrottle/**/*', 'targetSpeedThrottle.groovy', 'Jenkinsfile', 'tools/**/*'])

                    // Define child pipeline for crs_controller if specific files are changed
                    buildJobIfFilesChanged('crs_controller', ['Design/crs_controller/**/*', 'crs_controller.groovy', 'Jenkinsfile', 'tools/**/*'])
                }
            }
        }
    }
}

def buildJobIfFilesChanged(jobName, filePatterns) {
    def changed = filePatterns.any { isFilesChanged(it) }
    if (changed) {
        build job: jobName, parameters: [], wait: false
    }
}

def isFilesChanged(String path) {
    def changedFiles = checkout([$class: 'GitSCM']).poll().getRemoteChangeset()
    return changedFiles.any { change ->
        change.getPath().startsWith(path)
    }
}
