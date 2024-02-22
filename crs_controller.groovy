pipeline {
    agent none
    environment {
        LOGS_PATH = "Code"
        AWS_REGION = 'eu-central-1' // Specify a valid AWS region
        BUCKET_NAME = 'cruisecontrolsystem' // S3 Bucket name
        //FILE_NAME = 'crc_controllerBuildLog.json'
        FILE_PATH = 'Code/Logs/'
        FILE_NAME = 'hello_world.txt'
        FILE_CONTENT = 'Hello World!'
        DOWNLOAD_DIR = "${env.WORKSPACE}/DownloadedFiles" // Directory to download the file
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
        stage('Upload to S3') {
            agent {
                label 'EC2MatlabServer' // Label for Windows agent
            }
            steps {
                script {
                    def amazonS3 = new com.amazonaws.services.s3.AmazonS3Client()
                    amazonS3.setRegion(com.amazonaws.regions.Region.getRegion(com.amazonaws.regions.Regions.fromName(env.AWS_REGION)))

                    def fileObject = new java.io.ByteArrayInputStream(env.FILE_CONTENT.bytes)
                    amazonS3.putObject(env.BUCKET_NAME, env.FILE_NAME, fileObject, new com.amazonaws.services.s3.model.ObjectMetadata())

                    echo "File uploaded successfully to S3 bucket."
                }
            }
        }
    }
}

def matlabScript(String script) {
    bat "matlab -nodesktop -batch \"openProject('CruiseControlSystem.prj'); ${script}\""
}
