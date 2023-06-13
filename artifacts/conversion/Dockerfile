# Stage 1: Build stage
FROM openjdk:8-jdk AS builder

# Set the working directory
WORKDIR /app

# Copy the source code into the container
COPY ./src ./src

# Compile the Scala code
RUN scalac ./src/main.scala

# Stage 2: Final stage
FROM openjdk:8-jdk

# Set the working directory
WORKDIR /app

# Copy the compiled files from the previous stage
COPY --from=builder /app/src/Main.class .

# Set the entry point
CMD ["scala", "Main"]
