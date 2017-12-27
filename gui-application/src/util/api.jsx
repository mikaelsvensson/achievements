import $ from "jquery";
import {renderError} from "../error.jsx";

function beforeSendHandler(xhr) {
    const username = localStorage.getItem("username");
    const password = localStorage.getItem("password");
    if (username && password) {
        xhr.setRequestHeader("Authorization", "Basic " + btoa(username + ":" + password));
    }
}

export function isLoggedIn() {
    return !!localStorage.getItem("username") && !!localStorage.getItem("password");
}

export function setCredentials(username, password) {
    localStorage.setItem("username", username);
    localStorage.setItem("password", password);
}

export function post(url, dataObject, onSuccess, onFail) {
    $.ajax({
        url: url,
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

export function put(url, dataObject, onSuccess, onFail) {
    $.ajax({
        url: url,
        type: "PUT",
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

export function get(url, onSuccess, onFail) {
    $.ajax({
        url: url,
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
