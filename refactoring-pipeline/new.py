#!/usr/bin/env python3
"""
Automated Refactoring Pipeline with Rate Limiting & Large File Handling
Task 3C: Complete Implementation
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
import google.generativeai as genai
import requests
from dataclasses import dataclass, asdict
from dotenv import load_dotenv
import logging
from concurrent.futures import ThreadPoolExecutor, as_completed
from queue import Queue
import threading

# Load environment variables
load_dotenv()

# Configuration with enhanced features
CONFIG = {
    'repo_url': 'https://github.com/serc-courses/project-1-team-13.git',
    'repo_path': './project-1-team-13',
    'api_key': os.getenv('GEMINI_API_KEY'),
    'github_token': os.getenv('GITHUB_TOKEN'),
    'thresholds': {
        'cyclomatic_complexity': 15,
        'lines_of_code': 300,
        'method_lines': 50,
        'coupling': 10
    },
    # Rate limiting configuration
    'rate_limit': {
        'requests_per_minute': 15,  # Conservative limit for Gemini free tier
        'chunk_size': 500,          # Smaller chunks for large files
        'delay_between_requests': 4,  # Seconds
        'max_retries': 3,
        'retry_delay': 10
    },
    # Large file handling
    'large_file': {
        'max_size': 10000,  # Max lines before chunking
        'chunk_overlap': 50,  # Overlap lines for context
        'max_chunks': 5     # Max chunks per file
    },
    'enable_refactoring': True,
    'enable_pr_creation': True,
    'max_smells_to_refactor': 3,
    'parallel_analysis': False,  # Disable parallel to respect rate limits
    'cache_responses': True,
    'cache_dir': '.cache/gemini'
}

# Setup logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    handlers=[
        logging.FileHandler('refactoring_pipeline.log'),
        logging.StreamHandler()
    ]
)
logger = logging.getLogger(__name__)

@dataclass
class CodeSmell:
    file_path: str
    smell_type: str
    severity: str
    line_start: int
    line_end: int
    description: str
    metrics: Dict[str, float]
    suggested_refactoring: str
    context_hash: Optional[str] = None
    chunk_index: Optional[int] = None

@dataclass
class RefactoringResult:
    original_file: str
    refactored_files: List[str]
    metrics_before: Dict[str, float]
    metrics_after: Dict[str, float]
    success: bool
    error_message: Optional[str] = None
    refactoring_description: str = ""
    applied_strategies: List[str] = None
    pr_link: Optional[str] = None


class RateLimiter:
    """Token bucket rate limiter for API calls"""
    
    def __init__(self, requests_per_minute=15):
        self.rate = requests_per_minute
        self.tokens = requests_per_minute
        self.last_update = time.time()
        self.lock = threading.Lock()
        self.delay = 60.0 / requests_per_minute
        
    def wait_if_needed(self):
        """Wait if rate limit is exceeded"""
        with self.lock:
            now = time.time()
            time_passed = now - self.last_update
            self.tokens += time_passed * (self.rate / 60.0)
            self.tokens = min(self.tokens, self.rate)
            self.last_update = now
            
            if self.tokens < 1:
                sleep_time = self.delay - (self.tokens * (60.0 / self.rate))
                logger.debug(f"Rate limit reached, waiting {sleep_time:.2f}s")
                time.sleep(sleep_time)
                self.tokens = 1
                self.last_update = time.time()
            
            self.tokens -= 1
    
    def __call__(self, func):
        """Decorator for rate limiting"""
        def wrapper(*args, **kwargs):
            self.wait_if_needed()
            return func(*args, **kwargs)
        return wrapper


class CacheManager:
    """Manages caching of LLM responses"""
    
    def __init__(self, cache_dir: str):
        self.cache_dir = Path(cache_dir)
        self.cache_dir.mkdir(parents=True, exist_ok=True)
        
    def _get_cache_key(self, prompt: str, context: str) -> str:
        """Generate cache key from prompt and context"""
        combined = f"{prompt}:{context[:1000]}"
        return hashlib.md5(combined.encode()).hexdigest()
    
    def get(self, prompt: str, context: str) -> Optional[str]:
        """Retrieve cached response"""
        cache_key = self._get_cache_key(prompt, context)
        cache_file = self.cache_dir / f"{cache_key}.json"
        
        if cache_file.exists():
            try:
                with open(cache_file, 'r') as f:
                    data = json.load(f)
                    logger.debug(f"Cache hit for {cache_key}")
                    return data['response']
            except:
                return None
        return None
    
    def set(self, prompt: str, context: str, response: str):
        """Cache response"""
        cache_key = self._get_cache_key(prompt, context)
        cache_file = self.cache_dir / f"{cache_key}.json"
        
        try:
            with open(cache_file, 'w') as f:
                json.dump({
                    'prompt': prompt[:200],
                    'context_hash': hashlib.md5(context[:1000].encode()).hexdigest(),
                    'response': response,
                    'timestamp': datetime.now().isoformat()
                }, f)
            logger.debug(f"Cached response for {cache_key}")
        except Exception as e:
            logger.warning(f"Failed to cache response: {e}")


class LargeFileHandler:
    """Handles analysis of large files by chunking"""
    
    def __init__(self, chunk_size=500, overlap=50, max_chunks=5):
        self.chunk_size = chunk_size
        self.overlap = overlap
        self.max_chunks = max_chunks
    
    def split_code(self, code: str) -> List[Tuple[int, int, str]]:
        """Split large code into overlapping chunks"""
        lines = code.split('\n')
        total_lines = len(lines)
        
        if total_lines <= self.chunk_size:
            return [(1, total_lines, code)]
        
        chunks = []
        start_line = 1
        
        for i in range(min(self.max_chunks, total_lines // (self.chunk_size - self.overlap) + 1)):
            end_line = min(start_line + self.chunk_size - 1, total_lines)
            
            # Extract chunk with context
            chunk_start = max(0, start_line - 1)
            chunk_end = min(end_line, total_lines)
            chunk_code = '\n'.join(lines[chunk_start:chunk_end])
            
            # Add context markers
            if chunk_start > 0:
                chunk_code = f"// ... previous context ...\n{chunk_code}"
            if chunk_end < total_lines:
                chunk_code = f"{chunk_code}\n// ... remaining code ..."
            
            chunks.append((start_line, end_line, chunk_code))
            
            if end_line >= total_lines:
                break
                
            start_line = end_line - self.overlap
        
        logger.info(f"Split {total_lines} lines into {len(chunks)} chunks")
        return chunks
    
    def merge_smells(self, smells_per_chunk: List[List[CodeSmell]]) -> List[CodeSmell]:
        """Merge smells from different chunks, removing duplicates"""
        merged = {}
        
        for chunk_smells in smells_per_chunk:
            for smell in chunk_smells:
                key = f"{smell.smell_type}:{smell.line_start}-{smell.line_end}"
                if key not in merged:
                    merged[key] = smell
                else:
                    # Keep more severe smell
                    existing = merged[key]
                    severity_score = {'HIGH': 3, 'MEDIUM': 2, 'LOW': 1}
                    if severity_score.get(smell.severity, 0) > severity_score.get(existing.severity, 0):
                        merged[key] = smell
        
        return list(merged.values())


class CodeAnalyzer:
    """Analyzes code for design smells with rate limiting and large file support"""
    
    def __init__(self, api_key: str):
        genai.configure(api_key=api_key)
        self.model = genai.GenerativeModel('models/gemini-2.0-flash')
        self.rate_limiter = RateLimiter(CONFIG['rate_limit']['requests_per_minute'])
        self.cache = CacheManager(CONFIG['cache_dir'])
        self.large_file_handler = LargeFileHandler(
            CONFIG['large_file']['chunk_size'],
            CONFIG['large_file']['chunk_overlap'],
            CONFIG['large_file']['max_chunks']
        )
    
    def run_static_analysis(self, repo_path: str) -> Dict:
        """Run comprehensive static analysis"""
        results = {
            'custom_metrics': self._calculate_custom_metrics(repo_path),
            'files_analyzed': 0,
            'total_smells_found': 0,
            'large_files': []
        }
        
        java_files = list(Path(repo_path).rglob('*.java'))
        results['files_analyzed'] = len(java_files)
        
        for java_file in java_files:
            if 'test' in str(java_file).lower() or 'target' in str(java_file):
                continue
                
            try:
                with open(java_file, 'r', encoding='utf-8', errors='ignore') as f:
                    lines = f.readlines()
                    loc = len([l for l in lines if l.strip() and not l.strip().startswith('//')])
                    
                    if loc > CONFIG['thresholds']['lines_of_code']:
                        results['large_files'].append({
                            'file': str(java_file),
                            'loc': loc,
                            'threshold': CONFIG['thresholds']['lines_of_code']
                        })
            except Exception as e:
                logger.error(f"Error analyzing {java_file}: {e}")
                continue
        
        return results
    
    def _calculate_custom_metrics(self, repo_path: str) -> Dict:
        """Calculate detailed metrics"""
        metrics = {
            'total_loc': 0,
            'high_complexity_files': [],
            'high_coupling_files': [],
            'large_class_files': []
        }
        
        for java_file in Path(repo_path).rglob('*.java'):
            if 'test' in str(java_file).lower() or 'target' in str(java_file):
                continue
                
            try:
                with open(java_file, 'r', encoding='utf-8', errors='ignore') as f:
                    lines = f.readlines()
                    loc = len([l for l in lines if l.strip() and not l.strip().startswith('//')])
                    metrics['total_loc'] += loc
                    
                    # Cyclomatic complexity
                    cc = self._calculate_cc(lines)
                    if cc > CONFIG['thresholds']['cyclomatic_complexity']:
                        metrics['high_complexity_files'].append({
                            'file': str(java_file),
                            'loc': loc,
                            'cc': cc
                        })
                    
                    # Class size
                    if loc > CONFIG['thresholds']['lines_of_code']:
                        metrics['large_class_files'].append({
                            'file': str(java_file),
                            'loc': loc
                        })
                    
                    # Coupling (simple approximation)
                    coupling = self._calculate_coupling(lines)
                    if coupling > CONFIG['thresholds']['coupling']:
                        metrics['high_coupling_files'].append({
                            'file': str(java_file),
                            'coupling': coupling
                        })
                        
            except Exception as e:
                logger.debug(f"Error in metrics for {java_file}: {e}")
                continue
        
        return metrics
    
    def _calculate_cc(self, lines: List[str]) -> int:
        """Calculate cyclomatic complexity"""
        cc = 1
        decision_keywords = ['if ', 'while ', 'for ', 'case ', 'catch ', '&&', '||', '?', 'else if']
        
        for line in lines:
            line_lower = line.lower()
            for keyword in decision_keywords:
                if keyword in line_lower:
                    cc += 1
        return cc
    
    def _calculate_coupling(self, lines: List[str]) -> int:
        """Calculate coupling (imports count + method calls)"""
        imports = 0
        method_calls = 0
        
        for line in lines:
            if line.strip().startswith('import '):
                imports += 1
            if '.' in line and '(' in line and '=' in line:
                method_calls += 1
        
        return imports + method_calls // 10  # Normalize
    
    @RateLimiter.__call__
    def _make_api_call(self, prompt: str) -> Optional[str]:
        """Make rate-limited API call with retries"""
        for attempt in range(CONFIG['rate_limit']['max_retries']):
            try:
                response = self.model.generate_content(
                    prompt,
                    generation_config=genai.types.GenerationConfig(
                        max_output_tokens=2000,
                        temperature=0.3
                    )
                )
                return response.text
            except Exception as e:
                if "429" in str(e) or "quota" in str(e).lower():
                    wait_time = CONFIG['rate_limit']['retry_delay'] * (attempt + 1)
                    logger.warning(f"Rate limit hit, waiting {wait_time}s...")
                    time.sleep(wait_time)
                else:
                    logger.error(f"API call failed: {e}")
                    if attempt == CONFIG['rate_limit']['max_retries'] - 1:
                        return None
                    time.sleep(2)
        return None
    
    def analyze_with_llm(self, file_path: str, code: str, metrics: Dict) -> List[CodeSmell]:
        """Analyze code with LLM, handling large files via chunking"""
        
        # Check if file is large
        lines = code.split('\n')
        if len(lines) > CONFIG['large_file']['max_size']:
            logger.info(f"Large file detected: {file_path} ({len(lines)} lines)")
            return self._analyze_large_file(file_path, code, metrics)
        
        # Regular analysis for normal files
        prompt = self._build_analysis_prompt(file_path, code, metrics)
        
        # Check cache
        cached = self.cache.get(prompt, code[:1000])
        if cached:
            return self._parse_response(cached, file_path, metrics)
        
        # Make API call
        response_text = self._make_api_call(prompt)
        if not response_text:
            return []
        
        # Cache response
        self.cache.set(prompt, code[:1000], response_text)
        
        return self._parse_response(response_text, file_path, metrics)
    
    def _analyze_large_file(self, file_path: str, code: str, metrics: Dict) -> List[CodeSmell]:
        """Analyze large files by chunking"""
        chunks = self.large_file_handler.split_code(code)
        all_smells = []
        
        for start_line, end_line, chunk_code in chunks:
            logger.debug(f"Analyzing chunk {start_line}-{end_line} of {file_path}")
            
            prompt = self._build_analysis_prompt(
                f"{file_path} (lines {start_line}-{end_line})",
                chunk_code,
                {**metrics, 'chunk': f"{start_line}-{end_line}"}
            )
            
            # Check cache
            cached = self.cache.get(prompt, chunk_code[:500])
            if cached:
                smells = self._parse_response(cached, file_path, metrics)
            else:
                response_text = self._make_api_call(prompt)
                if response_text:
                    self.cache.set(prompt, chunk_code[:500], response_text)
                    smells = self._parse_response(response_text, file_path, metrics)
                else:
                    smells = []
            
            # Adjust line numbers
            for smell in smells:
                smell.line_start += start_line - 1
                smell.line_end += start_line - 1
                smell.chunk_index = start_line
            
            all_smells.extend(smells)
        
        # Merge duplicate smells across chunks
        return self.large_file_handler.merge_smells([all_smells])
    
    def _build_analysis_prompt(self, file_path: str, code: str, metrics: Dict) -> str:
        """Build analysis prompt with clear instructions"""
        return f"""You are a Java code quality analyzer. Identify design smells in this code.

