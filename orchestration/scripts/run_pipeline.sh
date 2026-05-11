#!/bin/bash
set -e

echo "=== Running Refactoring Pipeline ==="
echo "Time: $(date)"

# Load environment
if [ -f .env ]; then
    export $(cat .env | xargs)
fi

# Create timestamp
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
LOG_FILE="logs/pipeline_${TIMESTAMP}.log"

# Run pipeline
echo "Starting pipeline..."
python3 refactor_pipeline.py 2>&1 | tee "$LOG_FILE"

# Check exit code
if [ $? -eq 0 ]; then
    echo "✓ Pipeline completed successfully"
    echo "Log: $LOG_FILE"
else
    echo "✗ Pipeline failed"
    echo "Check log: $LOG_FILE"
    exit 1
fi