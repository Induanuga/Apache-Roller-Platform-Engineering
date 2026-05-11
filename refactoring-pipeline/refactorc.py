#!/usr/bin/env python3
"""
Task 3C: Automated Refactoring Pipeline with LLM Integration
============================================================

This pipeline:
1. DETECTS: Scans GitHub repository for design smells
2. REFACTORS: Uses LLMs to generate refactored code
3. PR GENERATION: Creates detailed Pull Requests with metrics

Features:
- Large file handling with chunking strategy
- Comprehensive design smell detection
- LLM-powered refactoring suggestions
- Automated PR generation with detailed metrics
- Robust error handling and logging
"""

import os
import sys
import json
import subprocess
import time
import hashlib
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Optional, Tuple
from dataclasses import dataclass, asdict
from enum import Enum
import requests

# Import LLM libraries
try:
    import google.generativeai as genai
except ImportError:
    print("Warning: google.generativeai not installed")
    genai = None

try:
    from openai import OpenAI
except ImportError:
    print("Warning: openai not installed")
    OpenAI = None


# ============================================================================
# Configuration
# ============================================================================

class Config:
    """Centralized configuration"""
    
    # Repository settings
    REPO_URL = 'https://github.com/serc-courses/project-1-team-13.git'
    REPO_OWNER = 'serc-courses'
    REPO_NAME = 'project-1-team-13'
    LOCAL_REPO_PATH = './project-1-team-13'
    
    # API Configuration
    GEMINI_API_KEY = os.getenv('GEMINI_API_KEY')
    OPENAI_API_KEY = os.getenv('OPENAI_API_KEY')
    GITHUB_TOKEN = os.getenv('GITHUB_TOKEN')
    
    # LLM Model Selection
    USE_GEMINI = True  # Set to False to use OpenAI
    GEMINI_MODEL = 'models/gemini-2.5-flash'
    OPENAI_MODEL = 'gpt-4o'
    
    # Analysis Thresholds
    CYCLOMATIC_COMPLEXITY_THRESHOLD = 15
    LINES_OF_CODE_THRESHOLD = 300
    METHOD_LENGTH_THRESHOLD = 50
    CLASS_SIZE_THRESHOLD = 500
    COUPLING_THRESHOLD = 10
    
    # Chunking Strategy for Large Files
    MAX_TOKENS_PER_CHUNK = 3000  # Approximate tokens
    CHUNK_OVERLAP = 200  # Lines of overlap between chunks
    MAX_FILE_SIZE_BYTES = 1_000_000  # 1MB
    
    # Pipeline Control
    ENABLE_DETECTION = True
    ENABLE_REFACTORING = True
    ENABLE_PR_CREATION = True
    MAX_SMELLS_TO_REFACTOR = 5
    MAX_FILES_TO_ANALYZE = 10
    
    # Output directories
    RESULTS_DIR = './results'
    LOGS_DIR = './logs'
    REFACTORED_CODE_DIR = './refactored_code'
    
    @classmethod
    def validate(cls):
        """Validate configuration"""
        if cls.USE_GEMINI and not cls.GEMINI_API_KEY:
            raise ValueError("GEMINI_API_KEY not set")
        if not cls.USE_GEMINI and not cls.OPENAI_API_KEY:
            raise ValueError("OPENAI_API_KEY not set")
        if cls.ENABLE_PR_CREATION and not cls.GITHUB_TOKEN:
            raise ValueError("GITHUB_TOKEN not set for PR creation")


# ============================================================================
# Data Models
# ============================================================================

class SmellType(Enum):
    """Types of design smells"""
    GOD_CLASS = "God Class"
    LONG_METHOD = "Long Method"
    FEATURE_ENVY = "Feature Envy"
    DATA_CLASS = "Data Class"
    SHOTGUN_SURGERY = "Shotgun Surgery"
    DIVERGENT_CHANGE = "Divergent Change"
    LAZY_CLASS = "Lazy Class"
    SPECULATIVE_GENERALITY = "Speculative Generality"
    DUPLICATE_CODE = "Duplicate Code"
    LARGE_CLASS = "Large Class"
    LONG_PARAMETER_LIST = "Long Parameter List"
    SWITCH_STATEMENTS = "Switch Statements"
    INAPPROPRIATE_INTIMACY = "Inappropriate Intimacy"


class Severity(Enum):
    """Severity levels"""
    CRITICAL = "CRITICAL"
    HIGH = "HIGH"
    MEDIUM = "MEDIUM"
    LOW = "LOW"


@dataclass
class CodeMetrics:
    """Metrics for a code file/method"""
    lines_of_code: int
    cyclomatic_complexity: int
    method_count: int
    class_count: int
    coupling_count: int
    cohesion_score: float
    comment_ratio: float
    duplication_ratio: float
    
    def to_dict(self):
        return asdict(self)


@dataclass
class CodeSmell:
    """Represents a detected code smell"""
    smell_id: str
    file_path: str
    smell_type: SmellType
    severity: Severity
    line_start: int
    line_end: int
    description: str
    metrics: CodeMetrics
    suggested_refactoring: str
    affected_code_snippet: str
    confidence_score: float
    
    def to_dict(self):
        return {
            'smell_id': self.smell_id,
            'file_path': self.file_path,
            'smell_type': self.smell_type.value,
            'severity': self.severity.value,
            'line_start': self.line_start,
            'line_end': self.line_end,
            'description': self.description,
            'metrics': self.metrics.to_dict(),
            'suggested_refactoring': self.suggested_refactoring,
            'affected_code_snippet': self.affected_code_snippet[:500],
            'confidence_score': self.confidence_score
        }


@dataclass
class RefactoringPlan:
    """Detailed refactoring plan"""
    smell_id: str
    strategy: str
    description: str
    files_to_modify: List[str]
    files_to_create: List[str]
    estimated_complexity_reduction: float
    estimated_loc_reduction: int
    risks: List[str]
    testing_recommendations: List[str]


@dataclass
class RefactoringResult:
    """Result of applying refactoring"""
    smell_id: str
    original_file: str
    refactored_files: List[Dict[str, str]]  # [{'path': '...', 'content': '...'}]
    metrics_before: CodeMetrics
    metrics_after: CodeMetrics
    success: bool
    refactoring_strategy: str
    improvements: Dict[str, float]
    error_message: Optional[str] = None
    warnings: List[str] = None
    
    def __post_init__(self):
        if self.warnings is None:
            self.warnings = []
    
    def to_dict(self):
        return {
            'smell_id': self.smell_id,
            'original_file': self.original_file,
            'refactored_files': self.refactored_files,
            'metrics_before': self.metrics_before.to_dict(),
            'metrics_after': self.metrics_after.to_dict() if self.metrics_after else None,
            'success': self.success,
            'refactoring_strategy': self.refactoring_strategy,
            'improvements': self.improvements,
            'error_message': self.error_message,
            'warnings': self.warnings
        }


# ============================================================================
# Logging Utility
# ============================================================================