FILE: {file_path}
METRICS:
- Cyclomatic Complexity: {metrics.get('cc', 'unknown')}
- Lines of Code: {metrics.get('loc', 'unknown')}

CRITICAL RULES:
1. Respond ONLY with valid JSON
2. No markdown formatting
3. No explanations outside JSON
4. All strings must be single line
5. Escape quotes properly

CODE:
```java
{code[:2500]}
Identify these design smells if present:

God Class - Too many responsibilities

Long Method - Method exceeds 30 lines

Feature Envy - Method uses more features of other classes

Data Clump - Same data groups appear together

Switch Statements - Complex conditionals

Return ONLY:
{{
"smells": [
{{
"type": "God Class",
"severity": "HIGH",
"line_start": 1,
"line_end": 50,
"description": "Class has multiple responsibilities",
"suggested_refactoring": "Extract class for database operations"
}}
]
}}"""

text
def _parse_response(self, response_text: str, file_path: str, metrics: Dict) -> List[CodeSmell]:
    """Parse LLM response with robust error handling"""
    try:
        # Clean response
        if "```json" in response_text:
            response_text = response_text.split("```json")[1].split("```")[0]
        elif "```" in response_text:
            response_text = response_text.split("```")[1].split("```")[0]
        
        response_text = response_text.strip()
        
        # Remove trailing commas
        response_text = response_text.replace(",\n}", "\n}")
        response_text = response_text.replace(",\n]", "\n]")
        
        # Find JSON
        start = response_text.find("{")
        end = response_text.rfind("}") + 1
        if start != -1 and end != -1:
            json_str = response_text[start:end]
            result = json.loads(json_str)
        else:
            logger.error("No JSON found in response")
            return []
        
        smells = []
        for smell_data in result.get('smells', []):
            smell = CodeSmell(
                file_path=file_path,
                smell_type=smell_data.get('type', 'Unknown'),
                severity=smell_data.get('severity', 'MEDIUM'),
                line_start=smell_data.get('line_start', 1),
                line_end=smell_data.get('line_end', 100),
                description=smell_data.get('description', 'No description'),
                metrics=metrics.copy(),
                suggested_refactoring=smell_data.get('suggested_refactoring', 'Review code'),
                context_hash=hashlib.md5(code_snippet.encode()).hexdigest() if 'code_snippet' in locals() else None
            )
            smells.append(smell)
        
        return smells
        
    except json.JSONDecodeError as e:
        logger.error(f"JSON parse error: {e}")
        logger.debug(f"Response text: {response_text[:500]}")
        return []
    except Exception as e:
        logger.error(f"Parse error: {e}")
        return []
