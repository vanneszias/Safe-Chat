#!/bin/bash

# --- Config ---
REMOTE_HOST="remotezias"
EMULATOR_DIR="/opt/android-sdk/emulator"
BACKEND_DIR="/home/data/Files/dev/Safe-Chat/backend"
APP_DIR="$HOME/dev/Safe-Chat/app"
EMULATOR_AVD1="phone1"
EMULATOR_AVD2="phone2"
LOCAL_PORT1="5555"
LOCAL_PORT2="5557"
WAIT_TIME_SECONDS=30

# --- Colors ---
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NO_COLOR='\033[0m'

# --- Logging Setup ---
LOG_DIR="logs"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
MAIN_LOG_FILE="$LOG_DIR/launch_dev_env_$TIMESTAMP.log"
REMOTE_LOG_FILE="$LOG_DIR/remote_output_$TIMESTAMP.log"
REMOTE_CLEANUP_LOG_FILE="$LOG_DIR/remote_cleanup_$TIMESTAMP.log"

mkdir -p "$LOG_DIR"
exec > >(tee -a "$MAIN_LOG_FILE") 2>&1

# --- Cleanup function ---
cleanup() {
    echo -e "${CYAN}--- Initiating Cleanup ---${NO_COLOR}"
    
    # Ensure logs directory exists
    mkdir -p "$LOG_DIR"
    
    # Clean up local scrcpy processes first
    echo -e "${YELLOW}Terminating local scrcpy processes...${NO_COLOR}"
    if [ -n "$SCRCPY_PID1" ] && kill -0 "$SCRCPY_PID1" 2>/dev/null; then
        kill "$SCRCPY_PID1" 2>/dev/null || true
        echo -e "${GREEN}Scrcpy process 1 (PID: $SCRCPY_PID1) terminated.${NO_COLOR}"
    fi
    if [ -n "$SCRCPY_PID2" ] && kill -0 "$SCRCPY_PID2" 2>/dev/null; then
        kill "$SCRCPY_PID2" 2>/dev/null || true
        echo -e "${GREEN}Scrcpy process 2 (PID: $SCRCPY_PID2) terminated.${NO_COLOR}"
    fi
    
    echo -e "${YELLOW}Attempting to terminate remote services on $REMOTE_HOST...${NO_COLOR}"
    echo -e "${BLUE}  Remote cleanup output will be logged to: $REMOTE_CLEANUP_LOG_FILE${NO_COLOR}"

    # Create a temporary file for remote cleanup output and then move it
    TEMP_CLEANUP_FILE=$(mktemp)
    
    ssh "$REMOTE_HOST" << 'EOF_REMOTE_CLEANUP' > "$TEMP_CLEANUP_FILE" 2>&1
        echo "Starting remote cleanup on $(date)"

        echo "  -> Attempting to stop Docker Compose in backend dir with volume cleanup..."
        if cd "/home/data/Files/dev/Safe-Chat/backend"; then
            echo "    Stopping containers and removing volumes..."
            docker compose down -v || echo "    Warning: docker compose down -v failed."
            echo "    Pruning unused Docker resources..."
            docker system prune -f --volumes || echo "    Warning: docker system prune failed."
        else
            echo "    Error: Could not change directory for docker cleanup."
        fi

        echo "  -> Killing emulator processes (comprehensive cleanup)..."
        # Kill emulator processes by name pattern
        EMULATOR_PIDS=$(pgrep -f "emulator.*-avd.*(phone1|phone2)")
        if [ -n "$EMULATOR_PIDS" ]; then
            echo "    Found emulator PIDs: $EMULATOR_PIDS. Terminating gracefully..."
            kill -TERM $EMULATOR_PIDS 2>/dev/null || echo "    Warning: Failed to send TERM signal to emulator PIDs."
            sleep 5
            # Force kill if still running
            REMAINING_PIDS=$(pgrep -f "emulator.*-avd.*(phone1|phone2)")
            if [ -n "$REMAINING_PIDS" ]; then
                echo "    Force killing remaining emulator PIDs: $REMAINING_PIDS"
                kill -KILL $REMAINING_PIDS 2>/dev/null || echo "    Warning: Failed to force kill emulator PIDs."
            fi
        else
            echo "    No specific emulator processes found for phone1/phone2."
        fi

        # Additional cleanup for any remaining emulator processes
        echo "  -> Cleaning up any remaining emulator processes..."
        ALL_EMULATOR_PIDS=$(pgrep -f "emulator")
        if [ -n "$ALL_EMULATOR_PIDS" ]; then
            echo "    Found additional emulator PIDs: $ALL_EMULATOR_PIDS. Cleaning up..."
            kill -TERM $ALL_EMULATOR_PIDS 2>/dev/null || true
            sleep 3
            # Force kill any stubborn processes
            STUBBORN_PIDS=$(pgrep -f "emulator")
            if [ -n "$STUBBORN_PIDS" ]; then
                echo "    Force killing stubborn emulator PIDs: $STUBBORN_PIDS"
                kill -KILL $STUBBORN_PIDS 2>/dev/null || true
            fi
        else
            echo "    No additional emulator processes found."
        fi

        echo "  -> Cleaning up ADB server on remote..."
        adb kill-server 2>/dev/null || echo "    Warning: adb kill-server failed or ADB not available."

        echo "  -> Checking for any remaining processes..."
        REMAINING_PROCESSES=$(pgrep -f "emulator|adb" || true)
        if [ -n "$REMAINING_PROCESSES" ]; then
            echo "    Warning: Some processes may still be running: $REMAINING_PROCESSES"
        else
            echo "    All emulator and ADB processes cleaned up successfully."
        fi

        echo "Remote cleanup finished on $(date)"
EOF_REMOTE_CLEANUP

    # Move the temporary file to the final location and check SSH result
    if [ $? -ne 0 ]; then
        echo -e "${RED}Warning: Remote cleanup SSH command failed.${NO_COLOR}"
        mv "$TEMP_CLEANUP_FILE" "$REMOTE_CLEANUP_LOG_FILE" 2>/dev/null || true
    else
        echo -e "${GREEN}Remote services cleanup initiated.${NO_COLOR}"
        mv "$TEMP_CLEANUP_FILE" "$REMOTE_CLEANUP_LOG_FILE" 2>/dev/null || true
    fi
    
    # Clean up temp file if move failed
    rm -f "$TEMP_CLEANUP_FILE" 2>/dev/null || true

    echo -e "${YELLOW}Disconnecting ADB devices...${NO_COLOR}"
    adb disconnect localhost:$LOCAL_PORT1 2>/dev/null || true
    adb disconnect localhost:$LOCAL_PORT2 2>/dev/null || true
    
    echo -e "${YELLOW}Killing local ADB server...${NO_COLOR}"
    adb kill-server 2>/dev/null || true

    echo -e "${YELLOW}Cleaning up background SSH tunnel...${NO_COLOR}"
    if [ -n "$SSH_PID" ] && kill -0 "$SSH_PID" 2>/dev/null; then
        kill "$SSH_PID"
        echo -e "${GREEN}SSH tunnel (PID: $SSH_PID) terminated.${NO_COLOR}"
    else
        echo -e "${YELLOW}No active SSH tunnel to terminate.${NO_COLOR}"
    fi

    echo -e "${CYAN}Script finished.${NO_COLOR}"
    echo -e "${CYAN}--- Logs ---${NO_COLOR}"
    echo -e "${BLUE}  Main Log: $MAIN_LOG_FILE${NO_COLOR}"
    echo -e "${BLUE}  Remote Setup Log: $REMOTE_LOG_FILE${NO_COLOR}"
    echo -e "${BLUE}  Remote Cleanup Log: $REMOTE_CLEANUP_LOG_FILE${NO_COLOR}"
}

