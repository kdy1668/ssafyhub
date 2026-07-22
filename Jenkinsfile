pipeline {
    agent any

    stages {
        // 1. 코드 체크아웃 (GitLab에서 소스 가져오기)
        stage('Checkout') {
            steps {
                echo 'GitLab에서 최신 코드를 가져오는 중입니다...'
            }
        }

        // 2. 빌드 단계 (Gradle 기준)
        stage('Build') {
            steps {
                echo 'Gradle 빌드를 시작합니다...'
                sh 'chmod +x ./gradlew'
                // 테스트를 제외하고 빌드하려면 -x test 유지 (필요시 제거 가능)
                sh './gradlew clean build -x test'
            }
        }

        // 3. 배포 단계 (도커 기반 자동 배포)
        stage('Deploy') {
            steps {
                echo '도커 이미지를 빌드하고 서버에 배포합니다...'

                // 1) 최신 코드를 기반으로 새 도커 이미지 생성
                sh 'docker build -t ssafyhub-backend .'

                // 2) 기존에 돌고 있던 컨테이너가 있다면 안전하게 중지 및 삭제
                sh 'docker stop ssafyhub-backend-container || true'
                sh 'docker rm ssafyhub-backend-container || true'

                // 3) 새로 만든 이미지로 백그라운드 컨테이너 실행 (포트 8080 매핑)
                sh 'docker run -d --name ssafyhub-backend-container -p 8080:8080 ssafyhub-backend'
            }
        }
    }

    // 4. 빌드 결과 후처리 (매터모스트 알림 연동)
    post {
        success {
            script {
                mattermostSend color: 'good',
                message: "✅ 성공: ${env.JOB_NAME} #${env.BUILD_NUMBER} 빌드가 성공적으로 끝났습니다! (<${env.BUILD_URL}|빌드 결과 보기>)"
            }
        }
        failure {
            script {
                mattermostSend color: 'danger',
                message: "❌ 실패: ${env.JOB_NAME} #${env.BUILD_NUMBER} 빌드가 실패했습니다. 로그를 확인해주세요! (<${env.BUILD_URL}|빌드 결과 보기>)"
            }
        }
    }
}