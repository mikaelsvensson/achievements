import $ from "jquery";

export function post(url, dataObject, onSuccess) {
    $.ajax({
        url: url,
        type: "POST",
        data: JSON.stringify(dataObject),
        dataType: "json",
        contentType: "application/json; charset=utf-8",
        success: onSuccess
    });
}

export function get(url, onSuccess) {
    $.ajax({
        url: url,
        type: "GET",
        dataType: "json",
        contentType: "application/json; charset=utf-8",
        success: onSuccess
    });
}
