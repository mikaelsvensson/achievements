# Why?

# Testing

## Create a Demo Organization

Step 1: Start the server.

    $ java -jar server-application-1.0-SNAPSHOT.jar server environments/local.yaml

Step 2: Run administrative task (note that port number is 8081).

    $ curl -X POST http://localhost:8081/tasks/bootstrap-data
    
    Created organization Monsters, Inc. (id 5e8dbb62-009d-4f15-8808-c07b5866db39)
    Created person James P. Sullivan (id 1)
    Created person Mike Wazowski (id 2)
    Created person Randall Boggs (id 3)
    Created person Celia Mae (id 4)
    Created person Roz (id 5)