#!/bin/bash
TMP_PATH=/data/bridging/tools/monitor
EXEC_DATE=`date +%Y%m%d%H`
#echo $EXEC_DATE
echo "Gathering bridging statisticts.."
if [ -f $TMP_PATH/all_servers_stat.txt ]; then 
	rm -f $TMP_PATH/all_servers_stat.txt
	rm -f $TMP_PATH/bridging_hourly_stat_$EXEC_DATE.txt
fi
bridging-sendcommand -h ALL -c "sudo sh /data/bridging/tools/bin/bridging_stat.sh">>$TMP_PATH/all_servers_stat.txt
echo -e HOUR"\t\t"TOTAL_REQUEST_RECEIVED"\t\t"TOTAL_REQUEST_PARSED"\t\t"TOTAL_ERRORS >> $TMP_PATH/bridging_hourly_stat_$EXEC_DATE.txt
for hh in `seq -w 01 23`
do 
	if [ `cat $TMP_PATH/all_servers_stat.txt | grep -e "-$hh:00"|wc -l` -gt 1 ]; then 
	TOTAL_REQUEST=`cat $TMP_PATH/all_servers_stat.txt | grep -e "-$hh:00"|awk -F' ' '{print $2}'|awk '{ SUM += $1} END { printf SUM }'`
	TOTAL_SUCCESS=`cat $TMP_PATH/all_servers_stat.txt | grep -e "-$hh:00"|awk -F' ' '{print $3}'|awk '{ SUM += $1} END { printf SUM }'`
	TOTAL_ERRORS=`expr $TOTAL_REQUEST - $TOTAL_SUCCESS`
	echo -e `expr $hh - 1`":00-$hh:00\t"$TOTAL_REQUEST"\t\t\t\t"$TOTAL_SUCCESS"\t\t\t\t"$TOTAL_ERRORS>>$TMP_PATH/bridging_hourly_stat_$EXEC_DATE.txt
	fi
done

scp -q $TMP_PATH/bridging_hourly_stat_$EXEC_DATE.txt L2_support@10.29.101.9:/home/L2_support/ITBridging 