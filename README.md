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

Yes, visit https://scout-admin.herokuapp.com.

# How?

Required to host the application (end-users of course only need a web browser):

 * Java 8
 * Docker (maybe not strictly necessary but it does make the database set-up easier)
 
Required to build and deploy the application:

 * Java 8 SDK
 * Maven
 * npm
 
Strongly recommended:

 * Google API client Id (enables users to sign in using their Google accounts)
 * Microsoft API client id (enables users to sign in using their Outlook or Hotmail accounts)

## Run Locally

Start back-end

    $ mvn clean install
    $ docker-compose build
    $ docker-compose up

Set the CUSTOMER_SUPPORT_EMAIL to your customer support e-mail address in order to show it on error pages:

    $ export CUSTOMER_SUPPORT_EMAIL=achievements@example.com

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

## Deploy to Heroku

### Before you can deploy

0. Create an Heroku account

0. Install the Heroku CLI

### First deploy

Either use ```heroku-configure.sh.template``` to create a set-up script...

    $ cp heroku-configure.sh.template heroku-configure.sh
    $ chmod +x heroku-configure.sh
    $ ./heroku-configure.sh

...or do the steps manually:

0. Create an Heroku application with a Postgres database

    ```$ heroku apps:create scout-admin --region eu```

    ```$ heroku addons:create heroku-postgresql:hobby-dev```

    The application has now been created and the "Heroku config variable" DATABASE_URL has been set to
    the correct JDBC connection string. The config variable is available as an environment variable.

    The environment variable PORT will be set to the port that the application must listen on.

0. Configure secrets for Heroku application if you want to enable users to sign in using Google and Microsoft:

    ```$ heroku config:set GOOGLE_CLIENT_ID=...```
    
    ```$ heroku config:set GOOGLE_CLIENT_SECRET=...```

    ```$ heroku config:set MICROSOFT_CLIENT_ID=...```
    
    ```$ heroku config:set MICROSOFT_CLIENT_SECRET=...```

0. Configure secrets for Heroku application if you want to enable users to sign in using links sent by e-mail:

    ```$ heroku config:set SMTP_HOST=smtp.googlemail.com```
    
    ```$ heroku config:set SMTP_PORT=465```
    
    ```$ heroku config:set SMTP_USERNAME=your complete gmail address```
    
    ```$ heroku config:set SMTP_PASSWORD=your gmail password```
    
    ```$ heroku config:set SMTP_FROM=your complete gmail address```

0. It is also highly recommended to set the JWT_SIGNING_SECRET to a random string (but of course not this particular one):

    ```$ heroku config:set JWT_SIGNING_SECRET=kQCTd0ypLpoFbRt8vTSaHp37kPsKMdh1wzDiuJUa```

0. Deploy to Heroku using Maven and npm (npm run from Maven)

    ```$ cd server-application```

    ```$ mvn heroku:deploy -Pheroku```

### Subsequent deploys

Build everything from the project root:

    $ mvn clean install
    
Move to the server-application folder:

    $ cd server-application
    $ mvn heroku:deploy -Pheroku
    
### Creating some test data

Create an organization:

    $ heroku ps:exec
    ...
    ~ $ curl -X POST http://localhost:9001/tasks/bootstrap-data


Add some achievements (scout merit badges from www.scouterna.se):

    $ heroku ps:exec
    ...
    ~ $ curl -X POST http://localhost:9001/tasks/import-badges

### Heroku database backup

Create a database backup and download it using this Bash script:

    $ ./heroku-database-download.sh 

### More reading

Want to know more about Java and Heroku? Here are some reading suggestions:

* https://devcenter.heroku.com/articles/getting-started-with-java
* https://devcenter.heroku.com/articles/deploying-java
* https://devcenter.heroku.com/articles/deploying-java-applications-with-the-heroku-maven-plugin
* https://devcenter.heroku.com/articles/logging

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

## Log to Sentry

The application supports logging exceptions to the cloud-based error reporting service Sentry.

First, set the environment variable `SENTRY_DSN` to your Sentry DSN value (retrieved from the Sentry web console)

Then, set this logging configuration in your application configuration file:

    logging:
      loggers:
        "io.dropwizard.jersey.errors.LoggingExceptionMapper":
          appenders:
            - type: sentry
              threshold: WARN
        "se.devscout.achievements.server":
          appenders:
            - type: sentry
              threshold: WARN
    
Optionally, start the application with the Sentry `environment` parameter to enable better filtering in Sentry:
 
    java ... -Dsentry.environment=production ... se.devscout.achievements.server.AchievementsApplication server