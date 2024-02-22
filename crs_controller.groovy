pipeline {
    //agent any
    agent {
        label 'EC2MatlabServer' // Label for Windows agent
        }
    environment {
        AWS_REGION = 'eu-central-1' // Specify a valid AWS region
        BUCKET_NAME = 'cruisecontrolsystem'
        FILE_NAME = 'hello_keerthi.txt'
        FILE_CONTENT = 'Hello World!'
    }

    stages {
        stage('Upload to S3') {
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
