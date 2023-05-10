#!/bin/bash
logFN=`basename $0`.log

ALBBaseURL="http://internal-pn-in-Appli-W4JQG2LOHGXX-1267308714.eu-central-1.elb.amazonaws.com:8080"

while getopts 'asepc' opt ; do
  case "$opt" in
    a)
      sendSMS=1
      sendEmail=1
      sendPEC=1
      sendPaper=1
    ;;
    s)
      sendSMS=1
    ;;
    e)
      sendEmail=1
    ;;
    p)
      sendPEC=1
    ;;
    c)
      sendPaper=1
    ;;
    ?|h)
      echo "Usage: $(basename $0) -a|-s|-e|-p|-c"
      exit 1
    ;;
  esac
done

if [ x$sendSMS == x -a x$sendEmail == x -a x$sendPEC == x -a x$sendPaper == x ] ; then
  echo "set at least one of -a|-s|-e|-p|-c"
  exit 1
fi

if [ x$sendSMS == x1 ] ; then

  SMSSender="+393293707683"
  SMSReceiver="+393293707683"

  SMSRequestId="TestSMS-"$(date +%s)

  IFS=_
  SMSRequestBody="{\"requestId\":\"${SMSRequestId}\",\"correlationId\":\"string\",\"eventType\":\"string\",\"qos\":\"INTERACTIVE\",\"clientRequestTimeStamp\":\"2023-02-01T07:41:35.717Z\",\"receiverDigitalAddress\":\"${SMSReceiver}\",\"messageText\":\"Test message - requestId: ${SMSRequestId}\",\"senderDigitalAddress\":\"${SMSSender}\",\"channel\":\"SMS\",\"tags\":{\"iun\":\"string\",\"additionalProperties\":\"string\"}}"

  echo -n "Invio SMS: ${SMSRequestId} - "
  curl -H "x-pagopa-extch-cx-id: pn-delivery" -H "Accept: application/json" -H "Content-type: application/json" -X PUT -d ${SMSRequestBody} ${ALBBaseURL}/external-channels/v1/digital-deliveries/courtesy-simple-message-requests/${SMSRequestId}
  [ $? -eq 0 ] && echo "Ok" || echo "***KO***"
  unset IFS
fi

if [ x$sendEmail == x1 ] ; then

  EmailSender="federicotestpn@outlook.it"
  EmailReceiver="mario.ottone@dgsspa.com"

  EmailRequestId="TestEmail-"$(date +%s)

  IFS=_
  EmailRequestBody="{\"requestId\":\"${EmailRequestId}\",\"correlationId\":\"string\",\"eventType\":\"string\",\"qos\":\"INTERACTIVE\",\"clientRequestTimeStamp\":\"2023-02-01T07:41:35.717Z\",\"receiverDigitalAddress\":\"${EmailReceiver}\",\"messageText\":\"Test message - requestId: ${EmailRequestId}\",\"senderDigitalAddress\":\"${EmailSender}\",\"channel\":\"EMAIL\",\"tags\":{\"iun\":\"string\",\"additionalProperties\":\"string\"},\"subjectText\":\"Test message\",\"messageContentType\":\"text/plain\",\"attachmentsUrls\":[]}"

  echo -n "Invio Email: ${EmailRequestId} - "
  curl -H "x-pagopa-extch-cx-id: pn-delivery" -H "Accept: application/json" -H "Content-type: application/json" -X PUT -d ${EmailRequestBody} ${ALBBaseURL}/external-channels/v1/digital-deliveries/courtesy-full-message-requests/${EmailRequestId}
  [ $? -eq 0 ] && echo "Ok" || echo "***KO***"
  unset IFS
fi
