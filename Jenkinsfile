pipeline {
    agent any

    environment {
        // --- VARIABLES DE DEFECTDOJO ---
        // Cambia la IP por la IP real de tu Ubuntu Server
        DOJO_URL = "http://192.168.100.242:8080" 
        
        // Llamamos a la credencial que creaste (¡cero hardcodeo!)
        DOJO_TOKEN = credentials('defectdojo-token') 
        
        // Estos nombres DEBEN ser exactamente iguales a como los creaste en DefectDojo
        PRODUCT_NAME = "Sistema de Control de Alquileres" 
        ENGAGEMENT_NAME = "V2 - Insegura"
        DOCKER_HOST = "tcp://host.docker.internal:2375"
    }

    stages {
        stage('1. Checkout del Código') {
            steps {
                // Descarga el código de la rama configurada en el Job
                checkout scm
            }
        }

        stage('2. SAST (Semgrep)') {
            steps {
                echo 'Ejecutando análisis de código fuente (SAST)...'
                // Usamos la imagen oficial de Semgrep. 
                // El "|| true" evita que el pipeline se rompa si encuentra errores, para poder enviar el reporte.
                sh '''
                docker run --rm -v "${WORKSPACE}:/src" returntocorp/semgrep semgrep scan --config=p/ci --json -o /src/semgrep-report.json /src || true
                '''
            }
        }

        stage('3. SCA (Trivy)') {
            steps {
                echo 'Escaneando vulnerabilidades en dependencias...'
                // Usamos Trivy para escanear el sistema de archivos del proyecto
                sh '''
                docker run --rm -v "${WORKSPACE}:/src" aquasec/trivy fs --format json --output /src/trivy-sca-report.json /src || true
                '''
            }
        }
    }

    // Esta sección siempre se ejecuta al final, pase lo que pase
    post {
        always {
            echo 'Enviando reportes JSON a DefectDojo...'
            
            // Envío del reporte SAST (Semgrep)
            sh '''
                curl -X POST "${DOJO_URL}/api/v2/import-scan/" \
                -H "Authorization: Token ${DOJO_TOKEN}" \
                -F "scan_type=Semgrep JSON Report" \
                -F "file=@semgrep-report.json" \
                -F "product_name=${PRODUCT_NAME}" \
                -F "engagement_name=${ENGAGEMENT_NAME}"
            '''
            
            // Envío del reporte SCA (Trivy)
            sh '''
                curl -X POST "${DOJO_URL}/api/v2/import-scan/" \
                -H "Authorization: Token ${DOJO_TOKEN}" \
                -F "scan_type=Trivy Scan" \
                -F "file=@trivy-sca-report.json" \
                -F "product_name=${PRODUCT_NAME}" \
                -F "engagement_name=${ENGAGEMENT_NAME}"
            '''
        }
    }
}