# Guía de Instalación y Configuración de Tailscale (Royal Shield)

Tailscale crea una Red Privada Virtual (VPN) basada en WireGuard que nos permite acceder a los servidores, bases de datos y paneles de administración de forma encriptada, sin necesidad de abrir puertos sensibles en el firewall público.

## 1. Instalación en el Servidor Ubuntu/Linux (Backend)

Ejecuta el script de instalación oficial de Tailscale:
```bash
curl -fsSL https://tailscale.com/install.sh | sh
```

Luego, inicializa la conexión con el comando:
```bash
sudo tailscale up --ssh
```
*(El flag `--ssh` permite usar Tailscale para acceder por SSH al servidor, lo que nos permitirá luego cerrar el puerto 22 en el firewall público).*

Te devolverá un enlace. Ábrelo en tu navegador y autentícate con la cuenta administrativa (ej. GitHub, Google o Microsoft).

Para confirmar tu IP dentro de Tailscale, ejecuta:
```bash
tailscale ip -4
```
*Anota esta IP (usualmente empieza por 100.x.x.x). Será tu IP maestra para administrar el servidor.*

## 2. Instalación en la Máquina Local de Desarrollo (Linux Mint / Android)

**En Linux Mint:**
```bash
curl -fsSL https://tailscale.com/install.sh | sh
sudo tailscale up
```
**En Android:**
Descarga la app oficial de "Tailscale" desde la Google Play Store y conéctate con la misma cuenta.

## 3. Acceso a Servicios Internos

Una vez ambos dispositivos (Servidor y tu PC) están en la misma red de Tailscale, puedes acceder a los servicios privados usando la IP de Tailscale del servidor:

- **SSH Seguro:** `ssh root@100.x.x.x` (o directamente `ssh server-hostname` gracias al MagicDNS de Tailscale).
- **PostgreSQL / pgAdmin:** Los puertos de base de datos están cerrados en Internet, pero usando un túnel SSH o mapeos directos sobre la interfaz de Tailscale, puedes conectar tu DBeaver a PostgreSQL. (El override actual de Docker los mapea a 127.0.0.1, así que puedes hacer un túnel SSH).
- **Ejemplo Túnel SSH para DBeaver:**
  ```bash
  ssh -N -L 5432:127.0.0.1:5432 usuario@100.x.x.x
  ```
  *(Luego conectas DBeaver a localhost:5432 y el tráfico viaja encriptado por la VPN hasta el servidor).*

## 4. Configuración de Control de Acceso (ACLs)

Desde el panel de administración web de Tailscale (https://login.tailscale.com/admin/acls), sugerimos implementar las siguientes reglas básicas para máxima seguridad (Zero Trust):

```json
{
  "acls": [
    // Permitir a los 'admin' acceder a cualquier dispositivo en todos los puertos
    {"action": "accept", "src": ["group:admin"], "dst": ["*:*"]},

    // Opcional: Impedir que dispositivos comunes se conecten entre sí (aislamiento)
    // {"action": "accept", "src": ["autogroup:members"], "dst": ["tag:server:8000"]}
  ],
  "groups": {
    "group:admin": ["tu_correo@gmail.com"]
  },
  "ssh": [
    // Solo permitir conexiones SSH mediante Tailscale al grupo admin
    {
      "action": "check",
      "src": ["group:admin"],
      "dst": ["autogroup:self"],
      "users": ["autogroup:nonroot", "root"]
    }
  ]
}
```
