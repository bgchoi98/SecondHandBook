pipeline {
    agent any

    environment {
        DEPLOY_HOST = '192.168.219.101'
        DEPLOY_TMP_WAR = '/tmp/ROOT.war'
        JAVA17_HOME = '/usr/lib/jvm/java-17-openjdk-amd64'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build WAR') {
            steps {
                sh '''
                    export JAVA_HOME=$JAVA17_HOME
                    export PATH=$JAVA_HOME/bin:$PATH

                    java -version
                    mvn -version
                    mvn clean package -DskipTests
                '''
            }
        }

        stage('Deploy') {
            steps {
                withCredentials([
                    sshUserPrivateKey(
                        credentialsId: 'deploy-ssh-key',
                        keyFileVariable: 'SSH_KEY',
                        usernameVariable: 'DEPLOY_USER'
                    )
                ]) {
                    sh '''
                        WAR_FILE=$(ls target/*.war | head -n 1)

                        scp -i "$SSH_KEY" -o StrictHostKeyChecking=no "$WAR_FILE" ${DEPLOY_USER}@${DEPLOY_HOST}:${DEPLOY_TMP_WAR}

                        ssh -i "$SSH_KEY" -o StrictHostKeyChecking=no ${DEPLOY_USER}@${DEPLOY_HOST} "
                            sudo systemctl stop tomcat &&
                            sudo rm -rf /opt/tomcat/webapps/ROOT &&
                            sudo rm -f /opt/tomcat/webapps/ROOT.war &&
                            sudo mv /tmp/ROOT.war /opt/tomcat/webapps/ROOT.war &&
                            sudo chown tomcat:tomcat /opt/tomcat/webapps/ROOT.war &&
                            sudo systemctl start tomcat
                        "
                    '''
                }
            }
        }
    }

    post {
        success {
            echo 'SecondHandBook 배포 성공'
        }
        failure {
            echo 'SecondHandBook 배포 실패'
        }
    }
}