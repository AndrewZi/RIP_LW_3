#!/bin/bash

###############################################################################
# Load Test Script for Sensor Reactive System
#
# Генерирует параллельные HTTP запросы к /api/sensors/stream
# Мониторит потребление ресурсов (HEAP, CPU, Threads)
# Логирует метрики в CSV файл
###############################################################################

set -e

# Configuration
API_URL="${1:-http://localhost:8080}"
DURATION="${2:-180}"  # 180 seconds = 3 minutes
PARALLEL_REQUESTS="${3:-10}"
METRICS_CSV="load_test_metrics.csv"
CONTAINER_NAME="sensor-app"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Helper functions
print_header() {
    echo -e "\n${BLUE}=== $1 ===${NC}\n"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ $1${NC}"
}

# Cleanup function
cleanup() {
    print_info "Завершение нагрузочного теста..."
    # Kill all background curl processes
    pkill -P $$ curl 2>/dev/null || true
    pkill -P $$ jstat 2>/dev/null || true
    pkill -P $$ ps 2>/dev/null || true
}

trap cleanup EXIT

# Check prerequisites
print_header "Проверка предварительных условий"

if ! command -v curl &> /dev/null; then
    print_error "curl не установлен"
    exit 1
fi
print_success "curl установлен"

if ! command -v jstat &> /dev/null; then
    print_error "jstat не установлен (требуется Java JDK)"
    exit 1
fi
print_success "jstat установлен"

# Check if API is accessible
print_info "Проверка доступности API на $API_URL..."
if ! curl -sf "$API_URL/actuator/health" > /dev/null 2>&1; then
    print_error "API недоступен на $API_URL"
    exit 1
fi
print_success "API доступен"

# Create jfr directory if doesn't exist
mkdir -p jfr

# Initialize CSV file with headers
print_info "Инициализация CSV файла: $METRICS_CSV"
cat > "$METRICS_CSV" << 'EOF'
timestamp,heap_used_mb,heap_max_mb,cpu_percent,thread_count,allocation_rate_mb_s,gc_count,gc_time_ms,heap_committed_mb,vz_mb,rss_mb,request_count
EOF

# Get Java process PID from Docker container
print_info "Поиск Java процесса в контейнере $CONTAINER_NAME..."
JAVA_PID=$(docker exec "$CONTAINER_NAME" ps aux | grep java | grep -v grep | awk '{print $2}')
if [ -z "$JAVA_PID" ]; then
    print_error "Java процесс не найден в контейнере $CONTAINER_NAME"
    exit 1
fi
print_success "Найден Java процесс с PID: $JAVA_PID (в контейнере)"

# Function to generate random sensor ID
get_random_sensor_id() {
    echo $((RANDOM % 100 + 1))
}

# Function to generate random limit
get_random_limit() {
    echo $((RANDOM % 100 + 10))
}

# Function to make API request
make_request() {
    local sensor_id=$(get_random_sensor_id)
    local limit=$(get_random_limit)
    local url="$API_URL/api/sensors/stream?sensorId=$sensor_id&limit=$limit"

    # Make request with timeout, suppress output
    curl -s --max-time 10 "$url" > /dev/null 2>&1 &
    echo $!
}

# Function to collect metrics from Docker container
collect_metrics() {
    local timestamp=$(date +%s)
    local request_count=$1

    # Get metrics from jstat inside container
    local jstat_output=$(docker exec "$CONTAINER_NAME" jstat -gc "$JAVA_PID" 2>/dev/null || echo "")

    if [ -z "$jstat_output" ]; then
        # Fallback if jstat fails
        echo "$timestamp,0,0,0,0,0,0,0,0,0,0,$request_count"
        return
    fi

    # Parse jstat output (columns: S0C S1C S0U S1U EC EU OC OU OGCMN OGCMX OGC OU MCMN MCMX MC MU CCSMN CCSMX CCSU YGC YGCT FGC FGCT GCT)
    local line=$(echo "$jstat_output" | tail -1)
    local heap_used=$(awk '{print ($3 + $4 + $8 + $10) / 1024}' <<< "$line") # Convert KB to MB
    local heap_max=$(awk '{print ($1 + $2 + $7 + $9) / 1024}' <<< "$line")
    local gc_count=$(awk '{print $20 + $22}' <<< "$line")  # YGC + FGC
    local gc_time=$(awk '{print $21 + $23}' <<< "$line")   # YGCT + FGCT

    # Get CPU usage from /proc/stat inside container (simplified)
    local cpu_percent=$(docker exec "$CONTAINER_NAME" ps aux | grep "$JAVA_PID" | awk '{print $3}' || echo "0")

    # Get thread count from jcmd inside container
    local thread_count=$(docker exec "$CONTAINER_NAME" jcmd "$JAVA_PID" Thread.print 2>/dev/null | grep "tid" | wc -l || echo "0")

    # Get VSZ and RSS from ps inside container
    local ps_data=$(docker exec "$CONTAINER_NAME" ps aux | grep "$JAVA_PID" | head -1)
    local vz_mb=$(echo "$ps_data" | awk '{printf "%.0f", $5 / 1024}' || echo "0")
    local rss_mb=$(echo "$ps_data" | awk '{printf "%.0f", $6 / 1024}' || echo "0")

    echo "$timestamp,$heap_used,$heap_max,$cpu_percent,$thread_count,0,$gc_count,$gc_time,0,$vz_mb,$rss_mb,$request_count"
}