class Logger:
    """Simple logging utility"""
    
    def __init__(self, log_file: str = None):
        self.log_file = log_file
        if log_file:
            os.makedirs(os.path.dirname(log_file), exist_ok=True)
    
    def log(self, message: str, level: str = "INFO"):
        """Log message to console and file"""
        timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        log_entry = f"[{timestamp}] [{level}] {message}"
        print(log_entry)
        
        if self.log_file:
            try:
                # Ensure directory exists
                os.makedirs(os.path.dirname(self.log_file), exist_ok=True)
                with open(self.log_file, 'a') as f:
                    f.write(log_entry + "\n")
            except Exception as e:
                # If logging fails, just print to console
                print(f"[WARNING] Could not write to log file: {e}")
    
    def info(self, message: str):
        self.log(message, "INFO")
    
    def warning(self, message: str):
        self.log(message, "WARNING")
    
    def error(self, message: str):
        self.log(message, "ERROR")
    
    def success(self, message: str):
        self.log(message, "SUCCESS")


# ============================================================================
# LLM Interface
# ============================================================================

class LLMInterface:
    """Unified interface for LLM APIs"""
    
    def __init__(self, use_gemini: bool = True):
        self.use_gemini = use_gemini
        
        if use_gemini:
            if not genai:
                raise ImportError("google.generativeai not installed")
            genai.configure(api_key=Config.GEMINI_API_KEY)
            self.model = genai.GenerativeModel(Config.GEMINI_MODEL)
        else:
            if not OpenAI:
                raise ImportError("openai not installed")
            self.client = OpenAI(api_key=Config.OPENAI_API_KEY)
    
    def generate(self, prompt: str, temperature: float = 0.3, 
                 max_tokens: int = 4000, max_retries: int = 2) -> str:
        """Generate response from LLM with retry logic"""
        import time
        
        for attempt in range(max_retries + 1):
            try:
                if self.use_gemini:
                    response = self.model.generate_content(
                        prompt,
                        generation_config=genai.types.GenerationConfig(
                            max_output_tokens=max_tokens,
                            temperature=temperature
                        ),
                        #request_options={'timeout': 60}  # 60 second timeout
                    )
                    return response.text
                else:
                    response = self.client.chat.completions.create(
                        model=Config.OPENAI_MODEL,
                        messages=[{"role": "user", "content": prompt}],
                        temperature=temperature,
                        max_tokens=max_tokens,
                        timeout=60  # 60 second timeout
                    )
                    return response.choices[0].message.content
                    
            except Exception as e:
                if attempt < max_retries:
                    wait_time = 2 ** attempt  # Exponential backoff: 1s, 2s, 4s
                    print(f"[WARNING] LLM call failed (attempt {attempt + 1}/{max_retries + 1}), retrying in {wait_time}s: {e}")
                    time.sleep(wait_time)
                else:
                    raise RuntimeError(f"LLM generation failed after {max_retries + 1} attempts: {str(e)}")


# ============================================================================
# Large File Handler
# ============================================================================

class LargeFileHandler:
    """Handles large files by chunking"""
    
    @staticmethod
    def chunk_code(code: str, max_lines: int = 500, 
                   overlap: int = 50) -> List[Tuple[str, int, int]]:
        """
        Split code into overlapping chunks
        Returns: List of (chunk_content, start_line, end_line)
        """
        lines = code.split('\n')
        total_lines = len(lines)
        chunks = []
        
        start = 0
        while start < total_lines:
            end = min(start + max_lines, total_lines)
            chunk_lines = lines[start:end]
            chunks.append((
                '\n'.join(chunk_lines),
                start + 1,  # 1-indexed
                end
            ))
            start = end - overlap if end < total_lines else end
        
        return chunks
    
    @staticmethod
    def should_chunk(file_path: str) -> bool:
        """Determine if file should be chunked"""
        try:
            size = os.path.getsize(file_path)
            if size > Config.MAX_FILE_SIZE_BYTES:
                return True
            
            with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
                lines = f.readlines()
                return len(lines) > 500
        except:
            return False


# ============================================================================
# Code Analyzer (Detection Phase)
# ============================================================================

