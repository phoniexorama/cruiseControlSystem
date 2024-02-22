pipeline {
    agent {
        label 'EC2MatlabServer' // Label for Windows agent
    }

    environment {
        FILE_NAME = 'hello_world.txt' // Name of the file to upload
        FILE_CONTENT = 'Hello, World!' // Content of the file
        AWS_REGION = 'eu-central-1' // Specify a valid AWS region
        BUCKET_NAME = 'cruisecontrolsystem' // Specify your S3 bucket name
    }

    stages {
        stage('Upload to S3') {
            steps {
                script {
                    def awsCredentials = com.amazonaws.auth.DefaultAWSCredentialsProviderChain.getInstance().getCredentials()
                    def s3Client = new com.amazonaws.services.s3.AmazonS3ClientBuilder.standard()
                            .withRegion(env.AWS_REGION)
                            .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                            .build()

                    def fileContent = env.FILE_CONTENT.bytes
                    def metadata = new com.amazonaws.services.s3.model.ObjectMetadata()
                    metadata.setContentLength(fileContent.length)

                    // Upload the file to S3
                    s3Client.putObject(env.BUCKET_NAME, env.FILE_NAME, new ByteArrayInputStream(fileContent), metadata)

                    echo "File uploaded successfully to S3 bucket."
                }
            }
        }
    }
}
