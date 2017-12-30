const scriptjs = require("scriptjs");

// GOOGLE_CLIENT_ID fetched from environment variable by Webpack during build.
const GOOGLE_CLIENT_ID = process.env.GOOGLE_CLIENT_ID;

export function initGoogleSigninButton(onSuccess, onFailure) {
    scriptjs("https://apis.google.com/js/platform.js", function () {
        console.log("Google API client loaded");
        window.gapi.load('auth2', function () {
            console.log("Google auth2 library loaded");
            window.gapi.auth2.init({client_id: GOOGLE_CLIENT_ID});
            window.gapi.load('signin2', function () {
                console.log("Google signin2 library loaded");
                window.gapi.signin2.render('googleSigninButtonContainer', {
                    'scope': 'profile email',
                    'longtitle': true,
                    'theme': 'dark',
                    'onsuccess': onSuccess,
                    'onfailure': onFailure
                });
            });
        });
    });

}

export function googleSignOut() {
    scriptjs("https://apis.google.com/js/platform.js", function () {
        console.log("Google API client loaded");
        window.gapi.load('auth2', function () {
            const googleApiAuth2 = window.gapi.auth2;
            if (googleApiAuth2) {
                const auth2 = googleApiAuth2.getAuthInstance();
                if (auth2) {
                    auth2.signOut().then(function () {
                        console.log('Google user has been signed out.');
                    });
                } else {
                    console.log("No Google Auth instance found");
                }
            } else {
                console.log("No Google API client provided");
            }
        });
    });
}