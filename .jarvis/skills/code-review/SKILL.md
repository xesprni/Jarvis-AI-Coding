---
name: code-review
description: Comprehensive code review for git staged changes. Analyzes syntax, coding standards (P3C), security issues, performance problems, test coverage, and potential bugs. Use when the user requests code review, asks to review staged changes, or wants to check code quality before committing. Triggers on phrases like "review my code", "check staged changes", "review before commit", or "code quality check".
---

# Code Review

## Overview

Perform comprehensive code review on git staged changes, analyzing code quality across multiple dimensions: syntax correctness, coding standards compliance (P3C for Java), security vulnerabilities, performance issues, test coverage, and potential bugs.

## Review Workflow

### 1. Get Staged Changes

```bash
# Get list of staged files
git diff --cached --name-only

# Get detailed diff of staged changes
git diff --cached
```

### 2. Review Checklist

Analyze each staged file across these dimensions:

#### 🔍 Syntax & Basic Issues
- Compilation errors
- Type errors
- Undefined variables/methods
- Import issues
- Unreachable code

#### 📋 Coding Standards (P3C)
- Naming conventions (classes, methods, variables)
- Code formatting and indentation
- Magic numbers (use constants)
- Exception handling patterns
- Logging practices
- Comment quality and necessity

#### 🔒 Security Issues
- Hardcoded credentials/secrets
- SQL injection vulnerabilities
- Unsafe deserialization
- Path traversal risks
- Sensitive data exposure in logs
- Input validation missing

#### ⚡ Performance Issues
- Inefficient algorithms (O(n²) when O(n) possible)
- Resource leaks (unclosed streams, connections)
- Unnecessary object creation in loops
- Database N+1 queries
- Large data loading without pagination
- Blocking operations in critical paths

#### ✅ Testing
- New code without tests
- Test coverage for edge cases
- Mock usage appropriateness
- Test naming clarity
- Assertion completeness

#### 🐛 Potential Bugs
- Null pointer exceptions
- Resource leaks
- Race conditions
- Off-by-one errors
- Incorrect logic
- Missing error handling

### 3. Output Format

Present findings as a categorized checklist:

```
## 代码审查结果

### 📁 审查的文件
- file1.java
- file2.java

### 🔍 语法问题 (X 个)
- file.java:42 - 未使用的变量 `unusedVar`
- file.java:58 - 可能的空指针异常

### 📋 编码规范 (P3C) (X 个)
- file.java:15 - 类名应使用 UpperCamelCase
- file.java:23 - 魔法值 100 应定义为常量

### 🔒 安全问题 (X 个)
- file.java:67 - 敏感信息不应记录到日志

### ⚡ 性能问题 (X 个)
- file.java:89 - 循环内创建对象，考虑重用

### ✅ 测试覆盖 (X 个)
- 新增方法 `getUserData()` 缺少单元测试

### 🐛 潜在Bug (X 个)
- file.java:120 - 资源未正确关闭

### ✨ 总结
- 共发现 X 个问题
- 严重: X, 警告: X, 建议: X
- [如无问题] 代码质量良好，可以提交
```

### 4. Priority Classification

Classify issues by severity:
- **🔴 严重 (Blocker)**: Security vulnerabilities, critical bugs, compilation errors
- **🟡 警告 (Warning)**: Performance issues, standard violations, missing tests
- **🟢 建议 (Suggestion)**: Code style improvements, optimization opportunities

## Sentinel Project Specific Rules

When reviewing Sentinel codebase:

### Build & Validation
```bash
# Run PMD check (P3C rules)
mvn pmd:check

# Run tests
mvn test

# Full verification
mvn clean verify
```

### Sentinel-Specific Checks
- Core module (`sentinel-core`) should have no framework dependencies
- Test coverage: core ≥70%, extension ≥60%, adapter ≥50%
- Logs output to `~/logs/csp/` directory
- Use SLF4J interface, not concrete implementations
- Check thread safety for shared state
- Validate metrics collection overhead
- Review slot chain modifications carefully

### Common Patterns
- FlowRule/DegradeRule configuration validation
- Entry/Exit resource management (try-with-resources)
- Context initialization in concurrent scenarios
- Proper exception handling in slots

## Resources

This skill does not require additional scripts, references, or assets. All review logic is executed through direct code analysis and tool invocations.
