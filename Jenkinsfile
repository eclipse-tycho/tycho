pipeline {
	options {
		timeout(time: 180, unit: 'MINUTES')
		buildDiscarder(logRotator(numToKeepStr:'10'))
	}
	agent {
		label "migration"
	}
	tools {
		maven 'apache-maven-latest'
		jdk 'openjdk-jdk11-latest'
	}
	stages {
		stage('Build') {
			steps {
				sh 'mvn -U -V -e clean install -Pits -Dmaven.repo.local=$WORKSPACE/.m2/repository'
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
		stage('Build downstream') {
			when {
				branch 'master'
			}
			steps {
				build job: '/tycho-sitedocs'
			}
		}
	}
}
