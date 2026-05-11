#!/usr/bin/env python3
"""
Full Automated Refactoring Pipeline with PR Creation
"""

import os
import sys
import json
import subprocess
from datetime import datetime
from pathlib import Path
from typing import Dict, List, Optional
import google.generativeai as genai
import requests
from dataclasses import dataclass

# Configuration
CONFIG = {
    'repo_url': 'https://github.com/serc-courses/project-1-team-13.git',
    'repo_path': './project-1-team-13',
    'api_key': os.getenv('GEMINI_API_KEY'),
    'github_token': os.getenv('GITHUB_TOKEN'),
    'thresholds': {
        'cyclomatic_complexity': 15,
        'lines_of_code': 300,
    },
    'chunk_size': 800,
    # NEW: Enable refactoring and PR
    'enable_refactoring': True,
    'enable_pr_creation': True,
    'max_smells_to_refactor': 2  # Only refactor top 2 smells
}

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

@dataclass
class RefactoringResult:
    original_file: str
    refactored_files: List[str]
    metrics_before: Dict[str, float]
    metrics_after: Dict[str, float]
    success: bool
    error_message: Optional[str] = None
    refactoring_description: str = ""


class CodeAnalyzer:
    """Analyzes code for design smells"""
    
    def __init__(self, api_key: str):
        genai.configure(api_key=api_key)
        self.model = genai.GenerativeModel('models/gemini-2.0-flash')
        
    def run_static_analysis(self, repo_path: str) -> Dict:
        """Run static analysis"""
        results = {'custom_metrics': self._calculate_custom_metrics(repo_path)}
        return results
    
    def _calculate_custom_metrics(self, repo_path: str) -> Dict:
        """Calculate metrics"""
        metrics = {
            'files_analyzed': 0,
            'total_loc': 0,
            'high_complexity_files': []
        }
        
        for java_file in Path(repo_path).rglob('*.java'):
            if 'test' in str(java_file).lower() or 'target' in str(java_file):
                continue
                
            metrics['files_analyzed'] += 1
            
            try:
                with open(java_file, 'r', encoding='utf-8', errors='ignore') as f:
                    lines = f.readlines()
                    loc = len([l for l in lines if l.strip() and not l.strip().startswith('//')])
                    metrics['total_loc'] += loc
                    cc = self._calculate_cc(lines)
                    
                    if cc > CONFIG['thresholds']['cyclomatic_complexity']:
                        metrics['high_complexity_files'].append({
                            'file': str(java_file),
                            'loc': loc,
                            'cc': cc
                        })
            except Exception as e:
                continue
        
        return metrics
    
    def _calculate_cc(self, lines: List[str]) -> int:
        """Calculate cyclomatic complexity"""
        cc = 1
        decision_keywords = ['if ', 'while ', 'for ', 'case ', 'catch ', '&&', '||', '?']
        
        for line in lines:
            line = line.strip()
            for keyword in decision_keywords:
                if keyword in line:
                    cc += 1
        return cc
    
    def analyze_with_llm(self, file_path: str, code: str, metrics: Dict) -> List[CodeSmell]:
        """Analyze with Gemini"""
        
        prompt = f"""You are a static analysis engine.

CRITICAL INSTRUCTIONS:
- Respond ONLY with valid JSON.
- Do NOT include explanations.
- Do NOT include markdown.
- ALL string values must be SINGLE LINE.
- Do NOT use newlines inside JSON string fields.
- Escape any quotes properly.
- Do NOT include trailing commas.

Output format:

{{
  "smells": [
    {{
      "type": "God Class",
      "severity": "HIGH",
      "line_start": 1,
      "line_end": 100,
      "description": "Single line description only",
      "suggested_refactoring": "Single line suggestion only"
    }}
  ]
}}

Now analyze this Java code:



File: {file_path}
Cyclomatic Complexity: {metrics.get('cc', 'unknown')}

Code:
```java
{code[:3000]}
```

Identify design smells. Respond with ONLY JSON:
{{
  "smells": [
    {{
      "type": "God Class",
      "severity": "HIGH",
      "line_start": 1,
      "line_end": 100,
      "description": "Brief description",
      "suggested_refactoring": "Extract class X"
    }}
  ]
}}
"""
        
        try:
            response = self.model.generate_content(
                prompt,
                generation_config=genai.types.GenerationConfig(
                    max_output_tokens=2000,
                    temperature=0.3
                )
            )
            print("Raw Gemini Response:")
            print(response.text)

            response_text = response.text

            # Try to extract JSON block if present
            if "```json" in response_text:
                response_text = response_text.split("```json")[1].split("```")[0]
            elif "```" in response_text:
                response_text = response_text.split("```")[1].split("```")[0]

            # Clean common LLM formatting issues
            response_text = response_text.strip()

            # Remove trailing commas
            response_text = response_text.replace(",\n}", "\n}")
            response_text = response_text.replace(",\n]", "\n]")

            try:
                result = json.loads(response_text)
            except json.JSONDecodeError:
                # Fallback: try to recover by finding first { ... } block
                start = response_text.find("{")
                end = response_text.rfind("}") + 1
                if start != -1 and end != -1:
                    result = json.loads(response_text[start:end])
                else:
                    raise

            smells = []
            
            for smell_data in result.get('smells', []):
                smell = CodeSmell(
                    file_path=file_path,
                    smell_type=smell_data['type'],
                    severity=smell_data['severity'],
                    line_start=smell_data.get('line_start', 1),
                    line_end=smell_data.get('line_end', 100),
                    description=smell_data['description'],
                    metrics=metrics,
                    suggested_refactoring=smell_data.get('suggested_refactoring', 'Refactor needed')
                )
                smells.append(smell)
            
            return smells
            
        except Exception as e:
            print(f"Error analyzing with Gemini: {e}")
            return []


