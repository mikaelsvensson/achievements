import $ from "jquery";
import {parseHash, isPathMatch} from "./util/routing.jsx";
import {renderMain} from "./main.jsx";
import {renderStats} from "./stats.jsx";
import {renderOrganization} from "./organization.jsx";
import {renderOrganizations} from "./organizations.jsx";
import {renderAchievement} from "./achievement.jsx";
import {renderAchievements} from "./achievements.jsx";
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
        const appPathParams = parseHash(appPath);

        const routes = {
            'stats': renderStats,
            'organizations': renderOrganizations,
            'organizations/*': renderOrganization,
            'achievements': renderAchievements,
            'achievements/*': renderAchievement,
            '': renderMain
        };

        for (let routePattern in routes) {
            if (isPathMatch(appPathParams, routePattern)) {
                const routeRenderer = routes[routePattern];
                routeRenderer.call(this, appPathParams);
                break;
            }
        }
    };

    $(window).trigger('hashchange');
});