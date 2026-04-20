// Jenkins Global Pipeline Library 설정 (한 번만):
// Manage Jenkins → Configure System → Global Pipeline Libraries
//   Name           : occount-jenkins
//   Default version: main
//   Retrieval      : Modern SCM → Git → <이 레포 URL>
//   Library root   : jenkins/shared-library

def call(Map cfg) {

    def SERVICES = [
        [path: 'gateway/',         task: ':gateway:api-gateway:bootJar',              name: 'api-gateway',  dir: 'gateway/api-gateway',              yamlKey: 'apiGateway'],
        [path: 'domains/member/',  task: ':domains:member:member-bootstrap:bootJar',  name: 'member-api',   dir: 'domains/member/member-bootstrap',   yamlKey: 'memberApi'],
        [path: 'domains/item/',    task: ':domains:item:item-bootstrap:bootJar',      name: 'item-api',     dir: 'domains/item/item-bootstrap',        yamlKey: 'itemApi'],
        [path: 'domains/order/',   task: ':domains:order:order-bootstrap:bootJar',    name: 'order-api',    dir: 'domains/order/order-bootstrap',      yamlKey: 'orderApi'],
        [path: 'domains/payment/', task: ':domains:payment:payment-bootstrap:bootJar',name: 'payment-api',  dir: 'domains/payment/payment-bootstrap',  yamlKey: 'paymentApi'],
    ]

    def TRIGGER_ALL_PATHS = [
        'build.gradle', 'build.gradle.kts',
        'settings.gradle', 'settings.gradle.kts',
        'gradle.properties', 'gradle/', 'buildSrc/',
    ]

    pipeline {
        agent {
            kubernetes {
                yamlFile 'jenkins/pod-template.yaml'
            }
        }

        options {
            timestamps()
            timeout(time: 30, unit: 'MINUTES')
        }

        parameters {
            booleanParam(name: 'FORCE_ALL', defaultValue: false, description: '전체 서비스 강제 빌드')
        }

        stages {

            stage('Init') {
                steps {
                    script {
                        env.IMAGE_PREFIX  = cfg.imagePrefix
                        env.TAG_PREFIX    = cfg.tagPrefix ?: ''
                        env.VALUES_FILE   = cfg.valuesFile
                        env.MANIFEST_REPO = cfg.manifestRepo
                        env.COMMIT_PREFIX = cfg.commitPrefix ?: 'chore'
                        env.GIT_COMMIT_SHORT = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                        echo "Commit: ${env.GIT_COMMIT_SHORT}"
                    }
                }
            }

            stage('Detect Changes') {
                steps {
                    script {
                        if (params.FORCE_ALL) {
                            env.CHANGED_SERVICES = SERVICES.collect { it.name }.join(',')
                            return
                        }

                        def changedFiles = sh(
                            script: 'git diff --name-only HEAD~1 HEAD 2>/dev/null || echo "__ALL__"',
                            returnStdout: true
                        ).trim()

                        if (changedFiles == '__ALL__') {
                            env.CHANGED_SERVICES = SERVICES.collect { it.name }.join(',')
                            return
                        }

                        def fileList = changedFiles.split('\n') as List
                        def triggerAll = TRIGGER_ALL_PATHS.any { path ->
                            fileList.any { f -> f.startsWith(path) || f == path }
                        }

                        if (triggerAll) {
                            env.CHANGED_SERVICES = SERVICES.collect { it.name }.join(',')
                            return
                        }

                        def changed = SERVICES.findAll { svc ->
                            fileList.any { f -> f.startsWith(svc.path) }
                        }.collect { it.name }

                        env.CHANGED_SERVICES = changed.isEmpty() ? '' : changed.join(',')
                        echo "빌드 대상: ${env.CHANGED_SERVICES ?: '없음'}"
                    }
                }
            }

            stage('Gradle Build') {
                when { expression { env.CHANGED_SERVICES } }
                steps {
                    container('gradle') {
                        script {
                            def changedList = env.CHANGED_SERVICES.split(',') as List
                            def targets = SERVICES
                                .findAll { changedList.contains(it.name) }
                                .collect { it.task }
                                .join(' ')
                            sh "find /home/gradle/.gradle/caches -name '*.lock' -delete 2>/dev/null || true"
                            sh "./gradlew ${targets} --no-daemon --parallel"
                        }
                    }
                }
            }

            stage('Build & Push Images') {
                when { expression { env.CHANGED_SERVICES } }
                steps {
                    script {
                        def changedList = env.CHANGED_SERVICES.split(',') as List
                        def svcsToBuild = SERVICES.findAll { changedList.contains(it.name) }
                        def imageTag = "${env.TAG_PREFIX}${env.GIT_COMMIT_SHORT}"

                        // fat jar + Dockerfile을 서비스 디렉토리로 복사
                        container('gradle') {
                            for (int i = 0; i < svcsToBuild.size(); i++) {
                                def svc = svcsToBuild[i]
                                def jarFile = sh(
                                    script: "ls ${env.WORKSPACE}/${svc.dir}/build/libs/*.jar 2>/dev/null | grep -v plain | tail -1",
                                    returnStdout: true
                                ).trim()
                                if (!jarFile) error "No JAR for ${svc.name}"
                                sh "cp ${jarFile} ${env.WORKSPACE}/${svc.dir}/build/app.jar"
                                sh "cp ${env.WORKSPACE}/jenkins/Dockerfile.service ${env.WORKSPACE}/${svc.dir}/Dockerfile"
                            }
                        }

                        // 각 서비스 Dockerfile 기반으로 병렬 빌드
                        def parallelStages = [:]
                        for (int i = 0; i < svcsToBuild.size(); i++) {
                            def svc = svcsToBuild[i]
                            parallelStages["${svc.name}"] = {
                                container('kaniko') {
                                    sh """
                                        /kaniko/executor \\
                                            --context=dir://${env.WORKSPACE}/${svc.dir} \\
                                            --dockerfile=${env.WORKSPACE}/${svc.dir}/Dockerfile \\
                                            --destination=${env.IMAGE_PREFIX}/${svc.name}:${imageTag} \\
                                            --build-arg BASE_IMAGE=${env.HARBOR_URL}/base/eclipse-temurin:21-jre-alpine \\
                                            --snapshot-mode=redo \\
                                            --skip-tls-verify \\
                                            --skip-tls-verify-pull
                                    """
                                }
                            }
                        }
                        parallel parallelStages
                    }
                }
            }

            stage('Update Manifest') {
                when { expression { env.CHANGED_SERVICES } }
                steps {
                    container('git') {
                        script {
                            def changedList = env.CHANGED_SERVICES.split(',') as List
                            def imageTag = "${env.TAG_PREFIX}${env.GIT_COMMIT_SHORT}"
                            def manifestDir = "${env.WORKSPACE}/manifest-update"
                            sshagent(['github-credentials']) {
                                sh """
                                    mkdir -p ~/.ssh
                                    ssh-keyscan github.com >> ~/.ssh/known_hosts 2>/dev/null
                                    git clone ${env.MANIFEST_REPO} ${manifestDir}
                                    chmod -R a+rw ${manifestDir}
                                """
                                def valuesPath = "${manifestDir}/helm/occount/${env.VALUES_FILE}"
                                def values = readYaml file: valuesPath
                                SERVICES
                                    .findAll { changedList.contains(it.name) }
                                    .each { svc -> values.apps[svc.yamlKey].image.tag = imageTag }
                                writeYaml file: valuesPath, data: values, overwrite: true
                                sh """
                                    cd ${manifestDir}
                                    git config user.email "jenkins@devcoop.local"
                                    git config user.name "Jenkins CI"
                                    git add helm/occount/${env.VALUES_FILE}
                                    git diff --cached --quiet || git commit -m "${env.COMMIT_PREFIX}: bump [${env.CHANGED_SERVICES}] to ${imageTag}"
                                    git push ${env.MANIFEST_REPO} main
                                """
                            }
                        }
                    }
                }
            }
        }

        post {
            always { cleanWs() }
            success { echo "Done: [${env.CHANGED_SERVICES ?: 'no changes'}] → ${env.TAG_PREFIX}${env.GIT_COMMIT_SHORT}" }
            failure { echo "Failed: ${env.GIT_COMMIT_SHORT}" }
        }
    }
}
