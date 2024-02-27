pipeline {
    agent none
    environment {
        LOGS_PATH = "Code"
        ZIP_PATH = "C:\\Program Files\\7-Zip\\7z.exe"
        ARTIFACTORY_URL = 'http://ec2-35-159-25-238.eu-central-1.compute.amazonaws.com:8081/artifactory'
        TARGET_PATH = 'cruisecontrolsystem/TargetSpeedThrottle/'
        MODEL_BUILD_LOG = 'TargetSpeedThrottleBuildLog.json'

        BUILD_ZIP = "build.zip"
        ANALYZER_PATH = ".\\Design\\TargetSpeedThrottle\\pipeline\\analyze\\"
        ZIP_OUTPUT_PATH = "${env.ANALYZER_PATH}${env.BUILD_ZIP}"
        BUILD_FOLDER_PATH = ".\\Design\\TargetSpeedThrottle\\pipeline\\analyze\\build\\"

        TARGET_SPEED_THROTTLE_ERT_RTW_ZIP = "TargetSpeedThrottle_ert_rtw.zip"
        CODE_GEN_FOLDER_PATH = ".\\Code\\codegen\\"
        TARGET_SPEED_THROTTLE_ERT_RTW_PATH = ".\\Code\\codegen\\TargetSpeedThrottle_ert_rtw\\"
        CODE_GEN_OUTPUT_PATH = "${env.CODE_GEN_FOLDER_PATH}${env.TARGET_SPEED_THROTTLE_ERT_RTW_ZIP}"
    }
    stages {
        stage('Verify') {
            agent {
                label 'EC2MatlabServer' // Label for Windows agent
            }
            steps {
                script {
                    // This job executes the Model Advisor Check for the model
                    matlabScript("TargetSpeedThrottleModelAdvisor;")
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: "$LOGS_PATH/logs/, ./Design/TargetSpeedThrottle/pipeline/analyze/**/*"
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
                    matlabScript("TargetSpeedThrottleBuild;")

                    //bat "\"${ZIP_PATH}\" a -tzip \"${ZIP_OUTPUT_PATH}\" \"${ANALYZER_PATH}\""
                    bat "\"${ZIP_PATH}\" a -tzip \"${ZIP_OUTPUT_PATH}\" \"${BUILD_FOLDER_PATH}\\*\""

                    // Set up HTTP request parameters
                    def buildUploadUrl = "${env.ARTIFACTORY_URL}/${env.TARGET_PATH}/${env.BUILD_ZIP}"
                    def folderToUpload = "Design/TargetSpeedThrottle/pipeline/analyze/${env.BUILD_ZIP}"

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
                    bat "\"${ZIP_PATH}\" a -tzip \"${CODE_GEN_FOLDER_PATH}${TARGET_SPEED_THROTTLE_ERT_RTW_ZIP}\" \"${TARGET_SPEED_THROTTLE_ERT_RTW_PATH}*\""

                    // Set up HTTP request parameters for the upload
                    def ertRtwUploadUrl = "${env.ARTIFACTORY_URL}/${env.TARGET_PATH}/${env.TARGET_SPEED_THROTTLE_ERT_RTW_ZIP}"
                    def ertRtwFolderToUpload = "${env.CODE_GEN_FOLDER_PATH}${env.TARGET_SPEED_THROTTLE_ERT_RTW_ZIP}"

                    // Perform HTTP request to upload the zipped folder
                    withCredentials([usernamePassword(credentialsId: 'artifactory_credentials', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                        bat "curl -u ${USERNAME}:${PASSWORD} -T ${ertRtwFolderToUpload} ${ertRtwUploadUrl}"
                    }
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: "$LOGS_PATH/codegen/TargetSpeedThrottle_ert_rtw/**/*, ./Design/TargetSpeedThrottle/pipeline/analyze/**/*, $LOGS_PATH/logs/"
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
                    matlabScript("TargetSpeedThrottleTest;")
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: "./Design/TargetSpeedThrottle/pipeline/analyze/**/*, $LOGS_PATH/logs/, $LOGS_PATH/codegen/TargetSpeedThrottle_ert_rtw/**/*"
                    //junit './Design/TargetSpeedThrottle/pipeline/analyze/testing/TargetSpeedThrottleJUnitFormatTestResults.xml'
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

                    // Set up HTTP request parameters
                    def codeGenDownloadUrl = "${env.ARTIFACTORY_URL}/${env.TARGET_PATH}/${env.TARGET_SPEED_THROTTLE_ERT_RTW_ZIP}"
                    def codeGenFolderToDownload = "${CODE_GEN_OUTPUT_PATH}"

                    // Perform HTTP request to upload the file
                    withCredentials([usernamePassword(credentialsId: 'artifactory_credentials', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                        bat "curl -u %USERNAME%:%PASSWORD% -o ${codeGenFolderToDownload} ${codeGenDownloadUrl}"
                    }

                    // Unzip the build.zip file
                    bat "\"${ZIP_PATH}\" x \"${CODE_GEN_OUTPUT_PATH}\" -o\"${TARGET_SPEED_THROTTLE_ERT_RTW_PATH}\""

                    // Delete the build.zip file after extraction
                    bat "del \"${CODE_GEN_OUTPUT_PATH}\""
                    // The summary report is generated which shows results from the previous stages.
                    // Any logs that were generated in the previous stages will be cleared after this stage
                    echo "The model TargetSpeedThrottle has been checked"
                    echo "There is a Summary report generated TargetSpeedThrottleReport.html"
                    matlabScript("generateXMLFromLogs('TargetSpeedThrottle'); generateHTMLReport('TargetSpeedThrottle'); deleteLogs;")
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: "Design/TargetSpeedThrottle/pipeline/analyze/**/*, $LOGS_PATH/codegen/TargetSpeedThrottle_ert_rtw/**/*"
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
                    //bat "curl.exe --location --output \"$ARTIFACTS_DOWNLOAD_PATH/TargetSpeedThrottleArtifacts.zip\" --header \"PRIVATE-TOKEN: %CIPROJECTTOKEN%\" \"%CI_SERVER_URL%/api/v4/projects/%CI_PROJECT_ID%/jobs/artifacts/%CI_COMMIT_BRANCH%/download?job=TargetSpeedThrottlePackage\""
                    publishHTML([
                            allowMissing: false,
                            alwaysLinkToLastBuild: false,
                            keepAll: true,
                            reportDir: 'Design/TargetSpeedThrottle/pipeline/analyze/verify/',
                            reportFiles: 'TargetSpeedThrottleModelAdvisorReport.html',
                            reportName: 'Model Advisor Report'
                    ])

                    publishHTML([
                            allowMissing: false,
                            alwaysLinkToLastBuild: false,
                            keepAll: true,
                            reportDir: 'Design/TargetSpeedThrottle/pipeline/analyze/package/',
                            reportFiles: 'TargetSpeedThrottleSummaryReport.html',
                            reportName: 'Summary Report'
                    ])
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: "Design/TargetSpeedThrottle/pipeline/analyze/**/*, $LOGS_PATH/codegen/TargetSpeedThrottle_ert_rtw/**/*"
                }
            }
        }
    }
}

def matlabScript(String script) {
    bat "matlab -nodesktop -batch \"openProject('CruiseControlSystem.prj'); ${script}\""
}
