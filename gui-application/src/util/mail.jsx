export function initContactUsLinks ($root) {
    const domain = 'gmail.com';
    const user = 'minamarken';

    // TODO: Get address from process.env.CUSTOMER_SUPPORT_EMAIL instead
    $root.find('.mail-link').attr('href', ['mailto:', user, '@', domain].join('')).text([user, '@', domain].join(''));
}