# Main load testing
print_header "Запуск нагрузочного теста"
print_info "Параметры:"
print_info "  API URL: $API_URL"
print_info "  Длительность: $DURATION секунд"
print_info "  Параллельные запросы: $PARALLEL_REQUESTS"
print_info "  Интервал сбора метрик: 2 секунды"
print_info ""

START_TIME=$(date +%s)
END_TIME=$((START_TIME + DURATION))
ITERATION=0
REQUEST_COUNT=0
LAST_METRIC_TIME=$START_TIME

# Start load generation
print_info "Начало генерации нагрузки..."
echo ""
printf "%-12s %-12s %-12s %-12s %-12s %-12s\n" "Time(s)" "Heap(MB)" "CPU(%)" "Threads" "Requests" "Status"
printf "%-12s %-12s %-12s %-12s %-12s %-12s\n" "----------" "----------" "----------" "----------" "----------" "----------"

while [ $(date +%s) -lt $END_TIME ]; do
    CURRENT_TIME=$(date +%s)
    ELAPSED=$((CURRENT_TIME - START_TIME))

    # Generate parallel requests
    for ((i=0; i<PARALLEL_REQUESTS; i++)); do
        make_request > /dev/null 2>&1 &
        REQUEST_COUNT=$((REQUEST_COUNT + 1))
    done

    # Collect metrics every 2 seconds
    if [ $((CURRENT_TIME - LAST_METRIC_TIME)) -ge 2 ]; then
        METRICS=$(collect_metrics "$REQUEST_COUNT")
        echo "$METRICS" >> "$METRICS_CSV"

        HEAP=$(echo "$METRICS" | cut -d',' -f2 | xargs printf "%.0f")
        CPU=$(echo "$METRICS" | cut -d',' -f4)
        THREADS=$(echo "$METRICS" | cut -d',' -f5)

        printf "%-12d %-12d %-12s %-12d %-12d %-12s\n" "$ELAPSED" "$HEAP" "$CPU" "$THREADS" "$REQUEST_COUNT" "OK"

        LAST_METRIC_TIME=$CURRENT_TIME
    fi

    # Small sleep to avoid busy waiting
    sleep 0.1
done

# Wait for remaining requests to complete
print_info "Ожидание завершения оставшихся запросов..."
sleep 5
wait

echo ""
print_success "Нагрузочное тестирование завершено!"
print_header "Результаты"

# Display summary
print_info "CSV файл сохранен: $METRICS_CSV"
print_info "Общее количество запросов: $REQUEST_COUNT"
print_info "Длительность теста: $DURATION секунд"

# Show statistics from CSV
print_info "Статистика (из CSV):"
echo ""

if [ -f "$METRICS_CSV" ]; then
    # Skip header
    tail -n +2 "$METRICS_CSV" | awk -F',' '
    BEGIN {
        count = 0
        min_heap = 999999
        max_heap = 0
        sum_heap = 0
        min_cpu = 999999
        max_cpu = 0
        sum_cpu = 0
        max_threads = 0
    }
    {
        if (NF > 0) {
            heap = int($2)
            cpu = int($4)
            threads = int($5)

            if (heap > 0) {
                if (heap < min_heap) min_heap = heap
                if (heap > max_heap) max_heap = heap
                sum_heap += heap
                count++
            }

            if (cpu > 0) {
                if (cpu < min_cpu) min_cpu = cpu
                if (cpu > max_cpu) max_cpu = cpu
                sum_cpu += cpu
            }

            if (threads > max_threads) max_threads = threads
        }
    }
    END {
        if (count > 0) {
            printf "  Heap Usage:\n"
            printf "    Min: %d MB\n", min_heap
            printf "    Max: %d MB\n", max_heap
            printf "    Avg: %.0f MB\n", sum_heap/count
            printf "  CPU Usage:\n"
            printf "    Min: %d %%\n", (min_cpu == 999999) ? 0 : min_cpu
            printf "    Max: %d %%\n", max_cpu
            printf "    Avg: %.0f %%\n", sum_cpu/count
            printf "  Max Threads: %d\n", max_threads
        }
    }
    ' "$METRICS_CSV"
fi

echo ""
print_success "Готово! Используйте 'cat $METRICS_CSV' для просмотра полных метрик"