trap cleanup EXIT

# --- Main Script ---
echo -e "${CYAN}--- Starting Development Environment Setup ---${NO_COLOR}"
echo -e "${BLUE}Logging to: $MAIN_LOG_FILE${NO_COLOR}"
echo ""

echo -e "${GREEN}1. Starting SSH tunnel to $REMOTE_HOST...${NO_COLOR}"
ssh -N \
    -L $LOCAL_PORT1:localhost:$LOCAL_PORT1 \
    -L $LOCAL_PORT2:localhost:$LOCAL_PORT2 \
    $REMOTE_HOST &
SSH_PID=$!
echo -e "${GREEN}SSH tunnel started with PID: $SSH_PID${NO_COLOR}"
sleep 3

echo -e "${GREEN}2. Executing remote startup commands...${NO_COLOR}"
ssh $REMOTE_HOST << EOF > "$REMOTE_LOG_FILE" 2>&1
    set -e

    echo "  -> Navigating to emulator directory: $EMULATOR_DIR"
    cd "$EMULATOR_DIR" || { echo "Error: Could not change to $EMULATOR_DIR"; exit 1; }

    echo "  -> Starting emulator $EMULATOR_AVD1..."
    DISPLAY="" QT_QPA_PLATFORM=offscreen ./emulator -avd "$EMULATOR_AVD1" -no-window -wipe-data >/dev/null 2>&1 & disown || true

    echo "  -> Starting emulator $EMULATOR_AVD2..."
    DISPLAY="" QT_QPA_PLATFORM=offscreen ./emulator -avd "$EMULATOR_AVD2" -no-window -wipe-data >/dev/null 2>&1 & disown || true

    echo "  -> Navigating to backend directory: $BACKEND_DIR"
    cd "$BACKEND_DIR" || { echo "Error: Could not change to $BACKEND_DIR"; exit 1; }

    echo "  -> Starting docker compose..."
    docker compose up --build -d || { echo "Error: Docker compose failed."; exit 1; }

    echo "Remote setup completed."
