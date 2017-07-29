export function parseHash(urlHash) {
    const pathComponents = [];
    const parts = urlHash.split(/\//);
    // console.log("parts", parts)
    for (let i = 0; i < parts.length; i += 2) {
        pathComponents.push({resource: parts[i], key: parts[i + 1] || ""});
    }
    // console.log("pathComponents", pathComponents)
    return pathComponents;
}

export function isPathMatch(pathComponents, pattern) {
    const patternComponents = parseHash(pattern);
    // console.log(pathComponents, patternComponents);
    if (patternComponents.length != pathComponents.length) {
        return false;
    }
    for (let i = 0; i < patternComponents.length; i++) {
        if (pathComponents[i].resource != patternComponents[i].resource) {
            return false;
        }
        if (patternComponents[i].key != '*' && pathComponents[i].key != patternComponents[i].key) {
            return false;
        }
    }
    // console.log('Match on ' + pattern);
    return true;
}