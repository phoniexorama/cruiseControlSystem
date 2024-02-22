pipeline {
    agent {
        label 'EC2MatlabServer' // Label for Windows agent
    }

    environment {
        LOGS_PATH = "Code"
        AWS_REGION = 'eu-central-1' // Specify a valid AWS region
        BUCKET_NAME = 'cruisecontrolsystem' // S3 Bucket name
        FILE_NAME = 'hello_world.txt'
        FILE_CONTENT = 'Hello World!'
    }

    stages {
        stage('Upload to S3') {
            steps {
                script {
                    // Construct the file path where the file will be stored temporarily
                    def filePath = "${env.WORKSPACE}\\${env.FILE_NAME}"

                    // Write the content to the file
                    writeFile file: filePath, text: env.FILE_CONTENT

                    // Find the location of the AWS CLI executable dynamically
                    def awsCliPath = bat(script: 'cmd /c where.exe aws.cmd', returnStdout: true).trim()

                    // Upload the file to S3 using AWS CLI
                    bat "\"${awsCliPath}\" s3 cp \"${filePath}\" s3://${BUCKET_NAME}/${FILE_NAME} --region ${AWS_REGION}"

                    echo "File uploaded successfully to S3 bucket."
                }
            }
        }
    }
}
