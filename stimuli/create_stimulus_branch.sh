#!/bin/bash

csv_file="stimuli/stimulus_to_num.csv"

# Ensure you are on a clean base branch
git checkout main
# git pull

while IFS=, read -r filepath number; do
  # Trim whitespace
  filepath=$(echo "$filepath" | xargs)
  number=$(echo "$number" | xargs)

  # Skip empty lines
  if [[ -z "$filepath" || -z "$number" ]]; then
    continue
  fi

  # Check file exists
  if [[ ! -f "$filepath" ]]; then
    echo "File $filepath not found, skipping."
    continue
  fi

  # Create and switch to new branch
  git checkout -b "$number"

  # Remove all files in the repo (except .git)
  # git rm -r --cached .
  # rm -rf *

  # Copy the target file to root
  cp "$filepath" .

  # Stage and commit
  git add "$(basename "$filepath")"
  git commit -m "$(basename "$filepath"), branch $number"

  # Push the branch to GitHub
  git push origin "$number"

  # Go back to base branch
  git checkout main

done < "$csv_file"