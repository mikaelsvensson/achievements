import $ from "jquery";
import {renderError} from "../error.jsx";
import {renderErrorBlock} from "../error-block.jsx";

const jwtDecode = require('jwt-decode');

const API_HOST = process.env.API_HOST;

let refreshTokenTimeoutHandle = null;

function beforeSendHandler(xhr) {
    const username = localStorage.getItem("username");
    const password = localStorage.getItem("password");
    const token = localStorage.getItem("token");
    const tokenGoogle = localStorage.getItem("token_google");
    const onetimePassword = localStorage.getItem("onetime_password");

    // console.log(username, password, tokenGoogle, token);

    if (username && password) {
        xhr.setRequestHeader("Authorization", "Basic " + btoa(username + ":" + password));
    } else if (token) {
        xhr.setRequestHeader("Authorization", "JWT " + token);
    } else if (tokenGoogle) {
        xhr.setRequestHeader("Authorization", "Google " + tokenGoogle);
    } else if (onetimePassword) {
        xhr.setRequestHeader("Authorization", "OneTime " + btoa(onetimePassword));
        // TODO: Storing one-time password in Local Storage and then removing it is a hack -- would be better to supply one-time password as an argument of some kind.
        localStorage.removeItem("onetime_password")
    }
}

export function isLoggedIn() {
    const isLoggedIn = (!!localStorage.getItem("username") && !!localStorage.getItem("password"))
        || !!localStorage.getItem("token")
        || !!localStorage.getItem("token_google");
    return isLoggedIn;
}

export function setCredentials(username, password) {
    unsetAuth(null);
    localStorage.setItem("username", username);
    localStorage.setItem("password", password);
}

export function setOneTimePassword(password) {
    unsetAuth(null);
    localStorage.setItem("onetime_password", password);
}

export function setToken(token) {
    unsetAuth(null);
    localStorage.setItem("token", token);
    const jwt = jwtDecode(token);
    initTokenRefresh();
    localStorage.setItem("user_organization", jwt.organization);
}

export function getUserOrganization() {
    return localStorage.getItem("user_organization");
}

export function setGoogleToken(tokenGoogle) {
    unsetAuth(null);
    localStorage.setItem("token_google", tokenGoogle);
}

export function unsetAuth(googleApiAuth2) {
    localStorage.removeItem("username");
    localStorage.removeItem("password");
    localStorage.removeItem("token");
    localStorage.removeItem("token_google");
    localStorage.removeItem("user_organization");
    localStorage.removeItem("onetime_password");
    if (refreshTokenTimeoutHandle) {
        console.log("Clearing existing refresh timeout handle");
        clearTimeout(refreshTokenTimeoutHandle);
        refreshTokenTimeoutHandle = null;
    }
}

export function post(url, dataObject, onSuccess, onFail) {
    $.ajax({
        url: API_HOST + url,
        type: "POST",
        data: JSON.stringify(dataObject),
        dataType: "json",
        contentType: "application/json; charset=utf-8",
        beforeSend: beforeSendHandler
    })
        .done(onSuccess)
        .fail(typeof onFail === 'function' ? onFail : function (jqXHR, textStatus, errorThrown) {
                const status = jqXHR.status;
                renderError(`Kan inte skapa ${url} eftersom servern svarade med felkod ${status}.`, status == 401)
            }
        );
}

export function remove(url, dataObject, onSuccess, onFail) {
    $.ajax({
        url: API_HOST + url,
        type: "DELETE",
        data: JSON.stringify(dataObject),
        dataType: "json",
        contentType: "application/json; charset=utf-8",
        beforeSend: beforeSendHandler
    })
        .done(onSuccess)
        .fail(typeof onFail === 'function' ? onFail : function (jqXHR, textStatus, errorThrown) {
                const status = jqXHR.status;
                renderError(`Kan inte ta bort ${url} eftersom servern svarade med felkod ${status}.`, status == 401)
            }
        );
}

export function put(url, dataObject, onSuccess, onFail) {
    internalPut(url, 'application/json; charset=utf-8', JSON.stringify(dataObject), onSuccess, onFail);
}

export function putCsv(url, data, onSuccess, onFail) {
    internalPut(url, 'text/csv', data, onSuccess, onFail);
}

