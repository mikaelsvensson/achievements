# Why? What?

This is a simple web application for keeping track of progress. The primary motivation for developing this app was to 
keep track of how kids are progressing in their knowledge and skills required for certain merit badges.

The core features of this application:
* For instructors and leaders:
  * Easy management of the kids' progress.
* For kids:
  * View of their own progress.
  * Look at all the achievements they can work towards.
* For parents:
  * View of their kids' progress.
* For all:
  * Authentication using Google, Microsoft and Facebook.

The application is still in its infancy and lacks a lot of the above mentioned features, 
but everything has to start somewhere.

# Can I Try It?

This service is not yet publicly available but we plan to make it free for everyone.

# How?

Required to run the application (back-end and front-end):

 * Maven
 * Docker
 * Java 8 SDK
 * NPM
 
Strongly recommended:

 * Google Client Id

## Run Locally

Start back-end

    $ mvn clean install
    $ docker-compose build
    $ docker-compose up

If you want to enable Google Sign-In, you must set your app's Google client id in the environment variable GOOGLE_CLIENT_ID:

    $ export GOOGLE_CLIENT_ID=your_client_id_goes_here

Start front-end

    gui-application$ npm run start
    
    
Visit http://localhost:9090/

## Debug Locally

Start back-end


    $ docker-compose up database
    $ java -jar server-application-1.0-SNAPSHOT.jar server environments/local.yaml -Ddw.database.url=jdbc:postgresql://localhost:6543/achievements


Start front-end

    gui-application$ npm run start
    
Visit http://localhost:9090/

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

Step 3: Start the GUI

    $ npm run start
    
## Authentication

### Signing Up

**User wants to sign-up to service as first member of a new organization:**

By POSTint to /organizations/signup you do these things:

  * Create a new person (you) with credentials. The credentials can be a regular password or a Google authentication token.
  * Create a new organization (yours).

**User wants to sign-up to service as member of an existing organization:**

By POSTint to /organizations/{id}/signup you do these things:

  * Find yourself in the system.
  * Associate your credentials with the existing person, i.e. you. The credentials can be a regular password or a Google authentication token.

**Users who are not associated with any organization, i.e. system administrators:**

  * Cannot be created using API or GUI (maybe using the API on the admin port in the future).
  * Created as Person without Organization.

The sign-up resource:
 * No authentication required.
 * Identification tokens from external identity providers could be supplied in http body.

### Signing In

After signing up, the user can log in by POSTing to /signin. Credentials are submitted in the Authorization header.

The response from /signin contains a token (a JWT string) which should be submitted in the Authorization header in subsequent requests:

    POST http://localhost:8080/api/signin
    Authorization: Basic dXNlcm5hbWU6cGFzc3dvcmQ=

    {"token":"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWV9.TJVA95OrM7E2cBab30RMHrHDcEfxjoYZgeFONFh7HgQ"}

Subsequest authorized request:

    GET http://localhost:8080/api/my/profile
    Authorization: JWT eeyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWV9.TJVA95OrM7E2cBab30RMHrHDcEfxjoYZgeFONFh7HgQ
    
    {
      "organization": {
        "id": "WMiIpiLdTqOFiaU8wHSQJA",
        "name": "Acme"
      },
      "person": {
        "id": 1,
        "name": "alice",
        "email": "alice@example.com",
        "custom_identifier": null,
        "organization": {
          "name": "Acme"
        }
      }
    }
