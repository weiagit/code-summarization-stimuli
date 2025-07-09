import random

input_file = 'stimuli/stimuli.csv'
output_file = 'stimuli/stimulus_to_num.csv'

nums = list(range(1, 35))
random.shuffle(nums)

# Read all lines from the file
with open(input_file, 'r') as infile:
    lines = infile.readlines()

# Write lines and assigned numbers to output file
with open(output_file, 'w') as outfile:
    for i, line in enumerate(lines):
        # Grab line, append comma and random number between 1,..,34, then write to output file
        line = line.rstrip('\n')
        outfile.write(f"{line}, {nums[i]}\n")
