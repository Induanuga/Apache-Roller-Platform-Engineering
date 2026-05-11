#!/usr/bin/env python3
import sys
import time
import subprocess
from datetime import datetime

def run_scheduler(interval_minutes: float, script_path: str):
    interval_seconds = interval_minutes * 60
    print(f"Scheduler started: running '{script_path}' every {interval_minutes} minute(s). Press Ctrl+C to stop.\n")

    while True:
        print(f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] Running: {script_path}")
        subprocess.run([sys.executable, script_path])
        print(f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] Done. Next run in {interval_minutes} minute(s)...\n")
        time.sleep(interval_seconds)

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Usage: python scheduler.py <minutes> <script.py>")
        print("Example: python scheduler.py 5 pipeline.py")
        sys.exit(1)

    try:
        minutes = float(sys.argv[1])
    except ValueError:
        print("Error: minutes must be a number (e.g. 5 or 0.5)")
        sys.exit(1)

    run_scheduler(minutes, sys.argv[2])