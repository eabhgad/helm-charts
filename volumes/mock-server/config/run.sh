nohup java -jar -XX:+UseG1GC -DconfigDir=/home/dmbridging/aris/0/config ArisServer &
echo $! > pid
