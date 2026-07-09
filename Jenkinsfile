pipeline {
    agent any

    environment {
        IMAGE_NAMESPACE = "deploy-history"
        DEPLOY_REPO_NAME = "deploy-history-manifest"
    }

    options {
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Set Image Variables') {
            steps {
                script {
                    env.IMAGE_TAG = sh(
                        script: "git rev-parse --short HEAD",
                        returnStdout: true
                    ).trim()

                    env.BACKEND_IMAGE = "${env.NEXUS_REGISTRY}/${env.IMAGE_NAMESPACE}/backend"
                    env.FRONTEND_IMAGE = "${env.NEXUS_REGISTRY}/${env.IMAGE_NAMESPACE}/frontend"
                }
            }
        }

        stage('Backend Build/Test') {
            steps {
                dir('backend') {
                    sh '''
                        set -e
                        chmod +x ./gradlew
                        ./gradlew clean build
                    '''
                }
            }
        }

        stage('Frontend Build/Test') {
            steps {
                dir('frontend') {
                    sh '''
                        set -e
                        npm install
                        npm run build
                    '''
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh '''
                    set -e
                    docker build -t ${BACKEND_IMAGE}:${IMAGE_TAG} ./backend
                    docker build -t ${FRONTEND_IMAGE}:${IMAGE_TAG} ./frontend
                '''
            }
        }

        stage('Docker Push to Nexus') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'nexus-credentials',
                    usernameVariable: 'NEXUS_USERNAME',
                    passwordVariable: 'NEXUS_PASSWORD'
                )]) {
                    sh '''
                        set -e
                        echo "$NEXUS_PASSWORD" | docker login $NEXUS_REGISTRY -u "$NEXUS_USERNAME" --password-stdin
                        docker push ${BACKEND_IMAGE}:${IMAGE_TAG}
                        docker push ${FRONTEND_IMAGE}:${IMAGE_TAG}
                    '''
                }
            }
        }

        stage('Create GitHub App Installation Token') {
            steps {
                withCredentials([file(
                    credentialsId: 'github-app-private-key',
                    variable: 'GITHUB_APP_PRIVATE_KEY_FILE'
                )]) {
                    sh '''
                        set -e

                        now=$(date +%s)
                        iat=$((now - 60))
                        exp=$((now + 540))

                        header='{"alg":"RS256","typ":"JWT"}'
                        payload="{\\"iat\\":${iat},\\"exp\\":${exp},\\"iss\\":\\"${GITHUB_APP_ID}\\"}"

                        b64enc() {
                          openssl base64 -A | tr '+/' '-_' | tr -d '='
                        }

                        header_b64=$(printf "%s" "$header" | b64enc)
                        payload_b64=$(printf "%s" "$payload" | b64enc)

                        unsigned_token="${header_b64}.${payload_b64}"

                        signature=$(printf "%s" "$unsigned_token" \
                          | openssl dgst -sha256 -sign "$GITHUB_APP_PRIVATE_KEY_FILE" \
                          | b64enc)

                        jwt="${unsigned_token}.${signature}"

                        INSTALLATION_TOKEN=$(curl -s -X POST \
                          -H "Authorization: Bearer ${jwt}" \
                          -H "Accept: application/vnd.github+json" \
                          https://api.github.com/app/installations/${GITHUB_INSTALLATION_ID}/access_tokens \
                          | jq -r '.token')

                        if [ -z "$INSTALLATION_TOKEN" ] || [ "$INSTALLATION_TOKEN" = "null" ]; then
                          echo "Failed to create GitHub App installation token"
                          exit 1
                        fi

                        echo "$INSTALLATION_TOKEN" > .github_app_installation_token
                    '''
                }
            }
        }

        stage('Update Helm values') {
            steps {
                sh '''
                    set -e

                    INSTALLATION_TOKEN=$(cat .github_app_installation_token)

                    rm -rf deploy-repo

                    set +x
                    git clone https://x-access-token:${INSTALLATION_TOKEN}@github.com/${GITHUB_OWNER}/${DEPLOY_REPO_NAME}.git deploy-repo
                    set -x

                    cd deploy-repo

                    sed -i "s|backendTag: .*|backendTag: ${IMAGE_TAG}|g" helm/values.yaml
                    sed -i "s|frontendTag: .*|frontendTag: ${IMAGE_TAG}|g" helm/values.yaml

                    git config user.name "jenkins"
                    git config user.email "jenkins@example.com"

                    git add helm/values.yaml

                    if git diff --cached --quiet; then
                        echo "No changes to commit"
                    else
                        git commit -m "Update image tag to ${IMAGE_TAG}"
                        git push origin main
                    fi
                '''
            }
        }
    }

    post {
        always {
            sh '''
                rm -f .github_app_installation_token
                rm -rf deploy-repo
                docker image prune -f
            '''
        }
    }
}