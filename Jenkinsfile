// https://jenkins.io/doc/book/pipeline/syntax/
// https://jenkins.io/doc/pipeline/steps/
// https://www.cloudbees.com/sites/default/files/declarative-pipeline-refcard.pdf

// https://vetlugin.wordpress.com/2017/01/31/guide-jenkins-pipeline-merge-requests/

// KWA TOOD :
// - estimate deviation from base branch (if relevant)
// - separate stage for the javadoc:aggregate-jar build (in order to -T 1C the packaging)
// - fix the partial build

pipeline {
    agent {
        label 'java11'
    }

    environment {
        MVN_BASE = "/usr/local/maven/bin/mvn --settings ${pwd()}/.ci/settings.xml"
        MVN_COMMAND = "${MVN_BASE} --show-version --batch-mode --errors --fail-at-end -DdeployAtEnd=true "
        M2_REPO = "${HOME}/.m2"
        CI = credentials("app-jenkins")
        SERVICE_SONAR_URL = credentials("service-sonar-java11-url")
        SERVICE_NEXUS_URL = credentials("service-nexus-url")
        SERVICE_REPO_SSHURL = credentials("repository-connection-string")
        SERVICE_GIT_URL = credentials("service-gitlab-url")
        SERVICE_DOCKER_PULL_URL=credentials("SERVICE_DOCKER_PULL_URL")
        SERVICE_REPOSITORY_URL=credentials("service-repository-url")
        GITHUB_ACCOUNT_TOKEN = credentials("vitam-prg-token")
        ES_VERSION="7.17.8"
        MONGO_VERSION="5.0.14"
        MINIO_VERSION="RELEASE.2020-04-15T00-39-01Z" // more precise than edge
        OPENIO_VERSION="18.10"

    }

    options {
        // disableConcurrentBuilds()
        buildDiscarder(
            logRotator(
                artifactDaysToKeepStr: '',
                artifactNumToKeepStr: '',
                daysToKeepStr: '100',
                numToKeepStr: '30'
            )
        )
    }


   stages {

       stage("Tools configuration") {
           steps {
               // Maven : nothing to do, the settings.xml file is passed to maven by command arg & configured by env variables
               // Npm : we could have chosen "npm config" command, but, using a file, we keep the same principle as for maven
               // KWA Note : Awful outside docker...
               // sh "cp -f .ci/.npmrc ~/"
               // sh "rm -f ~/.m2/settings.xml"
               echo "Workspace location : ${env.WORKSPACE}"
               echo "Branch : ${env.GIT_BRANCH}"
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
                sh '''git diff --name-only ${GIT_REV} ${GIT_PRECEDENT_COMMIT} | grep -oE '^[^/]+' | sort | uniq > .changed_roots.txt'''
                // GIT_PREVIOUS_SUCCESSFUL_COMMIT
                script {
                    def changedRoots = readFile(".changed_roots.txt").tokenize('\n')
                    // KWA Caution bis : check if the file is empty before...
                    env.CHANGED_VITAM = changedRoots.contains("sources") || changedRoots.contains("doc")
                    env.CHANGED_VITAM_PRODUCT = changedRoots.contains("rpm") || changedRoots.contains("deb")
                    // KWA Caution : need to get check conditions twice
                }
                // OMA: evaluate project version ; write directly through shell as I didn't find anything else
                sh "$MVN_BASE -q -f sources/pom.xml --non-recursive -Dexec.args='\${project.version}' -Dexec.executable=\"echo\" org.codehaus.mojo:exec-maven-plugin:1.3.1:exec > version_projet.txt"
                echo "Changed VITAM : ${env.CHANGED_VITAM}"
                echo "Changed VITAM : ${env.CHANGED_VITAM_PRODUCT}"
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
                    //  sh "sudo chmod -R 777 ${pwd}/dataminiossl"
                    deleteDir()
                }
                sh "mkdir ${pwd}/dataminiossl"
            }
        }
        stage ("Prepare Docker containers for testing") {
            steps {
                dir('sources') {
                    script {
                        // openstack swift+keystone
                        sh 'docker run -d -m 1g -p 5000:5000 -p 35357:35357 -p 8080:8080 --name swift ${SERVICE_DOCKER_PULL_URL}/jeantil/openstack-keystone-swift:pike'
                        // minIO with SSL
                        sh "docker run -d -m 512m --name miniossl -p 127.0.0.1:9000:9000 --user \$(id -u):\$(id -g) -v ${pwd}/dataminiossl:/data -v ${WORKSPACE}/sources/common/common-storage/src/test/resources/s3/tls:/root/.minio/certs -e \"MINIO_ACCESS_KEY=MKU4HW1K9HSST78MDY3T\" -e \"MINIO_SECRET_KEY=aSyBSStwp4JDZzpNKeJCc0Rdn12hOTa0EFejFfkd\" ${SERVICE_DOCKER_PULL_URL}/minio/minio:${MINIO_VERSION} server /data"
                        // elasticsearch
                        sh 'docker run -d -m 1g --name elasticsearch -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" -e "cluster.name=elasticsearch-data" ${SERVICE_DOCKER_PULL_URL}/elasticsearch:${ES_VERSION}'
                        // mongodb
                        sh "docker run -d -m 1g --name mongodb -p 27017:27017 -v $WORKSPACE/vitam-conf-dev/tests/initdb.d/:/docker-entrypoint-initdb.d/ --health-cmd 'test \$(echo \"rs.status().ok\" | mongo --quiet) -eq 1' --health-start-period 30s --health-interval 10s $SERVICE_DOCKER_PULL_URL/mongo:$MONGO_VERSION mongod --bind_ip_all --replSet rs0"
                        // minIO without SSL
                        sh "docker run -d -m 512m --name minionossl -p 127.0.0.1:9999:9000 -e \"MINIO_ACCESS_KEY=MKU4HW1K9HSST78MDY3T\" -e \"MINIO_SECRET_KEY=aSyBSStwp4JDZzpNKeJCc0Rdn12hOTa0EFejFfkd\" ${SERVICE_DOCKER_PULL_URL}/minio/minio:${MINIO_VERSION} server /data"
                        // openio
                        sh 'docker run -d -m 512m --name openio -p 127.0.0.1:6007:6007 -e "REGION=us-west-1" ${SERVICE_DOCKER_PULL_URL}/openio/sds:${OPENIO_VERSION}'
                        // Configure elasticsearch
                        sh 'while ! curl -v http://localhost:9200; do sleep 2; done'
                        sh 'curl -X PUT http://localhost:9200/_template/default -H \'Content-Type: application/json\' -d \'{"index_patterns": ["*"],"order": -1,"settings": {"number_of_shards": "1","number_of_replicas": "0"}}\''
                        sh 'curl -X PUT -H \'Content-Type: application/json\' http://localhost:9200/_cluster/settings -d \'{ "transient": { "cluster.routing.allocation.disk.threshold_enabled": false } }\''
                        // Configure swift
                        sh 'while ! curl -f http://127.0.0.10:35357/v3; do sleep 2; done'
                        sh 'docker exec swift /swift/bin/register-swift-endpoint.sh http://127.0.0.1:8080'
                    }
                }
            }
        }

        stage ("Execute unit and integration tests on master branches") {
            when {
                anyOf {
                    branch "develop*"
                    branch "master_*"
                    branch "master"
                    tag pattern: "^[1-9]+(\\.rc)?(\\.[0-9]+)?\\.[0-9]+(-.*)?", comparator: "REGEXP"
                }
            }
            options {
                timeout(time: 3, unit: "HOURS")
            }
            environment {
                LANG="fr_FR.UTF-8" // to bypass dateformat problem
            }
            steps {
                dir('sources') {
                    script {
                        try {
                            // Build Vitam
                            sh '$MVN_COMMAND -f pom.xml clean verify org.owasp:dependency-check-maven:aggregate sonar:sonar -Dsonar.projectName=$GIT_BRANCH -Dsonar.projectKey=$(sed -E \'s/[^[:alnum:]]+/_/g\' <<< ${GIT_BRANCH#*/}) -Ddownloader.quick.query.timestamp=false'
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
                    archiveArtifacts (
                        artifacts: '**/dependency-check-report.html'
                        , fingerprint: true
                        , allowEmptyArchive: true

                    )
                }
            }
        }

        stage ("Execute unit and integration tests on merge requests") {
            when {
                not{
                    anyOf {
                        branch "develop*"
                        branch "master_*"
                        branch "master"
                        tag pattern: "^[1-9]+(\\.rc)?(\\.[0-9]+)?\\.[0-9]+(-.*)?", comparator: "REGEXP"
                    }
                }
            }
            environment {
                MVN_COMMAND = "${MVN_BASE} --show-version --batch-mode --errors --fail-at-end -DinstallAtEnd=true -DdeployAtEnd=true "
                LANG="fr_FR.UTF-8" // to bypass dateformat problem
            }
            steps {
                updateGitlabCommitStatus name: 'mergerequest', state: "running"
                dir('sources') {
                    script {
                        try {
                            // Build Vitam
                            sh '$MVN_COMMAND -f pom.xml clean verify org.owasp:dependency-check-maven:aggregate sonar:sonar -Dsonar.projectName=$GIT_BRANCH -Dsonar.projectKey=$(sed -E \'s/[^[:alnum:]]+/_/g\' <<< ${GIT_BRANCH#*/}) -Ddownloader.quick.query.timestamp=false'
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
                    archiveArtifacts (
                        artifacts: '**/dependency-check-report.html'
                        , fingerprint: true
                        , allowEmptyArchive: true

                    )
                    updateGitlabCommitStatus name: 'mergerequest', state: "success"
                    addGitLabMRComment comment: "pipeline-job : [analyse sonar](https://sonar.dev.programmevitam.fr/dashboard?id=${gitlabSourceBranch}) de la branche"
                }
                failure {
                    updateGitlabCommitStatus name: 'mergerequest', state: "failed"
                }
                unstable {
                    updateGitlabCommitStatus name: 'mergerequest', state: "failed"
                }
                aborted {
                    updateGitlabCommitStatus name: 'mergerequest', state: "canceled"
                }
            }
        }

        stage("Build packages") {
            when {
                anyOf {
                    branch "develop*"
                    branch "master_*"
                    branch "master"
                    tag pattern: "^[1-9]+(\\.rc)?(\\.[0-9]+)?\\.[0-9]+(-.*)?", comparator: "REGEXP"
                }
            }
            steps {
                parallel(
                    "Package VITAM solution" : {
                        dir('sources') {
                            // Hack / workaround for javadoc build bug that causes locally built artefacts to be pulled from remote repository
                            // First clean / install artefacts to local repository without deploy
                            // Then rebuild with javadoc, and forcing usage of local artefacts
                            sh '$MVN_COMMAND -f pom.xml -Dmaven.test.skip=true -DskipTests=true clean install -T 1C'
                            sh '$MVN_COMMAND -f pom.xml -Dmaven.test.skip=true -DskipTests=true --no-snapshot-updates javadoc:aggregate-jar deploy rpm:attached-rpm jdeb:jdeb'
                            // -T 1C // Doesn't work with the javadoc:aggregate-jar goal, nor with jdeb plugin (works but not thread safe)
                        }
                    },
                    "Checkout publishing scripts" : {
                        checkout([$class: 'GitSCM',
                            branches: [[name: 'scaleway_j11']],
                            doGenerateSubmoduleConfigurations: false,
                            extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'vitam-build.git']],
                            submoduleCfg: [],
                            userRemoteConfigs: [[credentialsId: 'app-jenkins', url: "$SERVICE_GIT_URL"]]
                        ])
                    }
                )
            }
        }

        stage("Build doc package") {
            when {
                anyOf {
                    branch "develop*"
                    branch "master_*"
                    branch "master"
                    tag pattern: "^[1-9]+(\\.rc)?(\\.[0-9]+)?\\.[0-9]+(-.*)?", comparator: "REGEXP"
                }
            }
            steps {
                dir('doc') {
                    // -T 1C does not work with jdeb:jdeb (works but not thread safe)
                    sh '$MVN_COMMAND -f pom.xml clean install jdeb:jdeb rpm:attached-rpm deploy'
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
                anyOf {
                    branch "develop*"
                    branch "master_*"
                    branch "master"
                    tag pattern: "^[1-9]+(\\.rc)?(\\.[0-9]+)?\\.[0-9]+(-.*)?", comparator: "REGEXP"
                }
            }
            steps {
                sh 'rm -rf deb/vitam-external/target'
                sh 'rm -rf deb/vitam-product/target'
                sh 'rm -rf rpm/vitam-external/target'
                sh 'rm -rf rpm/vitam-product/target'
            }
        }

        stage("Build vitam-product & vitam-external packages") {
            when {
                anyOf {
                    branch "develop*"
                    branch "master_*"
                    branch "master"
                    tag pattern: "^[1-9]+(\\.rc)?(\\.[0-9]+)?\\.[0-9]+(-.*)?", comparator: "REGEXP"
                }
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
                anyOf {
                    branch "develop*"
                    branch "master_*"
                    branch "master"
                    tag pattern: "^[1-9]+(\\.rc)?(\\.[0-9]+)?\\.[0-9]+(-.*)?", comparator: "REGEXP"
                }
            }
            steps {
                parallel(
                    "Upload vitam-product packages": {
                        sshagent (credentials: ['jenkins_sftp_to_repository']) {
                            sh 'vitam-build.git/push_product_repo.sh commit $SERVICE_REPO_SSHURL'
                        }
                    },
                    "Upload vitam-external  packages": {
                        sshagent (credentials: ['jenkins_sftp_to_repository']) {
                            sh 'vitam-build.git/push_external_repo.sh commit $SERVICE_REPO_SSHURL'
                        }
                    },
                    "Upload documentation": {
                        sshagent (credentials: ['jenkins_sftp_to_repository']) {
                            sh 'vitam-build.git/push_doc_repo.sh commit $SERVICE_REPO_SSHURL'
                        }
                    },
                    "Upload sources packages": {
                        sshagent (credentials: ['jenkins_sftp_to_repository']) {
                            sh 'vitam-build.git/push_sources_repo.sh commit $SERVICE_REPO_SSHURL'
                        }
                    },
                    "Upload deployment": {
                        sshagent (credentials: ['jenkins_sftp_to_repository']) {
                            sh 'vitam-build.git/push_deployment_repo.sh commit $SERVICE_REPO_SSHURL'
                        }
                    }
                )
            }
            post {
                success {
                    slackSend (color: '#00aa5b', message: "Build OK de la branche ${env.GIT_BRANCH}, commit: ${env.GIT_COMMIT}", channel: "#pic-ci")
                }
                unstable {
                    slackSend (color: '#ffaa00', message: "Build Unstable de la branche ${env.GIT_BRANCH}, commit: ${env.GIT_COMMIT}", channel: "#pic-ci")
                }
                failure {
                    slackSend (color: '#a30000', message: "Build KO de la branche ${env.GIT_BRANCH}, commit: ${env.GIT_COMMIT}", channel: "#pic-ci")
                }
            }
        }
        stage("Update symlink") {
            when {
                anyOf {
                    branch "develop*"
                    branch "master_*"
                    branch "master"
                    tag pattern: "^[1-9]+(\\.rc)?(\\.[0-9]+)?\\.[0-9]+(-.*)?", comparator: "REGEXP"
                }
            }
            steps {
                sshagent (credentials: ['jenkins_sftp_to_repository']) {
                    sh 'vitam-build.git/push_symlink_repo.sh commit $SERVICE_REPO_SSHURL'
                }
            }
        }

        stage("Information") {
            steps {
                script {
                    for (String i : readFile('vitam_commit.txt').split("\r?\n")) {
                        println i
                    }
                    sh 'rm vitam_commit.txt'
                }
                script {
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
