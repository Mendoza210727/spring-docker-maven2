pipeline {
    agent any

    environment {
        // --- VARIABLES GENERALES ---
        DOJO_URL = "http://192.168.100.242:8080" 
        PRODUCT_NAME = "spring-docker-maven2" 
        ENGAGEMENT_NAME = "V2 - Segura"
        DOCKER_HOST = "tcp://host.docker.internal:2375"
    }

    stages {
        stage('1. Checkout del Código') {
            steps {
                checkout scm
                // Creamos las carpetas para todos los reportes, incluyendo ZAP (dast)
                sh 'mkdir -p reports/sast reports/sca reports/dast'
            }
        }

        stage('1.5. Build & Clean (Maven)') {
            steps {
                echo 'Limpiando caché vieja y compilando proyecto...'
                sh '''
                    # Construimos el proyecto en el contenedor
                    docker create --name maven-build -w /src maven:3.9-eclipse-temurin-17 mvn clean package -DskipTests
                    docker cp . maven-build:/src
                    docker start -a maven-build
                    
                    # 🧹 EL TRUCO: Borramos el target viejo y corrupto de Jenkins
                    rm -rf ./target
                    
                    # Extraemos el target nuevo y perfecto
                    docker cp maven-build:/src/target ./target || true
                    docker rm maven-build
                '''
            }
        }

        /* stage('2. SAST (Semgrep)') {
            steps {
                echo 'Ejecutando análisis de código fuente (SAST)...'
                sh '''
                    docker create --name semgrep-scan -w /src returntocorp/semgrep semgrep scan --config=p/ci --json -o /src/semgrep-report.json /src
                    docker cp . semgrep-scan:/src
                    docker start -a semgrep-scan || true
                    docker cp semgrep-scan:/src/semgrep-report.json reports/sast/semgrep-report.json || true
                    docker rm semgrep-scan
                '''
            }
        } */

       /*  stage('3. SCA (Trivy)') {
            steps {
                echo 'Escaneando vulnerabilidades en dependencias...'
                sh '''
                    docker create --name trivy-scan -w /src aquasec/trivy fs --format json --output /src/trivy-sca-report.json /src
                    docker cp . trivy-scan:/src
                    docker start -a trivy-scan || true
                    docker cp trivy-scan:/src/trivy-sca-report.json reports/sca/trivy-sca-report.json || true
                    docker rm trivy-scan
                '''
            }
        } */

        stage('4. Despliegue SEGURO') {
            steps {
                echo 'Desplegando la aplicación en Ubuntu Server...'
                sshagent(['mendoza-server-ssh']) {
                    sh '''
                        # Enviamos el JAR al servidor
                        scp -o StrictHostKeyChecking=no target/*.jar mendoza@192.168.100.242:~/deploy/seguro/app.jar
                        
                        # Matamos la app vieja y arrancamos la nueva en segundo plano
                        ssh -o StrictHostKeyChecking=no mendoza@192.168.100.242 "
                            fuser -k 8082/tcp || true
                            nohup java -jar ~/deploy/seguro/app.jar --server.port=8082 > ~/deploy/seguro/log.txt 2>&1 &
                            sleep 15 # Damos 15 segundos para que la app termine de arrancar antes de atacarla
                        "
                    '''
                }
            }
        }

        stage('5. DAST (ZAP)') {
            steps {
                echo 'Iniciando ataque dinámico con OWASP ZAP...'
                sh '''
                    # 1. Creamos un volumen temporal de Docker
                    docker volume create zap-temp-vol || true
                    
                    # 2. Corremos ZAP como ROOT (-u root) mapeando el volumen (-v) 
                    docker run --name zap-scan -u root -v zap-temp-vol:/zap/wrk -t ghcr.io/zaproxy/zaproxy:stable zap-baseline.py -t http://192.168.100.242/segura/ -J zap-report.json || true
                    
                    # 3. Extraemos el reporte desde el volumen hacia Jenkins
                    docker cp zap-scan:/zap/wrk/zap-report.json reports/dast/zap-report.json || true
                    
                    # 4. Limpiamos la basura
                    docker rm -f zap-scan || true
                    docker volume rm zap-temp-vol || true
                '''
            }
        }
    }

    post {
        always {
            echo ' Enviando TODOS los reportes JSON a DefectDojo...'
            
            withCredentials([string(credentialsId: 'defectdojo-token', variable: 'DD_TOKEN')]) {
                
                // 1. Envío SAST (Semgrep)
                sh '''
                    curl -s -X POST "${DOJO_URL}/api/v2/import-scan/" \
                    -H "Authorization: Token ${DD_TOKEN}" \
                    -F "scan_type=Semgrep JSON Report" \
                    -F "file=@reports/sast/semgrep-report.json" \
                    -F "product_name=${PRODUCT_NAME}" \
                    -F "engagement_name=${ENGAGEMENT_NAME}" > /dev/null
                '''
                
                // 2. Envío SCA (Trivy)
                sh '''
                    curl -s -X POST "${DOJO_URL}/api/v2/import-scan/" \
                    -H "Authorization: Token ${DD_TOKEN}" \
                    -F "scan_type=Trivy Scan" \
                    -F "file=@reports/sca/trivy-sca-report.json" \
                    -F "product_name=${PRODUCT_NAME}" \
                    -F "engagement_name=${ENGAGEMENT_NAME}" > /dev/null
                '''

                // 3. ¡NUEVO! Envío DAST (ZAP)
                sh '''
                    # Verificamos que el reporte exista antes de enviarlo
                    if [ -f "reports/dast/zap-report.json" ]; then
                        curl -s -X POST "${DOJO_URL}/api/v2/import-scan/" \
                        -H "Authorization: Token ${DD_TOKEN}" \
                        -F "scan_type=ZAP Scan" \
                        -F "file=@reports/dast/zap-report.json" \
                        -F "product_name=${PRODUCT_NAME}" \
                        -F "engagement_name=${ENGAGEMENT_NAME}" > /dev/null
                    fi
                '''
            }
        }
    }
}