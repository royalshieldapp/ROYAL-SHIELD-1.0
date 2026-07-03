# GitHub Actions - Auto Approve & Deploy Setup

## Overview

Automated CI/CD pipeline that:
- ✅ Builds and tests on every push/PR
- ✅ Auto-approves Dependabot/Renovate dependency updates
- ✅ Auto-merges safe dependency PRs
- ✅ Pushes Docker image to Docker Hub
- ✅ Triggers Render deployment on main branch

## Workflows

### 1. **ci-cd.yml** - Main Pipeline
Runs on every push and PR:
- Install dependencies
- Lint (if configured)
- Run tests (if configured)
- Build Docker image
- Push to Docker Hub (main branch only)
- Auto-approve bot PRs
- Deploy to Render (main branch only)

### 2. **auto-approve.yml** - Smart Auto-Approval
Auto-approves and merges:
- **Dependabot/Renovate PRs** - Dependency updates
- **Safe changes** - Only package.json/lock files
- **Passing tests** - Only if all checks pass
- **Approved authors** - PRs from trusted team members

## GitHub Secrets Required

Add these in: **Settings** → **Secrets and variables** → **Actions**

```
DOCKER_HUB_USERNAME          # Your Docker Hub username
DOCKER_HUB_PASSWORD          # Your Docker Hub access token
RENDER_SERVICE_ID            # From Render service URL
RENDER_API_KEY               # From Render account settings
GITHUB_TOKEN                 # Auto-provided by GitHub
```

### How to Get Render Credentials:

1. **RENDER_SERVICE_ID:**
   - Go to Render Dashboard → Your Service
   - URL: `https://dashboard.render.com/web/srv-xxxxxxxxxxxxx`
   - Copy the `srv-xxxxxxxxxxxxx` part

2. **RENDER_API_KEY:**
   - Go to Render Account Settings → API Keys
   - Create new key, copy it

### How to Get Docker Hub Credentials:

1. **DOCKER_HUB_USERNAME:** Your Docker Hub account username
2. **DOCKER_HUB_PASSWORD:** 
   - Go to Docker Hub → Account Settings → Security
   - Create a Personal Access Token
   - Copy and use as password

## Setup Instructions

### Step 1: Initialize Git & Push

```bash
cd backend
git init
git add .
git commit -m "Add GitHub Actions workflows"
git remote add origin https://github.com/YOUR_USERNAME/royal-shield-backend.git
git push -u origin main
```

### Step 2: Configure GitHub Secrets

```bash
# Using GitHub CLI (recommended)
gh secret set DOCKER_HUB_USERNAME -b "your_docker_username"
gh secret set DOCKER_HUB_PASSWORD -b "your_docker_token"
gh secret set RENDER_SERVICE_ID -b "srv-xxxxx"
gh secret set RENDER_API_KEY -b "your_render_api_key"

# Or manually in GitHub UI
# Settings → Secrets and variables → Actions → New repository secret
```

### Step 3: Enable Branch Protection (Optional)

Protect main branch from direct pushes:

```bash
gh api repos/:owner/:repo/branches/main/protection \
  -f required_status_checks='{"strict":true,"contexts":["build"]}' \
  -f enforce_admins=false \
  -f dismiss_stale_reviews=true
```

### Step 4: Test the Workflow

1. Create a test branch:
   ```bash
   git checkout -b test/workflow
   echo "test" >> README.md
   git add README.md
   git commit -m "test"
   git push origin test/workflow
   ```

2. Open a PR on GitHub
3. Watch the Actions tab for workflow execution
4. If all checks pass, merge it

## Auto-Approve Rules

The `auto-approve.yml` workflow auto-approves when:

✅ **Dependabot/Renovate** updates dependencies
✅ **Only safe files changed** (package.json, package-lock.json, .md files)
✅ **All CI checks pass**
✅ **No breaking changes** detected

ℹ️ To auto-approve your own PRs, add your GitHub username to `approvedAuthors` in the workflow.

## Deployment Flow

```
Developer pushes to main
    ↓
GitHub Actions builds & tests
    ↓
Build passes?
    ├─ YES → Push Docker image to Docker Hub
    ├─ YES → Trigger Render deployment
    └─ NO → Fail & notify developer
```

## Monitoring & Logs

1. **View Workflow Runs:**
   - GitHub → Actions tab
   - Click on workflow run for details

2. **Real-time Logs:**
   - Click on job (build, deploy, etc.)
   - Expand each step

3. **Render Deployment:**
   - After workflow completes, check Render dashboard
   - Logs appear in Render service logs

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Build fails | Check logs in Actions tab → build job |
| Docker push fails | Verify DOCKER_HUB credentials are correct |
| Render deploy doesn't trigger | Check RENDER_API_KEY and SERVICE_ID are set |
| Tests don't run | Add test script to package.json: `"test": "jest"` |
| Auto-merge not working | Ensure branch protection allows auto-merge |

## Advanced: Manual Trigger

To manually run a workflow:

```bash
gh workflow run ci-cd.yml --ref main
```

## Cost Optimization

- **GitHub Actions:** Free tier includes 2,000 minutes/month
- **Render:** Free tier deployed service, no cost for webhooks
- **Docker Hub:** Free tier, 1 private repo

## Next Steps

1. ✅ Create GitHub repo and push code
2. ✅ Add secrets in GitHub
3. ✅ Test CI/CD pipeline with a test PR
4. ✅ Verify Render deployment
5. ✅ Monitor first auto-deployment
6. ✅ Add more tests/linting as needed

## Example PR Scenarios

### Scenario 1: Dependabot updates express
```
PR: "build(deps): bump express from 4.18.0 to 4.18.2"
↓
Auto-approve checks: ✅ Bot, ✅ Safe files, ✅ Tests pass
↓
Auto-approved & merged
↓
Render deploys new version
```

### Scenario 2: Developer fixes a bug
```
PR: "fix: handle null location in panic endpoint"
↓
Build & tests run
↓
Manual review & approval needed (not auto-approved)
↓
Once approved, merge triggers Render deployment
```

## Disabling Auto-Approve

To disable auto-merge for certain PRs, add label:
```bash
git push origin -o merge_request.draft
```

Or disable workflow: **Actions** → **Disable auto-approve.yml**
