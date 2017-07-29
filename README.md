# Why?

# Testing

## Create a Demo Organization

Step 1: Start the server.

    $ java -jar server-application-1.0-SNAPSHOT.jar server environments/local.yaml

Step 2: Run administrative task (note that port number is 8081).

    $ curl -X POST http://localhost:8081/tasks/bootstrap-data
    
    Created organization Monsters, Inc. (id 9b71ee25-af0b-4b1e-962b-ee3baa93464d)
    Created person James P. Sullivan (id 6cdba3bd-a74c-43e0-aedb-5accae15e0fd)
    Created person Mike Wazowski (id a8266755-555f-4b07-8598-4c2067699ad0)
    Created person Randall Boggs (id 5e9bfa6e-2a9c-4dc5-b339-5abf0dcaf37b)
    Created person Celia Mae (id cbd39b48-15a6-4d0b-ba0f-a818e1ef26a0)
    Created person Roz (id 48275dca-85bc-475b-b281-6056734a2cf5)
