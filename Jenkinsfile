node {
	checkout scm
	sh './gradlew setupCiWorkspace clean build jar'
	sh 'rename 's/\.jar/-${BUILD_ID}.jar/' *.jar'
	archive 'build/libs/*jar'
}