EOF

if [ $? -ne 0 ]; then
    echo -e "${RED}Error: Remote setup failed. See $REMOTE_LOG_FILE for details.${NO_COLOR}"
    exit 1
fi

echo -e "${GREEN}Remote setup succeeded.${NO_COLOR}"
echo -e "${YELLOW}3. Waiting $WAIT_TIME_SECONDS seconds for services to start...${NO_COLOR}"
sleep $WAIT_TIME_SECONDS

echo -e "${GREEN}4. Connecting ADB to emulators...${NO_COLOR}"
adb connect localhost:$LOCAL_PORT1 || echo -e "${YELLOW}Warning: Could not connect on $LOCAL_PORT1.${NO_COLOR}"
adb connect localhost:$LOCAL_PORT2 || echo -e "${YELLOW}Warning: Could not connect on $LOCAL_PORT2.${NO_COLOR}"

echo -e "${GREEN}5. Building Android APK...${NO_COLOR}"
cd "$APP_DIR"
./gradlew assembleDebug

if [ ! -f "$APP_DIR/app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo -e "${RED}APK build failed or output not found.${NO_COLOR}"
    exit 1
fi

echo -e "${GREEN}6. Installing APKs to emulators...${NO_COLOR}"
adb -s localhost:$LOCAL_PORT1 install -r app/build/outputs/apk/debug/app-debug.apk || echo -e "${RED}Failed to install on port $LOCAL_PORT1.${NO_COLOR}"
adb -s localhost:$LOCAL_PORT2 install -r app/build/outputs/apk/debug/app-debug.apk || echo -e "${RED}Failed to install on port $LOCAL_PORT2.${NO_COLOR}"

echo -e "${GREEN}7. Launching scrcpy...${NO_COLOR}"
scrcpy -s localhost:$LOCAL_PORT1 &
SCRCPY_PID1=$!

scrcpy -s localhost:$LOCAL_PORT2 &
SCRCPY_PID2=$!

wait $SCRCPY_PID1 $SCRCPY_PID2
