# Contributing Guide

Thanks for your interest in contributing to **Kiddozz Backend API**!  
This document explains the development workflow, coding standards, and CI rules for this repository.

---

## Development Workflow

### 1. Setup
- Follow the [README.md](./README.md) for local setup instructions.
- Make sure you have Poetry installed and dependencies installed:
  ```bash
  cd backend
  poetry install
  ```

---

### 2. Linting & Formatting
We use **ruff** (linting) and **black** (formatting) to enforce consistent code style.

- Run auto-fixes before commit:
  ```bash
  make fix
  ```

- Check linting without fixing:
  ```bash
  make lint
  ```

If any issues remain after `make fix`, they must be fixed manually.

---

### 3. Running Tests
We use **pytest** for backend tests.

- Run all tests locally:
  ```bash
  make test
  ```

Tests will automatically use SQLite in-memory unless configured otherwise.

---

### 4. GitHub Actions CI
Every **push** and every **pull request into `main`** triggers GitHub Actions:

- ✅ **Lint check** (`Backend CI / lint`)  
  Runs `ruff` and `black --check` to ensure code style.

- ✅ **Test check** (`Backend CI / test`)  
  Runs `pytest` inside Poetry’s environment to validate backend functionality.

Merges into `main` are blocked unless both checks pass.

---

### 5. Branch Protection Rules
The `main` branch is protected:
- All PRs into `main` must pass **linting** and **tests**.  
- Direct pushes to `main` are discouraged.  
- (Optional) Code owner reviews can be enabled if/when more contributors join.

---

### 6. Commit Conventions
Please write clear commit messages. Recommended prefixes:
- `feat:` → new feature
- `fix:` → bug fix
- `docs:` → documentation changes
- `refactor:` → code refactor
- `test:` → test-related changes
- `chore:` → maintenance tasks

Example:
```
feat: add async event creation endpoint
fix: correct S3 presigned URL path
```

---

### 7. Submitting a PR
1. Create a feature branch from `main`.  
2. Run `make fix` and `make test`.  
3. Commit your changes with a clear message.  
4. Push your branch and open a pull request.  
5. Ensure CI checks are green before merging.

---

## License
By contributing, you agree that your contributions will be licensed under the MIT License.
