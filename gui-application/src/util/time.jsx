const TIME_SPANS = [
    {
        label: 'sekunder',
        max: 60.0
    },
    {
        label: 'minuter',
        max: 60.0
    },
    {
        label: 'timmar',
        max: 24.0
    },
    {
        label: 'dagar',
        max: 365.0 / 12
    },
    {
        label: 'månader',
        max: 12.0
    },
    {
        label: 'år',
        max: Number.MAX_VALUE
    }
]

export function toRelativeTime(secondsAgo) {
    let value = secondsAgo
    for (let timeSpan of TIME_SPANS) {
        if (value < timeSpan.max) {
            return Math.round(value) + ' ' + timeSpan.label
        }
        value /= timeSpan.max
    }
}