function sendFifoSocketMessage(){

	CONTROL=$1 
	DIR=$2
	
	if [ ! -p $CONTROL ]
	then
		echo "ERROR: Could not connect to keeper"
		return 1;
	fi
	
	RANDOM=$$  # Seeds the random number generator from PID
	SOCKET_ID=$RANDOM
	#let SOCKET_ID=($RANDOM*10000)+$RANDOM

	#echo "Connection $SOCKET_ID to $CONTROL $DIR"


	FROM_SERVER=$DIR/$SOCKET_ID.fifo.out
	TO_SERVER=$DIR/$SOCKET_ID.fifo.in
	CONTROL_RESPONSE=$DIR/$SOCKET_ID.fifo.control
	
	mkfifo $FROM_SERVER
	mkfifo $CONTROL_RESPONSE
		
	# SEND CONTROL COMMAND
	COMMAND="{$SOCKET_ID}"
	echo $COMMAND > $CONTROL
	
	# WAIT FOR CONTROL RESPONSE
	CREATE_RESPONSE=`cat $CONTROL_RESPONSE`
	if [ $CREATE_RESPONSE != "OK" ]
	then
		echo "ERROR: SERVER RETURNED $CREATE_RESPONSE"
		return -1;
	fi
	
	# THE SERVER LOVES US!  WE HAVE OUR FIFOS!  NOW WE SEND/RECEIVE
	
	#SEND
	cat > $TO_SERVER
	
	#RECEIVE
	cat $FROM_SERVER
	
	#CLEAN-UP
	rm $FROM_SERVER
	rm $CONTROL_RESPONSE
	
	return 0;
	
}