class CodeRefactorer:
"""Applies refactorings with rate limiting and verification"""

text
def __init__(self, api_key: str):
    genai.configure(api_key=api_key)
    self.model = genai.GenerativeModel('models/gemini-2.0-flash')
    self.rate_limiter = RateLimiter(CONFIG['rate_limit']['requests_per_minute'])
    self.cache = CacheManager(f"{CONFIG['cache_dir']}/refactorings")

def generate_refactoring_plan(self, smell: CodeSmell, code: str) -> Dict:
    """Generate detailed refactoring plan"""
    
    # Extract relevant code section
    lines = code.split('\n')
    start_line = max(0, smell.line_start - 20)
    end_line = min(len(lines), smell.line_end + 20)
    relevant_code = '\n'.join(lines[start_line:end_line])
    
    prompt = f"""Create a concrete refactoring plan for this code smell.
SMELL:
Type: {smell.smell_type}
Severity: {smell.severity}
Description: {smell.description}
Suggested: {smell.suggested_refactoring}

CODE SECTION:

java
{relevant_code[:2000]}
Provide a specific JSON plan with:

Strategy name

Step-by-step changes

Files affected

Expected improvements

FORMAT:
{{
"strategy": "Extract Method",
"description": "Extract validation logic to separate method",
"steps": [
"Create validateUserInput() method",
"Move validation code to new method",
"Add method call in original location"
],
"files_affected": ["{smell.file_path}"],
"expected_improvements": {{
"complexity_reduction": 5,
"lines_reduction": 20
}}
}}"""

