# Contributing to DroidAdMob

Thank you for your interest in contributing to DroidAdMob!

## Commit Convention

This project uses [Conventional Commits](https://www.conventionalcommits.org/) for semantic versioning and automated releases.

### Commit Format

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types

- `feat`: A new feature (triggers MINOR version bump)
- `fix`: A bug fix (triggers PATCH version bump)
- `perf`: Performance improvement (triggers PATCH version bump)
- `docs`: Documentation changes (triggers PATCH version bump)
- `refactor`: Code refactoring (triggers PATCH version bump)
- `test`: Adding or updating tests (no version bump)
- `build`: Build system changes (no version bump)
- `ci`: CI/CD changes (no version bump)
- `chore`: Other changes (no version bump)

### Breaking Changes

Add `BREAKING CHANGE:` in the footer or use `!` after the type to trigger a MAJOR version bump:

```
feat!: remove deprecated consent API

BREAKING CHANGE: The old consent methods have been removed
```

### Examples

```bash
# New feature (v1.0.0 → v1.1.0)
git commit -m "feat: add support for native ads"

# Bug fix (v1.0.0 → v1.0.1)
git commit -m "fix: resolve banner ad positioning issue on notched devices"

# Performance improvement (v1.0.0 → v1.0.1)
git commit -m "perf: optimize ad loading performance"

# Documentation (v1.0.0 → v1.0.1)
git commit -m "docs: add GDPR implementation examples"

# Breaking change (v1.0.0 → v2.0.0)
git commit -m "feat!: update to AdMob SDK 23.0.0

BREAKING CHANGE: Minimum Android API level increased to 23"

# No release
git commit -m "chore: update gradle dependencies"
git commit -m "ci: add PR build workflow"
```

## Development Workflow

1. **Fork the repository**
2. **Create a feature branch**: `git checkout -b feat/my-feature`
3. **Make your changes**
4. **Test thoroughly**: `./gradlew clean assemble`
5. **Commit using conventional commits**: `git commit -m "feat: add my feature"`
6. **Push to your fork**: `git push origin feat/my-feature`
7. **Open a Pull Request**

## Pull Request Process

1. Ensure your PR description clearly describes the problem and solution
2. Update the README.md if you're adding new features
3. The PR will trigger automated builds to verify your changes
4. Wait for review and address any feedback
5. Once approved, your PR will be merged to main
6. A new release will be automatically created if applicable

## Building Locally

```bash
# Build the plugin
./gradlew clean assemble

# Output will be in:
# - plugin/build/outputs/aar/DroidAdMob-debug.aar
# - plugin/build/outputs/aar/DroidAdMob-release.aar
```

## Testing

Before submitting a PR:

1. Build the plugin successfully
2. Test in a Godot project with the demo
3. Verify GDPR consent flow works
4. Test all ad types (Banner, Interstitial, Rewarded)
5. Check that no errors appear in logcat

## Code Style

- Follow existing code style in the project
- Use meaningful variable and function names
- Add comments for complex logic
- Keep methods focused and concise
- Add GDScript documentation comments for public APIs

## Questions?

Open an issue for discussion before starting work on major changes.

