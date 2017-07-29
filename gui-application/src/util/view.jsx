import $ from "jquery";

export function updateView(contentNodes, container) {
    const node = container || $('#app');
    node.empty().append(contentNodes);
}

export function getFormData($form) {
    var unindexed_array = $form.serializeArray();
    var indexed_array = {};

    $.map(unindexed_array, function (n, i) {
        indexed_array[n['name']] = n['value'];
    });

    return indexed_array;
}