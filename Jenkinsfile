def deployBranch = 'master'
def agentLabel
if(env.BRANCH_NAME == deployBranch) {
	//branches that are deployable must run on eclipse infra
	agentLabel = "centos-latest"
} else {
	//others (prs for example) can run on any infra
	agentLabel = "centos-latest || linux"
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
		maven 'apache-maven-3.8.5'
		jdk 'openjdk-jdk17-latest'
	}
	stages {
		stage('Build') {
			steps {
				sh 'mvn --batch-mode -U -V -e clean install -Pits -Dmaven.repo.local=$WORKSPACE/.m2/repository'
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
				branch deployBranch
			}
			steps {
				sh 'mvn --batch-mode -V deploy -DskipTests -DaltDeploymentRepository=repo.eclipse.org::default::https://repo.eclipse.org/content/repositories/tycho-snapshots/'
			}
		}
		stage('Deploy sitedocs') {
			when {
				branch 'master'
			}
			steps {
				sh 'mvn --batch-mode -V clean install site site:stage -DskipTests=true'
			}
		}
	}
}
