import $ from "jquery";
import {renderErrorAlert} from "../error-alert.jsx";

const jwtDecode = require('jwt-decode');

const API_HOST = process.env.API_HOST;

let refreshTokenTimeoutHandle = null;

function createBeforeSendHandler(button) {
    return function (xhr) {
        addAuthHeaders(xhr);
        if (button) {
            button.addClass('is-loading');
        }
    }
}

function createAlwaysHandler(button) {
    return function () {
        // Remove the "page level" loading animation if it is shown:
        $('#app').find('div.loader').closest('#app').empty();

        // Remove the "button" loading animation if it is shown:
        if (button) {
            button.removeClass('is-loading');
        }
    }
}

function addAuthHeaders(xhr) {
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

export function post(url, dataObject, onSuccess, button, extraHeaders = {}) {
    $.ajax({
        url: API_HOST + url,
        type: "POST",
        data: JSON.stringify(dataObject),
        dataType: "json",
        headers: extraHeaders,
        contentType: "application/json; charset=utf-8",
        beforeSend: createBeforeSendHandler(button)
    })
        .done(onSuccess)
        .always(createAlwaysHandler(button))
        .fail(createOnFailHandler(`Kan inte skapa ${url}.`));
}

export function remove(url, dataObject, onSuccess, button) {
    $.ajax({
        url: API_HOST + url,
        type: "DELETE",
        data: JSON.stringify(dataObject),
        dataType: "json",
        contentType: "application/json; charset=utf-8",
        beforeSend: createBeforeSendHandler(button)
    })
        .done(onSuccess)
        .always(createAlwaysHandler(button))
        .fail(createOnFailHandler(`Kan inte ta bort ${url}.`));
}

export function put(url, dataObject, onSuccess, button, extraHeaders = {}) {
    internalPut(url, 'application/json; charset=utf-8', JSON.stringify(dataObject), onSuccess, button, extraHeaders);
}

export function postFormData(url, data, onSuccess, button) {
    $.ajax({
        url: API_HOST + url,
        type: 'POST',
        data: data,
        dataType: "json",
        // contentType: 'multipart/form-data',
        contentType: false, // NEEDED, DON'T OMIT THIS (requires jQuery 1.6+)
        processData: false, // NEEDED, DON'T OMIT THIS
        beforeSend: createBeforeSendHandler(button)
    })
        .done(onSuccess)
        .always(createAlwaysHandler(button))
        .fail(createOnFailHandler(`Kan inte skapa ${url}.`));
}

function internalPut(url, contentType, data, onSuccess, button, extraHeaders = {}) {
    $.ajax({
        url: API_HOST + url,
        type: "PUT",
        data: data,
        dataType: "json",
        contentType: contentType,
        headers: extraHeaders,
        beforeSend: createBeforeSendHandler(button)
    })
        .done(onSuccess)
        .always(createAlwaysHandler(button))
        .fail(createOnFailHandler(`Kan inte uppdatera ${url}.`));
}

export function get(url, onSuccess, button) {
    $.ajax({
        url: API_HOST + url,
        type: "GET",
        dataType: "json",
        contentType: "application/json; charset=utf-8",
        beforeSend: createBeforeSendHandler(button)
    })
        .done(onSuccess)
        // TODO: Replace with createOnFailHandler
        .always(createAlwaysHandler(button))
        .fail(createOnFailHandler(`Kan inte visa sidan.`));
}

function createOnFailHandler(message) {
    return function (jqXHR, textStatus, errorThrown) {
        //TODO: Add this kind of error handling to all post, put and get calls.

        const responseMessage = jqXHR.responseJSON && jqXHR.responseJSON.message ? jqXHR.responseJSON.message : null

        if (400 <= jqXHR.status && jqXHR.status < 500) {
            if (jqXHR.status === 400) {
                renderErrorAlert('Något verkar var galet i det du fyllde i.')
            } else if (jqXHR.status === 401) {
                renderErrorAlert('Du måste vara inloggad för att se den här sidan.', true)
            } else if (jqXHR.status === 403) {
                renderErrorAlert('Du har tyvärr inte möjlighet att se den här sidan.')
            } else if (jqXHR.status === 429) {
                renderErrorAlert('Vi tycker det är för högt tryck på vår server just nu så just nu kan du inte göra det du vill. Vi ber om ursäkt för detta och ber dig försöka igen om en liten stund.')
            } else {
                renderErrorAlert('Vår server gillade inte vad du försökte göra men vi vet tyvärr inte riktigt varför.')
            }
        } else if (500 <= jqXHR.status && jqXHR.status < 600) {
            renderErrorAlert('Vår server misslyckades med det du vill få gjort. Vi ber om ursäkt för detta.')
        } else {
            renderErrorAlert('Tyvärr hände något som vi inte riktigt kan förklara. Därför kan vi heller inte ge något förslag på vad du kan göra för att undvika detta fel.')
        }
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