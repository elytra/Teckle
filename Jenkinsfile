pipeline {
	agent any
	stages {
		stage('Build') {
			steps {
				sh 'rm -f private.gradle'
				sh './gradlew setupCiWorkspace clean build'
				archive 'build/libs/*jar'
			}
		}
		stage('Deploy') {
			when {
			    expression {
			        GIT_BRANCH = sh(returnStdout: true, script: 'git rev-parse --abbrev-ref HEAD').trim()
			        return GIT_BRANCH == '1.12'
			    }
			}
			steps {
				withCredentials([file(credentialsId: 'privateGradleNoSnapshotShadow', variable: 'PRIVATEGRADLE')]) {
					sh '''
						cp "$PRIVATEGRADLE" private.gradle
						./gradlew uploadShadow
					'''
				}
			}
		}
	}
}