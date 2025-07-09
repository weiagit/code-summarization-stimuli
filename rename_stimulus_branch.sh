#!/bin/bash

for i in {1..34}; do
  branch="$i"
  git checkout "$branch"

  # Find the single Java file in the root (adjust path if needed)
  file=$(ls *.java 2>/dev/null)

  if [[ -z "$file" ]]; then
    echo "No Java file found on branch $branch, skipping."
    continue
  fi

  # Extract first part before underscore, keep .java extension
  base="${file%%_*}.java"

  # Rename the file only if different, push to GitHub
  if [[ "$file" != "$base" ]]; then
    git mv "$file" "$base"
    git commit -m "$branch"
    git push origin "$branch"

  else
    echo "Filename $file on branch $branch already correct."
  fi
done
