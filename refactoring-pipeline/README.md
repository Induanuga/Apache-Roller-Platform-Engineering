# 🔧 Automated Refactoring Pipeline

> **Task 3C** — An end-to-end pipeline that detects design smells in Java repositories, generates refactored code using an LLM, and automatically opens a Pull Request with a detailed description and metrics.

---

## Table of Contents

1. [Overview](#overview)
2. [Features](#features)
3. [Architecture](#architecture)
4. [Pipeline Phases](#pipeline-phases)
5. [Large File Handling](#large-file-handling)
6. [Configuration](#configuration)
7. [Installation](#installation)
8. [Usage](#usage)
9. [Scheduler](#scheduler)
10. [Output Files](#output-files)
11. [Design Smells Detected](#design-smells-detected)
12. [Supported LLM Backends](#supported-llm-backends)
13. [Error Handling](#error-handling)
14. [Known Limitations](#known-limitations)

---

## Overview

This pipeline automatically improves Java codebases through three main activities:

- **Detect** — Scans a GitHub repo for well-known design smells using an LLM
- **Refactor** — Generates refactored versions of the affected files
- **PR** — Commits the changes to a new branch and opens a Pull Request

All three phases are configurable and can be toggled independently.

---

## Features

- LLM-powered smell detection (Gemini or OpenAI)
- Chunking strategy for large files (> 500 lines or > 1 MB)
- Detects 13 design smell categories with severity scoring
- Produces refactoring plans with estimated complexity and LOC reduction
- JSON reports for both detection and refactoring results
- Automated Git branch creation, commit, push, and PR
- LLM-generated PR description with full Markdown formatting
- Retry logic with exponential backoff for all LLM calls
- Scheduler script to re-run the pipeline at a fixed interval

---

## Architecture

```
RefactoringPipeline          (orchestrator)
├── RepositoryManager        (git clone / pull)
├── CodeAnalyzer             (detection phase)
│   ├── LargeFileHandler     (chunking)
│   └── LLMInterface         (Gemini / OpenAI)
├── CodeRefactorer           (refactoring phase)
│   └── LLMInterface
├── GitHubPRGenerator        (PR creation phase)
│   └── LLMInterface
└── ReportGenerator          (JSON reports)
```

---

## Pipeline Phases

### Phase 1 — Repository Setup

- Clones the target repository if not already present
- If it exists, checks out `main` or `master` and pulls latest
- Validates that all required API keys are set before proceeding

### Phase 2 — Design Smell Detection

1. Walks all `.java` files, skipping `test/`, `target/`, `build/`
2. Caps analysis at `MAX_FILES_TO_ANALYZE` (default: 10)
3. For each file, calculates static metrics (LOC, cyclomatic complexity, coupling, comment ratio)
4. Decides whether to chunk the file (see [Large File Handling](#large-file-handling))
5. Sends each file/chunk to the LLM with the metrics embedded in the prompt
6. Parses the JSON response into typed `CodeSmell` objects
7. Sorts all smells by severity (CRITICAL → LOW) then confidence score
8. Saves `smell_detection_report.json`

### Phase 3 — Code Refactoring

For each of the top `MAX_SMELLS_TO_REFACTOR` smells:

1. **Plan** — LLM selects a strategy (Extract Method, Extract Class, Move Method, etc.) and estimates impact
2. **Generate** — LLM rewrites the affected file(s) following the plan, preserving all behaviour
3. Computes before/after metrics and improvement percentages
4. Saves original + refactored code to `./refactored_code/<smell_id>/` for documentation
5. Saves `refactoring_report.json`

### Phase 4 — Pull Request Creation

1. Creates a new branch `refactor/automated-YYYYMMDD-HHMMSS`
2. Writes the refactored files into the local repo
3. Runs `git add . → commit → push`
4. LLM generates a full Markdown PR description (summary, smell list, metrics table, risks, checklist)
5. Calls the GitHub REST API to open the PR

---

## Large File Handling

Files over **500 lines** or **1 MB** are split into overlapping chunks before analysis.

```
File (600 lines)
│
├── Chunk 1  →  lines   1 – 500   sent to LLM
│                    ↑ 50-line overlap
└── Chunk 2  →  lines 451 – 600   sent to LLM
```

Key details:

- **Window size**: 500 lines per chunk
- **Overlap**: 50 lines — ensures smells at chunk boundaries are not lost
- **Line number adjustment**: each smell's `line_start` / `line_end` is offset by the chunk's starting line before being stored
- **Smell ID deduplication**: IDs are MD5 hashes of `(file_path + line_start + smell_type)`, so the same smell detected in two overlapping chunks is naturally deduplicated
- All chunk results are merged into a single flat list before sorting and reporting

---

## Configuration

All settings live in the `Config` class at the top of the script.

| Variable | Default | Description |
|---|---|---|
| `REPO_URL` | *(set in code)* | GitHub repo to analyse |
| `USE_GEMINI` | `True` | `True` = Gemini, `False` = OpenAI |
| `GEMINI_MODEL` | `models/gemini-2.5-flash` | Gemini model identifier |
| `OPENAI_MODEL` | `gpt-4o` | OpenAI model identifier |
| `MAX_FILES_TO_ANALYZE` | `10` | Max Java files to scan |
| `MAX_SMELLS_TO_REFACTOR` | `5` | Max smells to attempt refactoring |
| `LINES_OF_CODE_THRESHOLD` | `300` | LOC threshold for smell flagging |
| `CYCLOMATIC_COMPLEXITY_THRESHOLD` | `15` | CC threshold |
| `METHOD_LENGTH_THRESHOLD` | `50` | Lines per method threshold |
| `MAX_FILE_SIZE_BYTES` | `1_000_000` | 1 MB — triggers chunking |
| `CHUNK_OVERLAP` | `200` *(lines)* | Overlap between chunks |
| `ENABLE_DETECTION` | `True` | Toggle detection phase |
| `ENABLE_REFACTORING` | `True` | Toggle refactoring phase |
| `ENABLE_PR_CREATION` | `True` | Toggle PR creation phase |

### Environment Variables

```bash
export GEMINI_API_KEY="your-key-here"
export OPENAI_API_KEY="your-key-here"   # only if USE_GEMINI = False
export GITHUB_TOKEN="your-token-here"
```

---

## Installation

```bash
# 1. Clone this repo
git clone <your-pipeline-repo>
cd <your-pipeline-repo>

# 2. Install dependencies
pip install google-generativeai openai requests

# 3. Set environment variables (see above)

# 4. Run
python pipeline.py
```

> **Note:** If you hit `Unknown field for GenerateContentRequest: request_options`, remove the `request_options={'timeout': 60}` line from `LLMInterface.generate()`. This parameter requires a newer version of the SDK. To fix it cleanly: `pip install --upgrade google-generativeai`

---

## Usage

### Run once

```bash
python pipeline.py
```

### Run on a schedule

Use the included `scheduler.py` to re-run the pipeline automatically:

```bash
python scheduler.py <minutes> pipeline.py
```

Examples:

```bash
python scheduler.py 30 pipeline.py     # every 30 minutes
python scheduler.py 0.5 pipeline.py    # every 30 seconds (for testing)
python scheduler.py 1440 pipeline.py   # once a day
```

Press `Ctrl+C` to stop the scheduler.

---

## Output Files

```
./results/
  smell_detection_report.json     ← all smells, metrics, severity
  refactoring_report.json         ← all refactoring attempts + improvements

./refactored_code/
  smell_<id>/
    ORIGINAL.java                 ← original file snapshot
    REFACTORED_1_<filename>.java  ← LLM-generated replacement
    METADATA.json                 ← strategy, improvements, metrics

./logs/
  pipeline_YYYYMMDD_HHMMSS.log   ← full run log
```

---

## Design Smells Detected

| Smell | Description |
|---|---|
| God Class | Single class with too many responsibilities |
| Long Method | Methods that are too long or deeply nested |
| Feature Envy | Method uses more data from another class than its own |
| Data Class | Class with only getters/setters, no behaviour |
| Duplicate Code | Similar logic repeated in multiple places |
| Large Class | Class that has grown beyond a manageable size |
| Long Parameter List | Methods with too many parameters |
| Switch Statements | Complex switch/if-else chains (use polymorphism instead) |
| Shotgun Surgery | One change requires edits in many unrelated places |
| Divergent Change | One class changes for many different reasons |
| Lazy Class | Class that does too little to justify its existence |
| Speculative Generality | Unnecessary abstraction added "just in case" |
| Inappropriate Intimacy | Classes that depend too heavily on each other's internals |

---

## Supported LLM Backends

### Google Gemini (default)

```python
USE_GEMINI = True
GEMINI_MODEL = 'models/gemini-2.0-flash-exp'
```

Requires `GEMINI_API_KEY`.

### OpenAI

```python
USE_GEMINI = False
OPENAI_MODEL = 'gpt-4o'
```

Requires `OPENAI_API_KEY`.

All LLM calls use the same `LLMInterface.generate()` method, so switching backends requires only changing `USE_GEMINI`.

---

## Error Handling

| Situation | Behaviour |
|---|---|
| LLM call fails | Retries up to 2× with exponential backoff (1s, 2s). Raises after 3rd failure. |
| JSON parse failure | Falls back through 4 recovery strategies (strip markdown, escape newlines, flatten, substring). |
| Refactoring fails | Logs error, skips smell, pipeline continues with the next one. |
| PR creation fails | Logs HTTP error body. Branch and commits are still on remote. |
| No smells found | Pipeline exits cleanly after Phase 2 with a summary message. |
| `Ctrl+C` | Caught gracefully. Partial results already saved to disk are preserved. |

---

## Known Limitations

- Only Java files are supported (`.java` glob)
- Cyclomatic complexity and coupling are estimated heuristically, not via AST parsing
- Refactored code is LLM-generated and must be manually reviewed before merging
- No test execution — the pipeline cannot verify that refactored code compiles or passes tests
- GitHub token requires `repo` scope to create PRs on private repositories
- The `request_options` timeout parameter requires `google-generativeai >= 0.8`; remove it for older versions

---

*Generated by Automated Refactoring Pipeline — Task 3C*