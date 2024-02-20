pipeline {
    agent {
    label 'EC2MatlabServer'
}
    environment {
        LOGS_PATH = "./Code"
        ARTIFACTS_DOWNLOAD_PATH = "C:/Users/${env.GITLAB_USER_LOGIN}/Downloads"
    }
    stages {
        stage('testing') {
            steps {
                script {
                    node('EC2MatlabServer') {
                        ignoringErrorsAndWarnings {
                            matlabScript("crs_controllerTestFile;")
                        }
                    }
                }
            }
        }
    }  
}

def ignoringErrorsAndWarnings(body) {
    try {
        body()
    } catch (e) {
        if (!(e.toString().contains("LicenseManager") && e.toString().contains("checkout failed"))) {
            echo "Error message: ${e}"
            currentBuild.result = 'FAILURE'
        }
    }
}

def matlabScript(String script) {
    bat "matlab -batch \"openProject('CruiseControlSystem.prj'); ${script}\""
}
