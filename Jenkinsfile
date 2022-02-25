pipeline {
	options {
		timeout(time: 180, unit: 'MINUTES')
		buildDiscarder(logRotator(numToKeepStr:'10'))
		disableConcurrentBuilds(abortPrevious: true)
	}
	agent {
		label "centos-8"
	}
	tools {
		maven 'apache-maven-latest'
		jdk 'openjdk-jdk11-latest'
	}
	stages {
		stage('Build') {
			steps {
				sh 'mvn -U -V -e clean install org.eclipse.dash:license-tool-plugin:license-check -Pits -Dmaven.repo.local=$WORKSPACE/.m2/repository'
			}
			post {
				always {
					archiveArtifacts artifacts: '*/*/target/work/data/.metadata/.log,*/*/target/work/configuration/*.log'
					junit '*/target/surefire-reports/TEST-*.xml,*/*/target/surefire-reports/TEST-*.xml,*/*/*/target/surefire-reports/TEST-*.xml'
				}
			}
		}
		stage('Deploy Snapshot') {
			when {
				branch 'master'
			}
			steps {
				sh 'mvn -V deploy -DskipTests -DaltDeploymentRepository=repo.eclipse.org::default::https://repo.eclipse.org/content/repositories/tycho-snapshots/'
			}
		}
		stage('Deploy sitedocs') {
			when {
				branch 'master'
			}
			steps {
				sh 'mvn -V clean install site site:stage -DskipTests=true'
			}
		}
	}
}
