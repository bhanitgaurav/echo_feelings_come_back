#!/bin/bash

PORT=8080

echo "Checking for process on port $PORT..."
PID=$(lsof -t -i:$PORT)

if [ -n "$PID" ]; then
    echo "Found process $PID running on port $PORT. Killing it..."
    kill -9 $PID
    echo "Process killed."
else
    echo "No process found on port $PORT."
fi

echo "Starting Server..."
./gradlew :server:run
