pipeline {
    agent none

    environment {
        LOGS_PATH = "Code"
        ZIP_PATH = "\"C:\\Program Files\\7-Zip\\7z.exe\"" // Enclose the path in double quotes
        BUILD_ZIP = "build.zip"
        ANALYZER_PATH = ".\\Design\\crs_controller\\pipeline\\analyze\\"
        ARTIFACTORY_URL = 'http://ec2-35-159-25-238.eu-central-1.compute.amazonaws.com:8081/artifactory'
        TARGET_PATH = 'cruisecontrolsystem/crs_controller/'
        MODEL_BUILD_LOG = 'crs_controllerBuildLog.json'
        BUILD_FOLDER_PATH = ".\\Design\\crs_controller\\pipeline\\analyze\\build\\"
        CRS_CONTROLLER_ERT_RTW_ZIP = "crs_controller_ert_rtw.zip"
        CODE_GEN_FOLDER_PATH = "Code\\codegen\\"
        CRS_CONTROLLER_ERT_RTW_PATH = "Code\\codegen\\crs_controller_ert_rtw\\*.*" // Include all files in the directory
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

                    // Zip the contents of crs_controller_ert_rtw into crs_controller_ert_rtw.zip
                    bat "${ZIP_PATH} a -tzip ${CODE_GEN_OUTPUT_PATH} ${CRS_CONTROLLER_ERT_RTW_PATH}"

                    // Set up HTTP request parameters for the upload
                    def ertRtwUploadUrl = "${env.ARTIFACTORY_URL}/${env.TARGET_PATH}/${env.CRS_CONTROLLER_ERT_RTW_ZIP}"

                    // Perform HTTP request to upload the file
                    withCredentials([usernamePassword(credentialsId: 'artifactory_credentials', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                        sh "curl -u ${USERNAME}:${PASSWORD} -T ${CODE_GEN_OUTPUT_PATH} ${ertRtwUploadUrl}"
                    }
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: "$LOGS_PATH/logs/, ./Design/crs_controller/pipeline/analyze/**/*"
                }
            }
        }
    }
}

def matlabScript(String script) {
    bat "matlab -nodesktop -batch \"openProject('CruiseControlSystem.prj'); ${script}\""
}