class CodeAnalyzer:
    """Detects design smells in code"""
    
    def __init__(self, llm: LLMInterface, logger: Logger):
        self.llm = llm
        self.logger = logger
        self.file_handler = LargeFileHandler()
    
    def scan_repository(self, repo_path: str) -> List[str]:
        """Scan repository for Java files"""
        self.logger.info(f"Scanning repository: {repo_path}")
        
        java_files = []
        for java_file in Path(repo_path).rglob('*.java'):
            # Skip test files and build directories
            if any(skip in str(java_file).lower() 
                   for skip in ['test', 'target', 'build', '.git']):
                continue
            java_files.append(str(java_file))
        
        self.logger.info(f"Found {len(java_files)} Java files")
        return java_files[:Config.MAX_FILES_TO_ANALYZE]
    
    def calculate_metrics(self, code: str, file_path: str) -> CodeMetrics:
        """Calculate code metrics"""
        lines = code.split('\n')
        code_lines = [l for l in lines if l.strip() and not l.strip().startswith('//')]
        
        loc = len(code_lines)
        cc = self._calculate_cyclomatic_complexity(lines)
        method_count = code.count('public ') + code.count('private ') + code.count('protected ')
        class_count = code.count('class ') + code.count('interface ')
        
        # Comment ratio
        comment_lines = sum(1 for l in lines if l.strip().startswith('//') or '/*' in l)
        comment_ratio = comment_lines / max(len(lines), 1)
        
        return CodeMetrics(
            lines_of_code=loc,
            cyclomatic_complexity=cc,
            method_count=method_count,
            class_count=class_count,
            coupling_count=self._estimate_coupling(code),
            cohesion_score=0.0,  # Simplified
            comment_ratio=comment_ratio,
            duplication_ratio=0.0  # Simplified
        )
    
    def _calculate_cyclomatic_complexity(self, lines: List[str]) -> int:
        """Calculate cyclomatic complexity"""
        cc = 1
        keywords = ['if ', 'else if', 'while ', 'for ', 'case ', 
                   'catch ', '&&', '||', '?', 'switch']
        
        for line in lines:
            line = line.strip().lower()
            for keyword in keywords:
                cc += line.count(keyword)
        
        return cc
    
    def _estimate_coupling(self, code: str) -> int:
        """Estimate coupling by counting imports and dependencies"""
        import_count = code.count('import ')
        new_count = code.count(' new ')
        return import_count + (new_count // 2)
    
    def analyze_file(self, file_path: str) -> List[CodeSmell]:
        """Analyze a single file for design smells"""
        self.logger.info(f"Analyzing file: {os.path.basename(file_path)}")
        
        try:
            with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
                code = f.read()
        except Exception as e:
            self.logger.error(f"Failed to read file {file_path}: {e}")
            return []
        
        # Calculate metrics
        metrics = self.calculate_metrics(code, file_path)
        
        # Check if file needs chunking
        all_smells = []
        if self.file_handler.should_chunk(file_path):
            self.logger.info(f"  Large file detected - using chunking strategy")
            chunks = self.file_handler.chunk_code(code)
            
            for i, (chunk, start_line, end_line) in enumerate(chunks, 1):
                self.logger.info(f"  Analyzing chunk {i}/{len(chunks)} (lines {start_line}-{end_line})")
                smells = self._analyze_code_chunk(
                    file_path, chunk, metrics, start_line, end_line
                )
                all_smells.extend(smells)
        else:
            all_smells = self._analyze_code_chunk(
                file_path, code, metrics, 1, len(code.split('\n'))
            )
        
        self.logger.info(f"  Detected {len(all_smells)} smells")
        return all_smells
    
    def _analyze_code_chunk(self, file_path: str, code: str, 
                           metrics: CodeMetrics, start_line: int, 
                           end_line: int) -> List[CodeSmell]:
        """Analyze a code chunk using LLM"""
        
        prompt = self._create_detection_prompt(file_path, code, metrics)
        
        try:
            response = self.llm.generate(prompt, temperature=0.2)
            smells = self._parse_llm_response(
                response, file_path, metrics, code, start_line, end_line
            )
            return smells
        except Exception as e:
            self.logger.error(f"LLM analysis failed: {e}")
            return []
    
    def _create_detection_prompt(self, file_path: str, code: str, 
                                 metrics: CodeMetrics) -> str:
        """Create prompt for design smell detection"""
        
        return f"""You are an expert code analyzer specializing in design pattern detection and code quality assessment.

**Task:** Analyze the following Java code and identify design smells.

**File:** {os.path.basename(file_path)}

**Current Metrics:**
- Lines of Code: {metrics.lines_of_code}
- Cyclomatic Complexity: {metrics.cyclomatic_complexity}
- Method Count: {metrics.method_count}
- Class Count: {metrics.class_count}
- Coupling: {metrics.coupling_count}

**Code to Analyze:**
```java
{code[:4000]}
```

**Design Smells to Detect:**
1. God Class - Class with too many responsibilities
2. Long Method - Methods that are too long or complex
3. Feature Envy - Method uses more data from another class
4. Data Class - Class with only getters/setters
5. Duplicate Code - Similar code in multiple places
6. Large Class - Class that has grown too large
7. Long Parameter List - Too many parameters
8. Switch Statements - Complex switch/if-else chains

**Required Output Format (JSON ONLY):**
{{
  "smells": [
    {{
      "type": "God Class",
      "severity": "HIGH",
      "line_start": 10,
      "line_end": 150,
      "description": "Single-line description of the smell",
      "suggested_refactoring": "Specific refactoring technique (e.g., Extract Class, Extract Method)",
      "confidence": 0.85
    }}
  ]
}}

**Important Rules:**
- Respond with ONLY valid JSON
- No markdown, no explanations, no code blocks
- All string values must be single-line (no newlines)
- Severity: CRITICAL, HIGH, MEDIUM, or LOW
- Confidence: 0.0 to 1.0
- If no smells detected, return {{"smells": []}}

Analyze the code now:"""
    
    def _parse_llm_response(self, response: str, file_path: str, 
                           metrics: CodeMetrics, code: str,
                           start_line: int, end_line: int) -> List[CodeSmell]:
        """Parse LLM response and create CodeSmell objects"""
        
        # Clean response
        response = response.strip()
        if '```json' in response:
            response = response.split('```json')[1].split('```')[0]
        elif '```' in response:
            response = response.split('```')[1].split('```')[0]
        
        response = response.strip()
        response = response.replace(',\n}', '\n}').replace(',\n]', '\n]')
        
        try:
            data = json.loads(response)
        except json.JSONDecodeError as e:
            self.logger.warning(f"Failed to parse JSON: {e}")
            # Try to extract JSON object
            start = response.find('{')
            end = response.rfind('}') + 1
            if start != -1 and end > start:
                try:
                    data = json.loads(response[start:end])
                except:
                    return []
            else:
                return []
        
        smells = []
        for smell_data in data.get('smells', []):
            try:
                # Adjust line numbers based on chunk
                actual_line_start = start_line + smell_data.get('line_start', 0)
                actual_line_end = start_line + smell_data.get('line_end', 50)
                
                # Extract affected code
                lines = code.split('\n')
                snippet_start = max(0, smell_data.get('line_start', 0) - 1)
                snippet_end = min(len(lines), smell_data.get('line_end', 50))
                affected_snippet = '\n'.join(lines[snippet_start:snippet_end])
                
                # Create unique smell ID
                smell_id = hashlib.md5(
                    f"{file_path}{actual_line_start}{smell_data['type']}".encode()
                ).hexdigest()[:12]
                
                smell = CodeSmell(
                    smell_id=smell_id,
                    file_path=file_path,
                    smell_type=self._map_smell_type(smell_data['type']),
                    severity=self._map_severity(smell_data.get('severity', 'MEDIUM')),
                    line_start=actual_line_start,
                    line_end=actual_line_end,
                    description=smell_data['description'],
                    metrics=metrics,
                    suggested_refactoring=smell_data.get('suggested_refactoring', 'Refactor needed'),
                    affected_code_snippet=affected_snippet,
                    confidence_score=smell_data.get('confidence', 0.5)
                )
                smells.append(smell)
            except Exception as e:
                self.logger.warning(f"Failed to parse smell: {e}")
                continue
        
        return smells
    
    def _map_smell_type(self, type_str: str) -> SmellType:
        """Map string to SmellType enum"""
        type_map = {
            'god class': SmellType.GOD_CLASS,
            'long method': SmellType.LONG_METHOD,
            'feature envy': SmellType.FEATURE_ENVY,
            'data class': SmellType.DATA_CLASS,
            'duplicate code': SmellType.DUPLICATE_CODE,
            'large class': SmellType.LARGE_CLASS,
            'long parameter list': SmellType.LONG_PARAMETER_LIST,
            'switch statements': SmellType.SWITCH_STATEMENTS,
        }
        return type_map.get(type_str.lower(), SmellType.GOD_CLASS)
    
    def _map_severity(self, severity_str: str) -> Severity:
        """Map string to Severity enum"""
        severity_map = {
            'critical': Severity.CRITICAL,
            'high': Severity.HIGH,
            'medium': Severity.MEDIUM,
            'low': Severity.LOW,
        }
        return severity_map.get(severity_str.lower(), Severity.MEDIUM)


# ============================================================================
# Code Refactorer (Refactoring Phase)
# ============================================================================

class CodeRefactorer:
    """Generates refactored code using LLM"""
    
    def __init__(self, llm: LLMInterface, logger: Logger):
        self.llm = llm
        self.logger = logger
    
    def create_refactoring_plan(self, smell: CodeSmell, code: str) -> RefactoringPlan:
        """Create a detailed refactoring plan"""
        self.logger.info(f"Creating refactoring plan for smell: {smell.smell_id}")
        
        prompt = f"""You are an expert software architect specializing in code refactoring.

**Task:** Create a refactoring plan for the detected design smell.

**Design Smell:**
- Type: {smell.smell_type.value}
- Severity: {smell.severity.value}
- Location: Lines {smell.line_start} to {smell.line_end}
- Description: {smell.description}
- Suggested Technique: {smell.suggested_refactoring}

**Current Metrics:**
- Lines of Code: {smell.metrics.lines_of_code}
- Cyclomatic Complexity: {smell.metrics.cyclomatic_complexity}
- Method Count: {smell.metrics.method_count}

**Affected Code:**
```java
{smell.affected_code_snippet[:2000]}
```

**CRITICAL RULES:**
1. Respond with ONLY valid JSON
2. NO markdown, NO code blocks, NO explanations
3. ALL string values MUST be single-line (no newlines, no line breaks)
4. Use spaces instead of newlines in descriptions
5. Escape any quotes within strings

**Required Output Format:**
{{
  "strategy": "Extract Method",
  "description": "Single line description of the refactoring approach",
  "files_to_modify": ["{smell.file_path}"],
  "files_to_create": ["NewClassName.java"],
  "estimated_complexity_reduction": 30,
  "estimated_loc_reduction": 50,
  "risks": ["Risk 1 in single line", "Risk 2 in single line"],
  "testing_recommendations": ["Test 1 in single line", "Test 2 in single line"]
}}

**Available Strategies:**
- Extract Method: Break down long methods
- Extract Class: Split responsibilities into separate classes
- Move Method: Relocate methods to appropriate classes
- Replace Conditional with Polymorphism: Eliminate switch statements
- Introduce Parameter Object: Reduce parameter lists
- Inline Method: Simplify overly abstracted code

Generate the refactoring plan now (ONLY JSON, NO markdown):"""
        
        try:
            response = self.llm.generate(prompt, temperature=0.3, max_tokens=2000)
            plan_data = self._parse_json_response(response)
            
            return RefactoringPlan(
                smell_id=smell.smell_id,
                strategy=plan_data.get('strategy', 'Extract Method'),
                description=plan_data.get('description', 'Refactor to improve code quality'),
                files_to_modify=plan_data.get('files_to_modify', [smell.file_path]),
                files_to_create=plan_data.get('files_to_create', []),
                estimated_complexity_reduction=plan_data.get('estimated_complexity_reduction', 10),
                estimated_loc_reduction=plan_data.get('estimated_loc_reduction', 20),
                risks=plan_data.get('risks', ['Manual review recommended']),
                testing_recommendations=plan_data.get('testing_recommendations', ['Test all affected functionality'])
            )
        except Exception as e:
            self.logger.error(f"Failed to create refactoring plan: {e}")
            # Return safe default plan
            return RefactoringPlan(
                smell_id=smell.smell_id,
                strategy="Extract Method",
                description="Auto-generated refactoring plan due to parsing error",
                files_to_modify=[smell.file_path],
                files_to_create=[],
                estimated_complexity_reduction=10,
                estimated_loc_reduction=20,
                risks=["Automated plan - review carefully"],
                testing_recommendations=["Manual testing required", "Verify functionality preserved"]
            )
    
    def generate_refactored_code(self, smell: CodeSmell, 
                                 plan: RefactoringPlan, 
                                 original_code: str) -> RefactoringResult:
        """Generate refactored code using LLM"""
        self.logger.info(f"Generating refactored code for smell: {smell.smell_id}")
        
        prompt = f"""You are an expert Java developer performing code refactoring.

**Refactoring Task:**
- Smell Type: {smell.smell_type.value}
- Strategy: {plan.strategy}
- Plan: {plan.description}

**Original Code:**
```java
{original_code[:3000]}
```

**CRITICAL INSTRUCTIONS:**
1. Apply the refactoring strategy: {plan.strategy}
2. Preserve all functionality - NO behavioral changes
3. Follow Java best practices and conventions
4. Add appropriate JavaDoc comments
5. Ensure code compiles and is syntactically correct

**CRITICAL JSON RULES:**
- Respond with ONLY valid JSON (no markdown, no explanations)
- ALL string values MUST be single-line
- For code content, use \\n for newlines INSIDE the JSON string
- Escape all quotes and special characters properly
- NO line breaks within JSON string values

**Required Output Format:**
{{
  "refactored_files": [
    {{
      "path": "path/to/file.java",
      "content": "Complete Java code with \\n for line breaks"
    }}
  ],
  "new_files": [
    {{
      "path": "path/to/NewClass.java",
      "content": "Complete Java code with \\n for line breaks"
    }}
  ],
  "summary": "Brief single-line description of changes"
}}

**IMPORTANT:**
- Include COMPLETE, compilable Java code in content fields
- Use proper \\n escape sequences for newlines in code
- Maintain original package declarations and imports
- Do NOT truncate or abbreviate the code

Generate the refactored code now (ONLY JSON):"""
        
        try:
            response = self.llm.generate(prompt, temperature=0.4, max_tokens=6000)
            refactored_data = self._parse_json_response(response)
            
            # Process refactored files
            refactored_files = []
            for file_data in refactored_data.get('refactored_files', []):
                # Unescape newlines if they're escaped
                content = file_data.get('content', '')
                if '\\n' in content and '\n' not in content:
                    content = content.replace('\\n', '\n')
                
                refactored_files.append({
                    'path': file_data.get('path', smell.file_path),
                    'content': content
                })
            
            for file_data in refactored_data.get('new_files', []):
                content = file_data.get('content', '')
                if '\\n' in content and '\n' not in content:
                    content = content.replace('\\n', '\n')
                    
                refactored_files.append({
                    'path': file_data.get('path', 'NewClass.java'),
                    'content': content
                })
            
            # If no files generated, use original with comment
            if not refactored_files:
                refactored_files = [{
                    'path': smell.file_path,
                    'content': f"// Refactoring suggested but code generation incomplete\n{original_code}"
                }]
            
            # Calculate metrics for refactored code
            if refactored_files:
                refactored_content = refactored_files[0]['content']
                analyzer = CodeAnalyzer(self.llm, self.logger)
                metrics_after = analyzer.calculate_metrics(refactored_content, smell.file_path)
            else:
                metrics_after = smell.metrics
            
            # Calculate improvements
            improvements = {
                'complexity_reduction': smell.metrics.cyclomatic_complexity - metrics_after.cyclomatic_complexity,
                'loc_reduction': smell.metrics.lines_of_code - metrics_after.lines_of_code,
                'complexity_percentage': ((smell.metrics.cyclomatic_complexity - metrics_after.cyclomatic_complexity) / 
                                        max(smell.metrics.cyclomatic_complexity, 1)) * 100
            }
            
            return RefactoringResult(
                smell_id=smell.smell_id,
                original_file=smell.file_path,
                refactored_files=refactored_files,
                metrics_before=smell.metrics,
                metrics_after=metrics_after,
                success=bool(refactored_files),
                refactoring_strategy=plan.strategy,
                improvements=improvements,
                warnings=plan.risks
            )
            
        except Exception as e:
            self.logger.error(f"Failed to generate refactored code: {e}")
            return RefactoringResult(
                smell_id=smell.smell_id,
                original_file=smell.file_path,
                refactored_files=[],
                metrics_before=smell.metrics,
                metrics_after=smell.metrics,
                success=False,
                refactoring_strategy=plan.strategy,
                improvements={},
                error_message=str(e)
            )
    
    def _parse_json_response(self, response: str) -> Dict:
        """Parse JSON from LLM response with aggressive recovery"""
        response = response.strip()
        
        # Remove markdown code blocks
        if '```json' in response:
            response = response.split('```json')[1].split('```')[0]
        elif '```' in response:
            parts = response.split('```')
            if len(parts) >= 2:
                response = parts[1]
        
        response = response.strip()
        
        # Clean common JSON issues
        response = response.replace(',\n}', '\n}').replace(',\n]', '\n]')
        response = response.replace(',}', '}').replace(',]', ']')
        
        # Fix unterminated strings by escaping newlines within strings
        import re
        
        # Try multiple parsing strategies
        strategies = [
            lambda r: json.loads(r),  # Direct parse
            lambda r: json.loads(re.sub(r'\n(?=[^"]*"(?:[^"]*"[^"]*")*[^"]*$)', '\\n', r)),  # Escape newlines in strings
            lambda r: json.loads(r.replace('\n', ' ')),  # Remove all newlines
            lambda r: json.loads(r[r.find('{'):r.rfind('}')+1]),  # Extract {...} only
        ]
        
        last_error = None
        for strategy in strategies:
            try:
                return strategy(response)
            except Exception as e:
                last_error = e
                continue
        
        # If all strategies fail, try to manually construct a safe response
        try:
            # Extract key-value pairs manually
            result = {}
            
            # Look for strategy
            strategy_match = re.search(r'"strategy"\s*:\s*"([^"]*)"', response)
            if strategy_match:
                result['strategy'] = strategy_match.group(1)
            else:
                result['strategy'] = 'Extract Method'
            
            # Look for description
            desc_match = re.search(r'"description"\s*:\s*"([^"]*)"', response)
            if desc_match:
                result['description'] = desc_match.group(1)
            else:
                result['description'] = 'Refactor to improve code quality'
            
            # Add safe defaults
            result['files_to_modify'] = []
            result['files_to_create'] = []
            result['estimated_complexity_reduction'] = 10
            result['estimated_loc_reduction'] = 20
            result['risks'] = ['Manual review required']
            result['testing_recommendations'] = ['Test all affected functionality']
            
            if result:
                return result
        except:
            pass
        
        # Ultimate fallback
        raise last_error if last_error else ValueError("Could not parse JSON")


# ============================================================================
# Pull Request Generator (PR Creation Phase)
# ============================================================================

class GitHubPRGenerator:
    """Handles Git operations and PR creation"""
    
    def __init__(self, github_token: str, repo_owner: str, 
                 repo_name: str, logger: Logger):
        self.token = github_token
        self.repo_owner = repo_owner
        self.repo_name = repo_name
        self.logger = logger
        self.api_base = f"https://api.github.com/repos/{repo_owner}/{repo_name}"
        self.llm = LLMInterface(use_gemini=Config.USE_GEMINI)
        self.base_branch = 'main'  # Will be detected dynamically
    
    def create_branch(self, branch_name: str) -> bool:
        """Create a new Git branch"""
        self.logger.info(f"Creating branch: {branch_name}")
        
        try:
            os.chdir(Config.LOCAL_REPO_PATH)
            
            # Detect the main branch (main or master)
            result = subprocess.run(['git', 'branch', '-r'], 
                                  capture_output=True, text=True)
            branches = result.stdout
            
            if 'origin/main' in branches:
                self.base_branch = 'main'
            elif 'origin/master' in branches:
                self.base_branch = 'master'
            else:
                # Get current branch as fallback
                result = subprocess.run(['git', 'branch', '--show-current'],
                                      capture_output=True, text=True)
                self.base_branch = result.stdout.strip() or 'main'
            
            self.logger.info(f"Using base branch: {self.base_branch}")
            
            # Try to checkout base branch
            subprocess.run(['git', 'checkout', self.base_branch], 
                         capture_output=True)
            
            # Pull latest (ignore errors)
            subprocess.run(['git', 'pull'], capture_output=True)
            
            # Create new branch (or checkout if exists)
            result = subprocess.run(['git', 'checkout', '-b', branch_name],
                                  capture_output=True, text=True)
            
            if result.returncode == 0:
                self.logger.success(f"Branch created: {branch_name}")
                return True
            elif 'already exists' in result.stderr:
                # Branch exists, just check it out
                subprocess.run(['git', 'checkout', branch_name],
                             capture_output=True)
                self.logger.success(f"Switched to existing branch: {branch_name}")
                return True
            else:
                self.logger.error(f"Failed to create branch: {result.stderr}")
                return False
                
        except Exception as e:
            self.logger.error(f"Error creating branch: {e}")
            return False
        finally:
            os.chdir('..')
    
    def save_refactored_files(self, results: List[RefactoringResult]) -> bool:
        """Save refactored files to repository"""
        self.logger.info("Saving refactored files")
        
        try:
            os.chdir(Config.LOCAL_REPO_PATH)
            
            for result in results:
                if not result.success:
                    continue
                
                for file_data in result.refactored_files:
                    file_path = file_data['path']
                    content = file_data['content']
                    
                    # Ensure directory exists
                    os.makedirs(os.path.dirname(file_path), exist_ok=True)
                    
                    # Save file
                    with open(file_path, 'w', encoding='utf-8') as f:
                        f.write(content)
                    
                    self.logger.info(f"  Saved: {os.path.basename(file_path)}")
            
            return True
            
        except Exception as e:
            self.logger.error(f"Error saving files: {e}")
            return False
        finally:
            os.chdir('..')
    
    def commit_changes(self, commit_message: str) -> bool:
        """Commit changes to Git"""
        self.logger.info("Committing changes")
        
        try:
            os.chdir(Config.LOCAL_REPO_PATH)
            
            # Stage all changes
            subprocess.run(['git', 'add', '.'], check=True)
            
            # Commit
            result = subprocess.run(
                ['git', 'commit', '-m', commit_message],
                capture_output=True, text=True
            )
            
            if result.returncode == 0:
                self.logger.success("Changes committed")
                return True
            else:
                # Check if no changes
                if 'nothing to commit' in result.stdout:
                    self.logger.warning("No changes to commit")
                    return False
                self.logger.error(f"Commit failed: {result.stderr}")
                return False
                
        except Exception as e:
            self.logger.error(f"Error committing: {e}")
            return False
        finally:
            os.chdir('..')
    
    def push_branch(self, branch_name: str) -> bool:
        """Push branch to remote"""
        self.logger.info(f"Pushing branch: {branch_name}")
        
        try:
            os.chdir(Config.LOCAL_REPO_PATH)
            
            result = subprocess.run(
                ['git', 'push', '-u', 'origin', branch_name],
                capture_output=True, text=True
            )
            
            if result.returncode == 0:
                self.logger.success("Branch pushed successfully")
                return True
            else:
                self.logger.error(f"Push failed: {result.stderr}")
                return False
                
        except Exception as e:
            self.logger.error(f"Error pushing: {e}")
            return False
        finally:
            os.chdir('..')
    
    def generate_pr_description(self, smells: List[CodeSmell], 
                               results: List[RefactoringResult]) -> str:
        """Generate comprehensive PR description using LLM"""
        self.logger.info("Generating PR description")
        
        # Prepare smell summary
        smells_summary = []
        for smell in smells[:10]:
            smells_summary.append({
                'type': smell.smell_type.value,
                'file': os.path.basename(smell.file_path),
                'severity': smell.severity.value,
                'description': smell.description
            })
        
        # Prepare refactoring summary
        refactoring_summary = []
        for result in results:
            if result.success:
                refactoring_summary.append({
                    'file': os.path.basename(result.original_file),
                    'strategy': result.refactoring_strategy,
                    'improvements': result.improvements
                })
        
        prompt = f"""Create a professional GitHub Pull Request description for automated code refactoring.

**Detected Design Smells ({len(smells)} total):**
{json.dumps(smells_summary, indent=2)}

**Refactorings Applied ({len(results)} total, {sum(1 for r in results if r.success)} successful):**
{json.dumps(refactoring_summary, indent=2)}

**Requirements:**
1. Create a compelling title (one line)
2. Provide executive summary
3. List all detected smells with details
4. Describe each refactoring with metrics
5. Show before/after comparisons
6. List potential risks
7. Add testing recommendations
8. Use proper Markdown formatting

**Format:**
```markdown
# [Title]

## 📋 Summary
[Brief overview]

## 🔍 Detected Design Smells
[Detailed list with severity and location]

## ✨ Refactorings Applied
[Each refactoring with strategy and improvements]

## 📊 Metrics Improvements
[Before/after comparison table]

## ⚠️ Potential Risks
[List of risks]

## 🧪 Testing Recommendations
[Testing guidelines]

## 📝 Review Checklist
- [ ] Code compiles without errors
- [ ] All tests pass
- [ ] No functional changes
- [ ] Code quality improved
```

Generate the PR description now:"""
        
        try:
            response = self.llm.generate(prompt, temperature=0.5, max_tokens=4000)
            return response
        except Exception as e:
            self.logger.error(f"Failed to generate PR description: {e}")
            return self._generate_default_pr_description(smells, results)
    
    def _generate_default_pr_description(self, smells: List[CodeSmell], 
                                        results: List[RefactoringResult]) -> str:
        """Generate default PR description if LLM fails"""
        
        smell_list = "\n".join([
            f"- **{s.smell_type.value}** in `{os.path.basename(s.file_path)}` "
            f"({s.severity.value}): {s.description}"
            for s in smells[:10]
        ])
        
        refactoring_list = "\n".join([
            f"- `{os.path.basename(r.original_file)}`: {r.refactoring_strategy} "
            f"(Complexity: {r.improvements.get('complexity_reduction', 0):.0f} reduction)"
            for r in results if r.success
        ])
        
        return f"""# 🔧 Automated Code Quality Improvements

## 📋 Summary
This PR contains automated refactorings to address {len(smells)} detected design smells.
{len([r for r in results if r.success])} refactorings were successfully applied.

## 🔍 Detected Design Smells
{smell_list}

## ✨ Refactorings Applied
{refactoring_list}

## 📊 Metrics Improvements
- Total Smells Addressed: {len(smells)}
- Files Refactored: {len(set(r.original_file for r in results if r.success))}
- Estimated Complexity Reduction: {sum(r.improvements.get('complexity_reduction', 0) for r in results if r.success):.0f}

## ⚠️ Potential Risks
- Automated refactoring may require manual review
- Ensure all tests pass before merging
- Verify functional equivalence

## 🧪 Testing Recommendations
- Run full test suite
- Manual code review recommended
- Check for any behavioral changes

## 📝 Review Checklist
- [ ] Code compiles without errors
- [ ] All tests pass
- [ ] No functional changes introduced
- [ ] Code quality metrics improved

---
*Generated by Automated Refactoring Pipeline on {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}*
"""
    
    def create_pull_request(self, branch_name: str, title: str, 
                           description: str) -> Optional[str]:
        """Create PR on GitHub"""
        self.logger.info(f"Creating Pull Request: {title}")
        
        url = f"{self.api_base}/pulls"
        headers = {
            "Authorization": f"token {self.token}",
            "Accept": "application/vnd.github.v3+json"
        }
        
        data = {
            "title": title,
            "body": description,
            "head": branch_name,
            "base": self.base_branch  # Use detected base branch
        }
        
        try:
            response = requests.post(url, headers=headers, json=data)
            
            if response.status_code == 201:
                pr_data = response.json()
                pr_url = pr_data['html_url']
                self.logger.success(f"PR created: {pr_url}")
                return pr_url
            else:
                self.logger.error(f"Failed to create PR: {response.status_code}")
                self.logger.error(f"Response: {response.text}")
                return None
                
        except Exception as e:
            self.logger.error(f"Error creating PR: {e}")
            return None


# ============================================================================
# Repository Manager
# ============================================================================

class RepositoryManager:
    """Manages repository cloning and updates"""
    
    def __init__(self, logger: Logger):
        self.logger = logger
    
    def setup_repository(self, repo_url: str, local_path: str) -> bool:
        """Clone or update repository"""
        
        if os.path.exists(local_path):
            self.logger.info("Repository exists, pulling latest changes")
            try:
                os.chdir(local_path)
                
                # Get current branch
                result = subprocess.run(['git', 'branch', '--show-current'], 
                                      capture_output=True, text=True)
                current_branch = result.stdout.strip()
                
                # Try to checkout main or master
                for branch in ['main', 'master']:
                    result = subprocess.run(['git', 'checkout', branch], 
                                          capture_output=True, text=True)
                    if result.returncode == 0:
                        self.logger.info(f"Checked out branch: {branch}")
                        break
                else:
                    # Stay on current branch if main/master don't exist
                    self.logger.warning(f"Could not find main/master branch, staying on {current_branch}")
                
                # Pull latest changes
                result = subprocess.run(['git', 'pull'], 
                                      capture_output=True, text=True)
                if result.returncode != 0:
                    self.logger.warning(f"Pull failed: {result.stderr}")
                    # Continue anyway - we can still analyze existing code
                
                os.chdir('..')
                self.logger.success("Repository ready")
                return True
            except Exception as e:
                self.logger.error(f"Failed to update repository: {e}")
                os.chdir('..')
                # Still return True if directory exists - we can analyze it
                return True
        else:
            self.logger.info(f"Cloning repository: {repo_url}")
            try:
                result = subprocess.run(
                    ['git', 'clone', repo_url, local_path],
                    capture_output=True, text=True
                )
                if result.returncode == 0:
                    self.logger.success("Repository cloned successfully")
                    return True
                else:
                    self.logger.error(f"Clone failed: {result.stderr}")
                    return False
            except Exception as e:
                self.logger.error(f"Error cloning repository: {e}")
                return False


# ============================================================================
# Report Generator
# ============================================================================

class ReportGenerator:
    """Generates analysis and refactoring reports"""
    
    def __init__(self, logger: Logger):
        self.logger = logger
    
    def generate_smell_report(self, smells: List[CodeSmell], 
                             output_path: str):
        """Generate comprehensive smell detection report"""
        
        report = {
            'timestamp': datetime.now().isoformat(),
            'total_smells': len(smells),
            'smells_by_severity': self._count_by_severity(smells),
            'smells_by_type': self._count_by_type(smells),
            'files_affected': len(set(s.file_path for s in smells)),
            'smells': [s.to_dict() for s in smells]
        }
        
        with open(output_path, 'w') as f:
            json.dump(report, f, indent=2)
        
        self.logger.success(f"Smell report saved: {output_path}")
    
    def generate_refactoring_report(self, results: List[RefactoringResult], 
                                   output_path: str):
        """Generate refactoring results report"""
        
        successful = [r for r in results if r.success]
        failed = [r for r in results if not r.success]
        
        report = {
            'timestamp': datetime.now().isoformat(),
            'total_attempts': len(results),
            'successful': len(successful),
            'failed': len(failed),
            'total_complexity_reduction': sum(
                r.improvements.get('complexity_reduction', 0) for r in successful
            ),
            'total_loc_reduction': sum(
                r.improvements.get('loc_reduction', 0) for r in successful
            ),
            'results': [r.to_dict() for r in results]
        }
        
        with open(output_path, 'w') as f:
            json.dump(report, f, indent=2)
        
        self.logger.success(f"Refactoring report saved: {output_path}")
    
    def _count_by_severity(self, smells: List[CodeSmell]) -> Dict[str, int]:
        """Count smells by severity"""
        counts = {'CRITICAL': 0, 'HIGH': 0, 'MEDIUM': 0, 'LOW': 0}
        for smell in smells:
            counts[smell.severity.value] += 1
        return counts
    
    def _count_by_type(self, smells: List[CodeSmell]) -> Dict[str, int]:
        """Count smells by type"""
        counts = {}
        for smell in smells:
            smell_type = smell.smell_type.value
            counts[smell_type] = counts.get(smell_type, 0) + 1
        return counts


# ============================================================================
# Main Pipeline Orchestrator
# ============================================================================

class RefactoringPipeline:
    """Main pipeline orchestrator"""
    
    def __init__(self):
        # Setup directories
        os.makedirs(Config.RESULTS_DIR, exist_ok=True)
        os.makedirs(Config.LOGS_DIR, exist_ok=True)
        os.makedirs(Config.REFACTORED_CODE_DIR, exist_ok=True)
        
        # Initialize logger
        log_file = os.path.join(
            Config.LOGS_DIR, 
            f"pipeline_{datetime.now().strftime('%Y%m%d_%H%M%S')}.log"
        )
        self.logger = Logger(log_file)
        
        # Validate configuration
        try:
            Config.validate()
        except ValueError as e:
            self.logger.error(f"Configuration error: {e}")
            sys.exit(1)
        
        # Initialize components
        self.llm = LLMInterface(use_gemini=Config.USE_GEMINI)
        self.analyzer = CodeAnalyzer(self.llm, self.logger)
        self.refactorer = CodeRefactorer(self.llm, self.logger)
        self.pr_generator = GitHubPRGenerator(
            Config.GITHUB_TOKEN,
            Config.REPO_OWNER,
            Config.REPO_NAME,
            self.logger
        )
        self.repo_manager = RepositoryManager(self.logger)
        self.report_generator = ReportGenerator(self.logger)
    
    def run(self):
        """Execute the complete pipeline"""
        
        self.print_banner()
        
        try:
            # Phase 1: Setup Repository
            if not self._phase_setup():
                return
            
            # Phase 2: Detection
            smells = self._phase_detection()
            if not smells:
                self.logger.info("No design smells detected. Pipeline complete.")
                return
            
            # Phase 3: Refactoring
            results = []
            if Config.ENABLE_REFACTORING:
                results = self._phase_refactoring(smells)
            
            # Phase 4: PR Creation
            if Config.ENABLE_PR_CREATION and results:
                self._phase_pr_creation(smells, results)
            
            self.print_summary(smells, results)
            
        except KeyboardInterrupt:
            self.logger.warning("\n\n⚠️  Pipeline interrupted by user")
            self.logger.info("\nPartial results have been saved:")
            self.logger.info(f"  - Smell reports: {Config.RESULTS_DIR}")
            self.logger.info(f"  - Logs: {Config.LOGS_DIR}")
            self.logger.info(f"  - Refactored code: {Config.REFACTORED_CODE_DIR}")
            self.logger.info("\nYou can resume by running the pipeline again.")
        except Exception as e:
            self.logger.error(f"\n\n❌ Pipeline failed with error: {e}")
            self.logger.info("\nCheck the log file for details:")
            if hasattr(self, 'logger') and self.logger.log_file:
                self.logger.info(f"  Log: {self.logger.log_file}")
            import traceback
            self.logger.error(f"\nFull traceback:\n{traceback.format_exc()}")
    
    def _phase_setup(self) -> bool:
        """Phase 1: Setup repository"""
        self.logger.info("\n" + "=" * 80)
        self.logger.info("PHASE 1: REPOSITORY SETUP")
        self.logger.info("=" * 80)
        
        success = self.repo_manager.setup_repository(
            Config.REPO_URL,
            Config.LOCAL_REPO_PATH
        )
        
        if success:
            self.logger.success("✓ Repository ready")
        else:
            self.logger.error("✗ Repository setup failed")
        
        return success
    
    def _phase_detection(self) -> List[CodeSmell]:
        """Phase 2: Design smell detection"""
        self.logger.info("\n" + "=" * 80)
        self.logger.info("PHASE 2: DESIGN SMELL DETECTION")
        self.logger.info("=" * 80)
        
        # Scan repository
        files = self.analyzer.scan_repository(Config.LOCAL_REPO_PATH)
        
        if not files:
            self.logger.warning("No Java files found")
            return []
        
        # Analyze files
        all_smells = []
        for i, file_path in enumerate(files, 1):
            self.logger.info(f"\n[{i}/{len(files)}] Analyzing: {os.path.basename(file_path)}")
            smells = self.analyzer.analyze_file(file_path)
            all_smells.extend(smells)
        
        # Sort by severity and confidence
        all_smells.sort(
            key=lambda s: (
                {'CRITICAL': 4, 'HIGH': 3, 'MEDIUM': 2, 'LOW': 1}[s.severity.value],
                s.confidence_score
            ),
            reverse=True
        )
        
        # Generate report
        report_path = os.path.join(Config.RESULTS_DIR, 'smell_detection_report.json')
        self.report_generator.generate_smell_report(all_smells, report_path)
        
        self.logger.success(f"\n✓ Detection complete: {len(all_smells)} smells found")
        return all_smells
    
    def _phase_refactoring(self, smells: List[CodeSmell]) -> List[RefactoringResult]:
        """Phase 3: Code refactoring"""
        self.logger.info("\n" + "=" * 80)
        self.logger.info("PHASE 3: CODE REFACTORING")
        self.logger.info("=" * 80)
        
        # Select top smells to refactor
        smells_to_refactor = smells[:Config.MAX_SMELLS_TO_REFACTOR]
        self.logger.info(f"Refactoring top {len(smells_to_refactor)} smells")
        
        results = []
        for i, smell in enumerate(smells_to_refactor, 1):
            self.logger.info(f"\n[{i}/{len(smells_to_refactor)}] Refactoring smell: {smell.smell_id}")
            self.logger.info(f"  Type: {smell.smell_type.value}")
            self.logger.info(f"  File: {os.path.basename(smell.file_path)}")
            self.logger.info(f"  Severity: {smell.severity.value}")
            
            try:
                # Read original code
                with open(smell.file_path, 'r', encoding='utf-8') as f:
                    original_code = f.read()
                
                # Create refactoring plan
                plan = self.refactorer.create_refactoring_plan(smell, original_code)
                self.logger.info(f"  Strategy: {plan.strategy}")
                
                # Generate refactored code
                result = self.refactorer.generate_refactored_code(
                    smell, plan, original_code
                )
                
                if result.success:
                    self.logger.success(f"  ✓ Refactoring successful")
                    self.logger.info(f"    Complexity reduction: {result.improvements.get('complexity_reduction', 0):.0f}")
                else:
                    self.logger.error(f"  ✗ Refactoring failed: {result.error_message}")
                
                results.append(result)
                
                # Save refactored code to separate directory for documentation
                self._save_refactored_code_for_documentation(result, i)
                
            except Exception as e:
                self.logger.error(f"  ✗ Error: {e}")
                continue
        
        # Generate refactoring report
        report_path = os.path.join(Config.RESULTS_DIR, 'refactoring_report.json')
        self.report_generator.generate_refactoring_report(results, report_path)
        
        successful = [r for r in results if r.success]
        self.logger.success(f"\n✓ Refactoring complete: {len(successful)}/{len(results)} successful")
        
        return results
    
    def _save_refactored_code_for_documentation(self, result: RefactoringResult, index: int):
        """Save refactored code to documentation directory (not applied to repo)"""
        if not result.success:
            return
        
        doc_dir = os.path.join(Config.REFACTORED_CODE_DIR, f"smell_{result.smell_id}")
        os.makedirs(doc_dir, exist_ok=True)
        
        # Save original code
        try:
            with open(result.original_file, 'r') as f:
                original_content = f.read()
            
            with open(os.path.join(doc_dir, 'ORIGINAL.java'), 'w') as f:
                f.write(original_content)
        except:
            pass
        
        # Save refactored code
        for i, file_data in enumerate(result.refactored_files):
            filename = f"REFACTORED_{i+1}_{os.path.basename(file_data['path'])}"
            with open(os.path.join(doc_dir, filename), 'w') as f:
                f.write(file_data['content'])
        
        # Save metadata
        metadata = {
            'smell_id': result.smell_id,
            'original_file': result.original_file,
            'strategy': result.refactoring_strategy,
            'improvements': result.improvements,
            'metrics_before': result.metrics_before.to_dict(),
            'metrics_after': result.metrics_after.to_dict() if result.metrics_after else None
        }
        
        with open(os.path.join(doc_dir, 'METADATA.json'), 'w') as f:
            json.dump(metadata, f, indent=2)
        
        self.logger.info(f"  Documentation saved: {doc_dir}")
    
    def _phase_pr_creation(self, smells: List[CodeSmell], 
                          results: List[RefactoringResult]):
        """Phase 4: Pull request creation"""
        self.logger.info("\n" + "=" * 80)
        self.logger.info("PHASE 4: PULL REQUEST CREATION")
        self.logger.info("=" * 80)
        
        successful_results = [r for r in results if r.success]
        
        if not successful_results:
            self.logger.warning("No successful refactorings to include in PR")
            return
        
        # Create branch
        branch_name = f"refactor/automated-{datetime.now().strftime('%Y%m%d-%H%M%S')}"
        if not self.pr_generator.create_branch(branch_name):
            self.logger.error("Failed to create branch")
            return
        
        # Save refactored files
        if not self.pr_generator.save_refactored_files(successful_results):
            self.logger.error("Failed to save refactored files")
            return
        
        # Commit changes
        commit_msg = f"refactor: Automated fixes for {len(successful_results)} design smells"
        if not self.pr_generator.commit_changes(commit_msg):
            self.logger.error("Failed to commit changes")
            return
        
        # Push branch
        if not self.pr_generator.push_branch(branch_name):
            self.logger.error("Failed to push branch")
            return
        
        # Generate PR description
        pr_description = self.pr_generator.generate_pr_description(
            smells, successful_results
        )
        
        # Create PR
        pr_url = self.pr_generator.create_pull_request(
            branch_name,
            f"[Automated] Refactoring: {len(successful_results)} Design Smells Fixed",
            pr_description
        )
        
        if pr_url:
            self.logger.success(f"\n✓ Pull Request created: {pr_url}")
        else:
            self.logger.error("Failed to create Pull Request")
    
    def print_banner(self):
        """Print pipeline banner"""
        banner = f"""
{'=' * 80}
   AUTOMATED REFACTORING PIPELINE - TASK 3C
{'=' * 80}
   Repository: {Config.REPO_URL}
   LLM: {'Gemini ' + Config.GEMINI_MODEL if Config.USE_GEMINI else 'OpenAI ' + Config.OPENAI_MODEL}
   Timestamp: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}
   
   Pipeline Configuration:
   - Detection: {'ENABLED' if Config.ENABLE_DETECTION else 'DISABLED'}
   - Refactoring: {'ENABLED' if Config.ENABLE_REFACTORING else 'DISABLED'}
   - PR Creation: {'ENABLED' if Config.ENABLE_PR_CREATION else 'DISABLED'}
   - Max Smells to Refactor: {Config.MAX_SMELLS_TO_REFACTOR}
   - Max Files to Analyze: {Config.MAX_FILES_TO_ANALYZE}
{'=' * 80}
"""
        print(banner)
        self.logger.info(banner)
    
    def print_summary(self, smells: List[CodeSmell], 
                     results: List[RefactoringResult]):
        """Print pipeline summary"""
        successful_refactorings = [r for r in results if r.success]
        
        summary = f"""
{'=' * 80}
   PIPELINE EXECUTION SUMMARY
{'=' * 80}
   
   Detection Results:
   - Total Smells Detected: {len(smells)}
   - Critical: {sum(1 for s in smells if s.severity == Severity.CRITICAL)}
   - High: {sum(1 for s in smells if s.severity == Severity.HIGH)}
   - Medium: {sum(1 for s in smells if s.severity == Severity.MEDIUM)}
   - Low: {sum(1 for s in smells if s.severity == Severity.LOW)}
   
   Refactoring Results:
   - Attempted: {len(results)}
   - Successful: {len(successful_refactorings)}
   - Failed: {len(results) - len(successful_refactorings)}
   - Total Complexity Reduction: {sum(r.improvements.get('complexity_reduction', 0) for r in successful_refactorings):.0f}
   - Total LOC Reduction: {sum(r.improvements.get('loc_reduction', 0) for r in successful_refactorings):.0f}
   
   Output Files:
   - Smell Report: {os.path.join(Config.RESULTS_DIR, 'smell_detection_report.json')}
   - Refactoring Report: {os.path.join(Config.RESULTS_DIR, 'refactoring_report.json')}
   - Refactored Code Documentation: {Config.REFACTORED_CODE_DIR}
   
{'=' * 80}
"""
        print(summary)
        self.logger.info(summary)


# ============================================================================
# Entry Point
# ============================================================================

def main():
    """Main entry point"""
    print("\n🚀 Starting Automated Refactoring Pipeline (Task 3C)\n")
    
    # Create and run pipeline
    pipeline = RefactoringPipeline()
    pipeline.run()
    
    print("\n✅ Pipeline execution complete!\n")


if __name__ == "__main__":
    main()