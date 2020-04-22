while read SOURCE SERVER DESTINATION
do
  scp -i /Users/rodrigo/invectorIQ/projects/securitize/cordaAzure "$SOURCE" "$SERVER":"$DESTINATION"
done
