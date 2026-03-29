def SERVICES = [
    [path: 'gateway/',         task: ':gateway:api-gateway:bootJar',                  name: 'api-gateway',  dir: 'gateway/api-gateway'],
    [path: 'domains/member/',  task: ':domains:member:member-bootstrap:bootJar',       name: 'member-api',   dir: 'domains/member/member-bootstrap'],
    [path: 'domains/item/',    task: ':domains:item:item-bootstrap:bootJar',           name: 'item-api',     dir: 'domains/item/item-bootstrap'],
    [path: 'domains/order/',   task: ':domains:order:order-bootstrap:bootJar',         name: 'order-api',    dir: 'domains/order/order-bootstrap'],
    [path: 'domains/payment/', task: ':domains:payment:payment-bootstrap:bootJar',     name: 'payment-api',  dir: 'domains/payment/payment-bootstrap'],
    [path: 'domains/point/',   task: ':domains:point:point-bootstrap:bootJar',         name: 'point-api',    dir: 'domains/point/point-bootstrap'],
]

pipeline {
    agent {
        kubernetes {
            yaml """
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: gradle
    image: gradle:8.5-jdk21
    command: ['sleep']
    args: ['infinity']
    volumeMounts:
    - name: gradle-cache
      mountPath: /home/gradle/.gradle
  - name: kaniko
    image: gcr.io/kaniko-project/executor:debug
    command: ['sleep']
    args: ['infinity']
    volumeMounts:
    - name: harbor-creds
      mountPath: /kaniko/.docker
  - name: jnlp
    image: jenkins/inbound-agent:latest
  volumes:
  - name: harbor-creds
    secret:
      secretName: harbor-registry-secret
      items:
      - key: .dockerconfigjson
        path: config.json
  - name: gradle-cache
    emptyDir: {}
"""
        }
    }

    environment {
        HARBOR_URL   = 'harbor.harbor.svc'
        IMAGE_PREFIX = "${HARBOR_URL}/occount"
    }

    stages {

        // ---------------------------------------------------------------
        // 1. 변경 감지 — 변경된 서비스의 Gradle 태스크 목록 수집
        // ---------------------------------------------------------------
        stage('Detect Changes') {
            steps {
                script {
                    env.GIT_COMMIT_SHORT = sh(
                        script: 'git rev-parse --short HEAD',
                        returnStdout: true
                    ).trim()

                    def tasks   = [] as Set
                    def targets = [] as Set

                    def changedFiles = sh(
                        script: '''
                            if [ -n "$GIT_PREVIOUS_SUCCESSFUL_COMMIT" ]; then
                                git diff --name-only $GIT_PREVIOUS_SUCCESSFUL_COMMIT $GIT_COMMIT
                            else
                                git diff --name-only HEAD~1 HEAD
                            fi
                        ''',
                        returnStdout: true
                    ).trim().split('\n') as List

                    if (changedFiles.any { it.startsWith('core/') || it.startsWith('modules/') }) {
                        echo "Common module changed → building all services"
                        SERVICES.each { svc -> tasks << svc.task; targets << svc.name }
                    } else {
                        SERVICES.each { svc ->
                            if (changedFiles.any { f -> f.startsWith(svc.path) }) {
                                tasks   << svc.task
                                targets << svc.name
                            }
                        }
                        if (targets.isEmpty()) {
                            echo "No service-related changes detected → skipping build"
                        }
                    }

                    env.GRADLE_TASKS  = tasks.join(' ')
                    env.BUILD_TARGETS = targets.join(',')
                    echo "Changed services : ${targets}"
                    echo "Gradle tasks     : ${env.GRADLE_TASKS}"
                }
            }
        }

        // ---------------------------------------------------------------
        // 2. 변경된 모듈만 Gradle 빌드 (jar 생성)
        //    buildNeeded → 해당 모듈이 의존하는 하위 프로젝트까지 자동 빌드
        // ---------------------------------------------------------------
        stage('Gradle Build') {
            when { expression { env.GRADLE_TASKS?.trim() } }
            steps {
                container('gradle') {
                    sh "./gradlew ${env.GRADLE_TASKS} -x test --parallel --continue"
                }
            }
        }

        // ---------------------------------------------------------------
        // 3. Kaniko 이미지 빌드 & Harbor push — 동적 병렬 스테이지
        // ---------------------------------------------------------------
        stage('Build & Push Images') {
            when { expression { env.BUILD_TARGETS?.trim() } }
            steps {
                script {
                    def targets        = env.BUILD_TARGETS.split(',') as List
                    def parallelStages = [:]

                    SERVICES.each { svc ->
                        if (targets.contains(svc.name)) {
                            def s = svc  // 클로저 캡처용 로컬 복사
                            parallelStages[s.name] = {
                                container('kaniko') {
                                    buildAndPush(s.name, s.dir)
                                }
                            }
                        }
                    }

                    parallel parallelStages
                }
            }
        }
    }

    post {
        always {
            node('') {
                cleanWs()
            }
        }
        success { echo "Build succeeded: ${env.GIT_COMMIT_SHORT}" }
        failure { echo "Build failed: ${env.GIT_COMMIT_SHORT}" }
    }
}

def buildAndPush(String serviceName, String moduleDir) {
    def image = "${env.IMAGE_PREFIX}/${serviceName}:${env.GIT_COMMIT_SHORT}"
    sh """
        /kaniko/executor \\
            --context=dir://${env.WORKSPACE}/${moduleDir} \\
            --dockerfile=${env.WORKSPACE}/${moduleDir}/Dockerfile \\
            --destination=${image} \\
            --cache=true \\
            --cache-repo=${env.HARBOR_URL}/occount/cache \\
            --snapshotMode=redo \\
            --use-new-run \\
            --skip-tls-verify \\
            --insecure \\
            --insecure-pull
    """
    echo "Pushed: ${image}"
}
