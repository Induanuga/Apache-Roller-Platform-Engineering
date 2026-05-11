FROM python:3.11-slim

# Install system dependencies
RUN apt-get update && apt-get install -y \
    git \
    default-jdk \
    maven \
    wget \
    && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy requirements
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application
COPY . .

# Download Designite
RUN wget https://www.designite-tools.com/static/downloads/DesigniteJava.jar

# Create directories
RUN mkdir -p logs results temp

# Set environment
ENV PYTHONUNBUFFERED=1

# Default command
CMD ["python3", "refactor_pipeline.py"]