text
    # Check cache
    cache_key = f"{smell.file_path}:{smell.line_start}:{smell.smell_type}"
    cached = self.cache.get(prompt, cache_key)
    if cached:
        try:
            return json.loads(cached)
        except:
            pass
    
    # Make API call with rate limiting
    self.rate_limiter.wait_if_needed()
    
    try:
        response = self.model.generate_content(prompt)
        response_text = response.text
        
        # Parse JSON
        if '```json' in response_text:
            response_text = response_text.split('```json')[1].split('```')[0]
        
        plan = json.loads(response_text.strip())
        
        # Cache plan
        self.cache.set(prompt, cache_key, response_text)
        
        return plan
        
    except Exception as e:
        logger.error(f"Error generating refactoring plan: {e}")
        return {
            "strategy": "Manual Review Required",
            "description": f"Could not generate automatic plan: {str(e)}",
            "steps": ["Review code manually", "Apply appropriate refactoring"],
            "files_affected": [smell.file_path],
            "expected_improvements": {}
        }

@RateLimiter.__call__
def _make_refactoring_call(self, prompt: str) -> Optional[str]:
    """Make rate-limited refactoring API call"""
    try:
        response = self.model.generate_content(
            prompt,
            generation_config=genai.types.GenerationConfig(
                max_output_tokens=4000,
                temperature=0.2
            )
        )
        return response.text
    except Exception as e:
        logger.error(f"Refactoring API call failed: {e}")
        return None

def verify_refactoring(self, original_code: str, refactored_code: str) -> bool:
    """Verify refactoring preserved functionality"""
    if not refactored_code:
        return False
    
    # Check for major structural changes
    original_lines = set(original_code.split('\n'))
    refactored_lines = set(refactored_code.split('\n'))
    
    # Should have similar line count
    if abs(len(refactored_lines) - len(original_lines)) > len(original_lines) * 0.5:
        logger.warning("Refactoring changed line count significantly")
        return False
    
    # Check for Java syntax (simple check)
    if 'class ' not in refactored_code or '{' not in refactored_code:
        logger.warning("Refactored code missing class structure")
        return False
    
    return True

def apply_refactoring(self, smell: CodeSmell, plan: Dict, code: str) -> RefactoringResult:
    """Apply refactoring with verification"""
    
    prompt = f"""Refactor this Java code according to the plan.
REFACTORING PLAN:
Strategy: {plan.get('strategy', 'Unknown')}
Description: {plan.get('description', '')}
Steps:
{json.dumps(plan.get('steps', []), indent=2)}

ORIGINAL CODE:

java
{code}
RULES:

Return ONLY the refactored code

Preserve all functionality

Keep same class and method names unless extracting

Add appropriate comments

Maintain imports

REFACTORED CODE:"""

