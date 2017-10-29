import {renderMain} from "../main.jsx";
import {renderStats} from "../stats.jsx";
import {renderOrganization} from "../organizations/organization.jsx";
import {renderOrganizations} from "../organizations/organizations.jsx";
import {renderAchievement} from "../achievements/achievement.jsx";
import {renderAchievements} from "../achievements/achievements.jsx";
import {renderLogout} from "../auth/logout.jsx";
import {renderLogin} from "../auth/login.jsx";
import {renderError} from "../error.jsx";
import {renderLoginCreateAccount} from "../auth/login.create-account.jsx";
import {renderPerson} from "../organizations/person.jsx";
import {renderOrganizationSignup} from "../organizations/organization.signup.jsx";

export function navigateTo(appPath) {
    window.location.hash = '#' + appPath;
}

export function hashChangeHandler() {
    // On every hash change the render function is called with the new hash.
    // This is how the navigation of our app happens.

    const appPath = decodeURI(window.location.hash.substr(1));

    renderRoute(appPath);
}

function renderRoute(appPath) {
    const appPathParams = parseHash(appPath);

    const routes = {
        'statistik': renderStats,
        'loggaut': renderLogout,
        'loggain': renderLogin,
        'loggain-skapa-konto': renderLoginCreateAccount,
        'karer': renderOrganizations,
        'karer/*': renderOrganization,
        'karer/*/borja': renderOrganizationSignup,
        'karer/*/personer/*': renderPerson,
        'marken': renderAchievements,
        'marken/*': renderAchievement,
        '': renderMain
    };

    for (let routePattern in routes) {
        if (isPathMatch(appPathParams, routePattern)) {
            const routeRenderer = routes[routePattern];
            routeRenderer.call(this, appPathParams);
            return;
        }
    }
    renderError(appPath + " does not exist.");
}

function isPathMatch(pathComponents, pattern) {
    const patternComponents = parseHash(pattern);
    if (patternComponents.length != pathComponents.length) {
        return false;
    }
    for (let i = 0; i < patternComponents.length; i++) {
        if (patternComponents[i].resource != pathComponents[i].resource) {
            return false;
        }
        if (patternComponents[i].key != '*' && pathComponents[i].key != patternComponents[i].key) {
            return false;
        }
    }
    return true;
}

function parseHash(urlHash) {
    const pathComponents = [];
    const parts = urlHash.split(/\//);
    for (let i = 0; i < parts.length; i += 2) {
        pathComponents.push({resource: parts[i], key: parts[i + 1] || ""});
    }
    return pathComponents;
}
