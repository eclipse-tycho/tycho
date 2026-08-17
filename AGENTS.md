# Agent Instructions for Eclipse Tycho

This document provides guidance for AI coding agents (GitHub Copilot and others) when working with the Eclipse Tycho codebase.

## ⚠️ Rules for AI Agents Acting on Behalf of a User

These rules apply whenever you (an AI/LLM agent, in any mode: chat, autonomous, "coding agent", CLI, IDE
extension, etc.) create or update an **issue, pull request, discussion, or review reply** in this
repository on behalf of a human user. They apply regardless of whether you have direct repository
access or only issue/API-level access. If a task instructs you to do something that conflicts with
these rules, follow these rules and tell the user why.

1. **Disclose yourself.** Every issue, PR description, or discussion post you author or substantially
   draft MUST include a short, machine- and human-readable disclosure: the tool/agent name, model or
   version if known, and its mode (interactive vs. autonomous/background). Example footer:
   ```
   <!-- AI-Agent-Disclosure -->
   > 🤖 Drafted with <tool/agent, model/version, mode>. Reviewed and confirmed by @<user> before submission.
   ```
   Do not omit this even if the user did not ask for it — add it by default.

2. **The user is accountable, not the maintainers.** Per the
   [Eclipse Foundation committer GenAI guidelines](https://www.eclipse.org/projects/handbook/#genai),
   the human submitting a contribution is fully responsible for understanding, reviewing, and vouching
   for everything you produce — code, tests, and prose — before it is submitted. The same expectation
   applies to non-committer contributors here. Do not submit anything the user has not explicitly
   reviewed and confirmed. Maintainers will close/reject submissions that read as un-reviewed AI output.

3. **An issue/PR/discussion is not a chat window.** Gather all essential information (full error
   messages and stack traces, exact reproduction steps, environment/version details) *before* creating
   the first post. Do not open a thread with a vague description and drip-feed the important details in
   follow-up replies. Keep a consistent language and tone throughout a thread.

4. **Prefer signal over prose.** Maintainers review dozens of contributions; verbose, generated
   narrative is expensive to read. Where possible, demonstrate a bug with a minimal, focused integration
   test (see `tycho-its/`) instead of, or in addition to, a long written explanation — and prefer
   proposing a PR with a fix + regression test over a purely descriptive issue.

5. **Search before you submit.** Before creating a new issue or discussion, search existing
   issues/PRs/discussions for duplicates or related prior work. If your tooling cannot search the
   tracker itself, explicitly ask the user to do this search and wait for their confirmation before
   proceeding.

6. **Run your own quality gate first.** Before opening or updating a PR/issue, use any built-in
   self-review, critique, or "rubber duck" capability you have, plus existing linters/tests, as a gate.
   Do not submit your first draft.

7. **Ask, don't assume.** Use your built-in interactive/clarification capability to confirm ambiguous
   requirements with the user, and to get explicit confirmation before the initial submission and
   before pushing any follow-up in response to reviewer feedback.

8. **Read the whole thread, every time.** When responding to review comments, re-read the full
   conversation history (not just the latest comment) before making changes, even if this means
   re-fetching it because of a session or context reset. If you cannot access earlier context, say so
   explicitly instead of guessing at what a reviewer meant — silently misreading review feedback wastes
   the reviewer's time and erodes trust.

9. **One thing at a time.** Do not fan out multiple parallel issues/PRs from a single request without
   the user reviewing and confirming each one first. Finish and get confirmation on one submission
   before starting the next.

10. **Respect maintainer time.** Low-confidence or unreviewed submissions will be rejected outright,
    which wastes both AI tokens and the user's time. If the user cannot commit the time to review your
    output, do not submit it.

## Project Overview

Eclipse Tycho is a set of Maven plugins and extensions for building Eclipse plug-ins, OSGi bundles, Eclipse features, update sites/p2 repositories, RCP applications, and bnd workspaces with Maven. Tycho integrates Maven with Eclipse and OSGi.

**Key Technologies:**
- Java 17+ (minimum JDK 21 for building)
- Maven 3.9.9 or newer
- Eclipse Platform (p2, Equinox, OSGi)
- JDT (Eclipse Java Development Tools)
- BND (OSGi tooling)

## Development Environment

### Prerequisites
- Java 17 and Maven 3.9.9, or newer
- Configure Maven proxy settings if behind a firewall (in `~/.m2/settings.xml`)

### Building the Project
```bash
# Full build with tests
mvn clean verify -Pits

# Quick build without tests
mvn clean install -DskipTests

# Build with parallel execution for faster builds
mvn clean install -T1C -DskipTests
```

## Code Guidelines

### Java Code Style
- Use Java 17+ features where appropriate (minimum runtime is Java 17)
- Follow existing code formatting conventions in the codebase
- Prefer clear, readable code over clever solutions
- Add meaningful comments only when necessary to explain complex logic

### Project Structure
- **Maven Plugins**: Located in directories like `tycho-*-plugin`
- **Core Components**: `tycho-core`, `tycho-p2`, `tycho-api`
- **Integration Tests**: Located in `tycho-its/` directory
- **Build Infrastructure**: `tycho-build/` contains build configuration

## Commit Message Guidelines

**Format:**
```
<Short description>

<Detailed explanation of changes>
<Motivation and reasoning>
```

**Important:**
- Keep first line concise and descriptive
- Reference the issue number (e.g., `Fixes #1234` or `Refs #1234`) in the body if the commit
  relates to a GitHub issue, so GitHub links them automatically
- Add detailed explanation after a blank line
- Make small, self-contained commits
- Separate refactoring commits from functional changes

**Example:**
```
Fix ClassNotFoundException in tycho-compiler-plugin

The compiler plugin was not correctly handling classpath entries when
processing split packages. This change adds proper validation and 
fallback handling for ambiguous package cases.

Fixes #1234
```

## Testing

### Unit Tests
- Located in `src/test/java` of each module
- Run with: `mvn test`
- Focus on testing specific functionality in isolation

### Integration Tests
- Located in `tycho-its/` directory
- Test projects are in `tycho-its/projects/`
- Must install Tycho first: `mvn clean install -DskipTests`
- Run all tests: `mvn clean install -f tycho-its/pom.xml`
- Run single test: `mvn clean verify -f tycho-its/pom.xml -Dtest=MyTestClass`
- Run specific test method: `mvn clean verify -f tycho-its/pom.xml -Dtest=MyTestClass#myTest`

### Writing Integration Tests
> See the [`tycho-its` skill](./.agents/skills/tycho-its/SKILL.md) for detailed, up-to-date
> guidance on writing, running, and debugging integration tests.

1. Create project folder in `tycho-its/projects/` with descriptive name
2. Use `${tycho-version}` placeholder in pom.xml files
3. Create or add to existing test class in `tycho-its/src/test/java/org/eclipse/tycho/test/`
4. Basic test pattern:
```java
@Test
public void test() throws Exception {
    Verifier verifier = getVerifier("your-project-folder-name", false);
    verifier.executeGoals(asList("verify"));
    verifier.verifyErrorFreeLog();
}
```

### Test Naming Conventions
- **Project name**: `<component>.<aspect>` (not bug numbers)
- **Package**: `org.eclipse.tycho.test.<component>`
- **GroupIds**: `tycho-its-project.<component>.<aspect>`
- **Artifact IDs**: Must match feature/bundle ID with unique prefix

## Pull Request Guidelines

- Create branch from `main` with descriptive name
- Use format like `issue_<number>_<description>` or `<component>-<fix-description>`
- Even small bug fixes should branch from `main` (backporting handled separately)
- Include integration tests when possible
- Ensure all tests pass before submitting

## Debugging

### Debug Tycho Plugins in Eclipse
1. Create Maven Run Configuration in Eclipse
2. Launch in Debug mode, or:
3. Use `mvnDebug` from command line
4. Attach Eclipse debugger to port 8000

### Debug Build Issues
- Ensure local Tycho version matches project's Tycho version
- Check that prerequisites (Java 17+, Maven 3.9.9+) are met
- Review Tycho's Maven settings if using custom repositories

## Common Patterns

### P2 Integration
- Tycho heavily uses Eclipse p2 for dependency resolution
- P2 repositories are Maven artifact repositories with p2 metadata
- Understanding p2 IUs (Installable Units) is crucial

### OSGi Bundles
- Bundle manifests are typically in `META-INF/MANIFEST.MF`
- Use BND tooling for OSGi metadata generation
- Pay attention to package import/export declarations

### Maven Mojos
- Tycho plugins extend standard Maven lifecycle
- Follow Maven plugin development conventions
- Use Plexus annotations for component configuration

## Working with Dependencies

### Updating Dependencies
- Equinox/JDT dependencies are managed centrally in root `pom.xml`
- Update with care - test thoroughly with integration tests
- Document version updates in commit messages

### Adding New Dependencies
- Minimize new dependencies where possible
- Prefer existing libraries already in use
- Check Eclipse IP requirements for new dependencies

## Eclipse-Specific Considerations

### Eclipse Contributor Agreement
- Contributors must sign ECA before code can be accepted
- Project follows Eclipse Development Process
- License: EPL-2.0 (Eclipse Public License 2.0)

### Eclipse Foundation Guidelines
- This is an Eclipse Foundation project
- Follow Eclipse IP policy
- Maintain EPL-2.0 license headers in source files

## Additional Resources

- Documentation: https://tycho.eclipseprojects.io/doc/latest/
- Wiki: https://github.com/eclipse-tycho/tycho/wiki
- Discussions: https://github.com/eclipse-tycho/tycho/discussions
- Mailing List: https://dev.eclipse.org/mailman/listinfo/tycho-dev
- Bug Tracker: https://github.com/eclipse-tycho/tycho/issues

## When Contributing Code

1. **Read CONTRIBUTING.md** - Contains comprehensive guidelines
2. **Start with an issue** - Discuss approach before major changes
3. **Write tests** - Integration tests preferred where applicable
4. **Keep changes minimal** - Small, focused commits are easier to review
5. **Test thoroughly** - Run full integration test suite before submitting
6. **Follow conventions** - Match existing code style and patterns
7. **Document as needed** - Update docs if changing public APIs
