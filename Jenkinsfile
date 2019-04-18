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
        label 'boost'
    }

    environment {
        MVN_BASE = "/usr/local/maven/bin/mvn --settings ${pwd()}/.ci/settings.xml"
        MVN_COMMAND = "${MVN_BASE} --show-version --batch-mode --errors --fail-at-end -DinstallAtEnd=true -DdeployAtEnd=true "
        DEPLOY_GOAL = " " // Deploy goal used by maven ; typically "deploy" for master* branches & "" (nothing) for everything else (we don't deploy) ; keep a space so can work in other branches than develop
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

        stage ("Execute unit and integration tests") {
         // when {
        //     //     environment(name: 'CHANGED_VITAM', value: 'true')
        //     // }
            steps {
                dir('sources') {
                    script {
                        docker.withRegistry("http://${env.SERVICE_DOCKER_PULL_URL}") {
                            docker.image("${env.SERVICE_DOCKER_PULL_URL}/elasticsearch/elasticsearch:6.7.1").withRun('-p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" -e "cluster.name=elasticsearch-data"') { c ->
                                docker.withRegistry("http://${env.SERVICE_DOCKER_PULL_URL}") {
                                    docker.image("${env.SERVICE_DOCKER_PULL_URL}/mongo:4.0.6").withRun('-p 27017:27017') { o ->
                                        withEnv(["JAVA_TOOL_OPTIONS=-Dhttp.proxyHost=${env.SERVICE_PROXY_HOST} -Dhttp.proxyPort=${env.SERVICE_PROXY_PORT} -Dhttps.proxyHost=${env.SERVICE_PROXY_HOST} -Dhttps.proxyPort=${env.SERVICE_PROXY_PORT} -Dhttp.nonProxyHosts=${env.SERVICE_NOPROXY}"]) {
                                            sh 'while ! curl -v http://localhost:9200; do sleep 2; done'
                                            sh 'curl -X PUT http://localhost:9200/_template/default -H \'Content-Type: application/json\' -d \'{"index_patterns": ["*"],"order": -1,"settings": {"number_of_shards": "1","number_of_replicas": "0"}}\''
                                            sh '$MVN_COMMAND -f pom.xml clean verify org.owasp:dependency-check-maven:aggregate sonar:sonar -Dsonar.branch=$GIT_BRANCH -Ddownloader.quick.query.timestamp=false'
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
                        artifacts: '**/dependency-check-report.html',
                        fingerprint: true
                    )
                }
            }
        }

        stage("Build packages") {
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
                            sh '$MVN_COMMAND -f pom.xml -Dmaven.test.skip=true -DskipTests=true clean package javadoc:aggregate-jar rpm:attached-rpm jdeb:jdeb $DEPLOY_GOAL'
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
            // when {
            //     environment(name: 'CHANGED_VITAM', value: 'true')
            // }
            environment {
                DEPLOY_GOAL = readFile("deploy_goal.txt")
            }
            steps {
                dir('doc') {
                    sh '$MVN_COMMAND -f pom.xml -T 1C clean package rpm:attached-rpm jdeb:jdeb $DEPLOY_GOAL'
                }
            }
            post {
                always {
                    junit 'doc/**/target/surefire-reports/*.xml'
                }
            }
        }

        stage("Prepare packages building") {
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
        }
        stage("Update symlink") {
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
                }
            }
            steps {
                sh 'mkdir -p target'
                sh 'mkdir -p logs'
                // KWA : Visibly, backslash escape hell. \\ => \ in groovy string.
                withEnv(["JAVA_TOOL_OPTIONS=-Dhttp.proxyHost=${env.SERVICE_PROXY_HOST} -Dhttp.proxyPort=${env.SERVICE_PROXY_PORT} -Dhttps.proxyHost=${env.SERVICE_PROXY_HOST} -Dhttps.proxyPort=${env.SERVICE_PROXY_PORT}"]) {
                    sh '/opt/CxConsole/runCxConsole.sh scan --verbose -Log "${PWD}/logs/cxconsole.log" -CxServer "$SERVICE_CHECKMARX_URL" -CxUser "VITAM openLDAP\\\\$CI_USR" -CxPassword \\"$CI_PSW\\" -ProjectName "CxServer\\SP\\Vitam\\Users\\vitam-parent $GIT_BRANCH" -LocationType folder -locationPath "${PWD}/sources"  -Preset "Default 2014" -LocationPathExclude test target bower_components node_modules dist -ReportPDF "${PWD}/target/checkmarx-report.pdf"'
                }
            }
            post {
                success {
                    archiveArtifacts (
                        artifacts: 'target/checkmarx-report.pdf',
                        fingerprint: true
                    )
                    slackSend (color: '#00aa5b', message: "Build OK de la branche ${env.GIT_BRANCH}, commit: ${env.GIT_COMMIT}", channel: "#pic-ci")
                }
                unstable {
                    slackSend (color: '#ffaa00', message: "Build Unstable de la branche ${env.GIT_BRANCH}, commit: ${env.GIT_COMMIT}", channel: "#pic-ci")
                }
                failure {
                    archiveArtifacts (
                        artifacts: 'logs/cxconsole.log',
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
