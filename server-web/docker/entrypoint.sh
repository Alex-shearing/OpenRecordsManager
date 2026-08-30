#!/bin/sh
set -eu

json_escape() {
	printf '%s' "$1" | sed 's/\\/\\\\/g; s/"/\\"/g'
}

API_BASE="${UI_API_BASE_URL%/}"

if [ -n "$API_BASE" ]; then
	ESCAPED="$(json_escape "$API_BASE")"
	sed -i "s#\"apiBaseUrl\": \"\"#\"apiBaseUrl\": \"${ESCAPED}\"#g" /usr/share/nginx/html/index.html
fi

exec nginx -g 'daemon off;'
