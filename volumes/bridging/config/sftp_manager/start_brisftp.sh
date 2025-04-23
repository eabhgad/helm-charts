#!/bin/bash
APP_HOME=/data/bridging/0/sftp_manager
PYTHON=python3.6
CONFIG_FILE="sftp-config.yaml"
echo "Starting Bridging SFTP process..."
cd $APP_HOME
echo "Command: $PYTHON sftp.py $CONFIG_FILE"
source /data/bridging/0/sftp_manager/brisftp_venv/bin/activate
$PYTHON brisftp.py $CONFIG_FILE
echo "Bridging SFTP process finished."
deactivate
