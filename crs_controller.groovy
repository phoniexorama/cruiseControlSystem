pipeline {
    agent none
    environment {
        LOGS_PATH = "./Code"
        ARTIFACTS_DOWNLOAD_PATH = "C:/Users/${env.GITLAB_USER_LOGIN}/Downloads"
    }

    stages {

        stage('testing') {
            agent {
                label 'EC2MatlabServer'
            }
            steps {
                script {
                    // This job executes the functional tests defined in the collection
                    matlabScript("crs_controllerTestFile;")
                }
                post {
                    always {
                        //archiveArtifacts(artifacts: ["./Design/crs_controller/pipeline/analyze/**/*", "$LOGS_PATH/logs/", "./Code/codegen/crs_controller_ert_rtw"])
                        junit './Design/crs_controller/pipeline/analyze/testing/crs_controllerJUnitFormatTestResults.xml'
                    }
                }
            }
        }

        stage('package') {
            agent {
                label 'EC2MatlabServer'
            }
            steps {
                script {
                    // The summary report is generated which shows results from the previous stages.
                    // Any logs that were generated in the previous stages will be cleared after this stage
                    echo "The model crs_controller has been checked"
                    echo "There is a Summary report generated crs_controllerReport.html"
                    //matlabScript("generateXMLFromLogs('crs_controller'); generateHTMLReport('crs_controller'); deleteLogs;")
                }
            }
        }

        stage('Deploy') {
            agent {
                label 'EC2MatlabServer'
            }
            steps {
                echo "Any deployments of code can be made here"
                echo "All artifacts of previous stage can be found here"
                
            }
            post {
                always {
                    archiveArtifacts(artifacts: ["./Design/crs_controller/pipeline/analyze/**/*", "./Code/codegen/crs_controller_ert_rtw"])
                }
            }
        }
    }
}

def matlabScript(String script) {
    bat "matlab -batch \"openProject('CruiseControlSystem.prj'); ${script}\""
}
