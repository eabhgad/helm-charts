#!/bin/sh
ESAPATH=/var/log/esa/pm3gppXml
ESACONF=/usr/local/esa/conf/pmCounters
COUNTER_FILE=/data/bridging/logs/bridging-counter.log
TMP_PATH=/data/bridging/tools/.tmp/monitor
OLD_LOGS=/data/bridging/logs/old-logs/
BR_LOGS=/data/bridging/logs

if [ -d $TMP_PATH ]; then 
	rm -f $TMP_PATH/*.log
	rm -f $TMP_PATH/old-logs/*.log
fi
if [ -z $1 ]; then 
	REP_DATE=`date +%Y"-"%m"-"%d`
else
	REP_DATE=$1
fi
COUNTER_INTERVAL=`sudo cat $ESACONF/bridgingPmCounters.xml | grep interval | awk -F'=' '{print $2}' | awk -F '"' '{print $2}'`
if [ $COUNTER_INTERVAL -gt 60 ]; then 
	COUNTER_INTERVAL=`expr $COUNTER_INTERVAL / 60`
fi
collect_log(){

for node in `cat /data/bridging/tools/conf/serverlist`
do 
	sudo su - bridging -c "scp -q  bridging@$node:$COUNTER_FILE $TMP_PATH/bridging-counter.$REP_DATE.$node.log"
	sudo su - bridging -c "scp -q bridging@$node:/data/bridging/logs/old-logs/bridging-counter.$REP_DATE*.log $TMP_PATH/old-logs/bridging-counter.$REP_DATE.$node.`date +%s`.log"
done
}

#Main Script
echo -e "HOUR\tTOTAL_REQUEST_RECEIVED\tTOTAL_REQUEST_PARSED\tTOTAL_ERRORS"
#collect_log
for hh in `seq -w 00 23`
do
	for i in BLookup_GET_REQUEST BLookup_GET_SUCCESS
	do 
	if [ `cat $COUNTER_FILE | grep $hh:00| grep $i | wc -l` -gt 1 ]; then 
		br_stat=$(cat $COUNTER_FILE | grep $hh:00 |grep $i | tail -1 )
		echo  "$br_stat" >> $TMP_PATH/bridging_monitor.log 
	else
		if [ `cat $OLD_LOGS/bridging-counter.$REP_DATE*.log | grep $hh:00 | grep $i | wc -l` -gt 1 ]; then 
			br_stat=$(cat $OLD_LOGS/bridging-counter.$REP_DATE*.log | grep $hh:00 | grep $i | tail -1 )
			echo "$br_stat" >> $TMP_PATH/bridging_monitor.log
		fi
	fi
	done
	#TOTAL_REQUEST=$(cat $TMP_PATH/bridging_monitor.log | grep $i | grep $hh:00)
#echo $TOTAL_REQUEST
#echo `expr $hh - 1`":00-$hh:00\t\t$TOTAL_REQUEST" 
done

for n in `seq 1 $(cat $TMP_PATH/bridging_monitor.log | grep -i request | wc -l)`
do
if [ $n -ne $(cat $TMP_PATH/bridging_monitor.log | grep -i request | wc -l) ]; then 
	o=`expr $n + 1`
	TEMP_SEP=$(cat $TMP_PATH/bridging_monitor.log | awk -F' ' '{print$2}'|awk -F':' '{print $3}'|awk -F',' '{print $1}')
	HOUR=$(cat $TMP_PATH/bridging_monitor.log| grep BLookup_GET_REQUEST |sed "$n,$o!d"|sort -r|awk -F' ' '{print $2}'|awk -F',' '{print $1}'| awk -F'$TEMP_SEP' '{print $1}'|sort | paste -sd-)
	#printf "%s\n",$HOUR
	TOTAL_REQUEST=$(cat $TMP_PATH/bridging_monitor.log | grep BLookup_GET_REQUEST | sed "$n,$o!d"|sort -r |awk -F' ' '{print $4}'|paste -sd- |bc)
	TOTAL_SUCCESS=$(cat $TMP_PATH/bridging_monitor.log | grep BLookup_GET_SUCCESS | sed "$n,$o!d"|sort -r |awk -F' ' '{print $4}'|paste -sd- |bc)
	TOTAL_ERRORS=`expr $TOTAL_REQUEST - $TOTAL_SUCCESS`
	echo -e $HOUR'\t'$TOTAL_REQUEST'\t\t\t'$TOTAL_SUCCESS'\t\t\t'$TOTAL_ERRORS
	#printf "%s\t%s\n" $HOUR,$TOTAL_REQUEST
fi
done