def IMPORTANT_BRANCH_OR_TAG = (env.BRANCH_NAME =~ /^(develop|master_.*)$/).matches() || env.TAG_NAME != null

pipeline {
    agent {
        label 'build'
    }

    tools {
        jdk 'java17'
        maven 'maven-3.9'
    }

    environment {
        LANG="fr_FR.UTF-8" // to bypass dateformat problem
        MVN_BASE = "/usr/local/maven/bin/mvn --settings ${pwd()}/.ci/settings.xml"
        MVN_COMMAND = "${MVN_BASE} --show-version --batch-mode --errors --fail-at-end -DinstallAtEnd=true -DdeployAtEnd=true"
        M2_REPO = "${HOME}/.m2"
        CI = credentials("app-jenkins")
        SERVICE_SONAR_URL = credentials("service-sonar-java11-url")
        SERVICE_SONAR_PUBLIC_URL = credentials("service-sonar-url")
        SERVICE_NEXUS_URL = credentials("service-nexus-url")
        SERVICE_GIT_URL = credentials("service-gitlab-url")
        SERVICE_REPO_SSHURL = credentials("repository-connection-string")
        SERVICE_DOCKER_PULL_URL=credentials("SERVICE_DOCKER_PULL_URL")
        SERVICE_REPOSITORY_URL=credentials("service-repository-url")
        GITHUB_ACCOUNT_TOKEN = credentials("vitam-prg-token")
        NVD_API_KEY = credentials("nvd-api-key")
        ES_VERSION="8.18.0"
        MONGO_VERSION="8.0.17"
        MINIO_VERSION="RELEASE.2020-04-15T00-39-01Z" // more precise than edge
        OPENIO_VERSION="18.10"
        JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"
    }

    options {
        timeout(time: 4, unit: 'HOURS')
        buildDiscarder(
            logRotator(
                artifactDaysToKeepStr: '',
                artifactNumToKeepStr: '',
                numToKeepStr: '100'
            )
        )
    }

    parameters {
        booleanParam(name: 'DO_TESTS', defaultValue: IMPORTANT_BRANCH_OR_TAG, description: 'Tick the box to execute unit and integration tests')
        booleanParam(name: 'ADD_OWASP', defaultValue: IMPORTANT_BRANCH_OR_TAG, description: 'Tick the box to run owasp analysis during tests stage')
        booleanParam(name: 'ADD_SONAR', defaultValue: IMPORTANT_BRANCH_OR_TAG, description: 'Tick the box to run sonar analysis during tests stage')
        booleanParam(name: 'DO_PUBLISH', defaultValue: IMPORTANT_BRANCH_OR_TAG, description: 'Tick the box to publish the artifacts to Nexus and Repository (deb/rpm)')
    }

    stages {

        stage('Determine trigger origin') {
            steps {
                script {
                    // https://plugins.jenkins.io/gitlab-plugin/#plugin-content-defined-variables
                    if (env.gitlabMergeRequestId) {
                        // Detect triggered by GitLab
                        echo "Merge request ${env.gitlabMergeRequestId} detected."

                        // https://www.jenkins.io/doc/pipeline/steps/gitlab-plugin/#updategitlabcommitstatus-update-the-commit-status-in-gitlab
                        updateGitlabCommitStatus name: 'mergerequest', state: "running"

                        env.IS_MR = true
                        env.DO_TESTS = true
                        // By default, we don't run owasp, sonar and publish on MR
                        env.ADD_OWASP = false
                        env.ADD_SONAR = false
                        env.DO_PUBLISH = false
                    } else {
                        env.IS_MR = false
                    }
                }
            }
        }

        stage('Ask for build execution (when parameters are not defined)') {
            agent none
            when {
                expression { env.DO_TESTS == null || env.DO_PUBLISH == null }
            }
            steps {
                script {
                    INPUT_PARAMS = input message: 'Configure your build',
                        parameters: [
                            booleanParam(name: 'DO_TESTS', defaultValue: IMPORTANT_BRANCH_OR_TAG, description: 'Tick the box to execute unit and integration tests'),
                            booleanParam(name: 'ADD_OWASP', defaultValue: IMPORTANT_BRANCH_OR_TAG, description: 'Tick the box to run owasp analysis during tests stage'),
                            booleanParam(name: 'ADD_SONAR', defaultValue: IMPORTANT_BRANCH_OR_TAG, description: 'Tick the box to run sonar analysis during tests stage'),
                            booleanParam(name: 'DO_PUBLISH', defaultValue: IMPORTANT_BRANCH_OR_TAG, description: 'Tick the box to publish the artifacts to Nexus and Repository (deb/rpm)')
                        ]

                    env.DO_TESTS = INPUT_PARAMS.DO_TESTS
                    env.ADD_OWASP = INPUT_PARAMS.ADD_OWASP
                    env.ADD_SONAR = INPUT_PARAMS.ADD_SONAR
                    env.DO_PUBLISH = INPUT_PARAMS.DO_PUBLISH
                }
            }
        }

        stage('Show Configuration') {
            steps {
                script {
                    def pom = readMavenPom file: 'sources/pom.xml'
                    env.POM_VERSION = pom.version

                    echo "WORKSPACE location : ${env.WORKSPACE}"
                    echo "GIT_BRANCH : ${env.GIT_BRANCH}"
                    echo "IS_MR : ${env.IS_MR}"
                    echo "IMPORTANT_BRANCH_OR_TAG = ${IMPORTANT_BRANCH_OR_TAG}"
                    echo "DO_TESTS = ${env.DO_TESTS}"
                    echo "ADD_OWASP = ${env.ADD_OWASP}"
                    echo "ADD_SONAR = ${env.ADD_SONAR}"
                    echo "DO_PUBLISH = ${env.DO_PUBLISH}"
                    echo "POM_VERSION = ${env.POM_VERSION}"
                }
            }
        }

        stage("Detecting changes for build") {
            steps {
                script {
                    // OMA : to get info from scm checkout
                    env.GIT_REV=checkout(scm).GIT_COMMIT
                    env.GIT_PRECEDENT_COMMIT=checkout(scm).GIT_PREVIOUS_SUCCESSFUL_COMMIT
                }
                sh "git --git-dir .git rev-parse HEAD > vitam_commit.txt"
                sh "git diff --name-only ${env.GIT_REV} ${env.GIT_PRECEDENT_COMMIT} | grep -oE '^[^/]+' | sort | uniq > .changed_roots.txt"
                // GIT_PREVIOUS_SUCCESSFUL_COMMIT
                script {
                    def changedRoots = readFile(".changed_roots.txt").tokenize('\n')
                    // KWA Caution bis : check if the file is empty before...
                    env.CHANGED_VITAM = changedRoots.contains("sources") || changedRoots.contains("doc")
                    env.CHANGED_VITAM_PRODUCT = changedRoots.contains("rpm") || changedRoots.contains("deb")
                    // KWA Caution : need to get check conditions twice
                }
                echo "Changed VITAM : ${env.CHANGED_VITAM}"
                echo "Changed VITAM_PRODUCT : ${env.CHANGED_VITAM_PRODUCT}"
            }
        }

        stage('Upgrade build context') {
            steps {
                sh 'sudo apt install -y build-essential make'
                sh 'sudo timedatectl set-timezone Europe/Paris'
                nvm('v18.20.3') { // We're installing correct Node version through NVM then update the path to make it available. Do NOT wrap your code in `nvm('...') {}` as it would override the whole PATH and then break tools (jdk, maven) configurations
                    script {
                        nvmPath = sh(script: 'dirname $(which node)', returnStdout: true).trim()
                        env.PATH = "${nvmPath}:${env.PATH}"
                    }
                }
            }
        }

        stage('Reinit host & containers') {
            steps {
                // Force termination / cleanup of containers
                sh 'docker rm -f miniossl elasticsearch mongodb minionossl openio swift'

                // Cleanup any remaining docker volumes
                sh 'docker volume prune -f'

                // Cleanup M2 repo
                sh 'rm -fr ${M2_REPO}/repository/fr/gouv/vitam/'

                // prepare storage for minIO SSL
                dir("${pwd}/dataminiossl") {
                    // bad rustine, as minIO docker writes as root
                    // sh "sudo chmod -R 777 ${pwd}/dataminiossl"
                    deleteDir()
                }
                sh "mkdir ${pwd}/dataminiossl"
            }
        }

        stage ("Prepare Docker containers for testing") {
            when {
                environment(name: 'DO_TESTS', value: 'true')
            }
            steps {
                dir('sources') {
                    script {
                        // openstack swift+keystone
                        sh "docker run -d -m 1g -p 5000:5000 -p 35357:35357 -p 8080:8080 --name swift ${SERVICE_DOCKER_PULL_URL}/jeantil/openstack-keystone-swift:pike"
                        // minIO with SSL
                        sh "docker run -d -m 512m --name miniossl -p 127.0.0.1:9000:9000 --user \$(id -u):\$(id -g) -v ${pwd}/dataminiossl:/data -v ${WORKSPACE}/sources/common/common-storage/src/test/resources/s3/tls:/root/.minio/certs -e \"MINIO_ACCESS_KEY=MKU4HW1K9HSST78MDY3T\" -e \"MINIO_SECRET_KEY=aSyBSStwp4JDZzpNKeJCc0Rdn12hOTa0EFejFfkd\" ${env.SERVICE_DOCKER_PULL_URL}/minio/minio:${env.MINIO_VERSION} server /data"
                        // elasticsearch
                        sh "docker run -d -m 2g --name elasticsearch -p 9200:9200 -p 9300:9300 -e \"xpack.security.enabled=false\" -e \"discovery.type=single-node\" -e \"cluster.name=elasticsearch-data\" ${env.SERVICE_DOCKER_PULL_URL}/elasticsearch:${env.ES_VERSION}"
                        // mongodb
                        sh "docker run -d -m 1g --name mongodb -p 27017:27017 -v $WORKSPACE/vitam-conf-dev/tests/initdb.d/:/docker-entrypoint-initdb.d/ --health-cmd 'test \$(echo \"rs.status().ok\" | mongo --quiet) -eq 1' --health-start-period 30s --health-interval 10s ${env.SERVICE_DOCKER_PULL_URL}/mongo:${env.MONGO_VERSION} mongod --bind_ip_all --replSet rs0"
                        // minIO without SSL
                        sh "docker run -d -m 512m --name minionossl -p 127.0.0.1:9999:9000 -e \"MINIO_ACCESS_KEY=MKU4HW1K9HSST78MDY3T\" -e \"MINIO_SECRET_KEY=aSyBSStwp4JDZzpNKeJCc0Rdn12hOTa0EFejFfkd\" ${env.SERVICE_DOCKER_PULL_URL}/minio/minio:${env.MINIO_VERSION} server /data"
                        // openio
                        sh "docker run -d -m 512m --name openio -p 127.0.0.1:6007:6007 -e \"REGION=us-west-1\" ${env.SERVICE_DOCKER_PULL_URL}/openio/sds:${env.OPENIO_VERSION}"
                        // Configure elasticsearch
                        sh 'while ! curl -v http://localhost:9200; do sleep 2; done'
                        sh 'curl -X PUT http://localhost:9200/_index_template/default -H \'Content-Type: application/json\' -d \'{"index_patterns": ["*"], "priority": 1,"template": { "settings": {"index.number_of_shards": "1", "index.number_of_replicas": "0"}}}\''
                        sh 'curl -X PUT -H \'Content-Type: application/json\' http://localhost:9200/_cluster/settings -d \'{ "transient": { "cluster.routing.allocation.disk.threshold_enabled": false } }\''
                        // Configure swift
                        sh 'while ! curl -f http://127.0.0.10:35357/v3; do sleep 2; done'
                        sh 'docker exec swift /swift/bin/register-swift-endpoint.sh http://127.0.0.1:8080'
                    }
                }
            }
        }

        stage ("Execute unit and integration tests") {
            when {
                environment(name: 'DO_TESTS', value: 'true')
            }
            steps {
                dir('sources') {
                    script {
                        try {
                            // Spotless check
                            sh "${env.MVN_COMMAND} -f pom.xml spotless:check -T 1C"

                            // Build and Test
                            def mvnCmd = "${env.MVN_COMMAND} -f pom.xml clean verify -Dspotless.check.skip"
                            if (env.ADD_OWASP.toBoolean()) {
                                // OWASP Analysis
                                mvnCmd += " -DnvdApiServerId=nvd org.owasp:dependency-check-maven:aggregate"
                            }
                            if (env.ADD_SONAR.toBoolean()) {
                                // Sonar Analysis
                                mvnCmd += " sonar:sonar -Dsonar.projectName=\$GIT_BRANCH -Dsonar.projectKey=\"\$(sed -E 's/[^[:alnum:]]+/_/g' <<< \${GIT_BRANCH#*/})\""
                            }
                            sh mvnCmd
                        } finally {
                            // Force termination / cleanup of containers
                            sh 'docker rm -f miniossl elasticsearch mongodb minionossl openio swift'
                        }
                    }
                }
            }
            post {
                always {
                    junit 'sources/**/target/surefire-reports/*.xml'
                }
                success {
                    script {
                        if (env.ADD_OWASP.toBoolean()) {
                            archiveArtifacts (
                                artifacts: '**/dependency-check-report.html',
                                fingerprint: true,
                                allowEmptyArchive: true
                            )
                        }
                        if (env.IS_MR.toBoolean()) {
                            updateGitlabCommitStatus name: 'mergerequest', state: "success"
                            if (env.ADD_SONAR.toBoolean()) {
                                addGitLabMRComment comment: "pipeline-job : [sonar analysis](${env.SERVICE_SONAR_PUBLIC_URL}/dashboard?id=${gitlabSourceBranch})"
                            }
                        }
                    }
                }
                failure {
                    script {
                        if (env.IS_MR.toBoolean()) {
                            updateGitlabCommitStatus name: 'mergerequest', state: "failed"
                        }
                    }
                }
                unstable {
                    script {
                        if (env.IS_MR.toBoolean()) {
                            updateGitlabCommitStatus name: 'mergerequest', state: "failed"
                        }
                    }
                }
                aborted {
                    script {
                        if (env.IS_MR.toBoolean()) {
                            updateGitlabCommitStatus name: 'mergerequest', state: "canceled"
                        }
                    }
                }
            }
        }

        stage("Build packages") {
            when {
                environment(name: 'DO_PUBLISH', value: 'true')
            }
            steps {
                dir('sources') {
                    // Hack / workaround for javadoc build bug that causes locally built artefacts to be pulled from remote repository
                    // First clean / install artefacts to local repository without deploy
                    // Then rebuild with javadoc, and forcing usage of local artefacts
                    sh "${env.MVN_COMMAND} -f pom.xml -Dmaven.test.skip=true -DskipTests=true -Dspotless.check.skip clean install -T 1C"
                    sh "${env.MVN_COMMAND} -f pom.xml -Dmaven.test.skip=true -DskipTests=true -Dspotless.check.skip --no-snapshot-updates javadoc:aggregate-jar deploy rpm:attached-rpm jdeb:jdeb"
                    // -T 1C // Doesn't work with the javadoc:aggregate-jar goal, nor with jdeb plugin (works but not thread safe)
                }
            }
        }

        stage("Build doc package") {
            when {
                environment(name: 'DO_PUBLISH', value: 'true')
            }
            steps {
                dir('doc') {
                    // -T 1C does not work with jdeb:jdeb (works but not thread safe)
                    sh "${env.MVN_COMMAND} -f pom.xml -Dspotless.check.skip clean install jdeb:jdeb rpm:attached-rpm deploy"
                }
            }
            post {
                always {
                    junit 'doc/**/target/surefire-reports/*.xml'
                }
            }
        }

        stage("Prepare packages building") {
            when {
                environment(name: 'DO_PUBLISH', value: 'true')
            }
            steps {
                sh 'rm -rf deb/vitam-external/target'
                sh 'rm -rf deb/vitam-product/target'
                sh 'rm -rf rpm/vitam-external/target'
                sh 'rm -rf rpm/vitam-product/target'

                // Checkout publishing scripts
                checkout([$class: 'GitSCM',
                    branches: [[name: 'scaleway_j11']],
                    doGenerateSubmoduleConfigurations: false,
                    extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'vitam-build.git']],
                    submoduleCfg: [],
                    userRemoteConfigs: [[credentialsId: "${env.CI_USR}", url: "${env.SERVICE_GIT_URL}"]]
                ])
            }
        }

        stage("Build vitam-product & vitam-external packages") {
            when {
                environment(name: 'DO_PUBLISH', value: 'true')
            }
            steps {
                parallel(
                    "Build vitam-product rpm": {
                        dir('rpm/vitam-product') {
                            sh './build-all-docker.sh'
                        }
                    },
                    "Build vitam-product deb": {
                        dir('deb/vitam-product') {
                            sh './build-all.sh'
                        }
                    },
                    "Download vitam-external rpm": {
                        dir('rpm/vitam-external') {
                            sh './build_repo.sh'
                        }
                    },
                    "Download vitam-external deb": {
                        dir('deb/vitam-external') {
                            sh './build_repo.sh'
                        }
                    }
                )
            }
        }

        stage("Publish packages") {
            when {
                environment(name: 'DO_PUBLISH', value: 'true')
            }
            steps {
                parallel(
                    "Upload vitam-product packages": {
                        sshagent (credentials: ['jenkins_sftp_to_repository']) {
                            sh "vitam-build.git/push_product_repo.sh commit ${env.SERVICE_REPO_SSHURL}"
                        }
                    },
                    "Upload vitam-external packages": {
                        sshagent (credentials: ['jenkins_sftp_to_repository']) {
                            sh "vitam-build.git/push_external_repo.sh commit ${env.SERVICE_REPO_SSHURL}"
                        }
                    },
                    "Upload documentation": {
                        sshagent (credentials: ['jenkins_sftp_to_repository']) {
                            sh "vitam-build.git/push_doc_repo.sh commit ${env.SERVICE_REPO_SSHURL}"
                        }
                    },
                    "Upload sources packages": {
                        sshagent (credentials: ['jenkins_sftp_to_repository']) {
                            sh "vitam-build.git/push_sources_repo.sh commit ${env.SERVICE_REPO_SSHURL}"
                        }
                    },
                    "Upload deployment": {
                        sshagent (credentials: ['jenkins_sftp_to_repository']) {
                            sh "vitam-build.git/push_deployment_repo.sh commit ${env.SERVICE_REPO_SSHURL}"
                        }
                    }
                )
            }
            post {
                success {
                    slackSend (color: '#00aa5b', message: "Publish OK de la branche ${env.GIT_BRANCH}, commit: ${env.GIT_COMMIT}", channel: "#pic-ci")
                }
                unstable {
                    slackSend (color: '#ffaa00', message: "Publish Unstable de la branche ${env.GIT_BRANCH}, commit: ${env.GIT_COMMIT}", channel: "#pic-ci")
                }
                failure {
                    slackSend (color: '#a30000', message: "Publish KO de la branche ${env.GIT_BRANCH}, commit: ${env.GIT_COMMIT}", channel: "#pic-ci")
                }
            }
        }

        stage("Update symlink") {
            when {
                expression { return IMPORTANT_BRANCH_OR_TAG && env.DO_PUBLISH.toBoolean() }
            }
            steps {
                sshagent (credentials: ['jenkins_sftp_to_repository']) {
                    sh "vitam-build.git/push_symlink_repo.sh commit ${env.SERVICE_REPO_SSHURL}"
                }
            }
        }

        stage("Information") {
            steps {
                script {
                    if (fileExists('vitam_commit.txt')) {
                        for (String i : readFile('vitam_commit.txt').split("\r?\n")) {
                            println i
                        }
                        sh 'rm vitam_commit.txt'
                    }
                    dir('.ci') {
                        sh './git_commands.sh'
                    }
                }
            }
        }

    }

    post {
        // Clean after build
        always {
            // Cleanup any remaining docker volumes
            sh 'docker volume prune -f'

            // Cleanup M2 repo
            sh 'rm -fr ${M2_REPO}/repository/fr/gouv/vitam/'

            // Cleanup workspace
            cleanWs()
        }
    }
}
