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
        MVN_COMMAND = "${MVN_BASE} --show-version --batch-mode --errors --fail-at-end -DinstallAtEnd=true -DdeployAtEnd=true "
        DEPLOY_GOAL = "install" // Deploy goal used by maven ; typically "deploy" for master* branches & "" (nothing) for everything else (we don't deploy) ; keep a space so can work in other branches than develop
        CI = credentials("app-jenkins")
        SERVICE_SONAR_URL = credentials("service-sonar-url")
        SERVICE_NEXUS_URL = credentials("service-nexus-url")
        SERVICE_CHECKMARX_URL = credentials("service-checkmarx-url")
        SERVICE_REPO_SSHURL = credentials("repository-connection-string")
        SERVICE_GIT_URL = credentials("service-gitlab-url")
        SERVICE_PROXY_HOST = credentials("http-proxy-host")
        SERVICE_PROXY_PORT = credentials("http-proxy-port")
        SERVICE_NOPROXY = credentials("http_nonProxyHosts")
        SERVICE_DOCKER_PULL_URL=credentials("SERVICE_DOCKER_PULL_URL")
        SERVICE_REPOSITORY_URL=credentials("service-repository-url")
        GITHUB_ACCOUNT_TOKEN = credentials("vitam-prg-token")
        ES_VERSION="7.6.2"
        MONGO_VERSION="4.2.5"
        MINIO_VERSION="RELEASE.2020-04-15T00-39-01Z" // more precise than edge
        OPENIO_VERSION="18.10"
    }

    options {
        // disableConcurrentBuilds()
        buildDiscarder(
            logRotator(
                artifactDaysToKeepStr: '',
                artifactNumToKeepStr: '',
                numToKeepStr: '100'
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

                    // init default deploy_goal.txt
                    writeFile file: 'deploy_goal.txt', text: "${env.DEPLOY_GOAL}"
                }
                // OMA: evaluate project version ; write directly through shell as I didn't find anything else
                sh "$MVN_BASE -q -f sources/pom.xml --non-recursive -Dexec.args='\${project.version}' -Dexec.executable=\"echo\" org.codehaus.mojo:exec-maven-plugin:1.3.1:exec > version_projet.txt"
                echo "Changed VITAM : ${env.CHANGED_VITAM}"
                echo "Changed VITAM : ${env.CHANGED_VITAM_PRODUCT}"		
            }
        }

        // Override the default maven deploy target when on master (publish on nexus)
        stage("Computing maven target") {
            when {
                anyOf {
                    branch "develop*"
                    branch "master_*"
                    branch "master"
                    tag pattern: "^[1-9]+\\.[0-9]+\\.[0-9]+-?[0-9]*\$", comparator: "REGEXP"
                }
            }
            environment {
                DEPLOY_GOAL = "deploy"
                MASTER_BRANCH = "true"
            }
            steps {
                script {
                    // overwrite file content with one more goal
                    writeFile file: 'deploy_goal.txt', text: "${env.DEPLOY_GOAL}"
                    writeFile file: 'master_branch.txt', text: "${env.MASTER_BRANCH}"
                 }
                echo "We are on master branch (${env.GIT_BRANCH}) ; deploy goal is \"${env.DEPLOY_GOAL}\""
            }
        }

        stage('Reinit local s3 storages when tests') {
            steps {
                // prepare storage for minIO
                dir("${pwd}/dataminio") {
                    // bad rustine, as minIO docker writes as root
                    //  sh "sudo chmod -R 777 ${pwd}/dataminio"
                    deleteDir()
                }
                sh "mkdir ${pwd}/dataminio"
                // prepare storage for minIO SSL
                dir("${pwd}/dataminiossl") {
                    // bad rustine, as minIO docker writes as root
                    //  sh "sudo chmod -R 777 ${pwd}/dataminiossl"
                    deleteDir()
                }
                sh "mkdir ${pwd}/dataminiossl"
                // test
                echo "echo ${WORKSPACE}"
            }
        }

        stage ("Execute unit and integration tests on master branches") {
            when {
                anyOf {
                    branch "develop*"
                    branch "master_*"
                    branch "master"
                    tag pattern: "^[1-9]+\\.[0-9]+\\.[0-9]+-?[0-9]*\$", comparator: "REGEXP"
                }
            }
            environment {
                LANG="fr_FR.UTF-8" // to bypass dateformat problem
            }
            steps {
                dir('sources') {
                    script {
                        docker.withRegistry("http://${env.SERVICE_DOCKER_PULL_URL}") {
                            // minIO SSL first
                            docker.image("${env.SERVICE_DOCKER_PULL_URL}/minio/minio:${env.MINIO_VERSION}").withRun("--user \$(id -u):\$(id -g) -p 127.0.0.1:9000:9000 -v ${pwd}/dataminiossl:/data -v ${WORKSPACE}/sources/common/common-storage/src/test/resources/s3/tls:/root/.minio/certs -e \"MINIO_ACCESS_KEY=MKU4HW1K9HSST78MDY3T\" -e \"MINIO_SECRET_KEY=aSyBSStwp4JDZzpNKeJCc0Rdn12hOTa0EFejFfkd\"",'server /data') { o ->
                                docker.image("${env.SERVICE_DOCKER_PULL_URL}/elasticsearch:${env.ES_VERSION}").withRun('-p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" -e "cluster.name=elasticsearch-data"') { d ->
                                    sh 'while ! curl -v http://localhost:9200; do sleep 2; done'
                                    sh 'curl -X PUT http://localhost:9200/_template/default -H \'Content-Type: application/json\' -d \'{"index_patterns": ["*"],"order": -1,"settings": {"number_of_shards": "1","number_of_replicas": "0"}}\''
                                    docker.image("${env.SERVICE_DOCKER_PULL_URL}/mongo:${env.MONGO_VERSION}").withRun('-p 27017:27017 -v ${WORKSPACE}/vitam-conf-dev/tests/initdb.d/:/docker-entrypoint-initdb.d/ --health-cmd "test $$(echo "rs.status().ok" | mongo --quiet) -eq 1" --health-start-period 30s --health-interval 10s','mongod --bind_ip_all --replSet rs0') { i ->
                                        //minIO without SSL
                                        docker.image("${env.SERVICE_DOCKER_PULL_URL}/minio/minio:${env.MINIO_VERSION}").withRun("--user \$(id -u):\$(id -g) -p 127.0.0.1:9999:9000 -v ${pwd}/dataminio:/data -e \"MINIO_ACCESS_KEY=MKU4HW1K9HSST78MDY3T\" -e \"MINIO_SECRET_KEY=aSyBSStwp4JDZzpNKeJCc0Rdn12hOTa0EFejFfkd\"",'server /data') { l ->
                                            docker.image("${env.SERVICE_DOCKER_PULL_URL}/openio/sds:${env.OPENIO_VERSION}").withRun("-p 127.0.0.1:6007:6007 -e \"REGION=us-west-1\"") { e ->
                                                sh '$MVN_COMMAND -f pom.xml clean verify org.owasp:dependency-check-maven:aggregate sonar:sonar -Dsonar.branch=$GIT_BRANCH -Ddownloader.quick.query.timestamp=false'
                                            }
                                        }
                                    }
                                }
                            }
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
                        branch "PR*" // do not try to update on github status
                        tag pattern: "^[1-9]+\\.[0-9]+\\.[0-9]+-?[0-9]*\$", comparator: "REGEXP"
                    }
                }
            }
            environment {
                MVN_COMMAND = "${MVN_BASE} --show-version --batch-mode --errors --fail-never -DinstallAtEnd=true -DdeployAtEnd=true "
                LANG="fr_FR.UTF-8" // to bypass dateformat problem
            }
            steps {
                updateGitlabCommitStatus name: 'mergerequest', state: "running"
                dir('sources') {
                    script {
                        docker.withRegistry("http://${env.SERVICE_DOCKER_PULL_URL}") {
                            // minIO SSL first
                            docker.image("${env.SERVICE_DOCKER_PULL_URL}/minio/minio:${env.MINIO_VERSION}").withRun("--user \$(id -u):\$(id -g) -p 127.0.0.1:9000:9000 -v ${pwd}/dataminiossl:/data -v ${WORKSPACE}/sources/common/common-storage/src/test/resources/s3/tls/private.key:/root/.minio/certs/private.key -v ${WORKSPACE}/sources/common/common-storage/src/test/resources/s3/tls/public.crt:/root/.minio/certs/public.crt  -e \"MINIO_ACCESS_KEY=MKU4HW1K9HSST78MDY3T\" -e \"MINIO_SECRET_KEY=aSyBSStwp4JDZzpNKeJCc0Rdn12hOTa0EFejFfkd\"",'server /data') { o ->
                                docker.image("${env.SERVICE_DOCKER_PULL_URL}/elasticsearch:${env.ES_VERSION}").withRun('-p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" -e "cluster.name=elasticsearch-data"') { d ->
                                    sh 'while ! curl -v http://localhost:9200; do sleep 2; done'
                                    sh 'curl -X PUT http://localhost:9200/_template/default -H \'Content-Type: application/json\' -d \'{"index_patterns": ["*"],"order": -1,"settings": {"number_of_shards": "1","number_of_replicas": "0"}}\''
                                    docker.image("${env.SERVICE_DOCKER_PULL_URL}/mongo:${env.MONGO_VERSION}").withRun('-p 27017:27017 -v ${WORKSPACE}/vitam-conf-dev/tests/initdb.d/:/docker-entrypoint-initdb.d/ --health-cmd "test $$(echo "rs.status().ok" | mongo --quiet) -eq 1" --health-start-period 30s --health-interval 10s','mongod --bind_ip_all --replSet rs0') { i ->
                                        //minIO without SSL
                                        docker.image("${env.SERVICE_DOCKER_PULL_URL}/minio/minio:${env.MINIO_VERSION}").withRun("--user \$(id -u):\$(id -g) -p 127.0.0.1:9999:9000 -v ${pwd}/dataminio:/data -e \"MINIO_ACCESS_KEY=MKU4HW1K9HSST78MDY3T\" -e \"MINIO_SECRET_KEY=aSyBSStwp4JDZzpNKeJCc0Rdn12hOTa0EFejFfkd\"",'server /data') { l ->
                                            docker.image("${env.SERVICE_DOCKER_PULL_URL}/openio/sds:${env.OPENIO_VERSION}").withRun("-p 127.0.0.1:6007:6007 -e \"REGION=us-west-1\"") { e ->
                                                sh '$MVN_COMMAND -f pom.xml clean verify org.owasp:dependency-check-maven:aggregate sonar:sonar -Dsonar.branch=$GIT_BRANCH -Ddownloader.quick.query.timestamp=false'
                                            }
                                        }
                                    }
                                }
                            }
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
				    addGitLabMRComment comment: "pipeline-job : [analyse sonar](https://sonar.dev.programmevitam.fr/dashboard?id=fr.gouv.vitam%3Aparent%3A${gitlabSourceBranch}) de la branche"
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

        stage ("Execute unit and integration tests on pull requests") {
            when {
                branch "PR*" // do not try to update on github status
            }
            environment {
                MVN_COMMAND = "${MVN_BASE} --show-version --batch-mode --errors --fail-never -DinstallAtEnd=true -DdeployAtEnd=true "
                LANG="fr_FR.UTF-8" // to bypass dateformat problem
            }
            steps {
                // updateGitlabCommitStatus name: 'mergerequest', state: "running"
                // script {
                    githubNotify status: "PENDING", description: "Building & testing", credentialsId: "vitam-prg-token"
                // }
                dir('sources') {
                    script {
                        docker.withRegistry("http://${env.SERVICE_DOCKER_PULL_URL}") {
                            // minIO SSL first
                            docker.image("${env.SERVICE_DOCKER_PULL_URL}/minio/minio:${env.MINIO_VERSION}").withRun("--user \$(id -u):\$(id -g) -p 127.0.0.1:9000:9000 -v ${pwd}/dataminiossl:/data -v ${WORKSPACE}/sources/common/common-storage/src/test/resources/s3/tls:/root/.minio/certs  -e \"MINIO_ACCESS_KEY=MKU4HW1K9HSST78MDY3T\" -e \"MINIO_SECRET_KEY=aSyBSStwp4JDZzpNKeJCc0Rdn12hOTa0EFejFfkd\"",'server /data') { o ->
                                docker.image("${env.SERVICE_DOCKER_PULL_URL}/elasticsearch:${env.ES_VERSION}").withRun('-p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" -e "cluster.name=elasticsearch-data"') { d ->
                                    sh 'while ! curl -v http://localhost:9200; do sleep 2; done'
                                    sh 'curl -X PUT http://localhost:9200/_template/default -H \'Content-Type: application/json\' -d \'{"index_patterns": ["*"],"order": -1,"settings": {"number_of_shards": "1","number_of_replicas": "0"}}\''
                                    docker.image("${env.SERVICE_DOCKER_PULL_URL}/mongo:${env.MONGO_VERSION}").withRun('-p 27017:27017 -v ${WORKSPACE}/vitam-conf-dev/tests/initdb.d/:/docker-entrypoint-initdb.d/ --health-cmd "test $$(echo "rs.status().ok" | mongo --quiet) -eq 1" --health-start-period 30s --health-interval 10s','mongod --bind_ip_all --replSet rs0') { i ->
                                        //minIO without SSL
                                        docker.image("${env.SERVICE_DOCKER_PULL_URL}/minio/minio:${env.MINIO_VERSION}").withRun("--user \$(id -u):\$(id -g) -p 127.0.0.1:9999:9000 -v ${pwd}/dataminio:/data -e \"MINIO_ACCESS_KEY=MKU4HW1K9HSST78MDY3T\" -e \"MINIO_SECRET_KEY=aSyBSStwp4JDZzpNKeJCc0Rdn12hOTa0EFejFfkd\"",'server /data') { l ->
                                            docker.image("${env.SERVICE_DOCKER_PULL_URL}/openio/sds:${env.OPENIO_VERSION}").withRun("-p 127.0.0.1:6007:6007 -e \"REGION=us-west-1\"") { e ->
                                                sh '$MVN_COMMAND -f pom.xml clean verify org.owasp:dependency-check-maven:aggregate sonar:sonar -Dsonar.branch=$GIT_BRANCH -Ddownloader.quick.query.timestamp=false'
                                            }
                                        }
                                    }
                                }
                            }
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
                    githubNotify status: "SUCCESS", description: "Build successul", credentialsId: "vitam-prg-token"
                }
                failure {
                    githubNotify status: "FAILURE", description: "Build failed", credentialsId: "vitam-prg-token"
                }
                unstable {
                    githubNotify status: "FAILURE", description: "Build unstable", credentialsId: "vitam-prg-token"
                }
                aborted {
                    githubNotify status: "ERROR", description: "Build canceled", credentialsId: "vitam-prg-token"
                }
            }
        }


        stage("Build packages") {
            when {
                anyOf {
                    branch "develop*"
                    branch "master_*"
                    branch "master"
                    tag pattern: "^[1-9]+\\.[0-9]+\\.[0-9]+-?[0-9]*\$", comparator: "REGEXP"
                }
            }
            // when {
            //     environment(name: 'CHANGED_VITAM', value: 'true')
            // }
            environment {
                DEPLOY_GOAL = readFile("deploy_goal.txt")
            }
            steps {
                parallel(
                    "Package VITAM solution" : {
                        dir('sources') {
                            sh '$MVN_COMMAND -f pom.xml -Dmaven.test.skip=true -DskipTests=true clean javadoc:aggregate-jar $DEPLOY_GOAL jdeb:jdeb rpm:attached-rpm'
                            // -T 1C // Doesn't work with the javadoc:aggregate-jar goal
                        }
                    },
                    "Checkout publishing scripts" : {
                        checkout([$class: 'GitSCM',
                            branches: [[name: 'oshimae']],
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
                    tag pattern: "^[1-9]+\\.[0-9]+\\.[0-9]+-?[0-9]*\$", comparator: "REGEXP"
                }
            }
            // when {
            //     environment(name: 'CHANGED_VITAM', value: 'true')
            // }
            environment {
                DEPLOY_GOAL = readFile("deploy_goal.txt")
            }
            steps {
                dir('doc') {
                    sh '$MVN_COMMAND -f pom.xml -T 1C clean install jdeb:jdeb rpm:attached-rpm $DEPLOY_GOAL'
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
                    tag pattern: "^[1-9]+\\.[0-9]+\\.[0-9]+-?[0-9]*\$", comparator: "REGEXP"
                }
            }
            // when {
            //     environment(name: 'CHANGED_VITAM_PRODUCT', value: 'true')
            // }
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
                    tag pattern: "^[1-9]+\\.[0-9]+\\.[0-9]+-?[0-9]*\$", comparator: "REGEXP"
                }
            }
            // when {
            //     environment(name: 'CHANGED_VITAM_PRODUCT', value: 'true')
            // }
            environment {
                http_proxy = credentials("http-proxy-url")
                https_proxy = credentials("http-proxy-url")
            }
            steps {
                parallel(
                    "Build vitam-product rpm": {
                        dir('rpm/vitam-product') {
                            sh './build-all.sh'
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
                    tag pattern: "^[1-9]+\\.[0-9]+\\.[0-9]+-?[0-9]*\$", comparator: "REGEXP"
                }
            }
            // when {
            //     //environment(name: 'CHANGED_VITAM_PRODUCT', value: 'true')
            //     environment(name: 'MASTER_BRANCH', value: 'true')
            // }
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
                    tag pattern: "^[1-9]+\\.[0-9]+\\.[0-9]+-?[0-9]*\$", comparator: "REGEXP"
                }
            }
            steps {
                sshagent (credentials: ['jenkins_sftp_to_repository']) {
                    sh 'vitam-build.git/push_symlink_repo.sh commit $SERVICE_REPO_SSHURL'
                }
            }
        }
        stage("Checkmarx analysis") {
            when {
                anyOf {
                    branch "develop*"
                    branch "master_*"
                    branch "master"
                    tag pattern: "^[1-9]+[0-9]*\\.[0-9]+\\.[0-9]+(\\-[0-9]+){0,1}\$", comparator: "REGEXP"
                }
            }
            steps {
                sh 'mkdir -p ${PWD}/target'
                sh 'mkdir -p ${PWD}/logs'
                sh 'touch ${PWD}/logs/cx_console.log'
                // KWA : Visibly, backslash escape hell. \\ => \ in groovy string.
                sh '/opt/CxConsole/runCxConsole.sh scan --verbose -Log "${PWD}/logs/cx_console.log" -CxServer "$SERVICE_CHECKMARX_URL" -CxUser "VITAM openLDAP\\\\$CI_USR" -CxPassword \\"$CI_PSW\\" -ProjectName "CxServer\\SP\\Vitam\\Users\\vitam-parent $GIT_BRANCH" -LocationType folder -locationPath "${PWD}/sources"  -Preset "Default 2014" -LocationPathExclude test target bower_components node_modules dist -forcescan -ReportPDF "${PWD}/target/checkmarx-report.pdf"'
                sh '[ ! -f ${PWD}/target/checkmarx-report.pdf ] && touch ${PWD}/target/checkmarx-report.pdf'
            }
            post {
                success {
                    archiveArtifacts (
                        artifacts: '${PWD}/target/checkmarx-report.pdf',
                        fingerprint: true
                    )
                    slackSend (color: '#00aa5b', message: "Build OK de la branche ${env.GIT_BRANCH}, commit: ${env.GIT_COMMIT}", channel: "#pic-ci")
                }
                unstable {
                    slackSend (color: '#ffaa00', message: "Build Unstable de la branche ${env.GIT_BRANCH}, commit: ${env.GIT_COMMIT}", channel: "#pic-ci")
                }
                failure {
                    archiveArtifacts (
                        artifacts: '${PWD}/logs/cx_console.log',
                        fingerprint: true
                    )
                    slackSend (color: '#a30000', message: "Build KO de la branche ${env.GIT_BRANCH}, commit: ${env.GIT_COMMIT}", channel: "#pic-ci")
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
}