class CodeRefactorer:
    """Applies refactorings"""
    
    def __init__(self, api_key: str):
        genai.configure(api_key=api_key)
        self.model = genai.GenerativeModel('models/gemini-2.0-flash')
    
    def generate_refactoring_plan(self, smell: CodeSmell, code: str) -> Dict:
        """Generate refactoring plan"""
        
        lines = code.split('\n')
        relevant_code = '\n'.join(lines[max(0, smell.line_start-10):smell.line_end+10])
        
        prompt = f"""Create a refactoring plan for this code smell.

Smell: {smell.smell_type}
Severity: {smell.severity}
Description: {smell.description}
Suggestion: {smell.suggested_refactoring}

Code:
```java
{relevant_code[:2000]}
```

Provide a JSON plan:
{{
  "strategy": "Extract Method",
  "description": "What changes to make",
  "files_affected": ["{smell.file_path}"],
  "estimated_improvement": "CC: 20 -> 10"
}}
"""
        
        try:
            response = self.model.generate_content(prompt)
            response_text = response.text
            
            if '```json' in response_text:
                response_text = response_text.split('```json')[1].split('```')[0]
            
            plan = json.loads(response_text.strip())
            return plan
            
        except Exception as e:
            print(f"Error generating plan: {e}")
            return {
                "strategy": "Manual Review Needed",
                "description": str(e),
                "files_affected": [smell.file_path]
            }
    
    def apply_refactoring(self, smell: CodeSmell, plan: Dict, code: str) -> RefactoringResult:
        """Apply refactoring using Gemini"""
        
        prompt = f"""Refactor this code based on the plan.

Plan: {json.dumps(plan, indent=2)}

Original Code:
```java
{code}
```

Provide ONLY the refactored code, no explanations.
"""
        
        try:
            response = self.model.generate_content(
                prompt,
                generation_config=genai.types.GenerationConfig(
                    max_output_tokens=4000,
                    temperature=0.2
                )
            )
            
            refactored_code = response.text
            
            # Extract code if wrapped in markdown
            if '```java' in refactored_code:
                refactored_code = refactored_code.split('```java')[1].split('```')[0]
            
            # Write refactored code
            with open(smell.file_path, 'w', encoding='utf-8') as f:
                f.write(refactored_code)
            
            return RefactoringResult(
                original_file=smell.file_path,
                refactored_files=[smell.file_path],
                metrics_before=smell.metrics,
                metrics_after={'cc': smell.metrics.get('cc', 0) - 5},  # Estimated
                success=True,
                refactoring_description=plan.get('description', 'Refactored')
            )
            
        except Exception as e:
            print(f"Error applying refactoring: {e}")
            return RefactoringResult(
                original_file=smell.file_path,
                refactored_files=[],
                metrics_before=smell.metrics,
                metrics_after={},
                success=False,
                error_message=str(e)
            )