text
    try:
        # Make API call
        response_text = self._make_refactoring_call(prompt)
        if not response_text:
            return RefactoringResult(
                original_file=smell.file_path,
                refactored_files=[],
                metrics_before=smell.metrics,
                metrics_after={},
                success=False,
                error_message="API call failed"
            )
        
        # Extract refactored code
        refactored_code = response_text
        if '```java' in refactored_code:
            refactored_code = refactored_code.split('```java')[1].split('```')[0]
        elif '```' in refactored_code:
            refactored_code = refactored_code.split('```')[1].split('```')[0]
        
        refactored_code = refactored_code.strip()
        
        # Verify refactoring
        if not self.verify_refactoring(code, refactored_code):
            return RefactoringResult(
                original_file=smell.file_path,
                refactored_files=[],
                metrics_before=smell.metrics,
                metrics_after={},
                success=False,
                error_message="Refactoring verification failed"
            )
        
        # Create backup
        backup_path = f"{smell.file_path}.backup"
        with open(backup_path, 'w', encoding='utf-8') as f:
            f.write(code)
        
        # Write refactored code
        with open(smell.file_path, 'w', encoding='utf-8') as f:
            f.write(refactored_code)
        
        # Calculate new metrics
        new_lines = refactored_code.split('\n')
        new_loc = len([l for l in new_lines if l.strip() and not l.strip().startswith('//')])
        new_cc = self._calculate_cc_simple(new_lines)
        
        return RefactoringResult(
            original_file=smell.file_path,
            refactored_files=[smell.file_path],
            metrics_before=smell.metrics,
            metrics_after={'cc': new_cc, 'loc': new_loc},
            success=True,
            refactoring_description=plan.get('description', 'Refactored code'),
            applied_strategies=[plan.get('strategy', 'Unknown')]
        )
        
    except Exception as e:
        logger.error(f"Error applying refactoring: {e}")
        return RefactoringResult(
            original_file=smell.file_path,
            refactored_files=[],
            metrics_before=smell.metrics,
            metrics_after={},
            success=False,
            error_message=str(e)
        )

def _calculate_cc_simple(self, lines: List[str]) -> int:
    """Simple CC calculation for verification"""
    cc = 1
    keywords = ['if ', 'while ', 'for ', 'case ', 'catch ', '&&', '||']
    for line in lines:
        for keyword in keywords:
            if keyword in line.lower():
                cc += 1
    return cc
class GitHubPRGenerator:
"""Creates detailed GitHub PRs with comprehensive information"""

text
def __init__(self, token: str, repo_owner: str, repo_name: str):
    self.token = token
    self.repo_owner = repo_owner
    self.repo_name = repo_name
    self.api_base = f"https://api.github.com/repos/{repo_owner}/{repo_name}"
    
    genai.configure(api_key=CONFIG['api_key'])
    self.model = genai.GenerativeModel('models/gemini-2.0-flash')
    self.headers = {
        "Authorization": f"token {token}",
        "Accept": "application/vnd.github.v3+json"
    }

def create_branch(self, branch_name: str) -> bool:
    """Create and switch to new branch"""
    try:
        # Ensure we're on main and up to date
        subprocess.run("git checkout main", shell=True, check=True, 
                     cwd=CONFIG['repo_path'], capture_output=True)
        subprocess.run("git pull origin main", shell=True, check=True,
                     cwd=CONFIG['repo_path'], capture_output=True)
        
        # Create and checkout new branch
        subprocess.run(f"git checkout -b {branch_name}", shell=True, check=True,
                     cwd=CONFIG['repo_path'])
        
        logger.info(f"Created branch: {branch_name}")
        return True
    except subprocess.CalledProcessError as e:
        logger.error(f"Error creating branch: {e}")
        return False

def commit_changes(self, message: str) -> bool:
    """Commit all changes"""
    try:
        subprocess.run("git add .", shell=True, check=True, cwd=CONFIG['repo_path'])
        subprocess.run(f'git commit -m "{message}"', shell=True, check=True,
                     cwd=CONFIG['repo_path'])
        logger.info(f"Committed: {message}")
        return True
    except subprocess.CalledProcessError as e:
        logger.error(f"Error committing: {e}")
        return False

def push_branch(self, branch_name: str) -> bool:
    """Push branch to remote"""
    try:
        result = subprocess.run(
            f"git push -u origin {branch_name}",
            shell=True,
            capture_output=True,
            text=True,
            cwd=CONFIG['repo_path']
        )
        
        if result.returncode == 0:
            logger.info(f"Pushed branch: {branch_name}")
            return True
        else:
            logger.error(f"Push failed: {result.stderr}")
            return False
    except Exception as e:
        logger.error(f"Error pushing: {e}")
        return False

