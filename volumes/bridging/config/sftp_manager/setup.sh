#!/bin/bash
APP_HOME=/data/bridging/0/sftp_manager
VENV_NAME=brisftp_venv
VENV_ACTIVATE="source $VENV_NAME/bin/activate"
VENV_DEACTIVATE="$VENV_NAME/bin/deactivate"
PIP=/usr/local/bin/pip3.6

echo "Setup Bridging SFTP application."

if ! type "virtualenv" &> /dev/null
then
	echo "WARNING: virtualenv not installed."
	echo "Installing virtualenv"
	sudo $PIP install virtualenv
	if [ $? -ne 0 ]; then
		echo "ERROR: Virtualenv not installed."
		exit 1
	fi
fi


# Check if virtualenv is already created
echo "Check of $APP_HOME/VENV_NAME exists..."
if [ ! -d $APP_HOME/$VENV_NAME ]; then
        echo "Creating Virtual Environment $VENV_NAME "
        virtualenv $VENV_NAME
fi

echo "Activating Virtual Environment source $VENV_NAME/bin/activate"
source /data/bridging/0/sftp_manager/brisftp_venv/bin/activate

if [ $? -ne 0 ]; then
        echo "ERROR: Virtualenv not activated."
        exit 1
fi

if [ ! -e requirements.txt ]
then
        echo "ERROR: File requirements.txt not found."
else
        echo "Installing required packages"
        pip install -r requirements.txt
        echo "Setup completed successfully."
fi

