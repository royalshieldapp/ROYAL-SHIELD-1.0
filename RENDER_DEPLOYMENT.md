# Royal Shield Backend - Render Deployment Guide

## Prerequisites
- GitHub account with repository containing the backend code
- Render account (https://render.com)
- Docker image pushed to Docker Hub or use render.yaml

## Step 1: Push to GitHub

```bash
cd backend
git init
git add .
git commit -m "Initial Royal Shield backend"
git remote add origin https://github.com/YOUR_USERNAME/royal-shield-backend.git
git push -u origin main
```

## Step 2: Deploy on Render

### Option A: Using GitHub (Recommended)

1. Go to https://dashboard.render.com/
2. Click **"New +"** → **"Web Service"**
3. Select **"Build and deploy from a Git repository"**
4. Connect your GitHub account and select the `royal-shield-backend` repo
5. Configure:
   - **Name:** `royal-shield-backend`
   - **Runtime:** `Node`
   - **Build Command:** `npm install`
   - **Start Command:** `node src/index.js`
   - **Plan:** Free (or Starter for production)
6. Click **"Create Web Service"**

### Option B: Using Docker

1. Push image to Docker Hub:
   ```bash
   docker tag royal-shield-backend:latest YOUR_DOCKERHUB_USERNAME/royal-shield-backend:latest
   docker login
   docker push YOUR_DOCKERHUB_USERNAME/royal-shield-backend:latest
   ```

2. In Render Dashboard:
   - Click **"New +"** → **"Web Service"**
   - Select **"Deploy an existing image"**
   - Enter: `YOUR_DOCKERHUB_USERNAME/royal-shield-backend:latest`
   - Configure as above

## Step 3: Set Environment Variables

In Render Dashboard, go to **Environment** tab and add:

```
NODE_ENV=production
PORT=3000
VIRUSTOTAL_API_KEY=your_key_here
TWILIO_ACCOUNT_SID=your_sid_here
TWILIO_AUTH_TOKEN=your_token_here
TWILIO_PHONE=+1234567890
EMERGENCY_CONTACT_EMAIL=emergency@yourcompany.com
```

## Step 4: Verify Deployment

Once deployed, Render will provide a URL like:
```
https://royal-shield-backend.onrender.com
```

Test the health endpoint:
```bash
curl https://royal-shield-backend.onrender.com/health
```

Expected response:
```json
{
  "status": "OK",
  "timestamp": "2026-01-08T18:30:00.000Z"
}
```

## Step 5: Update Android App

In your Royal Shield Android app, update the API base URL:

```kotlin
// In your Retrofit/OkHttp configuration or BuildConfig
const val API_BASE_URL = "https://royal-shield-backend.onrender.com"
```

Example endpoint calls from Android:
```kotlin
// Panic Button
POST /api/panic
{
  "userId": "user_123",
  "location": { "lat": 40.7128, "lon": -74.0060 },
  "emergencyContacts": ["contact@example.com"]
}

// Request Patrol
POST /api/patrol
{
  "userId": "user_123",
  "location": { "lat": 40.7128, "lon": -74.0060 }
}

// Scan URL
POST /api/scan-url
{
  "userId": "user_123",
  "url": "https://suspicious-site.com"
}

// Check Breach
POST /api/breach-check
{
  "userId": "user_123",
  "email": "user@example.com"
}
```

## Step 6: Optional - Add Database

For production, add PostgreSQL:

1. In Render, click **"New +"** → **"PostgreSQL"**
2. Name it `royal-shield-db`
3. Render will provide a `DATABASE_URL`
4. Add it to your Web Service environment variables
5. Update `src/index.js` to connect to the database

## Monitoring & Logs

- **View Logs:** Dashboard → Service → Logs tab
- **Health Check:** `/health` endpoint
- **Metrics:** Dashboard → Metrics tab

## Troubleshooting

**Build fails:**
- Check logs in Render dashboard
- Ensure `package-lock.json` is committed to Git
- Verify `npm install` works locally

**App can't connect:**
- Check CORS settings in `src/index.js` (currently allows all origins)
- Verify API base URL in Android app matches Render deployment URL
- Check network requests in Android logcat

**Container won't start:**
- Verify `PORT` environment variable is set
- Check that `src/index.js` exists
- Render logs should show the error

## Docker Image Size
Current: ~150MB (Node 18 Alpine)

To reduce:
```dockerfile
# Use distroless for ~90MB
FROM node:18-alpine AS builder
...
FROM gcr.io/distroless/nodejs18-debian11
```

## Next Steps

1. Integrate Twilio for SMS/emergency alerts
2. Add VirusTotal API for URL scanning
3. Set up PostgreSQL for event logging
4. Add authentication (Firebase/Auth0)
5. Implement real camera streaming
6. Add real-time notifications (WebSockets)
