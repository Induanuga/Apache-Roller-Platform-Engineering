#!/bin/bash
set -e

echo "=== Automated Refactoring Pipeline Setup ==="

# Check Python version
python_version=$(python3 --version | cut -d' ' -f2)
echo "✓ Python version: $python_version"

# Check Java version
java_version=$(java -version 2>&1 | head -1 | cut -d'"' -f2)
echo "✓ Java version: $java_version"

# Check Maven
mvn_version=$(mvn --version | head -1 | cut -d' ' -f3)
echo "✓ Maven version: $mvn_version"

# Install Python dependencies
echo ""
echo "Installing Python dependencies..."
pip install -r ../refactoring-pipeline/requirements.txt
echo "✓ Dependencies installed"

# Create directories
echo ""
echo "Creating directories..."
mkdir -p logs results temp
echo "✓ Directories created"

# Download Designite (if not present)
if [ ! -f "DesigniteJava.jar" ]; then
    echo ""
    echo "Downloading Designite..."
    wget https://www.designite-tools.com/static/downloads/DesigniteJava.jar
    echo "✓ Designite downloaded"
fi

# Verify environment variables
echo ""
echo "Checking environment variables..."

if [ -z "$ANTHROPIC_API_KEY" ] && [ -z "$OPENAI_API_KEY" ] && [ -z "$GEMINI_API_KEY" ]; then
    echo "⚠ Warning: No API key found. Please set one in .env file"
fi

if [ -z "$GITHUB_TOKEN" ]; then
    echo "⚠ Warning: GITHUB_TOKEN not set. Please set in .env file"
fi

echo ""
echo "✓ Setup complete!"
echo ""
echo "Next steps:"
echo "1. Edit .env file with your API keys"
echo "2. Edit config.yaml for your repository"
echo "3. Run: ./scripts/run_pipeline.sh"