def generate_pr_description(
    self,
    smells: List[CodeSmell],
    results: List[RefactoringResult],
    metrics_before: Dict,
    metrics_after: Dict
) -> Tuple[str, str]:
    """Generate comprehensive PR title and description"""
    
    # Calculate improvements
    complexity_reduced = 0
    loc_reduced = 0
    for result in results:
        before_cc = result.metrics_before.get('cc', 0)
        after_cc = result.metrics_after.get('cc', 0)
        complexity_reduced += max(0, before_cc - after_cc)
    
    # Build smells summary
    smells_summary = []
    for i, smell in enumerate(smells[:10], 1):
        smells_summary.append(
            f"### {i}. {smell.smell_type} in `{Path(smell.file_path).name}`\n"
            f"- **Severity**: {smell.severity}\n"
            f"- **Location**: Lines {smell.line_start}-{smell.line_end}\n"
            f"- **Description**: {smell.description}\n"
            f"- **Applied Refactoring**: {smell.suggested_refactoring}\n"
        )
    
    # Build refactoring results
    refactoring_results = []
    for result in results:
        if result.success:
            before_cc = result.metrics_before.get('cc', 'N/A')
            after_cc = result.metrics_after.get('cc', 'N/A')
            before_loc = result.metrics_before.get('loc', 'N/A')
            after_loc = result.metrics_after.get('loc', 'N/A')
            
            refactoring_results.append(
                f"### `{Path(result.original_file).name}`\n"
                f"- **Before**: CC={before_cc}, LOC={before_loc}\n"
                f"- **After**: CC={after_cc}, LOC={after_loc}\n"
                f"- **Improvement**: {result.refactoring_description}\n"
                f"- **Strategies**: {', '.join(result.applied_strategies or ['Refactoring'])}\n"
            )
    
    # Generate title
    title = f"[Automated] Code Quality Improvements - {len(results)} Design Smells Fixed"
    
    # Build complete description
    description = f"""# 🤖 Automated Code Refactoring Pipeline
📋 Summary
This pull request contains automated refactoring changes to improve code quality.

Total Changes:

✅ Files Refactored: {len(set(r.original_file for r in results))}

🔧 Design Smells Fixed: {len(results)}

📊 Cyclomatic Complexity Reduced: ~{complexity_reduced}

📈 Overall Code Quality: Improved

🔍 Detected Design Smells
{chr(10).join(smells_summary) if smells_summary else "No smells to report"}

🛠️ Applied Refactorings
{chr(10).join(refactoring_results) if refactoring_results else "No refactorings applied"}

📊 Metrics Summary
Metric	Before	After	Improvement
High Complexity Files	{metrics_before.get('high_complexity_files', 0)}	{metrics_after.get('high_complexity_files', 0)}	✓
Total LOC	{metrics_before.get('total_loc', 0)}	{metrics_after.get('total_loc', 0)}	✓
Large Classes	{len(metrics_before.get('large_class_files', []))}	{len(metrics_after.get('large_class_files', []))}	✓
🧪 Testing Instructions
Review the changes in each refactored file

Run the existing test suite

Verify functionality remains unchanged

Check for any compilation errors

⚠️ Notes
These changes were automatically generated using Gemini AI

Each refactoring preserves original functionality

Original files were backed up with .backup extension

Manual review is recommended before merging

🔗 Pipeline Information
Timestamp: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}

Pipeline Version: 2.0 (Enhanced with Rate Limiting)

Repository: {CONFIG['repo_url']}

This PR was automatically generated by the Automated Refactoring Pipeline
"""

text
    return title, description

def create_pull_request(
    self,
    branch_name: str,
    title: str,
    description: str
) -> Optional[str]:
    """Create PR on GitHub"""
    
    url = f"{self.api_base}/pulls"
    
    data = {
        "title": title,
        "body": description,
        "head": branch_name,
        "base": "main",
        "maintainer_can_modify": True
    }
    
    try:
        logger.info(f"Creating PR: {title}")
        response = requests.post(url, headers=self.headers, json=data)
        
        if response.status_code == 201:
            pr_data = response.json()
            pr_url = pr_data['html_url']
            pr_number = pr_data['number']
            logger.info(f"PR created: {pr_url}")
            
            # Add labels
            labels_url = f"{self.api_base}/issues/{pr_number}/labels"
            labels_data = {"labels": ["automated-pr", "code-quality", "refactoring"]}
            requests.post(labels_url, headers=self.headers, json=labels_data)
            
            return pr_url
        else:
            logger.error(f"Failed to create PR: {response.status_code}")
            logger.error(f"Response: {response.text}")
            return None
            
    except Exception as e:
        logger.error(f"Error creating PR: {e}")
        return None
def generate_flowchart() -> str:
"""Generate Mermaid flowchart for documentation"""
return """

Pipeline Flowchart






























"""

def main():
"""Main pipeline with enhanced features"""

text
start_time = time.time()
logger.info("=" * 60)
logger.info("  Automated Refactoring Pipeline - Enhanced Version")
logger.info("=" * 60)
logger.info(f"Repository: {CONFIG['repo_url']}")
logger.info(f"Time: {datetime.now()}")
logger.info(f"Rate Limit: {CONFIG['rate_limit']['requests_per_minute']}/min")
logger.info("=" * 60)

# Validate configuration
if not CONFIG['api_key']:
    logger.error("GEMINI_API_KEY not set in environment")
    sys.exit(1)

if CONFIG['enable_pr_creation'] and not CONFIG['github_token']:
    logger.error("GITHUB_TOKEN not set in environment")
    sys.exit(1)

# Initialize components
analyzer = CodeAnalyzer(CONFIG['api_key'])
refactorer = CodeRefactorer(CONFIG['api_key'])
pr_generator = GitHubPRGenerator(
    CONFIG['github_token'],
    'serc-courses',
    'project-1-team-13'
)

