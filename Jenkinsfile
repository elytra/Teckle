node {
	checkout scm
	sh './gradlew setupCiWorkspace clean build jar'
	archive 'build/libs/*jar'
}