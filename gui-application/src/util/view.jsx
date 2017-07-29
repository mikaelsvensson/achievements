import $ from "jquery";

export function updateView(contentNodes, container) {
    const node = container || $('#app');
    node.empty().append(contentNodes);
}