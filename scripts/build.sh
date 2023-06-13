#!/bin/bash

# Get the absolute path of the script's directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Set the root directory
ROOT_DIR="$SCRIPT_DIR/.."

# Set the source file path
SOURCE_FILE="$ROOT_DIR/src/main.scala"

# Delete previous build
rm -rf "$ROOT_DIR/build"

# Set the output directory for the compiled files
OUTPUT_DIR="$ROOT_DIR/build"

# Create the output directory if it doesn't exist
mkdir -p "$OUTPUT_DIR"

# Compile the Scala code
scalac -d "$OUTPUT_DIR" "$SOURCE_FILE"

# Check if compilation was successful
if [ $? -eq 0 ]; then
  echo "Scala code successfully compiled."
else
  echo "Error: Compilation failed."
fi