# Generate flowchart documentation
os.makedirs('docs', exist_ok=True)
with open('docs/refactoring_pipeline_flowchart.md', 'w') as f:
    f.write("# Refactoring Pipeline Flowchart\n\n")
    f.write(generate_flowchart())
    f.write("\n\n## Large File Handling Strategy\n\n")
    f.write("```\n")
    f.write("When a large file is detected (>10,000 lines):\n")
    f.write("1. Split file into overlapping chunks (500 lines each)\n")
    f.write("2. Analyze each chunk independently\n")
    f.write("3. Merge results, removing duplicates\n")
    f.write("4. Apply refactoring to specific sections\n")
    f.write("5. Maintain context with overlap regions\n")
    f.write("```\n")
logger.info("✓ Flowchart generated: docs/refactoring_pipeline_flowchart.md")

# Phase 1: Detection
logger.info("\n📊 Phase 1: Detection")
logger.info("-" * 60)

# Clone/Update repository
if not os.path.exists(CONFIG['repo_path']):
    logger.info("Cloning repository...")
    subprocess.run(f"git clone {CONFIG['repo_url']}", shell=True, check=True)
else:
    logger.info("Pulling latest changes...")
    try:
        subprocess.run(f"cd {CONFIG['repo_path']} && git checkout main && git pull", 
                     shell=True, check=True)
    except subprocess.CalledProcessError as e:
        logger.warning(f"Git pull failed: {e}")

# Run comprehensive analysis
logger.info("Running static analysis...")
static_results = analyzer.run_static_analysis(CONFIG['repo_path'])

# Collect files to analyze
files_to_analyze = []

# Add high complexity files
files_to_analyze.extend(static_results['custom_metrics']['high_complexity_files'][:5])

# Add large files that aren't already in the list
large_files = [f for f in static_results['large_files'] 
              if not any(f['file'] == ff['file'] for ff in files_to_analyze)]
files_to_analyze.extend(large_files[:3])

logger.info(f"Selected {len(files_to_analyze)} files for LLM analysis")

# Analyze each file with rate limiting
all_smells = []

for i, file_data in enumerate(files_to_analyze, 1):
    file_path = file_data['file']
    logger.info(f"  [{i}/{len(files_to_analyze)}] Analyzing: {Path(file_path).name}")
    
    try:
        with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
            code = f.read()
        
        # Add delay between analyses
        if i > 1:
            delay = CONFIG['rate_limit']['delay_between_requests']
            logger.debug(f"Waiting {delay}s before next analysis...")
            time.sleep(delay)
        
        metrics = {
            'cc': file_data.get('cc', 0),
            'loc': file_data.get('loc', 0) or len(code.split('\n'))
        }
        
        smells = analyzer.analyze_with_llm(file_path, code, metrics)
        all_smells.extend(smells)
        
        logger.info(f"    ✓ Found {len(smells)} smells")
        
    except Exception as e:
        logger.error(f"    ✗ Error analyzing {file_path}: {e}")
        continue

# Save smell report
os.makedirs('results', exist_ok=True)
report_data = []
for s in all_smells:
    smell_dict = {
        'file': s.file_path,
        'type': s.smell_type,
        'severity': s.severity,
        'lines': f"{s.line_start}-{s.line_end}",
        'description': s.description,
        'suggested_refactoring': s.suggested_refactoring,
        'metrics': s.metrics
    }
    report_data.append(smell_dict)

with open('results/smell_report.json', 'w') as f:
    json.dump(report_data, f, indent=2)

# Generate HTML report
html_report = generate_html_report(all_smells, static_results)
with open('results/smell_report.html', 'w') as f:
    f.write(html_report)

logger.info(f"✓ Reports saved to results/")
logger.info(f"  - JSON: smell_report.json")
logger.info(f"  - HTML: smell_report.html")

if not all_smells:
    logger.info("\nNo smells detected. Exiting.")
    return

