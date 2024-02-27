pipeline {
    agent none

    environment {

        LOGS_PATH = "Code"
        ZIP_PATH = "C:\\Program Files\\7-Zip\\7z.exe"
        ARTIFACTORY_URL = 'http://ec2-35-159-25-238.eu-central-1.compute.amazonaws.com:8081/artifactory'
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
                    archiveArtifacts artifacts: "Design/crs_controller/pipeline/analyze/**/*, ./Code/codegen/crs_controller_ert_rtw"
                }
            }
        }
        

    }
}

def matlabScript(String script) {
    bat "matlab -nodesktop -batch \"openProject('CruiseControlSystem.prj'); ${script}\""
}
