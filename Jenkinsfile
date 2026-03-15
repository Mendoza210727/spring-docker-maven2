pipeline {
    agent any

    environment {
        // --- VARIABLES GENERALES (Sin credenciales expuestas) ---
        DOJO_URL = "http://192.168.100.242:8080" 
        PRODUCT_NAME = "Sistema de Control de Alquileres" 
        ENGAGEMENT_NAME = "V1 - Insegura" // Corregido a V1
        
        // Conexión al Docker de Windows
        DOCKER_HOST = "tcp://host.docker.internal:2375"
    }

    stages {
        stage('1. Checkout del Código') {
            steps {
                // Descarga el código
                checkout scm
                // Creamos carpetas para organizar los reportes extraídos
                sh 'mkdir -p reports/sast reports/sca'
            }
        }

        stage('2. SAST (Semgrep con Contenedor Efímero)') {
            steps {
                echo 'Ejecutando análisis de código fuente (SAST)...'
                sh '''
                    # 1. Creamos el contenedor apagado
                    docker create --name semgrep-scan -w /src returntocorp/semgrep semgrep scan --config=p/ci --json -o /src/semgrep-report.json /src
                    
                    # 2. Inyectamos el código físicamente (Soluciona el error de 0 archivos)
                    docker cp . semgrep-scan:/src
                    
                    # 3. Encendemos y escaneamos
                    docker start -a semgrep-scan || true
                    
                    # 4. Extraemos el reporte
                    docker cp semgrep-scan:/src/semgrep-report.json reports/sast/semgrep-report.json || true
                    
                    # 5. Limpiamos
                    docker rm semgrep-scan
                '''
            }
        }

        stage('3. SCA (Trivy con Contenedor Efímero)') {
            steps {
                echo 'Escaneando vulnerabilidades en dependencias...'
                sh '''
                    # Usamos la misma técnica para Trivy
                    docker create --name trivy-scan -w /src aquasec/trivy fs --format json --output /src/trivy-sca-report.json /src
                    
                    docker cp . trivy-scan:/src
                    docker start -a trivy-scan || true
                    docker cp trivy-scan:/src/trivy-sca-report.json reports/sca/trivy-sca-report.json || true
                    docker rm trivy-scan
                '''
            }
        }
    }

    post {
        always {
            echo 'Enviando reportes JSON a DefectDojo...'
            
            // Llamamos al Token de forma segura SOLO cuando se va a usar
            withCredentials([string(credentialsId: 'defectdojo-token', variable: 'DD_TOKEN')]) {
                
                // Envío del reporte SAST (Semgrep)
                sh '''
                    curl -X POST "${DOJO_URL}/api/v2/import-scan/" \
                    -H "Authorization: Token ${DD_TOKEN}" \
                    -F "scan_type=Semgrep JSON Report" \
                    -F "file=@reports/sast/semgrep-report.json" \
                    -F "product_name=${PRODUCT_NAME}" \
                    -F "engagement_name=${ENGAGEMENT_NAME}"
                '''
                
                // Envío del reporte SCA (Trivy)
                sh '''
                    curl -X POST "${DOJO_URL}/api/v2/import-scan/" \
                    -H "Authorization: Token ${DD_TOKEN}" \
                    -F "scan_type=Trivy Scan" \
                    -F "file=@reports/sca/trivy-sca-report.json" \
                    -F "product_name=${PRODUCT_NAME}" \
                    -F "engagement_name=${ENGAGEMENT_NAME}"
                '''
            }
        }
    }
}