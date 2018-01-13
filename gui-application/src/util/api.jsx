import $ from "jquery";
import {renderError} from "../error.jsx";
import {renderErrorBlock} from "../error-block.jsx";

const API_HOST = process.env.API_HOST;

function beforeSendHandler(xhr) {
    const username = localStorage.getItem("username");
    const password = localStorage.getItem("password");
    const token = localStorage.getItem("token");
    const tokenGoogle = localStorage.getItem("token_google");

    // console.log(username, password, tokenGoogle, token);

    if (username && password) {
        xhr.setRequestHeader("Authorization", "Basic " + btoa(username + ":" + password));
    } else if (token) {
        xhr.setRequestHeader("Authorization", "JWT " + token);
    } else if (tokenGoogle) {
        xhr.setRequestHeader("Authorization", "Google " + tokenGoogle);
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

export function setToken(token) {
    unsetAuth(null);
    localStorage.setItem("token", token);
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
                renderError(`Kan inte skapa ${url} eftersom servern svarade med felkod ${status}.`, status == 401)
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