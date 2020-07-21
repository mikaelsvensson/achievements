import {renderMain} from "../main.jsx";
import {renderStats} from "../stats.jsx";
import {renderMyOrganization, renderOrganization} from "../organizations/organization.jsx";
import {renderOrganizations} from "../organizations/organizations.jsx";
import {renderAchievement} from "../achievements/achievement.jsx";
import {renderAchievements} from "../achievements/achievements.jsx";
import {renderSignout} from "../auth/signout.jsx";
import {renderSignin} from "../auth/signin.jsx";
import {renderSigninWaitForEmail} from "../auth/signin.wait-for-email.jsx";
import {renderSigninFailed} from "../auth/signin.failed.jsx";
import {renderRedirect} from "../auth/auth.redirect.jsx";
import {renderError} from "../error.jsx";
import {renderSignup} from "../auth/signup.jsx";
import {renderForgotPassword} from "../auth/forgot-password.jsx";
import {renderPeople} from "../organizations/people.jsx";
import {renderPerson} from "../organizations/person.jsx";
import {renderGroups} from "../organizations/groups.jsx";
import {renderGroup} from "../organizations/group.jsx";
import {renderBatchUpsert} from "../organizations/batchupsert.jsx";
import {renderMyProfile} from "../my/profile.jsx";
import {renderMyPassword, renderPasswordForgotten} from "../my/password.jsx";
import {renderOrganizationSignup} from "../organizations/organization.signup.jsx";
import {renderOrganizationAchievements} from "../organizations/organization.achievements.jsx";
import {renderAbout} from "../about.jsx";
import {renderImportScouternaSe} from "../admin/import-scouterna-se.jsx";

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
        'minprofil/losenord': renderMyPassword,
        'minprofil': renderMyProfile,
        'minkar': renderMyOrganization,
        'loggaut': renderSignout,
        'loggain': renderSignin,
        'skapa-konto': renderSignup,
        'glomt-losenord': renderForgotPassword,
        //TODO: Localize routes
        'set-password/*': renderPasswordForgotten,
        'karer': renderOrganizations,
        'karer/*': renderOrganization,
        'karer/*/marken': renderOrganizationAchievements,
        'karer/*/skapa-konto': renderOrganizationSignup,
        'karer/*/importera': renderBatchUpsert,
        'karer/*/personer': renderPeople,
        'karer/*/personer/*': renderPerson,
        'karer/*/patruller': renderGroups,
        'karer/*/patruller/*': renderGroup,
        'marken': renderAchievements,
        'marken/vy/sok/*': renderAchievements,
        'marken/*': renderAchievement,
        'signin-failed': renderSigninFailed,
        'signin-failed/*': renderSigninFailed,
        'signin/check-mail-box': renderSigninWaitForEmail,
        'signin/*': renderRedirect,
        'om': renderAbout,
        'admin/importera': renderImportScouternaSe,
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
