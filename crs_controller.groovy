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
                    archiveArtifacts artifacts: "Code/codegen/crs_controller_ert_rtw, Design/crs_controller/pipeline/analyze/**/*, $LOGS_PATH/logs/"
                }
            }
        }



    }
}

def matlabScript(String script) {
    bat "matlab -nodesktop -batch \"openProject('CruiseControlSystem.prj'); ${script}\""
}
