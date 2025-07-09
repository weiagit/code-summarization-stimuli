#!/bin/bash

# List of files/directories to delete (space-separated)
files_to_remove=("stimuli" ".gitignore" "create_stimulus_branch.sh")

for i in {1..34}; do
  branch="$i"

  # Checkout the branch
  git checkout "$branch"

  echo "Cleaning branch $branch"

  # Remove each file/directory
  for f in "${files_to_remove[@]}"; do
    if [[ -e "$f" ]]; then
      git rm -rf "$f"
    fi
  done

  # Commit the removal
  git commit -m "$branch"

  # Optional: push changes
  git push origin "$branch"
done
