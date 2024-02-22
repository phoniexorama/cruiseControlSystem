pipeline {
    agent none
    environment {
        LOGS_PATH = "Code"
        AWS_REGION = 'eu-central-1' // Specify a valid AWS region
        BUCKET_NAME = 'cruisecontrolsystem' // S3 Bucket name
        FILE_NAME = 'crc_controllerBuildLog.json'
        FILE_PATH = 'Code/Logs/'
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
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'your-aws-credentials-id', accessKeyVariable: 'AWS_ACCESS_KEY_ID', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
                    script {
                        def amazonS3 = new com.amazonaws.services.s3.AmazonS3Client()
                        amazonS3.setEndpoint("s3.${env.AWS_REGION}.amazonaws.com")

                        // Assuming you have the file to upload in your workspace
                        def fileContent = readFile(file: "${env.WORKSPACE}/${JOB_NAME}/Code/logs/${env.FILE_NAME}")
                        def fileObject = new java.io.ByteArrayInputStream(fileContent.bytes)

                        amazonS3.putObject(env.BUCKET_NAME, "${JOB_NAME}/Code/Logs/crc_controllerBuildLog.json", fileObject, new com.amazonaws.services.s3.model.ObjectMetadata())

                        echo "File uploaded successfully to S3 bucket."
                    }
                }
            }
        }
    }
}

def matlabScript(String script) {
    bat "matlab -nodesktop -batch \"openProject('CruiseControlSystem.prj'); ${script}\""
}