class GitHubPRGenerator:
    """Creates GitHub PRs"""
    
    def __init__(self, token: str, repo_owner: str, repo_name: str):
        self.token = token
        self.repo_owner = repo_owner
        self.repo_name = repo_name
        self.api_base = f"https://api.github.com/repos/{repo_owner}/{repo_name}"
        
        genai.configure(api_key=CONFIG['api_key'])
        self.model = genai.GenerativeModel('models/gemini-2.0-flash')
    
    def create_branch(self, branch_name: str) -> bool:
        """Create branch"""
        try:
            subprocess.run(
                f"git checkout -b {branch_name}",
                shell=True,
                check=True,
                cwd=CONFIG['repo_path']
            )
            print(f"✓ Created branch: {branch_name}")
            return True
        except subprocess.CalledProcessError as e:
            print(f"✗ Error creating branch: {e}")
            return False
    
    def commit_changes(self, message: str) -> bool:
        """Commit changes"""
        try:
            subprocess.run("git add .", shell=True, check=True, cwd=CONFIG['repo_path'])
            subprocess.run(
                f'git commit -m "{message}"',
                shell=True,
                check=True,
                cwd=CONFIG['repo_path']
            )
            print(f"✓ Committed: {message}")
            return True
        except subprocess.CalledProcessError as e:
            print(f"✗ Error committing: {e}")
            return False
    
    def push_branch(self, branch_name: str) -> bool:
        """Push branch to GitHub"""
        try:
            result = subprocess.run(
                f"git push -u origin {branch_name}",
                shell=True,
                capture_output=True,
                text=True,
                cwd=CONFIG['repo_path']
            )
            
            if result.returncode == 0:
                print(f"✓ Pushed branch: {branch_name}")
                return True
            else:
                print(f"✗ Error pushing: {result.stderr}")
                return False
                
        except Exception as e:
            print(f"✗ Error pushing branch: {e}")
            return False
    
    def generate_pr_description(
        self,
        smells: List[CodeSmell],
        results: List[RefactoringResult]
    ) -> str:
        """Generate PR description"""
        
        smells_list = "\n".join([
            f"- **{s.smell_type}** in `{os.path.basename(s.file_path)}` (Severity: {s.severity})"
            for s in smells[:5]
        ])
        
        refactorings_list = "\n".join([
            f"- {os.path.basename(r.original_file)}: {r.refactoring_description}"
            for r in results if r.success
        ])
        
        prompt = f"""Create a GitHub Pull Request description.

Detected Smells:
{smells_list}

Refactorings Applied:
{refactorings_list}

Write a professional PR description with:
- Title
- Summary of changes
- Design smells addressed
- Expected improvements

Use Markdown. Be concise.
"""
        
        try:
            response = self.model.generate_content(prompt)
            return response.text
        except:
            return f"""# Automated Code Quality Improvements

## Detected Smells
{smells_list}

## Refactorings Applied
{refactorings_list}

## Testing
Please review and test the changes.
"""
    
    def create_pull_request(
        self,
        branch_name: str,
        title: str,
        description: str
    ) -> Optional[str]:
        """Create PR on GitHub"""
        
        url = f"{self.api_base}/pulls"
        headers = {
            "Authorization": f"token {self.token}",
            "Accept": "application/vnd.github.v3+json"
        }
        
        data = {
            "title": title,
            "body": description,
            "head": branch_name,
            "base": "main"
        }
        
        print(f"\nCreating PR: {title}")
        response = requests.post(url, headers=headers, json=data)
        
        if response.status_code == 201:
            pr_data = response.json()
            print(f"✓ PR created: {pr_data['html_url']}")
            return pr_data['html_url']
        else:
            print(f"✗ Failed to create PR: {response.status_code}")
            print(f"   Response: {response.text}")
            return None


