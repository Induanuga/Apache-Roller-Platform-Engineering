import pytest
from refactor_pipeline import CodeAnalyzer, CodeSmell

def test_calculate_cc():
    analyzer = CodeAnalyzer("fake-key")
    
    code_lines = [
        "if (x > 0) {",
        "    while (y < 10) {",
        "        for (int i = 0; i < 5; i++) {",
        "        }",
        "    }",
        "}"
    ]
    
    cc = analyzer._calculate_cc(code_lines)
    assert cc == 4  # 1 base + 3 decision points

def test_smell_detection():
    analyzer = CodeAnalyzer("fake-key")
    
    # Mock code with God Class smell
    code = """
    public class GodClass {
        public void method1() { /* ... */ }
        public void method2() { /* ... */ }
        // ... 50 more methods
    }
    """
    
    # Test would use mock LLM response
    # smells = analyzer.analyze_with_llm("GodClass.java", code, {})
    # assert len(smells) > 0
    # assert any(s.smell_type == "God Class" for s in smells)