export function postFormData(url, data, onSuccess, onFail) {
    $.ajax({
        url: API_HOST + url,
        type: 'POST',
        data: data,
        dataType: "json",
        // contentType: 'multipart/form-data',
        contentType: false, // NEEDED, DON'T OMIT THIS (requires jQuery 1.6+)
        processData: false, // NEEDED, DON'T OMIT THIS
        beforeSend: beforeSendHandler
    })
        .done(onSuccess)
        .fail(typeof onFail === 'function' ? onFail : function (jqXHR, textStatus, errorThrown) {
                const status = jqXHR.status;
                renderError(`Kan inte skapa ${url} eftersom servern svarade med felkod ${status}.`, status === 401)
            }
        );
}

function internalPut(url, contentType, data, onSuccess, onFail) {
    $.ajax({
        url: API_HOST + url,
        type: "PUT",
        data: data,
        dataType: "json",
        contentType: contentType,
        beforeSend: beforeSendHandler
    })
        .done(onSuccess)
        .fail(typeof onFail === 'function' ? onFail : function (jqXHR, textStatus, errorThrown) {
                const status = jqXHR.status;
            renderError(`Kan inte skapa ${url} eftersom servern svarade med felkod ${status}.`, status === 401)
            }
        );
}

export function get(url, onSuccess, onFail) {
    $.ajax({
        url: API_HOST + url,
        type: "GET",
        dataType: "json",
        contentType: "application/json; charset=utf-8",
        beforeSend: beforeSendHandler
    })
        .done(onSuccess)
        .fail(typeof onFail === 'function' ? onFail : function (jqXHR, textStatus, errorThrown) {
                const status = jqXHR.status;
                const isAuthFailure = status == 401 || status == 403;
                let requestOrganizationId;
                if (isAuthFailure) {
                    const matches = /organizations\/([a-zA-Z0-9-]{22})/.exec(url);
                    if (matches && matches.length >= 2) {
                        requestOrganizationId = matches[1];
                    }
                }
                renderError(`Kan inte visa sidan pga. fel ${status}.`,
                    isAuthFailure,
                    requestOrganizationId,
                    url,
                    status,
                    isLoggedIn())
            }
        );
}

export function createOnFailHandler(container, button) {
    return function (jqXHR, textStatus, errorThrown) {
        if (button) {
            button.removeClass('is-loading');
        }
        //TODO: Add this kind of error handling to all post, put and get calls.
        if (container && container.length == 1) {
            renderErrorBlock(jqXHR.responseJSON.message, container);
        } else {
            renderError(jqXHR.responseJSON.message);
        }
        console.log("ERROR: " + errorThrown);
    }
}

export function initTokenRefresh() {
    if (!isLoggedIn()) {
        console.log("No need to init token refresh -- user not logged in.")
        return;
    }
    try {
        const token = localStorage.getItem("token");
        const jwt = jwtDecode(token);
        const msLeft = jwt.exp * 1000 - new Date().getTime();
        if (msLeft > 0) {
            const msToRefresh = msLeft - 60 * 1000;

            console.log("" +
                (msLeft / 1000 / 60) + " minutes left until token expires. " +
                (msToRefresh / 1000 / 60) + " minutes left until token will be refreshed.");

            if (refreshTokenTimeoutHandle) {
                console.log("Clearing existing refresh timeout handle");
                clearTimeout(refreshTokenTimeoutHandle);
                refreshTokenTimeoutHandle = null;
            }
            refreshTokenTimeoutHandle = setTimeout(function () {
                console.log("Refreshing token");
                post('/api/signin', null,
                    function (responseData) {
                        console.log("Got new token");
                        setToken(responseData.token);
                    },
                    function () {
                        console.log("Could not refresh token");
                    }
                );
            }, msToRefresh);
        } else {
            // Token has expired already
            unsetAuth(null);
        }
    } catch (e) {
        console.log('Something went wrong when setting up token refresh handler. Maybe the token has expired already.', e.message);
        unsetAuth(null);
    }
}

export function sortBy(field) {
    return function (a, b) {
        if (a[field] < b[field]) {
            return -1;
        } else if (a[field] > b[field]) {
            return 1;
        } else {
            return 0;
        }
    }
}