# Royal Shield Backend

Backend Node.js/Express para la app Royal Shield.

## Endpoints

| Método | Endpoint | Descripción |
| :--- | :--- | :--- |
| **GET** | `/api/phone/check?number=...` | Verifica reputación de teléfono |
| **POST** | `/api/sos/alert` | Envía alerta SOS |
| **GET** | `/api/threats?lat=...&lng=...` | Datos simulados de amenazas en mapa |
| **GET** | `/api/courses` | Lista de cursos de ciberseguridad |
| **GET** | `/api/system/status` | Estado operativo del sistema |
| **GET** | `/api/loyalty/status` | Puntos y nivel del usuario |
| **POST** | `/api/loyalty/points` | Añadir puntos por acciones |
| **POST** | `/api/scan/url` | Escanear URL con VirusTotal (Proxy) |

## Despliegue en Render

1. Crea un **Web Service** en Render.
2. Conecta tu repositorio.
3. Configura:
    * **Root Directory**: `backend` (Importante si está en subcarpeta)
    * **Build Command**: `npm install`
    * **Start Command**: `node server.js`
4. Agrega **Environment Variables**:
    * `VIRUSTOTAL_API_KEY`: Tu clave real (No hardcodeada en la app)
    * `TWILIO_ACCOUNT_SID` / `TWILIO_AUTH_TOKEN`: Para SMS reales

## Ejecutar Localmente

```bash
cd backend
npm install
npm start
```