def main():
    """Main pipeline"""
    
    print("=" * 60)
    print("  Automated Refactoring Pipeline - Full Mode")
    print("=" * 60)
    print(f"Repository: {CONFIG['repo_url']}")
    print(f"Time: {datetime.now()}")
    print(f"Refactoring: {'ENABLED' if CONFIG['enable_refactoring'] else 'DISABLED'}")
    print(f"PR Creation: {'ENABLED' if CONFIG['enable_pr_creation'] else 'DISABLED'}")
    print("=" * 60)
    
    # Initialize
    analyzer = CodeAnalyzer(CONFIG['api_key'])
    refactorer = CodeRefactorer(CONFIG['api_key'])
    pr_generator = GitHubPRGenerator(
        CONFIG['github_token'],
        'serc-courses',
        'project-1-team-13'
    )
    
    # Phase 1: Detection
    print("\n📊 Phase 1: Detection")
    print("-" * 60)
    
    if not os.path.exists(CONFIG['repo_path']):
        print("Cloning repository...")
        subprocess.run(f"git clone {CONFIG['repo_url']}", shell=True)
    else:
        print("Pulling latest changes...")
        os.chdir(CONFIG['repo_path'])
        subprocess.run("git checkout main", shell=True, capture_output=True)
        subprocess.run("git pull", shell=True)
        os.chdir('..')
    
    # Analyze
    static_results = analyzer.run_static_analysis(CONFIG['repo_path'])
    
    all_smells = []
    files_to_analyze = static_results['custom_metrics']['high_complexity_files'][:3]
    
    for file_data in files_to_analyze:
        file_path = file_data['file']
        print(f"  Analyzing: {os.path.basename(file_path)}")
        
        try:
            with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
                code = f.read()
            
            smells = analyzer.analyze_with_llm(
                file_path,
                code,
                {'cc': file_data['cc'], 'loc': file_data['loc']}
            )
            all_smells.extend(smells)
        except Exception as e:
            print(f"  ✗ Error: {e}")
            continue
    
    print(f"\n✓ Detected {len(all_smells)} design smells")
    
    # Save report
    os.makedirs('results', exist_ok=True)
    with open('results/smell_report.json', 'w') as f:
        json.dump([{
            'file': s.file_path,
            'type': s.smell_type,
            'severity': s.severity,
            'description': s.description,
            'suggested_refactoring': s.suggested_refactoring
        } for s in all_smells], f, indent=2)
    
    print("✓ Report saved to results/smell_report.json")
    
    if not all_smells:
        print("\nNo smells detected. Exiting.")
        return
    
    # Phase 2: Refactoring
    if CONFIG['enable_refactoring']:
        print("\n🔧 Phase 2: Refactoring")
        print("-" * 60)
        
        # Sort by severity
        all_smells.sort(
            key=lambda s: {'HIGH': 3, 'MEDIUM': 2, 'LOW': 1}.get(s.severity, 0),
            reverse=True
        )
        
        refactoring_results = []
        smells_to_refactor = all_smells[:CONFIG['max_smells_to_refactor']]
        
        for i, smell in enumerate(smells_to_refactor, 1):
            print(f"\n  [{i}/{len(smells_to_refactor)}] Refactoring: {smell.smell_type}")
            print(f"      File: {os.path.basename(smell.file_path)}")
            print(f"      Severity: {smell.severity}")
            
            try:
                with open(smell.file_path, 'r') as f:
                    code = f.read()
                
                # Generate plan
                plan = refactorer.generate_refactoring_plan(smell, code)
                print(f"      Strategy: {plan.get('strategy', 'Unknown')}")
                
                # Apply refactoring
                result = refactorer.apply_refactoring(smell, plan, code)
                
                if result.success:
                    print(f"      ✓ Successfully refactored")
                    refactoring_results.append(result)
                else:
                    print(f"      ✗ Failed: {result.error_message}")
                    
            except Exception as e:
                print(f"      ✗ Error: {e}")
                continue
        
        print(f"\n✓ Completed {len(refactoring_results)} refactorings")
        
        if not refactoring_results:
            print("\nNo successful refactorings. Exiting.")
            return
        
        # Phase 3: PR Creation
        if CONFIG['enable_pr_creation']:
            print("\n📤 Phase 3: Pull Request Creation")
            print("-" * 60)
            
            # Create branch
            branch_name = f"refactor/automated-{datetime.now().strftime('%Y%m%d-%H%M%S')}"
            if not pr_generator.create_branch(branch_name):
                print("Failed to create branch. Exiting.")
                return
            
            # Commit
            commit_msg = f"Automated refactoring: Fixed {len(refactoring_results)} design smells"
            if not pr_generator.commit_changes(commit_msg):
                print("Failed to commit. Exiting.")
                return
            
            # Push
            if not pr_generator.push_branch(branch_name):
                print("Failed to push. Exiting.")
                return
            
            # Generate description
            pr_description = pr_generator.generate_pr_description(
                smells_to_refactor,
                refactoring_results
            )
            
            # Create PR
            pr_url = pr_generator.create_pull_request(
                branch_name,
                "[Automated] Code Quality Improvements",
                pr_description
            )
            
            if pr_url:
                print(f"\n{'=' * 60}")
                print(f"✅ SUCCESS!")
                print(f"{'=' * 60}")
                print(f"Pull Request: {pr_url}")
                print(f"Branch: {branch_name}")
                print(f"Smells Fixed: {len(refactoring_results)}")
            else:
                print("\n✗ Failed to create PR")
    
    print(f"\n{'=' * 60}")
    print("  Pipeline Completed")
    print("=" * 60)


if __name__ == "__main__":
    main()