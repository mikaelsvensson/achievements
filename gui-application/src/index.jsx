import $ from "jquery";
import styles from "bulma/css/bulma.css";
import {hashChangeHandler} from "./util/routing.jsx";
import tableStyles from "./table.css";
import mdiStyles from "mdi/css/materialdesignicons.css";

// GOOGLE_CLIENT_ID fetched from environment variable by Webpack during build.
const GOOGLE_CLIENT_ID = process.env.GOOGLE_CLIENT_ID;

$(function () {
    $(window).on('hashchange', hashChangeHandler);

    const s = styles;
    const ts = tableStyles;
    const mdis = mdiStyles;

    $(window).trigger('hashchange');
});

window.onGoogleApiLoaded = function () {
    console.log("Google API client loaded");
    window.gapi.load('auth2', function () {
        console.log("Google auth2 library loaded");
        window.gapi.auth2.init({client_id: GOOGLE_CLIENT_ID});
    });
};