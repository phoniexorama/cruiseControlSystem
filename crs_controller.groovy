pipeline {
    when {
        anyOf {
            changeset "'Design/DriverSwRequest/**/*'"
            changeset "driverSwRequest.groovy"
            changeset "tools/**/*"
        }
    }

    agent none

    environment {

        LOGS_PATH = "Code"
        ZIP_PATH = "C:\\Program Files\\7-Zip\\7z.exe"
        ARTIFACTORY_URL = 'http://ec2-18-197-20-26.eu-central-1.compute.amazonaws.com:8081/artifactory'
        TARGET_PATH = 'cruisecontrolsystem/crs_controller/'
        MODEL_BUILD_LOG = 'crs_controllerBuildLog.json'

        BUILD_ZIP = "build.zip"
        ANALYZER_PATH = ".\\Design\\crs_controller\\pipeline\\analyze\\"
        ZIP_OUTPUT_PATH = "${env.ANALYZER_PATH}${env.BUILD_ZIP}"
        BUILD_FOLDER_PATH = ".\\Design\\crs_controller\\pipeline\\analyze\\build\\"

        CRS_CONTROLLER_ERT_RTW_ZIP = "crs_controller_ert_rtw.zip"
        CODE_GEN_FOLDER_PATH = ".\\Code\\codegen\\"
        CRS_CONTROLLER_ERT_RTW_PATH = ".\\Code\\codegen\\crs_controller_ert_rtw\\"
        CODE_GEN_OUTPUT_PATH = "${env.CODE_GEN_FOLDER_PATH}${env.CRS_CONTROLLER_ERT_RTW_ZIP}"
    }
    stages {

        stage('Verify') {
            agent {
                label 'EC2MatlabServer' // Label for Windows agent
            }
            steps {
                script {
                    // This job executes the Model Advisor Check for the model
                    matlabScript("crs_controllerModelAdvisor;")
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: "$LOGS_PATH/logs/, ./Design/crs_controller/pipeline/analyze/**/*"
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
                    matlabScript("crs_controllerBuild;")

                    //bat "\"${ZIP_PATH}\" a -tzip \"${ZIP_OUTPUT_PATH}\" \"${ANALYZER_PATH}\""
                    bat "\"${ZIP_PATH}\" a -tzip \"${ZIP_OUTPUT_PATH}\" \"${BUILD_FOLDER_PATH}\\*\""

                    // Set up HTTP request parameters
                    def buildUploadUrl = "${env.ARTIFACTORY_URL}/${env.TARGET_PATH}/${env.BUILD_ZIP}"
                    def folderToUpload = "Design/crs_controller/pipeline/analyze/${env.BUILD_ZIP}"

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

                    // Zip the contents of crs_controller_ert_rtw into crs_controller_ert_rtw.zip
                    bat "\"${ZIP_PATH}\" a -tzip \"${CODE_GEN_FOLDER_PATH}${CRS_CONTROLLER_ERT_RTW_ZIP}\" \"${CRS_CONTROLLER_ERT_RTW_PATH}*\""

                    // Set up HTTP request parameters for the upload
                    def ertRtwUploadUrl = "${env.ARTIFACTORY_URL}/${env.TARGET_PATH}/${env.CRS_CONTROLLER_ERT_RTW_ZIP}"
                    def ertRtwFolderToUpload = "${env.CODE_GEN_FOLDER_PATH}${env.CRS_CONTROLLER_ERT_RTW_ZIP}"

                    // Perform HTTP request to upload the zipped folder
                    withCredentials([usernamePassword(credentialsId: 'artifactory_credentials', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                        bat "curl -u ${USERNAME}:${PASSWORD} -T ${ertRtwFolderToUpload} ${ertRtwUploadUrl}"
                    }
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: "$LOGS_PATH/codegen/crs_controller_ert_rtw/**/*, Design/crs_controller/pipeline/analyze/**/*, $LOGS_PATH/logs/"
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
                    matlabScript("crs_controllerTestFile;")
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: "Design/crs_controller/pipeline/analyze/**/*, $LOGS_PATH/logs/, $LOGS_PATH/codegen/crs_controller_ert_rtw/**/*"
                    //junit 'Design/crs_controller/pipeline/analyze/testing/crs_controllerJUnitFormatTestResults.xml'
                }
            }
        }

        stage('Package') {
            agent {
                label 'EC2MatlabServer' // Assuming you have a label for Windows agents
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

                    // Set up HTTP request parameters
                    def codeGenDownloadUrl = "${env.ARTIFACTORY_URL}/${env.TARGET_PATH}/${env.CRS_CONTROLLER_ERT_RTW_ZIP}"
                    def codeGenFolderToDownload = "${CODE_GEN_OUTPUT_PATH}"

                    // Perform HTTP request to upload the file
                    withCredentials([usernamePassword(credentialsId: 'artifactory_credentials', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                        bat "curl -u %USERNAME%:%PASSWORD% -o ${codeGenFolderToDownload} ${codeGenDownloadUrl}"
                    }

                    // Unzip the build.zip file
                    bat "\"${ZIP_PATH}\" x \"${CODE_GEN_OUTPUT_PATH}\" -o\"${CRS_CONTROLLER_ERT_RTW_PATH}\""

                    // Delete the build.zip file after extraction
                    bat "del \"${CODE_GEN_OUTPUT_PATH}\""

                    echo "The model crs_controller has been checked"
                    echo "There is a Summary report generated crs_controllerReport.html"
                    // The summary report is generated which shows results from the previous stages.
                    // Any logs that were generated in the previous stages will be cleared after this stage
                    matlabScript("generateXMLFromLogs('crs_controller'); generateHTMLReport('crs_controller'); deleteLogs;")

                }
            }
            post {
                always {
                    archiveArtifacts artifacts: "Design/crs_controller/pipeline/analyze/**/*, $LOGS_PATH/codegen/crs_controller_ert_rtw/**/*"
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
                    //bat "curl.exe --location --output \"$ARTIFACTS_DOWNLOAD_PATH/Crs_ControllerArtifacts.zip\" --header \"PRIVATE-TOKEN: %CIPROJECTTOKEN%\" \"%CI_SERVER_URL%/api/v4/projects/%CI_PROJECT_ID%/jobs/artifacts/%CI_COMMIT_BRANCH%/download?job=Crs_ControllerPackage\""
                    publishHTML([
                            allowMissing: false,
                            alwaysLinkToLastBuild: false,
                            keepAll: true,
                            reportDir: 'Design/crs_controller/pipeline/analyze/verify/',
                            reportFiles: 'crs_controllerModelAdvisorReport.html',
                            reportName: 'Model Advisor Report'
                    ])

                    publishHTML([
                            allowMissing: false,
                            alwaysLinkToLastBuild: false,
                            keepAll: true,
                            reportDir: 'Design/crs_controller/pipeline/analyze/package/',
                            reportFiles: 'crs_controllerSummaryReport.html',
                            reportName: 'Summary Report'
                    ])
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: "Design/crs_controller/pipeline/analyze/**/*, $LOGS_PATH/Code/codegen/crs_controller_ert_rtw/**/*"
                }
            }
        }

    }
}

def matlabScript(String script) {
    bat "matlab -nodesktop -batch \"openProject('CruiseControlSystem.prj'); ${script}\""
}
