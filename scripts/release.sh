#!/bin/bash

# Exit on error
set -e

# Version pattern validation (X.Y.Z)
VERSION_PATTERN='^[0-9]+\.[0-9]+\.[0-9]+(-[a-zA-Z0-9]+)?$'

if [[ $# -ne 1 ]]; then
    echo "Usage: $0 <version>"
    echo "Example: $0 1.0.0"
    exit 1
fi

VERSION=$1

if [[ ! $VERSION =~ $VERSION_PATTERN ]]; then
    echo "Error: Version must follow the pattern X.Y.Z (e.g., 1.0.0 or 1.0.0-rc1)"
    exit 1
fi

BRANCH_NAME="release/$VERSION"

# Ensure we are on main
CURRENT_BRANCH=$(git branch --show-current)
if [[ "$CURRENT_BRANCH" != "main" ]]; then
    echo "Error: Release script must be run from the 'main' branch."
    exit 1
fi

# Ensure no uncommitted changes
if [[ -n $(git status --porcelain) ]]; then
    echo "Error: You have uncommitted changes. Please commit or stash them before releasing."
    exit 1
fi

echo "Pulling latest changes from main..."
git pull origin main

echo "Creating release branch: $BRANCH_NAME..."
git checkout -b "$BRANCH_NAME"

echo "Updating Tycho/Eclipse versions to $VERSION..."
mvn org.eclipse.tycho:tycho-versions-plugin:set-version -DnewVersion="$VERSION"
git commit -am "chore: bump version to $VERSION for release" || echo "Version was already set to $VERSION, nothing to commit."

echo "Pushing $BRANCH_NAME to origin..."
git push -u origin "$BRANCH_NAME"

echo "Switching back to main..."
git checkout main

# --- Bump main to next snapshot ---
read -p "Enter the next development version for main (e.g., 1.1.0-SNAPSHOT): " NEXT_VERSION

if [[ -n "$NEXT_VERSION" ]]; then
    echo "Updating main to next development version: $NEXT_VERSION..."
    mvn org.eclipse.tycho:tycho-versions-plugin:set-version -DnewVersion="$NEXT_VERSION"
    git commit -am "chore: start next development iteration $NEXT_VERSION" || echo "Nothing to commit."
    git push origin main
    echo "Success! main is now on $NEXT_VERSION"
else
    echo "Skipping main version bump."
fi
# ----------------------------------------

echo "Success! Release branch $BRANCH_NAME has been created and pushed."
echo "The deployment pipeline will now trigger automatically on GitHub."
