# Git Hooks

This directory contains Git hooks for the Kiddozz project.

## Pre-push Hook

The `pre-push` hook enforces code quality before allowing pushes by running:

1. **Code formatting** (`make format`) - Uses black to format Python code
2. **Linting** (`make lint`) - Uses ruff to check for code quality issues

### Setup

To activate the pre-push hook, run:

```bash
cp hooks/pre-push .git/hooks/pre-push
chmod +x .git/hooks/pre-push
```

### Behavior

- **Success**: If both format and lint checks pass, the push proceeds
- **Failure**: If either check fails, the push is aborted with a clear error message

### Manual Testing

You can test the hook manually by running:

```bash
.git/hooks/pre-push
```

This will run the same checks that would run during a push.
