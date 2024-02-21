pipeline {
    agent none
    environment {
        LOGS_PATH = "Code"
        AWS_REGION = 'eu-central-1' // Specify a valid AWS region
        BUCKET_NAME = 'cruisecontrolsystem'
        DIRECTORY_TO_ZIP = 'Design/crs_controller/pipeline/analyze'
        ZIP_FILE_NAME = 'crs_controller.zip'
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
                label 'EC2MatlabServer' // Label for Windows agent
            }
            steps {
                script {
                    // This job performs code generation on the model
                    echo "Bypass this stage"
                    //matlabScript("crs_controllerBuild;")
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
                    archiveArtifacts artifacts: "./Design/crs_controller/pipeline/analyze/**/*, $LOGS_PATH/logs/, ./Code/codegen/crs_controller_ert_rtw"
                    //junit './Design/crs_controller/pipeline/analyze/testing/crs_controllerJUnitFormatTestResults.xml'
                    //junit '**/CruiseControlModeJUnitFormatTestResults.xml'
                }
            }
        }

        stage('Package') {
            agent {
                label 'EC2MatlabServer' // Label for Windows agent
            }
            steps {
                script {
                    // The summary report is generated which shows results from the previous stages.
                    // Any logs that were generated in the previous stages will be cleared after this stage
                    echo "The model crs_controller has been checked"
                    echo "There is a Summary report generated crs_controllerReport.html"
                    matlabScript("generateXMLFromLogs('crs_controller'); generateHTMLReport('crs_controller'); deleteLogs;")
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: "Design/crs_controller/pipeline/analyze/**/*"
                }
            }
        }

        stage('Deploy to S3') {
            agent {
                label 'EC2MatlabServer' // Label for Windows agent
            }
            steps {
                script {
                    def zipFile = new File(ZIP_FILE_NAME)
                    zipFile.delete() // Delete the zip file if it already exists

                    // Create a new zip file
                    def zipOutput = new java.util.zip.ZipOutputStream(new FileOutputStream(zipFile))

                    // Function to add a file to the zip
                    def addToZip(File fileToAdd, String basePath) {
                        def relativePath = fileToAdd.absolutePath - basePath
                        if (fileToAdd.isDirectory()) {
                            zipOutput.putNextEntry(new java.util.zip.ZipEntry(relativePath + "/"))
                            fileToAdd.listFiles().each { nestedFile ->
                                addToZip(nestedFile, basePath)
                            }
                        } else {
                            zipOutput.putNextEntry(new java.util.zip.ZipEntry(relativePath))
                            fileToAdd.withInputStream { inputStream ->
                                zipOutput << inputStream
                            }
                        }
                    }

                    // Add files from the specified directory to the zip
                    addToZip(new File(DIRECTORY_TO_ZIP), DIRECTORY_TO_ZIP)

                    // Close the zip output stream
                    zipOutput.close()

                    // Upload the zip file to S3
                    def amazonS3 = new com.amazonaws.services.s3.AmazonS3Client()
                    amazonS3.setRegion(com.amazonaws.regions.Region.getRegion(com.amazonaws.regions.Regions.fromName(env.AWS_REGION)))

                    def fileObject = new FileInputStream(ZIP_FILE_NAME)
                    amazonS3.putObject(env.BUCKET_NAME, ZIP_FILE_NAME, fileObject, new com.amazonaws.services.s3.model.ObjectMetadata())

                    echo "Zip file uploaded successfully to S3 bucket."
                }
            }
        }
    }
}

def matlabScript(String script) {
    bat "matlab -nodesktop -batch \"openProject('CruiseControlSystem.prj'); ${script}\""
}
