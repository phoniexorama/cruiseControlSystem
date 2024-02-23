pipeline {
    agent none
    environment {
        LOGS_PATH = "Code"
        ZIP_PATH = "C:\\Program Files\\7-Zip\\7z.exe"
        BUILD_ZIP = "build.zip"
        ANALYZER_PATH = ".\\Design\\DriverSwRequest\\pipeline\\analyze\\"
        ZIP_OUTPUT_PATH = "${env.ANALYZER_PATH}${env.BUILD_ZIP}"
        ARTIFACTORY_URL = 'http://ec2-35-158-218-138.eu-central-1.compute.amazonaws.com:8081/artifactory'
        TARGET_PATH = 'cruisecontrolsystem/DriverSwRequest/'
        MODEL_BUILD_LOG = 'DriverSwRequestBuildLog.json'
        BUILD_FOLDER_PATH = ".\\Design\\DriverSwRequest\\pipeline\\analyze\\build\\"
    }
    stages {
        stage('Verify') {
            agent {
                label 'EC2MatlabServer' // Label for Windows agent
            }
            steps {
                script {
                    // This job executes the Model Advisor Check for the model
                    matlabScript("DriverSwRequestModelAdvisor;")
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: "$LOGS_PATH/logs/, ./Design/DriverSwRequest/pipeline/analyze/**/*"
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
                    matlabScript("DriverSwRequestBuild;")

                    //bat "\"${ZIP_PATH}\" a -tzip \"${ZIP_OUTPUT_PATH}\" \"${ANALYZER_PATH}\""
                    bat "\"${ZIP_PATH}\" a -tzip \"${ZIP_OUTPUT_PATH}\" \"${BUILD_FOLDER_PATH}\\*\""

                    // Set up HTTP request parameters
                    def buildUploadUrl = "${env.ARTIFACTORY_URL}/${env.TARGET_PATH}/${env.BUILD_ZIP}"
                    def folderToUpload = "Design/DriverSwRequest/pipeline/analyze/${env.BUILD_ZIP}"

                    // Perform HTTP request to upload the file
                    withCredentials([usernamePassword(credentialsId: 'artifactory_credentials', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                        sh "curl -u ${USERNAME}:${PASSWORD} -X PUT --data-binary @${folderToUpload} ${buildUploadUrl}"
                    }

                    // Set up HTTP request parameters
                    def uploadUrl = "${env.ARTIFACTORY_URL}/${env.TARGET_PATH}/${env.MODEL_BUILD_LOG}"
                    def fileToUpload = "Code/logs/${env.MODEL_BUILD_LOG}"

                    // Perform HTTP request to upload the file
                    withCredentials([usernamePassword(credentialsId: 'artifactory_credentials', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                        sh "curl -u ${USERNAME}:${PASSWORD} -X PUT --data-binary @${fileToUpload} ${uploadUrl}"

                    }
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: "./Code/codegen/DriverSwRequest_ert_rtw, ./Design/DriverSwRequest/pipeline/analyze/**/*, $LOGS_PATH/logs/"
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
                    matlabScript("DriverSwRequestTest;")
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: "./Design/DriverSwRequestTest/pipeline/analyze/**/*, $LOGS_PATH/logs/, ./Code/codegen/DriverSwRequestTest_ert_rtw"
                    //junit './Design/DriverSwRequestTest/pipeline/analyze/testing/DriverSwRequestTestJUnitFormatTestResults.xml'
                }
            }
        }

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
                        bat "curl -u %USERNAME%:%PASSWORD% -o ${fileToDownload} ${downloadUrl}"
                    }

                    // Set up HTTP request parameters
                    def buildDownloadUrl = "${env.ARTIFACTORY_URL}/${env.TARGET_PATH}/${env.BUILD_ZIP}"
                    def folderToDownload = "${ZIP_OUTPUT_PATH}"

                    // Perform HTTP request to upload the file
                    withCredentials([usernamePassword(credentialsId: 'artifactory_credentials', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                        bat "curl -u %USERNAME%:%PASSWORD% -o ${folderToDownload} ${buildDownloadUrl}"
                    }

                    // Unzip the build.zip file
                    bat "\"${ZIP_PATH}\" x \"${ZIP_OUTPUT_PATH}\" -o\"${BUILD_FOLDER_PATH}\""

                    // Delete the build.zip file after extraction
                    bat "del \"${ZIP_OUTPUT_PATH}\""
                    // The summary report is generated which shows results from the previous stages.
                    // Any logs that were generated in the previous stages will be cleared after this stage
                    echo "The model DriverSwRequest has been checked"
                    echo "There is a Summary report generated DriverSwRequestSummaryReport.html which is present in analyze folder"
                    matlabScript("generateXMLFromLogs('DriverSwRequest'); generateHTMLReport('DriverSwRequest'); deleteLogs;")
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: "Design/DriverSwRequest/pipeline/analyze/**/*, ./Code/codegen/DriverSwRequest_ert_rtw"
                }
            }
        }

        stage('Deploy') {
            agent {
                label 'EC2MatlabServer' // Label for Windows agent
            }
            steps {
                script {
                    echo "Any deployments of code can be made here"
                    echo "All artifacts of previous stage can be found here"
                    // Curl command to download artifacts
                    // bat "curl.exe --location --output \"$ARTIFACTS_DOWNLOAD_PATH/DriverSwRequestArtifacts.zip\" --header \"PRIVATE-TOKEN: %CIPROJECTTOKEN%\" \"%CI_SERVER_URL%/api/v4/projects/%CI_PROJECT_ID%/jobs/artifacts/%CI_COMMIT_BRANCH%/download?job=DriverSwRequestPackage\""
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: "Design/DriverSwRequest/pipeline/analyze/**/*, ./Code/codegen/DriverSwRequest_ert_rtw"
                }
            }
        }
    }
}

def matlabScript(String script) {
    bat "matlab -nodesktop -batch \"openProject('CruiseControlSystem.prj'); ${script}\""
}
