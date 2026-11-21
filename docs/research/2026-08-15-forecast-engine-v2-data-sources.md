# Forecast Engine v2: fuentes de datos reales y reproducibles

**Fecha de investigación:** 2026-08-15
**Ámbito:** Royal Shield, Android + backend FastAPI
**Estado:** investigación documental; no se descargaron datasets completos, no se usaron credenciales y no se modificó código.

## Resumen ejecutivo

La ruta más defendible para un primer modelo real esta semana es un modelo cibernético separado que estime:

> **Probabilidad de que una CVE publicada sea incorporada a CISA KEV en los próximos 30 días, usando únicamente información disponible antes del momento de predicción.**

Este objetivo es legítimo para priorización de vulnerabilidades. No significa “probabilidad de que el teléfono del usuario sea atacado”, no mide impacto y no demuestra exposición del dispositivo.

La combinación recomendada es:

- **CISA KEV** como label prospectivo de evidencia confirmada de explotación.
- **NVD/CVE API 2.0 + Change History** como universo de CVEs y features técnicas históricas.
- **FIRST EPSS histórico diario** como feature externa y baseline, nunca como “verdad” propia.
- **OTX fuera del P0**: su EULA indica que el acceso gratuito es para uso no comercial y restringe explotación comercial. Royal Shield no debe usar OTX para entrenamiento o redistribución en producción sin autorización escrita o términos comerciales específicos.

Los datos ambientales pueden sostener un segundo modelo, completamente separado, de riesgo diario/semanal para Miami-Dade. NOAA Storm Events puede aportar labels históricos; GHCNd y CO-OPS aportan features meteorológicas y de nivel del agua. NASA FIRMS es útil para fuego/thermal anomalies, pero en Miami-Dade tendrá pocos positivos y sesgo de detección. OpenFEMA sirve para contexto y validación de impacto, no para predicción de corto plazo.

## Decisiones de alcance

### Objetivo cibernético aprobado para un MVP

`target_30d = 1` si una CVE que no estaba en KEV en el cutoff aparece en KEV durante `(cutoff, cutoff + 30 días]`; `0` si continúa fuera de KEV después de completar la ventana y un embargo de ingestión.

- Unidad recomendada: CVE-cutoff semanal.
- Salida: probabilidad calibrada y ranking de priorización.
- Uso legítimo: ordenar revisión/parcheo junto con exposición del activo, criticidad e impacto.
- No prometer: explotación total, compromiso del usuario, severidad, impacto o protección garantizada.

### Objetivo ambiental propuesto, separado

`hazard_1d` o `hazard_7d`: probabilidad de que NOAA registre en Miami-Dade un evento de clima severo/inundación dentro de la siguiente ventana, usando observaciones anteriores al cutoff.

- Resolución inicial honesta: **condado-día**, no hotspot de calle ni H3 fino.
- Para lograr volumen suficiente, entrenar con varios condados costeros de Florida y evaluar Miami-Dade por separado.
- No mezclar CVEs, IOCs o EPSS con variables físicas en un único score. Son procesos causales y labels distintos.

## Matriz de recomendación

| Prioridad | Fuente | Rol permitido | Entrenamiento | Credenciales | Decisión |
|---|---|---|---|---|---|
| **P0** | CISA KEV | Label prospectivo | Supervisado | No | Adoptar; guardar snapshots/versiones y excluir campos post-outcome de features |
| **P0** | NVD/CVE 2.0 | Universo + features técnicas as-of | Supervisado / series de cambios | API key opcional | Adoptar; Change History y snapshots son necesarios para evitar leakage |
| **P0** | FIRST EPSS | Feature histórica + baseline | Series temporales / feature | No | Adoptar; fijar fecha y versión del modelo EPSS |
| **P0 ambiental** | NOAA Storm Events | Label de evento físico | Supervisado / serie temporal | No | Adoptar solo para el modelo ambiental separado |
| **P0 ambiental** | NOAA GHCNd | Features meteorológicas históricas | Series temporales / features | No para HTTPS; token para CDO API | Adoptar estaciones del sur de Florida, con flags de calidad |
| **P1** | NOAA CO-OPS | Nivel del agua, mareas y meteo costera | Series temporales / features | No | Piloto para inundación costera; cobertura depende de estación |
| **P1** | NASA FIRMS | Detección de fuego/thermal anomaly | Features o label auxiliar | MAP_KEY gratuito para API; Earthdata Login para ciertos archivos | Piloto, preferir standard-processing para entrenamiento |
| **P2** | OpenFEMA Disaster Declarations | Contexto/validación de impacto grueso | Label auxiliar de largo plazo | No | No usar como label de corto plazo; resolución y latencia insuficientes |
| **P2 / bloqueo legal** | AlienVault/LevelBlue OTX | Enriquecimiento de CTI | Solo condicional | Sí, cuenta + API key | No incorporar a producto comercial hasta aprobación legal escrita |

## Fuentes cibernéticas

### 1. CISA Known Exploited Vulnerabilities (KEV) — P0 label

**Fuentes oficiales directas**

