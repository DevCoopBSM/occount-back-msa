def SERVICES = [
    [paths: ['gateway/', 'core/', 'modules/'],          task: ':gateway:api-gateway:build',           name: 'api-gateway',  dir: 'gateway/api-gateway'],
    [paths: ['domains/member/', 'core/', 'modules/'],   task: ':domains:member:member-api:build',      name: 'member-api',   dir: 'domains/member/member-api'],
    [paths: ['domains/product/', 'core/', 'modules/'],  task: ':domains:product:product-api:build',    name: 'product-api',  dir: 'domains/product/product-api'],
    [paths: ['domains/order/', 'core/', 'modules/'],    task: ':domains:order:order-api:build',        name: 'order-api',    dir: 'domains/order/order-api'],
    [paths: ['domains/payment/', 'core/', 'modules/'],  task: ':domains:payment:payment-api:build',    name: 'payment-api',  dir: 'domains/payment/payment-api'],
    [paths: ['domains/point/', 'core/', 'modules/'],    task: ':domains:point:point-api:build',        name: 'point-api',    dir: 'domains/point/point-api'],
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

                    // HEAD~1이 없는 경우(첫 빌드, shallow clone) 전체 파일 목록으로 fallback
                    def changedFiles = sh(
                        script: '''
                            if git rev-parse HEAD~1 >/dev/null 2>&1; then
                                git diff --name-only HEAD~1 HEAD
                            else
                                git ls-files
                            fi
                        ''',
                        returnStdout: true
                    ).trim().split('\n') as List

                    def tasks   = [] as Set
                    def targets = [] as Set

                    SERVICES.each { svc ->
                        def hit = svc.paths.any { p -> changedFiles.any { f -> f.startsWith(p) } }
                        if (hit) {
                            tasks   << svc.task
                            targets << svc.name
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
