# Login to docker. Generates a .dockercfg file that is needed to 
# pull private docker repositories on this machines
docker login

# Verify the cluster us running
fleetctl list-machines

# Start redis
fleetctl submit redis.service
fleetctl load redis.service
fleetctl start redis.service

# Submit the unit file templates
fleetctl submit blikk-crawler@.service blikk-crawler-discovery@.service
fleetctl list-unit-files

# Load the units onto a server
fleetctl start blikk-crawler@10000.service blikk-crawler-discovery@10000.service
fleetctl list-units
fleetctl journal -f blikk-crawler@10000.service

# When done, stop the units
fleetctl stop blikk-crawler@10000.service
fleetctl list-units

# Destroy the units
fleetctl destroy blikk-crawler@10000.service blikk-crawler-discovery@10000.service
fleetctl unload blikk-crawler@10000.service blikk-crawler-discovery@10000.service

# Destroy the templates
fleetctl destroy blikk-crawler@.service blikk-crawler-discovery@.service