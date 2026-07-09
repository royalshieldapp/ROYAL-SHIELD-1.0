# Arquitectura de Red y Seguridad (Royal Shield)

Este documento describe la topología de red segura empleada en el backend de Royal Shield, asegurando que todos los componentes sensibles estén blindados del Internet público.

## 1. Topología de Exposición

| Componente | Puerto Nativo | Expuesto al Público? | Método Seguro de Acceso |
| :--- | :--- | :--- | :--- |
| **API FastAPI** | `8000` | **SÍ** (via Proxy) | A través de Nginx/Caddy en `443` (HTTPS) |
| **PostgreSQL** | `5432` | **NO** | SSH Tunnel o Interfaz de VPN |
| **Redis** | `6379` | **NO** | Solo red interna de Docker / VPN |
| **pgAdmin** | `5050` | **NO** | SSH Tunnel o Interfaz de VPN |
| **Servidor SSH**| `22` | **NO** (Idealmente) | Tailscale SSH |

## 2. Docker e Iptables (El Peligro Oculto)

En Docker, exponer un puerto usando `ports: - "5432:5432"` inyecta automáticamente reglas en `iptables` que bypassean al firewall (UFW).
Para evitar que la base de datos se exponga accidentalmente al mundo, **siempre utilizamos un `docker-compose.override.yml`** (o configuramos explícitamente el IP de bind) para forzar a los contenedores a escuchar solo en localhost:
```yaml
ports:
  - "127.0.0.1:5432:5432"
```

## 3. Configuración de Firewall (UFW)

Para el servidor Ubuntu/Debian en producción, la configuración base de UFW debe ser:

```bash
# 1. Denegar todo el tráfico entrante por defecto
sudo ufw default deny incoming
sudo ufw default allow outgoing

# 2. Permitir tráfico de Tailscale (la VPN confía en su interfaz)
sudo ufw allow in on tailscale0

# 3. Permitir HTTP/HTTPS para la API pública
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

# 4. (Opcional) Permitir SSH temporalmente en eth0 si aún no confías 100% en Tailscale SSH
sudo ufw allow 22/tcp

# 5. Activar el firewall
sudo ufw enable
```

**Nota:** NUNCA permitas los puertos 5432, 6379, 5050 o 8000 en el firewall público UFW.

## 4. Separación de Entornos (Dev vs Prod)

- **Desarrollo (Local):** Los desarrolladores pueden usar el `docker-compose.yml` base libremente sin proxy inverso para trabajar rápido en sus máquinas.
- **Producción:** Se aplica el `docker-compose.override.yml` automáticamente, aislando puertos y forzando a todo tráfico web a pasar por un contenedor proxy (Nginx/Caddy) que se encarga del certificado SSL de Let's Encrypt y de filtrar peticiones maliciosas (DDoS básico, Rate Limiting).
