#!/bin/sh
set -eu

# Host-local UI config: only the public API URL (branding comes from GET /api/web).
cat > /usr/share/nginx/html/config.json <<EOF
{
  "apiBaseUrl": "${UI_API_BASE_URL%/}"
}
EOF

exec nginx -g 'daemon off;'
