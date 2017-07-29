import $ from "jquery";
import {parseHash, isPathMatch} from "./util/routing.jsx";
import {renderMain} from "./main.jsx";
import {renderStats} from "./stats.jsx";
import {renderOrganization} from "./organization.jsx";
import {renderOrganizations} from "./organizations.jsx";
import styles from "bulma/css/bulma.css";

$(function () {
    $(window).on('hashchange', function () {
        // On every hash change the render function is called with the new hash.
        // This is how the navigation of our app happens.

        const appPath = decodeURI(window.location.hash.substr(1));

        render(appPath);
    });

    const s = styles;

    const render = function (appPath) {
        // console.log('Path has changed to this: ' + appPath);
        const appPathParams = parseHash(appPath);
        if (isPathMatch(appPathParams, 'stats')) {
            renderStats();
        } else if (isPathMatch(appPathParams, 'organizations')) {
            renderOrganizations();
        } else if (isPathMatch(appPathParams, 'organizations/*')) {
            renderOrganization(appPathParams);
        } else {
            renderMain();
        }
    };

    $(window).trigger('hashchange');
});