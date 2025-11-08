#!/bin/bash
set -e

echo "🔧 Setting up backend environment..."

if ! command -v python3 >/dev/null 2>&1; then
  echo "❌ Python 3 not found. Please install Python 3.9+"; exit 1
fi
echo "✅ Python: $(python3 --version)"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "📦 Installing dependencies..."
python3 -m pip install -r requirements.txt

if [ ! -f .env ]; then
  if [ -f .env.example ]; then
    cp .env.example .env
    echo "📋 Created .env from .env.example. Fill in values as needed."
  else
    echo "⚠️  .env.example missing; skipping .env creation."
  fi
fi

if [ ! -f firebase-credentials.json ] && [ -z "$FIREBASE_CREDENTIALS" ]; then
  echo "⚠️  Firebase credentials not found. Add firebase-credentials.json or set FIREBASE_CREDENTIALS."
else
  echo "✅ Firebase credentials present (file or env)."
fi

if [ -z "$OPENAI_API_KEY" ]; then
  echo "ℹ️  OPENAI_API_KEY not set. Backend will use dummy candidates."
else
  echo "✅ OPENAI_API_KEY detected."
fi

echo "✅ Setup complete. Start the server with: python3 server.py"


