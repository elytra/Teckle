pipeline {
	agent any
	stages {
		stage('Build') {
			steps {
				sh 'rm -f private.gradle'
				sh './gradlew setupCiWorkspace clean build'
				archiveArtifacts 'build/libs/*.jar'
			}
		}
		stage('Deploy') {
			when {
				branch '1.12'
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