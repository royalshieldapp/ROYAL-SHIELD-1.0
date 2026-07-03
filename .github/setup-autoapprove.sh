#!/bin/bash
# GitHub Actions setup for Royal Shield Backend - Pre-configured

set -e

echo "🚀 Royal Shield Backend - GitHub Actions Setup"
echo "=============================================="
echo ""

# Check if gh CLI is installed
if ! command -v gh &> /dev/null; then
    echo "❌ GitHub CLI (gh) not found. Install it from: https://cli.github.com"
    exit 1
fi

# Authenticate if needed
gh auth status > /dev/null 2>&1 || {
    echo "🔐 Authenticating with GitHub..."
    gh auth login
}

# Get repo info
OWNER=$(gh api user --jq '.login')
REPO="royal-shield-backend"

echo "📦 Repository: $OWNER/$REPO"
echo ""

# Credentials
DOCKER_USERNAME="royalshieldapp"
RENDER_SERVICE_ID="srv-d5qr6hshg0os73cii1s0"
RENDER_API_KEY="rnd_NGZkV1tf1fC6PonKqJu09TubHCFB"

echo "🔑 Setting up GitHub Secrets..."
echo ""

read -sp "Enter Docker Hub access token (password): " DOCKER_PASSWORD
echo ""

# Set secrets
gh secret set DOCKER_HUB_USERNAME --body "$DOCKER_USERNAME" --repo "$OWNER/$REPO"
gh secret set DOCKER_HUB_PASSWORD --body "$DOCKER_PASSWORD" --repo "$OWNER/$REPO"
gh secret set RENDER_SERVICE_ID --body "$RENDER_SERVICE_ID" --repo "$OWNER/$REPO"
gh secret set RENDER_API_KEY --body "$RENDER_API_KEY" --repo "$OWNER/$REPO"

echo "✅ All secrets configured"
echo ""
echo "Configured credentials:"
echo "  • Docker Hub: $DOCKER_USERNAME"
echo "  • Render Service: $RENDER_SERVICE_ID"
echo "  • Render API Key: [REDACTED]"
echo ""

# Configure branch protection
echo "🛡️  Configuring branch protection for 'main'..."

gh api repos/:owner/:repo/branches/main/protection \
  --input /dev/stdin <<EOF
{
  "required_status_checks": {
    "strict": true,
    "contexts": ["build"]
  },
  "enforce_admins": false,
  "dismiss_stale_reviews": true,
  "require_code_owner_reviews": false,
  "required_approving_review_count": 0,
  "allow_force_pushes": false,
  "allow_deletions": false
}
EOF

echo "✅ Branch protection enabled"
echo ""

echo "🎉 Setup complete!"
echo ""
echo "📋 Next steps:"
echo "1. Initialize git repo:"
echo "   cd backend"
echo "   git init"
echo "   git add ."
echo "   git commit -m 'Initial Royal Shield backend'"
echo ""
echo "2. Add remote and push:"
echo "   git remote add origin https://github.com/YOUR_USERNAME/royal-shield-backend.git"
echo "   git push -u origin main"
echo ""
echo "3. Watch the magic:"
echo "   - GitHub Actions builds & tests"
echo "   - Docker image pushed to Docker Hub"
echo "   - Render automatically deploys"
echo ""
echo "✨ Every commit to main = auto-deploy to Render"
echo "✨ Dependabot PRs = auto-approved & merged"
