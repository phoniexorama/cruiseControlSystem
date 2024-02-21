pipeline {
    agent none
    environment {
        LOGS_PATH = "Code"
        ARTIFACTS_DOWNLOAD_PATH = "C:/Users/${env.GITLAB_USER_LOGIN}/Downloads"
    }
    stages {
        stage('Verify') {
            agent {
                label 'EC2MatlabServer' // Label for Windows agent
            }
            steps {
                script {
                    // This job executes the Model Advisor Check for the model
                    matlabScript("CruiseControlModeModelAdvisor;")
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: "$LOGS_PATH/logs/, ./Design/CruiseControlMode/pipeline/analyze/**/*"
                }
            }
        }

        stage('Build') {
            agent {
                label 'LocalMatlabServer' // Label for Windows agent
            }
            steps {
                script {
                    // This job performs code generation on the model
                    matlabScript("CruiseControlModeBuild;")
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: "./Code/codegen/CruiseControlMode_ert_rtw, ./Design/CruiseControlMode/pipeline/analyze/**/*, $LOGS_PATH/logs/"
                }
            }
        }

        stage('Testing') {
            agent {
                label 'EC2MatlabServer' // Label for EC2 agent
            }
            steps {
                script {
                    // This job executes the unit tests defined in the collection
                    matlabScript("CruiseControlModeTest;")
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: "./Design/CruiseControlMode/pipeline/analyze/**/*, $LOGS_PATH/logs/, ./Code/codegen/CruiseControlMode_ert_rtw"
                    junit './Design/CruiseControlMode/pipeline/analyze/testing/CruiseControlModeJUnitFormatTestResults.xml'
                }
            }
        }

        stage('Package') {
            agent {
                label 'LocalMatlabServer' // Label for Windows agent
            }
            steps {
                script {
                    // The summary report is generated which shows results from the previous stages.
                    // Any logs that were generated in the previous stages will be cleared after this stage
                    echo "The model CruiseControlMode has been checked"
                    echo "There is a Summary report generated cruiseControlModeReport.html"
                    matlabScript("generateXMLFromLogs('CruiseControlMode'); generateHTMLReport('CruiseControlMode'); deleteLogs;")
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: "./Design/CruiseControlMode/pipeline/analyze/**/*, ./Code/codegen/CruiseControlMode_ert_rtw"
                }
            }
        }

        stage('Deploy') {
            agent {
                label 'WinLocalagent' // Label for Windows agent
            }
            steps {
                echo "Any deployments of code can be made here"
                echo "All artifacts of previous stage can be found here"
                script {
                    // Curl command to download artifacts
                    bat "curl.exe --location --output \"$ARTIFACTS_DOWNLOAD_PATH/CruiseControlModeArtifacts.zip\" --header \"PRIVATE-TOKEN: %CIPROJECTTOKEN%\" \"%CI_SERVER_URL%/api/v4/projects/%CI_PROJECT_ID%/jobs/artifacts/%CI_COMMIT_BRANCH%/download?job=CruiseControlModePackage\""
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: "./Design/CruiseControlMode/pipeline/analyze/**/*, ./Code/codegen/CruiseControlMode_ert_rtw"
                }
            }
        }
    }
}

def matlabScript(String script) {
    bat "matlab -nodesktop -batch \"openProject('CruiseControlSystem.prj'); ${script}\""
}
