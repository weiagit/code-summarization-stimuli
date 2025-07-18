#!/bin/bash

# Loop through existing branches 1 to 34
for i in {1..34}; do
  branch="$i"
  echo "Checking out branch $branch"
  git checkout "$branch"

  # Find the single file in the branch (assuming it's in the root)
  file=$(ls | grep '\.java$')

  if [[ -z "$file" ]]; then
    echo "No .java file found on branch $branch, skipping."
    continue
  fi

  echo "Processing $file on branch $branch"

  # Use sed to remove lines with github.com and @author
  sed -i '' '/github\.com/d' "$file"
  sed -i '' '/@author/d' "$file"

  # Stage, commit, and push if anything changed
  if ! git diff --cached --quiet || ! git diff --quiet; then
    git add "$file"
    git commit -m "$branch"
    git push origin "$branch"
  else
    echo "No changes to commit for branch $branch"
  fi
done

