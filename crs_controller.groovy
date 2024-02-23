pipeline {
    agent none
    environment {
        LOGS_PATH = "Code"
        ARTIFACTORY_URL = 'http://ec2-35-158-218-138.eu-central-1.compute.amazonaws.com:8081/artifactory'
        TARGET_PATH = 'cruisecontrolsystem/crs_controller/'
        MODEL_BUILD_LOG = 'crs_controllerBuildLog.json'
    }
    stages {

        stage('Package') {
            agent {
                label 'EC2MatlabServer' // Label for Windows agent
            }
            steps {
                script {
                    // Set up HTTP request parameters
                    def downloadUrl = "${env.ARTIFACTORY_URL}/${env.TARGET_PATH}/${env.MODEL_BUILD_LOG}"
                    def fileToDownload = "Code/logs/${env.MODEL_BUILD_LOG}"

                    // Perform HTTP request to download the file
                    withCredentials([usernamePassword(credentialsId: 'artifactory_credentials', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                        sh "curl -u \$USERNAME:\$PASSWORD -o ${fileToDownload} ${downloadUrl}"
                    }

                    echo "The model crs_controller has been checked"
                    echo "There is a Summary report generated crs_controllerReport.html"
                    matlabScript("generateXMLFromLogs('crs_controller'); generateHTMLReport('crs_controller'); deleteLogs;")
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: "Design/crs_controller/pipeline/analyze/**/*, ./Code/codegen/crs_controller_ert_rtw"
                }
            }
        }
    }
}

def matlabScript(String script) {
    bat "matlab -nodesktop -batch \"openProject('CruiseControlSystem.prj'); ${script}\""
}