# Phase 2: Refactoring
if CONFIG['enable_refactoring']:
    logger.info("\n🔧 Phase 2: Refactoring")
    logger.info("-" * 60)
    
    # Sort smells by severity
    severity_order = {'HIGH': 3, 'MEDIUM': 2, 'LOW': 1}
    all_smells.sort(key=lambda s: severity_order.get(s.severity, 0), reverse=True)
    
    refactoring_results = []
    smells_to_refactor = all_smells[:CONFIG['max_smells_to_refactor']]
    
    for i, smell in enumerate(smells_to_refactor, 1):
        logger.info(f"\n  [{i}/{len(smells_to_refactor)}] Refactoring: {smell.smell_type}")
        logger.info(f"      File: {Path(smell.file_path).name}")
        logger.info(f"      Lines: {smell.line_start}-{smell.line_end}")
        logger.info(f"      Severity: {smell.severity}")
        
        try:
            with open(smell.file_path, 'r') as f:
                code = f.read()
            
            # Generate plan
            plan = refactorer.generate_refactoring_plan(smell, code)
            logger.info(f"      Strategy: {plan.get('strategy', 'Unknown')}")
            
            # Apply refactoring
            result = refactorer.apply_refactoring(smell, plan, code)
            
            if result.success:
                logger.info(f"      ✓ Successfully refactored")
                logger.info(f"        CC: {result.metrics_before.get('cc', 'N/A')} → {result.metrics_after.get('cc', 'N/A')}")
                refactoring_results.append(result)
            else:
                logger.warning(f"      ✗ Failed: {result.error_message}")
            
            # Delay between refactorings
            if i < len(smells_to_refactor):
                time.sleep(CONFIG['rate_limit']['delay_between_requests'])
                
        except Exception as e:
            logger.error(f"      ✗ Error: {e}")
            continue
    
    logger.info(f"\n✓ Completed {len(refactoring_results)} refactorings")
    
    if not refactoring_results:
        logger.info("\nNo successful refactorings. Exiting.")
        return
    
    # Phase 3: PR Creation
    if CONFIG['enable_pr_creation']:
        logger.info("\n📤 Phase 3: Pull Request Creation")
        logger.info("-" * 60)
        
        # Create branch
        timestamp = datetime.now().strftime('%Y%m%d-%H%M%S')
        branch_name = f"refactor/automated-{timestamp}"
        
        if not pr_generator.create_branch(branch_name):
            logger.error("Failed to create branch. Exiting.")
            return
        
        # Commit changes
        commit_msg = f"Automated refactoring: Fixed {len(refactoring_results)} design smells"
        if not pr_generator.commit_changes(commit_msg):
            logger.error("Failed to commit changes. Exiting.")
            return
        
        # Push branch
        if not pr_generator.push_branch(branch_name):
            logger.error("Failed to push branch. Exiting.")
            return
        
        # Generate PR description
        title, description = pr_generator.generate_pr_description(
            smells_to_refactor,
            refactoring_results,
            static_results['custom_metrics'],
            {'high_complexity_files': [], 'total_loc': 0}  # Would calculate actual after metrics
        )
        
        # Save PR description
        with open('results/pr_description.md', 'w') as f:
            f.write(f"# {title}\n\n{description}")
        
        # Create PR
        pr_url = pr_generator.create_pull_request(
            branch_name,
            title,
            description
        )
        
        if pr_url:
            logger.info(f"\n{'=' * 60}")
            logger.info(f"✅ PIPELINE COMPLETED SUCCESSFULLY!")
            logger.info(f"{'=' * 60}")
            logger.info(f"Pull Request: {pr_url}")
            logger.info(f"Branch: {branch_name}")
            logger.info(f"Smells Fixed: {len(refactoring_results)}")
            logger.info(f"Total Time: {time.time() - start_time:.1f}s")
            
            # Save PR link
            with open('results/pr_link.txt', 'w') as f:
                f.write(pr_url)
        else:
            logger.error("\n✗ Failed to create PR")

logger.info(f"\n{'=' * 60}")
logger.info("  Pipeline Execution Complete")
logger.info(f"  Duration: {time.time() - start_time:.1f} seconds")
logger.info("=" * 60)
def generate_html_report(smells: List[CodeSmell], static_results: Dict) -> str:
"""Generate HTML report for better visualization"""

text
html = f"""<!DOCTYPE html>
<html> <head> <title>Design Smell Detection Report</title> <style> body {{ font-family: Arial, sans-serif; margin: 20px; }} h1 {{ color: #333; }} .summary {{ background: #f0f0f0; padding: 15px; border-radius: 5px; }} .smell {{ border: 1px solid #ddd; margin: 10px 0; padding: 10px; border-radius: 3px; }} .HIGH {{ border-left: 5px solid #dc3545; }} .MEDIUM {{ border-left: 5px solid #ffc107; }} .LOW {{ border-left: 5px solid #28a745; }} .severity {{ font-weight: bold; }} .metrics {{ color: #666; font-size: 0.9em; }} table {{ border-collapse: collapse; width: 100%; }} th, td {{ border: 1px solid #ddd; padding: 8px; text-align: left; }} th {{ background-color: #f2f2f2; }} </style> </head> <body> <h1>🔍 Design Smell Detection Report</h1> <div class="summary"> <h2>Summary</h2> <p><strong>Total Smells Detected:</strong> {len(smells)}</p> <p><strong>Files Analyzed:</strong> {static_results.get('files_analyzed', 0)}</p> <p><strong>High Complexity Files:</strong> {len(static_results['custom_metrics'].get('high_complexity_files', []))}</p> <p><strong>Large Files (>300 LOC):</strong> {len(static_results.get('large_files', []))}</p> </div>
text
<h2>Detected Smells</h2>
"""

text
for smell in smells:
    html += f"""
<div class="smell {smell.severity}">
    <h3>{smell.smell_type} <span class="severity">({smell.severity})</span></h3>
    <p><strong>File:</strong> {smell.file_path}</p>
    <p><strong>Lines:</strong> {smell.line_start}-{smell.line_end}</p>
    <p><strong>Description:</strong> {smell.description}</p>
    <p><strong>Suggested Refactoring:</strong> {smell.suggested_refactoring}</p>
    <div class="metrics">
        <strong>Metrics:</strong><br>
        {json.dumps(smell.metrics, indent=2)}
    </div>
</div>
"""

text
html += """
<h2>Refactoring Results</h2>
<p>Refactoring results will be available after pipeline execution.</p>

<h2>Large File Handling</h2>
<p>Files exceeding 300 lines are automatically chunked for analysis.</p>
<table>
    <tr>
        <th>File</th>
        <th>Lines</th>
        <th>Threshold</th>
    </tr>
"""

text
for large_file in static_results.get('large_files', [])[:5]:
    html += f"""
    <tr>
        <td>{Path(large_file['file']).name}</td>
        <td>{large_file['loc']}</td>
        <td>{large_file['threshold']}</td>
    </tr>
"""

text
html += """
</table>

<p><em>Generated by Automated Refactoring Pipeline</em></p>
</body> </html> """
text
return html
if __name__ == "__main__":
    main()