- [Catálogo web de CISA KEV](https://www.cisa.gov/known-exploited-vulnerabilities-catalog)
- [JSON canónico](https://www.cisa.gov/sites/default/files/feeds/known_exploited_vulnerabilities.json)
- [CSV canónico](https://www.cisa.gov/sites/default/files/csv/known_exploited_vulnerabilities.csv)
- [JSON Schema canónico](https://www.cisa.gov/sites/default/files/feeds/known_exploited_vulnerabilities_schema.json)
- [Mirror oficial cisagov/kev-data](https://github.com/cisagov/kev-data)
- [Licencia oficial del mirror CISA](https://raw.githubusercontent.com/cisagov/kev-data/develop/LICENSE)
- [BOD 22-01 y criterios de KEV](https://www.cisa.gov/news-events/directives/bod-22-01-reducing-significant-risk-known-exploited-vulnerabilities)

| Aspecto | Verificación |
|---|---|
| Acceso | Archivo JSON/CSV estático por HTTPS, sin autenticación. El mirror Git oficial permite clonar o recuperar revisiones. |
| Esquema | Cabecera: `catalogVersion`, `dateReleased`, `count`, `vulnerabilities`. Por CVE: `cveID`, `vendorProject`, `product`, `vulnerabilityName`, `dateAdded`, `shortDescription`, `requiredAction`, `dueDate`, `knownRansomwareCampaignUse`, `notes`, `cwes`. |
| Cobertura temporal | El catálogo comenzó con BOD 22-01 el 2021-11-03 e incluye CVEs mucho más antiguas. `dateAdded` es la fecha de inclusión en el catálogo, no la primera explotación real. |
| Actualización | Cuando CISA añade o corrige entradas. El mirror oficial informa que normalmente ocurre en días laborables y queda sincronizado con cisa.gov en minutos. No existe cadencia fija garantizada. |
| Licencia | **CC0 1.0**, incluido uso comercial; enlaces de terceros dentro de registros conservan sus propios términos. No autoriza usar logos de CISA/DHS ni sugerir endorsement. |
| Límites | No hay cuota API publicada porque son archivos. Usar `ETag`/`Last-Modified`, intervalos razonables y copia versionada; no hacer polling agresivo. |
| Credenciales | No. |
| Tamaño publicado | La página del catálogo expone `count` dinámico (alrededor de 1.5 mil entradas en la consulta de esta investigación). CISA no publica en la documentación un tamaño contractual del archivo. |

**Uso correcto**

- Label positivo confirmado para “incorporación futura a KEV”.
- Evaluación retrospectiva de lead time y priorización.
- Regla operacional: si una CVE ya está en KEV, debe prevalecer sobre el score predictivo.

**Riesgos de leakage y bias**

- KEV contiene solo positivos; no estar en KEV no significa “no explotada”. Los negativos son **right-censored**.
- `dateAdded`, `dueDate`, `requiredAction`, `knownRansomwareCampaignUse`, nombre/notes derivados de KEV y cualquier flag `hasKev` son post-outcome y no pueden entrar como features para predecir KEV.
- La fecha de inclusión depende de detección, verificación y proceso editorial de CISA; no equivale a primera explotación.
- El catálogo está orientado a riesgo significativo y al entorno federal estadounidense; subrepresenta explotación sin evidencia pública verificable.
- La carga inicial de 2021 agrupa vulnerabilidades antiguas y no representa un flujo natural. Excluir el seed inicial del entrenamiento principal o modelar un `catalog_era` explícito.

### 2. NVD / CVE API 2.0 — P0 universo y features

**Fuentes oficiales directas**

- [Documentación CVE API 2.0](https://nvd.nist.gov/developers/vulnerabilities)
- [Endpoint CVE API 2.0](https://services.nvd.nist.gov/rest/json/cves/2.0)
- [Guía de inicio, cuotas y condiciones](https://nvd.nist.gov/developers/start-here)
- [NVD Data Feeds y tamaños](https://nvd.nist.gov/vuln/data-feeds)
- [NVD API key y términos](https://nvd.nist.gov/developers/request-an-api-key)
- [Schema NVD JSON 2.0](https://csrc.nist.gov/schema/nvd/api/2.0/cve_api_json_2.0.schema)

| Aspecto | Verificación |
|---|---|
| Acceso | REST `GET /rest/json/cves/2.0` con filtros de publicación/modificación/CVE/CPE/CWE/CVSS. Feeds JSON 2.0 anuales, recent y modified. Change History: `/rest/json/cvehistory/2.0`. |
| Esquema | Envolvente: `resultsPerPage`, `startIndex`, `totalResults`, `format`, `version`, `timestamp`, `vulnerabilities`. CVE: `id`, `sourceIdentifier`, `published`, `lastModified`, `vulnStatus`, `descriptions`, `metrics` CVSS, `weaknesses` CWE, `configurations`/CPE, `references`, `vendorComments`; algunos registros incluyen propiedades CISA/KEV. |
| Cobertura temporal | Feeds por año; el feed 2002 contiene CVEs anteriores y de 2002. La cobertura refleja el corpus CVE/NVD, no un historial de explotación. |
| Actualización | API tan frecuente como el sitio; feeds `recent` y `modified` cada dos horas; feeds anuales una vez al día. Mantener incrementalmente con `lastModStartDate`/`lastModEndDate`. |
| Licencia/condiciones | Publicaciones NIST en dominio público. NVD pide mostrar: “This product uses data from the NVD API but is not endorsed or certified by the NVD.” API “as is”; no sugerir endorsement. |
| Límites | Sin key: 5 solicitudes por ventana móvil de 30 s. Con key: 50/30 s. Recomendación oficial: 6 s entre solicitudes. `resultsPerPage` máximo 2,000; rangos de fechas máximo 120 días; `cveIds` máximo 100. |
| Credenciales | Opcional. Sin key funciona con cuota pública; la key se solicita con email y va en header `apiKey`. No incluirla en Android ni logs. |
| Tamaño publicado | Los feeds publican tamaños vivos. El 2026-08-14: recent ~1.29 MB gzip, modified ~4.64 MB, 2026 ~21.93 MB; otros años rondaban decenas de MB. El CPE Match completo superaba 760 MB gzip, por lo que no debe formar parte del MVP. |

**Uso correcto**

- Construir el universo de CVEs elegibles.
- Features as-of: edad desde publicación, CWE, proveedor/producto normalizados, componentes CVSS disponibles, CPE breadth, número/tipos de referencias y ritmo de modificaciones.
- Reconstruir estado histórico con snapshots y Change History.
- NVD **no proporciona el label de explotación**; CVSS es severidad técnica, no probabilidad de explotación.

**Riesgos de leakage y bias**

- Campos NVD que reflejan KEV (`hasKev`, `cisaExploitAdd`, `cisaAction`, `cisaRequiredAction`, `cisaVulnerabilityName` o equivalentes) deben excluirse.
- `lastModified`, nuevas referencias, CWE/CVSS y CPE pueden aparecer después del cutoff. No usar el estado actual para entrenar una predicción histórica.
- Campos opcionales/missingness dependen de CNA, año, producto y cola de análisis; la ausencia puede codificar proceso editorial.
- Texto/referencias posteriores pueden mencionar explotación o KEV. Aplicar filtro temporal y lista de términos/fuentes prohibidas.
- No usar raw `cve_id` como feature; facilita memorización.

### 3. FIRST EPSS — P0 feature y baseline

**Fuentes oficiales directas**

- [EPSS: Get the Data](https://www.first.org/epss/data.html)
- [EPSS API](https://api.first.org/data/v1/epss)
- [Documentación del endpoint](https://api.first.org/epss/)
- [CSV diario actual](https://epss.empiricalsecurity.com/epss_scores-current.csv.gz)
- [Archivo histórico enlazado por FIRST](https://github.com/empiricalsec/epss_scores)
- [FAQ de uso/licencia](https://www.first.org/epss/faq.html)
- [Estadísticas y significado de campos](https://www.first.org/epss/data_stats.html)

| Aspecto | Verificación |
|---|---|
| Acceso | CSV gzip diario para bulk; archivo Git de CSV diarios; API para lookup y lotes pequeños. Sin registro. |
| Esquema | CSV: comentario con versión/fecha, después `cve`, `epss`, `percentile`. API añade envolvente (`status`, `total`, `offset`, `limit`) y `date`/`created`. |
| Cobertura temporal | Histórico diario desde 2021-04-14. Cambios de versión: v2 2022-02-04, v3 2023-03-07, v4 2025-03-17 y versiones posteriores documentadas en la página de datos. |
| Actualización | Diaria; FIRST indica publicación normalmente poco después de 13:30 UTC. |
| Licencia/condiciones | Scores publicados libremente, sin registro. FIRST solicita atribución en productos/publicaciones. La página no presenta una licencia SPDX convencional; conservar snapshot de FAQ/términos y atribuir FIRST EPSS/Empirical Security. |
| Límites | API diseñada para lookup, no bulk. Parámetro `cve` limitado a 2,000 caracteres; paginación con limit/offset; scope time-series devuelve hasta 30 días. Para todo el corpus usar CSV diario. |
| Credenciales | No. |
| Tamaño publicado | FIRST reportaba del orden de 350 mil CVEs puntuadas en 2026. El tamaño de cada gzip no es contractual y varía diariamente. |

**Uso correcto**

- Feature `epss_asof_cutoff`, `percentile_asof_cutoff`, delta 7/30 días y versión de EPSS.
- Baseline fuerte: comparar el modelo de Royal Shield contra ranking EPSS sin afirmar que se ha superado hasta validar en holdout.
- No multiplicar EPSS por CVSS: FIRST advierte que produce un número sin interpretación probabilística.

**Riesgos de leakage y bias**

- EPSS ya es un modelo entrenado con sensores de partners; es una feature model-derived, no ground truth independiente.
- Sus fuentes incluyen socios de inteligencia, entre ellos LevelBlue; combinar EPSS con OTX puede duplicar señales.
- Cambios de versión producen discontinuidades globales. Guardar `epss_model_version` y no interpretar el salto como cambio de amenaza.
- La probabilidad es de **observación compartida por sus partners** en 30 días, no explotación universal ni riesgo de un dispositivo específico.
- En entrenamiento histórico solo usar el archivo correspondiente a la fecha del cutoff.

### 4. AlienVault / LevelBlue OTX — P2, bloqueo legal

**Fuentes oficiales directas**

- [Página oficial OTX](https://levelblue.com/open-threat-exchange)
- [OTX DirectConnect API portal](https://otx.alienvault.com/api)
- [Documentación oficial OTX/USM](https://docs.levelblue.com/documentation/usm-anywhere/user-guide/otx/open-threat-exchange)
- [Cómo obtener/usar la OTX key](https://docs.levelblue.com/documentation/usm-anywhere/user-guide/otx/otx-key)
- [SDK oficial AlienVault-OTX](https://github.com/AlienVault-OTX/OTX-Node-SDK)
- [EULA oficial OTX](https://www.levelblue.com/legal/otx-eula-terms)

| Aspecto | Verificación |
|---|---|
| Acceso | DirectConnect API `/api/v1`; SDK; export de pulses como CSV/OpenIOC/STIX. Lecturas por pulse, actividad, suscripciones e indicadores, con `modified_since`, `limit` y `page`. |
| Esquema | Pulse: `id`, nombre, descripción, TLP, autor, tags, references, timestamps, adversary y lista de indicators. Indicator: `indicator`, `type`, descripción y timestamps; tipos incluyen IPv4/IPv6, dominio, hostname, URL/URI, email, hashes, CIDR, path, mutex y CVE. |
| Cobertura temporal | Plataforma en operación desde la década de 2010; la documentación permite look-back incluso “Unlimited”, pero no publica una fecha inicial completa ni garantiza conservación histórica inmutable. |
| Actualización | “Ongoing”/real-time según LevelBlue; pulses comunitarios se actualizan continuamente. No hay SLA público para entrenamiento reproducible. |
| Licencia/condiciones | **No es una licencia abierta de datos para un producto comercial.** La EULA declara OTX gratis para usuarios finales no comerciales, limita licencias/transferencia/distribución/explotación comercial y prohíbe compartir indicadores privados. Requiere revisión legal y autorización escrita antes de uso comercial o entrenamiento. |
| Límites | La documentación pública consultada no publica una cuota contractual de DirectConnect. No asumir que “free” significa bulk ilimitado. |
| Credenciales | Sí. Cuenta registrada y API key. La key debe permanecer solo en backend/secret manager. |
| Tamaño publicado | Marketing oficial informa más de 100,000 participantes en 140 países y más de 19 millones de contribuciones/indicadores diarios; no equivale a 19 millones de IOCs únicos ni a un corpus histórico descargable. |

**Uso técnicamente razonable, solo tras autorización**

- Enriquecimiento as-of: recuento de pulses, recencia, diversidad de autores, TLP permitido, tipos de IOC y menciones de CVE.
- Inferencia/triage en backend con TTL y provenance, no redistribución masiva.
- Nunca usar un pulse comunitario como label de explotación confirmada sin corroboración.

**Riesgos**

- Crowdsourcing: duplicados, falsos positivos, indicadores obsoletos y sesgo hacia campañas visibles.
- Poisoning/adversarial submissions, reputación desigual de autores y referencias externas no confiables.
- TLP, privacidad, contenido privado y derechos de terceros.
- Staleness: LevelBlue advierte que IOCs envejecen rápidamente y look-backs largos aumentan falsos positivos.
- Leakage: pulses publicados después del cutoff o que citan KEV/EPSS no pueden entrar en features históricas.

## Fuentes geoespaciales y ambientales para Miami-Dade

### 5. NOAA Storm Events Database — P0 ambiental label

**Fuentes oficiales directas**

- [Storm Events Database](https://www.ncei.noaa.gov/stormevents/)
- [Bulk CSV por año](https://www.ncei.noaa.gov/pub/data/swdi/stormevents/csvfiles/)
- [Formato oficial de archivos bulk](https://www.ncei.noaa.gov/pub/data/swdi/stormevents/csvfiles/Storm-Data-Bulk-csv-Format.pdf)
- [FAQ de calidad y geolocalización](https://www.ncei.noaa.gov/stormevents/faq.jsp)
- [Metadata oficial NCEI](https://www.ncei.noaa.gov/metadata/geoportal/rest/metadata/item/gov.noaa.ncdc%3AC00510/html)

| Aspecto | Verificación |
|---|---|
| Acceso | Bulk CSV gzip anual por HTTPS; búsqueda/export interactivo por estado/condado/fecha/evento. Sin credenciales. |
| Esquema | Archivos Details, Fatalities y Locations. Campos principales: `EPISODE_ID`, `EVENT_ID`, estado, `CZ_TYPE`, `CZ_FIPS`, `CZ_NAME`, begin/end date-time, `EVENT_TYPE`, source, magnitude, injuries/deaths, property/crop damage, begin/end lat/lon y narratives. |
| Cobertura temporal | 1950-presente, pero 1950-1954 solo tornados; 1955-1995 cobertura parcial de tipos; los 48 tipos actuales se registran desde 1996. |
| Actualización | Metadata: mensual, con retraso posible de hasta 120 días; páginas NOAA también comunican aproximadamente 75 días. No entrenar con meses recientes incompletos. |
| Licencia | Datos NOAA de gobierno de EE. UU., generalmente dominio público/CC0 salvo anotación; atribuir NOAA/NCEI y no sugerir endorsement. |
| Límites | No se publica cuota del directorio bulk. Descargar solo años necesarios, cachear y verificar hashes propios. |
| Credenciales | No. |
| Tamaño publicado | El índice publica tamaño por archivo anual; varía desde KB en años tempranos hasta archivos grandes en años recientes. La especificación PDF rondaba 341 KB en la consulta. |

**Objetivo legítimo**

- Label condado-día para `severe_weather_next_1d/7d`, o conteo de eventos/impacto en la ventana.
- Para Miami-Dade, filtrar por estado y FIPS/zone con tabla versionada; no depender solo del nombre textual.

**Bias/leakage**

- Cambios de cobertura por era y tipo; usar principalmente 1996+ y considerar 2010+ para consistencia geográfica.
- Eventos de área grande se registran por zona; eventos pequeños por condado. Coordenadas no siempre están presentes o representan puntos/segmentos aproximados.
- Daños son estimaciones, no mediciones homogéneas; reporting depende de población y proceso local.
- Narratives escritos después del evento son labels/contexto, no features de una predicción anterior.

### 6. NOAA GHCN-Daily (GHCNd) — P0 ambiental features

**Fuentes oficiales directas**

- [Producto GHCNd](https://www.ncei.noaa.gov/products/land-based-station/global-historical-climatology-network-daily)
- [Directorio HTTPS y archivos por estación](https://www.ncei.noaa.gov/pub/data/ghcn/daily/)
- [README y formato](https://www.ncei.noaa.gov/pub/data/ghcn/daily/readme.txt)
- [CDO API v2 y límites](https://www.ncei.noaa.gov/cdo-web/webservices/getstarted)
- [Política/copyright NOAA](https://sos.noaa.gov/copyright/)

| Aspecto | Verificación |
|---|---|
| Acceso | HTTPS por estación, por año o bulk; CDO REST API opcional. Para Miami-Dade conviene seleccionar estaciones con inventario y usar archivos por estación. |
| Esquema | Station metadata: ID, latitud, longitud, elevación, estado, nombre. Diario: elementos como `PRCP`, `TMAX`, `TMIN`, snowfall/depth donde aplique; `MFLAG`, `QFLAG`, `SFLAG`, hora de observación. |
| Cobertura temporal | Más de 100,000 estaciones, records desde el siglo XIX; el periodo y las variables dependen de cada estación. Para el modelo solo cuenta la cobertura de estaciones del sur de Florida. |
| Actualización | Diaria y reconstrucción semanal desde más de 25 fuentes. Fuentes archive-ready normalmente sustituyen real-time 45-60 días después de cerrar el mes. |
| Licencia | Para estaciones de la colección de EE. UU., datos NOAA de dominio público/CC0 salvo anotación. Atribuir NOAA/NCEI; no usar marcas ni implicar endorsement. |
| Límites | HTTPS bulk sin cuota publicada. CDO API requiere token y limita 5 requests/s y 10,000/día. |
| Credenciales | No para HTTPS; token gratuito para CDO API. |
| Tamaño publicado | El bulk global `ghcnd_all.tar.gz` superaba 3.7 GB en julio de 2026. No usarlo: seleccionar estaciones; inventario ~36 MB y catálogo de estaciones ~11 MB. |

**Objetivo legítimo**

- Features de precipitación, temperatura, rachas/secas, anomalías respecto de climatología y estacionalidad.
- Agregación causal: valores de los 1/3/7/30 días previos al cutoff; nunca usar datos revisados que no estaban disponibles en ese momento sin marcar latencia.

**Bias/leakage**

- Missingness, cambios de estación/instrumento/hora y fuentes real-time versus archive-ready.
- GHCNd no está homogeneizado para todos los cambios sistemáticos; NOAA advierte posibles sesgos históricos.
- Respetar `QFLAG`; no imputar extremos como cero.
- Un modelo retrospectivo con valores finalizados 60 días después puede sobreestimar la calidad disponible en inferencia real-time. Mantener dos vistas: `operational_asof` y `archive_quality`.

### 7. NOAA CO-OPS Water Levels — P1 ambiental features

**Fuentes oficiales directas**

- [CO-OPS Data Retrieval API](https://api.tidesandcurrents.noaa.gov/api/prod/)
- [Web Services y límites](https://www.tidesandcurrents.noaa.gov/web_services_info.html)
- [API URL Builder](https://tidesandcurrents.noaa.gov/api-helper/url-generator.html)
- [Response fields/flags](https://api.tidesandcurrents.noaa.gov/api/prod/responseHelp.html)

| Aspecto | Verificación |
|---|---|
| Acceso | REST por `station`, fechas, `product`, `datum`, timezone, units y format JSON/CSV/XML. Metadata API para ubicación/datum/sensores. Sin key. |
| Esquema | Para water level: timestamp, valor de altura, sigma y flags de calidad; también productos horarios, high/low, máximos/mínimos, predicciones y observaciones meteorológicas. Datum obligatorio para water levels. |
| Cobertura temporal | Depende de estación/producto; API y metadata distinguen estaciones activas e históricas. Validar previamente continuidad para estaciones relevantes a Miami/Miami Beach/Virginia Key. |
| Actualización | Observaciones preliminares en tiempo casi real; water levels verificados mensualmente. |
| Licencia | Datos NOAA de dominio público salvo anotación; atribuir NOAA/CO-OPS y no sugerir endorsement. |
| Límites | 1 min: 4 días/request; 6 min: 1 mes; hourly/high-low: 1 año; daily max/min: 10 años; monthly means: 200 años. |
| Credenciales | No. |
| Tamaño publicado | No se publica tamaño fijo; depende de estación, intervalo y rango. |

**Uso y riesgos**

- Feature para riesgo de inundación costera: residuo sobre marea astronómica, máximos recientes, tendencia y duración sobre umbral.
- No confundir predictions de marea con observaciones; no entrenar label con predicción futura.
- Cambios de datum, gaps, mantenimiento, tormentas y diferencia preliminary/verified exigen versionado.
- Una estación no representa todo Miami-Dade; evaluar cobertura espacial y no generar mapas de calle.

### 8. NASA FIRMS — P1 ambiental

**Fuentes oficiales directas**

- [FIRMS Active Fire Data y cobertura](https://firms.modaps.eosdis.nasa.gov/content/active_fire/)
- [Area API y límites](https://firms.modaps.eosdis.nasa.gov/api/area/)
- [Tutorial oficial API](https://firms.modaps.eosdis.nasa.gov/content/academy/data_api/firms_api_use.html)
- [Descripción de campos VIIRS](https://firms.modaps.eosdis.nasa.gov/content/descriptions/FIRMS_VIIRS_Firehotspots.html)
- [Política de uso NASA Earthdata](https://www.earthdata.nasa.gov/engage/open-data-services-software/data-use-policy)

| Aspecto | Verificación |
|---|---|
| Acceso | Area API CSV por bounding box, sensor, fecha y rango; Archive Download para histórico. Para entrenamiento preferir standard-processing/science-quality, no NRT. |
| Esquema | `latitude`, `longitude`, brightness (`bright_ti4`/`bright_ti5` o equivalente), `scan`, `track`, `acq_date`, `acq_time`, satellite, instrument, confidence, version, FRP y day/night; campos varían por sensor. |
| Cobertura temporal | MODIS Terra desde 2000-11, Aqua desde 2002-07; VIIRS S-NPP desde 2012-01-20; NOAA-20 desde 2018/2020 según producto publicado; NOAA-21 desde 2024-01-17. Fijar producto y no mezclar fechas de disponibilidad sin versionar. |
| Actualización | NRT global normalmente dentro de 3 horas; RT/URT más rápido para EE. UU./Canadá. Standard products sustituyen NRT normalmente en 2-3 meses, hasta 5 meses. |
| Licencia | Datos de misiones NASA-led sin restricción marcada se ofrecen bajo **CC0**; reconocer NASA y no implicar endorsement. Verificar si se incorpora un producto de tercero. |
| Límites | MAP_KEY gratuita; 5,000 transacciones por intervalo de 10 minutos. Area API acepta 1-5 días por consulta; consultas grandes consumen varias transacciones. |
| Credenciales | MAP_KEY para API; Earthdata Login para ciertos downloads. Secret solo en backend. |
| Tamaño publicado | No hay tamaño fijo para Miami-Dade. NASA advierte que una consulta mundial VIIRS puede devolver 30,000-100,000+ filas/día. |

**Uso y riesgos**

- Labels auxiliares de detección térmica o features de actividad de fuego; no equivalen a incendio confirmado ni área quemada.
- Cobertura por nubes, órbita, resolución y sensor cambia. El punto es centro de pixel, no ubicación exacta del fuego.
- Falsos positivos por fuentes térmicas estáticas; gaps por outages. Deduplicar sensores con reglas explícitas.
- NRT no es adecuado para una serie científica definitiva; entrenar con standard processing y servir NRT con un flag de calidad menor.
- Miami-Dade probablemente tendrá clase positiva escasa: P1, no fundamento del primer modelo local.

### 9. OpenFEMA Disaster Declarations Summaries — P2 contexto

**Fuentes oficiales directas**

- [Dataset y diccionario](https://www.fema.gov/about/openfema/disaster-declarations-summaries)
- [API v1](https://www.fema.gov/api/open/v1/DisasterDeclarationsSummaries)
- [OpenFEMA](https://www.fema.gov/about/reports-and-data/openfema)
- [Términos y citación](https://www.fema.gov/about/openfema/terms-conditions)

| Aspecto | Verificación |
|---|---|
| Acceso | REST read-only y archivos completos CSV/JSON/JSONL/Parquet; filtros OData. Sin registro. |
| Esquema | `disasterNumber`, declaration/type/date, state, title, incident type, begin/end, programas declarados, `placeCode`, `declaredCountyArea`, FIPS cuando disponible, `lastRefresh`, `hash`, `id`. |
| Cobertura temporal | Declaraciones desde 1953; información de condado no disponible antes de 1964; Fire Management histórico parcial. |
| Actualización | Dataset declara `R/PT20M`, aproximadamente cada 20 minutos. |
| Licencia/condiciones | Datos públicos OpenFEMA con términos/citación propios; API sin registro. Conservar versión de términos, atribuir FEMA y no implicar endorsement. |
| Límites | No se encontró una cuota contractual publicada en la página del dataset. Para bulk usar archivos completos y no paginar agresivamente. |
| Credenciales | No. |
| Tamaño publicado | El dataset no mostraba record count/tamaño estable en la ficha consultada. |

**Uso y riesgos**

- Contexto de impacto/validación por desastre y condado; no label de “mañana habrá riesgo”.
- Declaración es una decisión administrativa posterior, con umbral federal, latencia y granularidad gruesa.
- FEMA advierte error humano en datos históricos y parcialidad de algunas categorías.
- No usar `declarationDate` o programas aprobados como features para predecir el incidente anterior.

## Fuentes no promovidas en esta fase

- **Miami-Dade crime dashboard:** no se promueve como fuente de entrenamiento hasta confirmar un endpoint oficial estable de descarga histórica, diccionario, cobertura, licencia y política de redistribución. Una visualización o PDF no satisface reproducibilidad.
- **Noticias/OSINT:** no son input espacial hasta tener geocoding real, timestamps de publicación fiables, licencia y evaluación de precisión. No inferir coordenadas por texto de forma silenciosa.
- **OpenStreetMap POIs:** pueden ser contexto estático bajo ODbL, pero no son label ni historial de riesgo. Si se incorporan, revisar attribution/share-alike y congelar extractos versionados; quedan fuera del MVP de esta semana.
- **Pronósticos meteorológicos actuales:** útiles para inferencia, pero no se pueden usar para entrenamiento histórico salvo que exista archivo de forecasts emitidos as-of. Usar observaciones históricas actuales para predecir el pasado causaría leakage.

## Pipeline propuesto

### A. Pipeline cibernético P0

1. **Snapshot raw inmutable**
   - Recuperar KEV JSON, NVD incremental/Change History y EPSS CSV diario.
   - Guardar `source`, URL, `retrieved_at_utc`, fecha lógica, headers, versión/schema y SHA-256.
   - Nunca sobrescribir snapshots; crear manifest por corrida.

2. **Bronze / normalización**
   - Normalizar CVE ID, timestamps UTC, CWE, CPE/vendor/product y estados.
   - Conservar payload raw y tablas de rechazo con causa.
   - Deduplicar por clave fuente y conservar revisiones.

3. **Reconstrucción as-of**
   - Para cada cutoff semanal, materializar la última versión de NVD disponible antes del cutoff.
   - Unir EPSS del mismo día o último día anterior, con máximo staleness explícito.
   - Confirmar que la CVE no estaba en KEV en el cutoff.

4. **Labeling prospectivo**
   - Positivo: `dateAdded` KEV dentro de los siguientes 30 días.
   - Negativo provisional: fuera de KEV al final de la ventana + embargo.
   - Registrar `label_observed_through`; no relabel silencioso.

5. **Feature engineering causal**
   - Edad, CVSS vector components as-of, CWE, vendor/product normalizado, breadth CPE, referencias por tipo/dominio, ritmo de modificación, EPSS/deltas y missingness flags.
   - Excluir CVE ID raw, campos CISA/KEV, texto que mencione KEV/explotación después del cutoff y datos futuros.

6. **Entrenamiento y calibración**
   - Baseline: EPSS solo; baseline 2: regresión logística simple; después gradient boosting con class weights.
   - Calibración isotonic o Platt usando solo validation.
   - Métricas: PR-AUC, ROC-AUC secundaria, Brier/ECE, precision/recall@K, lift@K y lead time antes de KEV.

7. **Artefactos reproducibles**
   - Dataset manifest + hashes, feature schema, cutoff rules, code commit, environment lock, seed, model checksum, métricas por periodo/vendor/CWE y model card.
   - Estado de serving: `READY`, `DEGRADED` o `UNAVAILABLE`; nunca generar score si faltan modelo, schema o fuentes mínimas.

### B. Pipeline ambiental separado

1. Seleccionar eventos Storm Events 2010+ para Miami-Dade y condados costeros comparables.
2. Seleccionar estaciones GHCNd/CO-OPS con cobertura y documentar sus gaps, datum y relocations.
3. Crear tabla condado-día con features solo hasta el cutoff.
4. Label Storm Events en ventanas 1/7 días; FEMA únicamente como evaluación de eventos mayores.
5. Entrenar walk-forward por temporadas; evaluar específicamente Miami-Dade.
6. Publicar como riesgo de condado, con fecha/fuentes/quality flag; no presentar precisión de calle.

## Controles de calidad obligatorios

### Ingesta y esquema

- Verificación TLS, status, content-type, tamaño no cero y SHA-256.
- JSON Schema/column contract; quarantine ante campos faltantes, tipos nuevos o schema drift.
- Conteo raw/accepted/rejected/updated y comparación contra ejecución anterior.
- IDs únicos (`cve_id`, `event_id`, source/external_id) y detección de duplicados exactos/semánticos.
- Timestamps UTC, rangos válidos, monotonicidad y no fechas futuras.

### Calidad cibernética

- Auditoría automática de columnas prohibidas: KEV/dateAdded/dueDate/action/ransomware y aliases NVD CISA.
- “Point-in-time join” con assertion `feature_timestamp <= cutoff`.
- `label_timestamp > cutoff` y `<= cutoff + horizon`.
- Coverage por año, CNA, vendor, CWE, CVSS missingness y clase.
- Detección de texto/referencias con tokens de outcome (“known exploited”, “KEV”, “exploited in the wild”) y revisión.
- Baseline contra EPSS con intervalos de confianza bootstrap temporal.

### Calidad ambiental

- Bounding box/FIPS controlados y geometrías dentro de Florida; no aceptar `(0,0)`.
- Flags NOAA respetados; missing no equivale a cero.
- Unidades normalizadas con metadatos originales conservados.
- Separar preliminary versus verified/standard-processing.
- Sensores/estaciones/versiones como columnas; auditoría de gaps, outages y cambios de datum.
- Detección de duplicados cross-sensor FIRMS y de eventos NOAA segmentados.

### Gobernanza, seguridad y legal

- Registro por fuente de licencia/términos, URL, fecha de captura, attribution y restricciones.
- Secrets solo en backend/secret manager; nunca en APK, repositorio, logs o artefactos ML.
- No persistir contenido OTX privado/TLP restringido; OTX deshabilitado por defecto hasta clearance.
- Provenance por predicción y posibilidad de reproducir el feature vector as-of.
- Retención y borrado definidos; no recolectar PII para estos objetivos.

## Estrategia de split cronológico

### Cibernético

Ventana propuesta para un primer benchmark con labels maduros al 2026-08-15:

- **Train:** cutoffs 2022-02-01 a 2024-12-31.
- **Validation:** 2025-01-01 a 2025-12-31.
- **Test bloqueado:** 2026-01-01 a 2026-06-30.
- **Embargo/purge:** mínimo 30 días entre bloques, más latencia de ingestión definida. No evaluar cutoffs cuyo horizonte no esté completamente observado.

Reglas adicionales:

- Walk-forward mensual/trimestral, nunca split aleatorio.
- Reportar dos pruebas: (a) CVEs nuevas después del boundary y (b) todas las CVEs activas elegibles. Así se detecta memorización por CVE persistente.
- Si hay samples semanales repetidos por CVE, excluir raw ID y hacer un análisis secundario group-holdout por CVE/vendor.
- Congelar el test antes de feature selection/calibración; una sola apertura final.
- Purged windows: cualquier label cuya ventana cruce el siguiente bloque queda fuera.
- Evaluar drift por versión EPSS, año CVE, vendor, CWE y completitud NVD.

### Ambiental

- **Train:** 2010-2022.
- **Validation:** 2023-2024.
- **Test:** 2025, cuando NOAA lo considere suficientemente completo/archivado.
- **2026:** solo inferencia/monitoring hasta superar retrasos de 75-120 días y verificación de fuentes.
- Split adicional leave-one-hurricane-season-out y evaluación por condado para evitar que una sola tormenta aparezca en train y test a través de días adyacentes.
- Purge de al menos el horizonte objetivo alrededor de boundaries y agrupación por episodio Storm Events.

## Recomendación concreta para esta semana

### Entregable P0, sin simulación

1. Aprobar formalmente el objetivo **KEV-next-30-days** y el texto de no-claim.
2. Implementar solo conectores de metadata/snapshot para KEV, NVD y EPSS, respetando cuotas; no OTX.
3. Crear un manifest de un **sample pequeño y explícito** (por ejemplo, un rango de fechas limitado), con hashes y conteos; no entrenar todavía con archivos “actuales” usados como pasado.
4. Construir una prueba automática de point-in-time leakage que falle si aparece cualquier campo KEV/CISA post-outcome.
5. Ejecutar un baseline reproducible EPSS versus logística y producir métricas cronológicas. Si no existe volumen/estado histórico suficiente, devolver `UNAVAILABLE`, no datos sintéticos.
6. En paralelo, hacer un mini perfil no-ML de NOAA Storm Events + 2-4 estaciones GHCNd/CO-OPS de South Florida para decidir si el modelo ambiental tiene positivos y continuidad suficientes.
7. Solicitar revisión legal de OTX por escrito. Hasta entonces mantener `OTX_ENABLED=false` y no almacenar ni entrenar con sus IOCs.

**Criterio de éxito de la semana:** un dataset manifest reproducible, con timestamps as-of, labels maduros, cero columnas prohibidas, split cronológico congelado y baseline medido. No es necesario desplegar ni exponer predicciones al Android todavía.

## Riesgos abiertos que requieren decisión

- KEV mide confirmación/inclusión, no primera explotación. El nombre del modelo y la UI deben reflejarlo.
- NVD histórico as-of requiere snapshots o reconstrucción verificable; consultar el registro actual no basta.
- EPSS puede dominar el modelo. Si Royal Shield no mejora calibración/lift en holdout, es mejor usar EPSS con attribution que vender un modelo redundante.
- Un modelo ambiental solo Miami-Dade probablemente tendrá pocos positivos; entrenar multi-condado y validar localmente.
- La licencia de OTX es el bloqueador legal más claro; “free API” no equivale a permiso de entrenamiento comercial.
- Las fuentes oficiales pueden corregir el pasado. El pipeline debe conservar versión recibida y fecha de acceso.

## Registro de fuentes primarias consultadas

- CISA: KEV catalog, JSON/CSV/schema, BOD 22-01 y repositorio oficial `cisagov/kev-data`/CC0.
- NIST/NVD: API 2.0, Start Here, Data Feeds, schema y API terms.
- FIRST: EPSS data, API, FAQ, statistics e histórico enlazado por FIRST.
- LevelBlue/AlienVault: OTX portal, documentación, SDK oficial y EULA.
- NOAA/NCEI: Storm Events, GHCNd, CDO API y política de uso NOAA.
- NOAA CO-OPS: Data API, web services, limits y response fields.
- NASA Earthdata/FIRMS: Active Fire, Area API, atributos/cobertura y Data Use Policy.
- FEMA: OpenFEMA, Disaster Declarations Summaries, API y términos/citación.
