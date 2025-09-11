# Contributing Guide

## Table of Contents
- [Development Workflow](#development-workflow)
  - [1. Setup](#1-setup)
  - [2. Linting & Formatting](#2-linting--formatting)
  - [3. Running Tests](#3-running-tests)
  - [4. Database Migrations](#4-database-migrations)
  - [5. GitHub Actions CI](#5-github-actions-ci)
  - [6. Branch Protection Rules](#6-branch-protection-rules)
  - [7. Commit Conventions](#7-commit-conventions)
  - [8. Submitting a PR](#8-submitting-a-pr)
  - [9. Release Workflow](#9-release-workflow)
- [License](#license)

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

### 4. Database Migrations

We use **Alembic** to manage database schema changes.

- When to create a migration: whenever you add, remove, or modify SQLAlchemy models in app/models/.

- Create a migration:
  alembic revision --autogenerate -m "Describe your change"

- Apply migrations:
  alembic upgrade head

- Rollback migrations:
  alembic downgrade -1

Always inspect the generated script under alembic/versions/ before committing. Migration scripts must be version-controlled to ensure consistency for CI/CD and other developers.

---

### 5. GitHub Actions CI
Every **push** and every **pull request into `main`** triggers GitHub Actions:

- ✅ **Lint check** (`Backend CI / lint`)  
  Runs `ruff` and `black --check` to ensure code style.

- ✅ **Test check** (`Backend CI / test`)  
  Runs `pytest` inside Poetry's environment to validate backend functionality.

Merges into `main` are blocked unless both checks pass.

---

### 6. Branch Protection Rules
The `main` branch is protected:
- All PRs into `main` must pass **linting** and **tests**.  
- Direct pushes to `main` are discouraged.  
- (Optional) Code owner reviews can be enabled if/when more contributors join.

---

### 7. Commit Conventions
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

### 8. Submitting a PR
1. Create a feature branch from `main`.  
2. Run `make fix` and `make test`.  
3. Commit your changes with a clear message.  
4. Push your branch and open a pull request.  
5. Ensure CI checks are green before merging.

---

### 9. Release Workflow

We use separate workflows for staging and production:

- **Staging**
  - Auto-deploys on every push to `main`.
  - Used for internal testing (QA & partner feedback).
  - Staging APKs are auto-built for testers.

- **Production**
  - Deploys only when a release branch/tag is created in GitHub.
  - Backend redeploys from the release tag.
  - Production APKs are only built on release.
  - Ensure the release branch passes all CI checks before tagging.

**Developer steps for release:**
1. Finish development on feature branches → merge into `main`.
2. Verify staging deployment and APK functionality.
3. When stable, create a GitHub release (tag) from `main`.
4. Railway will redeploy production backend from the release.
5. GitHub Actions will build the production APK.

---

## License
By contributing, you agree that your contributions will be licensed under the MIT License.
