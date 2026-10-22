#!/usr/bin/env bash
# Stop hook: runs when Claude Code is about to finish its answer.
# If the code changed and the checks fail, it blocks the stop (exit 2) and
# sends the error to Claude, so it fixes the problem before handing over.

input=$(cat)

# Second stop in a row (Claude already got one failure from this hook):
# let it stop, so a problem it cannot fix does not become an endless loop.
if echo "$input" | grep -q '"stop_hook_active": *true'; then
    exit 0
fi

cd "$CLAUDE_PROJECT_DIR" || exit 0

# Backend: tests, ArchUnit, SpotBugs and coverage, only if backend files changed
if [ -n "$(git status --porcelain -- src pom.xml spotbugs-exclude.xml)" ]; then
    if ! output=$(./mvnw -B -q verify 2>&1); then
        echo "./mvnw verify failed. Fix the code before you finish. Do not weaken tests or checks." >&2
        echo "$output" | tail -40 >&2
        exit 2
    fi
fi

# Frontend: tests, only if frontend files changed
if [ -n "$(git status --porcelain -- frontend)" ]; then
    if ! output=$(cd frontend && npm test 2>&1); then
        echo "npm test failed in frontend/. Fix the code before you finish." >&2
        echo "$output" | tail -40 >&2
        exit 2
    fi
fi

exit 0