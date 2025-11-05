#!/bin/bash

# Script para iniciar ELK Stack y la aplicación Quarkus

echo "======================================"
echo "  Iniciando ELK Stack + Quarkus"
echo "======================================"
echo ""

# Colores para output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Función para verificar si un servicio está corriendo
check_service() {
    local service_name=$1
    local url=$2
    local max_attempts=30
    local attempt=1

    echo -e "${YELLOW}Verificando $service_name...${NC}"

    while [ $attempt -le $max_attempts ]; do
        if curl -s -f "$url" > /dev/null 2>&1; then
            echo -e "${GREEN}✓ $service_name está listo${NC}"
            return 0
        fi
        echo -n "."
        sleep 2
        attempt=$((attempt + 1))
    done

    echo -e "${RED}✗ $service_name no respondió después de $max_attempts intentos${NC}"
    return 1
}

# 1. Iniciar ELK Stack
echo -e "${YELLOW}Paso 1: Iniciando ELK Stack...${NC}"
docker-compose -f docker-compose-elk.yml up -d

if [ $? -ne 0 ]; then
    echo -e "${RED}Error al iniciar ELK Stack${NC}"
    exit 1
fi

echo ""

# 2. Esperar a que Elasticsearch esté listo
check_service "Elasticsearch" "http://localhost:9200/_cluster/health"
if [ $? -ne 0 ]; then
    echo -e "${RED}Error: Elasticsearch no está disponible${NC}"
    exit 1
fi

echo ""

# 3. Esperar a que Logstash esté listo
check_service "Logstash" "http://localhost:9600"
if [ $? -ne 0 ]; then
    echo -e "${RED}Error: Logstash no está disponible${NC}"
    exit 1
fi

echo ""

# 4. Esperar a que Kibana esté listo
check_service "Kibana" "http://localhost:5601/api/status"
if [ $? -ne 0 ]; then
    echo -e "${RED}Error: Kibana no está disponible${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}======================================"
echo "  ✓ ELK Stack iniciado correctamente"
echo "======================================${NC}"
echo ""
echo "Servicios disponibles:"
echo "  • Elasticsearch: http://localhost:9200"
echo "  • Kibana:        http://localhost:5601"
echo "  • Logstash:      localhost:5000 (TCP/UDP)"
echo ""
echo -e "${YELLOW}Paso 2: Compilando aplicación Quarkus...${NC}"

# 5. Compilar la aplicación
mvn clean compile -DskipTests

if [ $? -ne 0 ]; then
    echo -e "${RED}Error al compilar la aplicación${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}✓ Compilación exitosa${NC}"
echo ""
echo -e "${YELLOW}======================================"
echo "  Para iniciar la aplicación, ejecuta:"
echo "  mvn quarkus:dev"
echo "======================================${NC}"
echo ""
echo "Luego podrás ver los logs en Kibana:"
echo "  1. Abre http://localhost:5601"
echo "  2. Ve a Management → Data Views"
echo "  3. Crea un Data View: 'quarkus-logs-*'"
echo "  4. Ve a Discover para ver los logs"
echo ""
