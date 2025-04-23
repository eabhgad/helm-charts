#!/bin/sh 
echo "Running Migration Tool Script..."
SERVICE_NAME=MigrationTool
APP_DIR=/home/dmtools/app/0
CONFIG_DIR=$APP_DIR/config
echo "CONFIG_DIR=$CONFIG_DIR"
PATH_TO_JAR=$APP_DIR/dmtool 
PID_PATH_NAME=$APP_DIR/pid 
export IGNITE_HOME=/home/dmtools/3pp/ignite
echo "IGNITE_HOME = $IGNITE_HOME"
JAVA_CMD="java -jar -XX:+UseG1GC -Xmx8192m -Xms8192m -DconfigDir=$CONFIG_DIR -DIGNITE_QUIET=false -DbaseName=bridging -Dspring.profiles.active=dev -Dlogging.config=$CONFIG_DIR/logback/logback.xml -Dspring.config.location=$CONFIG_DIR/properties/bridging-dev.properties $PATH_TO_JAR"
echo "JAVA_CMD=$JAVA_CMD"
echo ""
case $1 in 
start)
	echo "Starting $SERVICE_NAME ..."
	if [ ! -f $PID_PATH_NAME ]; then 
		nohup $JAVA_CMD &
		echo $! > $PID_PATH_NAME  
		echo "$SERVICE_NAME started ..."         
	else 
		echo "$SERVICE_NAME is already running ..."
	fi
;;
stop)
	if [ -f $PID_PATH_NAME ]; then
		PID=$(cat $PID_PATH_NAME);
		echo "$SERVICE_NAME stoping ..." 
		kill $PID;         
		echo "$SERVICE_NAME stopped ..." 
		rm $PID_PATH_NAME       
	else          
		echo "$SERVICE_NAME is not running ..."   
	fi    
;;    
restart|reload)  
	if [ -f $PID_PATH_NAME ]; then 
		PID=$(cat $PID_PATH_NAME);    
		echo "$SERVICE_NAME stopping ..."; 
		kill $PID;           
		echo "$SERVICE_NAME stopped ...";  
		rm $PID_PATH_NAME     
		echo "$SERVICE_NAME starting ..."  
		nohup $JAVA_CMD > $APP_DIR/nohup.out 2>&1
		echo $! > $PID_PATH_NAME  
		echo "$SERVICE_NAME started ..."    
	else           
		echo "$SERVICE_NAME is not running ..."    
    fi     
;;
status)
	if [ ! -f $PID_PATH_NAME ]; then 
		echo "$SERVICE_NAME is not running."         
	else
		PID=$(cat $PID_PATH_NAME);
		echo "$SERVICE_NAME is already running (PID:$PID)"
	fi
;;
*)
	echo "Usage: `basename $0` start|stop|restart|reload|status"
;;
esac
echo ""

