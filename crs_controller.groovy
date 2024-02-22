pipeline {
    agent none
    environment {
        LOGS_PATH = "Code"
        AWS_REGION = 'eu-central-1' // Specify a valid AWS region
        BUCKET_NAME = 'cruisecontrolsystem' // S3 Bucket name
        FILE_NAME = 'crc_controllerBuildLog.json'
        FILE_PATH = 'Code/Logs/'
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

        stage('upload S3') {
            agent {
                label 'EC2MatlabServer' // Label for Windows agent
            }
            steps {
                script {
                    def amazonS3 = new com.amazonaws.services.s3.AmazonS3Client()
                    amazonS3.setRegion(com.amazonaws.regions.Region.getRegion(com.amazonaws.regions.Regions.fromName(env.AWS_REGION)))

                    // Assuming you have the file to upload in your workspace
                    def fileContent = readFile(file: "${env.WORKSPACE}/${env.FILE_NAME}")
                    def fileObject = new java.io.ByteArrayInputStream(fileContent.bytes)

                    amazonS3.putObject(env.BUCKET_NAME, env.FILE_PATH + env.FILE_NAME, fileObject, new com.amazonaws.services.s3.model.ObjectMetadata())

                    echo "File uploaded successfully to S3 bucket."
                }
            }
        }
    }
}

def matlabScript(String script) {
    bat "matlab -nodesktop -batch \"openProject('CruiseControlSystem.prj'); ${script}\""
}
