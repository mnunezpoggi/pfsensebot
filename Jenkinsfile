
pipeline {
    agent { label 'captive-portal' }

    stages {
        stage('Pull') {
            steps {
                ircNotify notifyOnStart:true
                checkout scm
            }
        }
        stage('Build') {
            steps {
                sh 'docker build -t pfsensebot:main .'
            }
        }
        stage('Migrate Database'){
            steps {
                script { try { sh 'docker container stop pfsensebot' } catch (Exception e) { } }
                script { try { sh 'docker container rm pfsensebot' } catch (Exception e) { } }
                
            }
        }
        stage('Deploy') {
            steps {
                  sh "docker run --name=pfsensebot -d --restart=always pfsensebot:main"
            }
        }

    }
    post {
        always {
            ircNotify notificationStrategy:'all'
        }
    }
}
