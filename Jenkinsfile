def deployBranch = 'main'
def agentLabel
if(env.BRANCH_NAME == deployBranch) {
	//branches that are deployable must run on eclipse infra
	agentLabel = "ubuntu-latest"
} else {
	//others (prs for example) can run on any infra
	agentLabel = "ubuntu-latest || linux"
}

pipeline {
	options {
		timeout(time: 180, unit: 'MINUTES')
		buildDiscarder(logRotator(numToKeepStr:'10'))
		disableConcurrentBuilds(abortPrevious: true)
	}
	agent {
		label agentLabel
	}
	tools {
		maven 'apache-maven-3.9.9'
		jdk 'temurin-jdk21-latest'
	}
	environment {
		MAVEN_OPTS = '-Xmx2500m -XX:+PrintFlagsFinal'
	}
	stages {
		stage('Build') {
			steps {
				sh 'mvn --batch-mode -U -V -e clean install site site:stage -Pits -Dmaven.repo.local=$WORKSPACE/.m2/repository'
			}
			post {
				always {
					junit '*/target/surefire-reports/TEST-*.xml,*/*/target/surefire-reports/TEST-*.xml,*/*/*/target/surefire-reports/TEST-*.xml'
				}
			}
		}
		stage('Deploy Snapshot') {
			when {
				branch deployBranch
			}
			steps {
				sh 'mvn --batch-mode -V -ntp deploy -DskipTests -DaltDeploymentRepository=repo.eclipse.org::https://repo.eclipse.org/content/repositories/tycho-snapshots/'
			}
		}
	}
}
