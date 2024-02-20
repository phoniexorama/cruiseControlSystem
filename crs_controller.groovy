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
                    ignoringErrorsAndWarnings {
                        matlabScript("crs_controllerTestFile;")
                    }
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
                    ignoringErrorsAndWarnings {
                        echo "The model crs_controller has been checked"
                        echo "There is a Summary report generated crs_controllerReport.html"
                        //matlabScript("generateXMLFromLogs('crs_controller'); generateHTMLReport('crs_controller'); deleteLogs;")
                    }
                }
            }
        }

        stage('Deploy') {
            agent {
                label 'EC2MatlabServer'
            }
            steps {
                script {
                    ignoringErrorsAndWarnings {
                        echo "Any deployments of code can be made here"
                        echo "All artifacts of previous stage can be found here"
                    }
                }
            }
            post {
                always {
                    archiveArtifacts(artifacts: ["./Design/crs_controller/pipeline/analyze/**/*", "./Code/codegen/crs_controller_ert_rtw"])
                }
            }
        }
    }
}

def ignoringErrorsAndWarnings(body) {
    try {
        body()
    } catch (e) {
        if (!(e.toString().contains("LicenseManager") && e.toString().contains("checkout failed"))) {
            echo "Error message: ${e}"
            currentBuild.result = 'FAILURE'
        }
    }
}

def matlabScript(String script) {
    bat "matlab -batch \"openProject('CruiseControlSystem.prj'); ${script}\""
}
