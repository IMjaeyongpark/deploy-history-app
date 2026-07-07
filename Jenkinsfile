pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    environment {
        IMAGE_NAMESPACE = "deploy-history"
        BACKEND_IMAGE = "${NEXUS_REGISTRY}/${IMAGE_NAMESPACE}/backend"
        FRONTEND_IMAGE = "${NEXUS_REGISTRY}/${IMAGE_NAMESPACE}/frontend"
        DEPLOY_REPO = "https://github.com/IMjaeyongpark/deploy-history-manifest.git"
        DEPLOY_REPO_BRANCH = "main"
        DEPLOY_REPO_CREDENTIALS_ID = "deploy-history-manifest-pat"
        NEXUS_CREDENTIALS_ID = "nexus-credentials"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Prepare Image Tag') {
            steps {
                script {
                    if (!env.NEXUS_REGISTRY?.trim()) {
                        error('NEXUS_REGISTRY Jenkins environment variable is required.')
                    }

                    def branch = (env.BRANCH_NAME ?: 'local')
                        .toLowerCase()
                        .replaceAll(/[^a-z0-9_.-]+/, '-')
                        .replaceAll(/^-+|-+$/, '')
                    def shortCommit = sh(script: 'git rev-parse --short=12 HEAD', returnStdout: true).trim()

                    env.IMAGE_TAG = "${branch}-${env.BUILD_NUMBER}-${shortCommit}"
                }
            }
        }

        stage('Backend Build and Test') {
            steps {
                dir('backend') {
                    sh '''
                        set -eu
                        chmod +x ./gradlew
                        ./gradlew clean test bootJar --no-daemon
                    '''
                }
            }
        }

        stage('Frontend Build and Test') {
            steps {
                dir('frontend') {
                    sh '''
                        set -eu
                        npm ci
                        npm run test
                        npm run build
                    '''
                }
            }
        }

        stage('Docker Image Build') {
            steps {
                sh '''
                    set -eu
                    docker build -t "$BACKEND_IMAGE:$IMAGE_TAG" ./backend
                    docker build -t "$FRONTEND_IMAGE:$IMAGE_TAG" ./frontend
                '''
            }
        }

        stage('Nexus Push') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: env.NEXUS_CREDENTIALS_ID,
                        usernameVariable: 'NEXUS_USERNAME',
                        passwordVariable: 'NEXUS_PASSWORD'
                    )
                ]) {
                    sh '''
                        set -eu
                        printf '%s' "$NEXUS_PASSWORD" | docker login "$NEXUS_REGISTRY" --username "$NEXUS_USERNAME" --password-stdin
                        docker push "$BACKEND_IMAGE:$IMAGE_TAG"
                        docker push "$FRONTEND_IMAGE:$IMAGE_TAG"
                    '''
                }
            }
        }

        stage('Update Manifest') {
            steps {
                withCredentials([
                    string(credentialsId: env.DEPLOY_REPO_CREDENTIALS_ID, variable: 'GITHUB_TOKEN')
                ]) {
                    sh '''
                        set -eu
                        rm -rf deploy-history-manifest

                        cat > .git-askpass <<'EOF'
#!/bin/sh
case "$1" in
  Username*) echo "x-access-token" ;;
  Password*) echo "$GITHUB_TOKEN" ;;
esac
EOF
                        chmod 700 .git-askpass
                        trap 'rm -f .git-askpass' EXIT

                        GIT_ASKPASS="$PWD/.git-askpass" GIT_TERMINAL_PROMPT=0 \
                            git clone --branch "$DEPLOY_REPO_BRANCH" "$DEPLOY_REPO" deploy-history-manifest

                        cd deploy-history-manifest
                        sed -i "s|backendTag: .*|backendTag: ${IMAGE_TAG}|g" helm/values.yaml
                        sed -i "s|frontendTag: .*|frontendTag: ${IMAGE_TAG}|g" helm/values.yaml

                        git config user.name "jenkins"
                        git config user.email "jenkins@local"
                        git add helm/values.yaml

                        if git diff --cached --quiet; then
                            echo "No manifest changes to commit."
                        else
                            git commit -m "Update deploy-history image tag ${IMAGE_TAG}"
                            GIT_ASKPASS="$OLDPWD/.git-askpass" GIT_TERMINAL_PROMPT=0 \
                                git push origin "HEAD:${DEPLOY_REPO_BRANCH}"
                        fi
                    '''
                }
            }
        }
    }

    post {
        always {
            sh 'docker logout "$NEXUS_REGISTRY" >/dev/null 2>&1 || true'
        }
    }
}
