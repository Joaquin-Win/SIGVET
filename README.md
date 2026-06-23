# SIGVET — Sistema de Gestión Clínica y de Stock para Centros Veterinarios

Prototipo funcional desarrollado para la asignatura **Seminario Final de Licenciatura en Informática**. Permite administrar agenda veterinaria, turnos, consultas médicas, inventario de medicamentos, alertas de stock y datos de dueños y mascotas, con persistencia en **MySQL 8.0** y interfaz de escritorio en **Java Swing**.

---

## Descripción

SIGVET centraliza la operación diaria de una clínica veterinaria:

- Configurar franjas horarias de atención por veterinario.
- Reservar, cancelar y consultar turnos.
- Registrar consultas médicas (con o sin turno previo) y recetas.
- Gestionar inventario y stock de medicamentos (entrada, salida, ajustes).
- Visualizar alertas automáticas por stock bajo o vencimiento próximo.
- Administrar dueños y mascotas.
- Consultar historial clínico y la agenda del día.

La base de datos se **crea e inicializa automáticamente** en el primer inicio si no existe, ejecutando los scripts `sigvet_ddl.sql` y `sigvet_dml.sql` incluidos en el proyecto.

> **Requisito obligatorio:** debe tener **MySQL Server 8.0+ instalado y en ejecución**. La aplicación **no funciona sin un servidor MySQL**; no incluye motor de base de datos embebido.

---

## Requisitos previos

| Componente | Versión mínima | Notas |
|------------|----------------|-------|
| **Java JDK** | 17 | [Eclipse Temurin (Adoptium)](https://adoptium.net/) |
| **Apache Maven** | 3.8+ | Para compilar desde fuentes |
| **MySQL Server** | 8.0+ | Debe estar **instalado y ejecutándose** |

Comprobar instalación en una terminal (PowerShell o CMD):

```bash
java -version
mvn -version
```

---

## Instalación y ejecución

### 1. Clonar el repositorio

```bash
git clone https://github.com/Joaquin-Win/SIGVET-Sistema.git
cd SIGVET
```

### 2. Instalar y ejecutar MySQL Server

**La aplicación no arranca sin MySQL.**

#### Opción A — MySQL Community Server (recomendado)

1. Descargar desde [https://dev.mysql.com/downloads/mysql/](https://dev.mysql.com/downloads/mysql/)
2. Instalar y, durante el asistente, definir usuario `root` y contraseña.
3. Asegurarse de que el servicio **MySQL80** (o similar) esté **Iniciado** en *Servicios de Windows*.

#### Opción B — XAMPP (más simple para pruebas)

1. Descargar [https://www.apachefriends.org/](https://www.apachefriends.org/)
2. Instalar XAMPP e iniciar el módulo **MySQL** desde el panel de control.
3. Por defecto: usuario `root`, contraseña vacía, puerto `3306`.

#### Verificar que MySQL responde

En PowerShell:

```powershell
mysql -u root -p -e "SELECT 1;"
```

(Si no tiene `mysql` en el PATH, use *MySQL Workbench* o el cliente incluido en XAMPP.)

### 3. Configurar la conexión (si es necesario)

Al **primer inicio**, si las credenciales por defecto no coinciden con su MySQL, SIGVET muestra automáticamente un **diálogo de configuración** con:

- Host, puerto, usuario y contraseña
- Botón **Probar Conexión**
- Opción **Crear base de datos automáticamente si no existe**

Los datos se guardan en `sigvet_config.properties` en la carpeta desde la que ejecuta la aplicación (no hace falta editar el JAR).

También puede editar manualmente:

- `src/main/resources/sigvet_config.properties` (antes de compilar), o
- `sigvet_config.properties` en el directorio de trabajo (después de guardar desde el diálogo).

### 4. Compilar y ejecutar (desarrollo)

Desde la raíz del proyecto:

```bash
mvn clean compile exec:java
```

### 5. Generar JAR ejecutable

```bash
mvn clean package
java -jar target/sigvet-1.0.0.jar
```

**En Windows**, también puede usar el script incluido:

```bat
EJECUTAR_SIGVET.bat
```

> Use el JAR `sigvet-1.0.0.jar` (~5 MB, con dependencias). No abra el `-slim` por doble clic sin el `.bat`.

### 6. Instalar Maven (si `mvn` no se reconoce)

1. Descargar Maven: [https://maven.apache.org/download.cgi](https://maven.apache.org/download.cgi)
2. Descomprimir (por ejemplo en `C:\apache-maven-3.9.6`)
3. Agregar `C:\apache-maven-3.9.6\bin` a la variable de entorno **PATH**
4. Abrir una **nueva** terminal y ejecutar `mvn -version`

---

## Base de datos

### Creación automática

Si la base `sigvet` no existe, SIGVET puede crearla al iniciar (con confirmación o automáticamente según el diálogo de configuración), ejecutando:

- **DDL:** tablas, índices, triggers, procedimientos almacenados, funciones y vistas.
- **DML:** datos de prueba iniciales.

### Tablas principales

`veterinario`, `dueno`, `especie`, `raza`, `mascota`, `medicamento`, `stock`, `alerta_stock`, `agenda_disponibilidad`, `slot_agenda`, `turno`, `consulta_medica`, `item_receta`.

### Objetos de base de datos

| Tipo | Cantidad | Ejemplos |
|------|----------|----------|
| **Triggers** | 14 | `trg_verificar_slot_disponible`, `trg_alerta_stock_bajo_insert`, … |
| **Procedimientos** | 10 | `sp_reservar_turno`, `sp_registrar_consulta_turno`, `sp_ingresar_stock`, … |
| **Funciones** | 2 | `fn_stock_total_medicamento`, `fn_calcular_edad_mascota` |
| **Vistas** | 4 | `vw_turnos_del_dia`, `vw_historial_clinico`, `vw_stock_medicamentos`, `vw_alertas_activas` |

---

## Casos de uso

| ID | Nombre | Descripción breve |
|----|--------|-------------------|
| **CU-01** | Configurar Agenda Veterinaria | Definir franjas horarias de disponibilidad por veterinario |
| **CU-02** | Gestionar Turnos | Reservar, cancelar y modificar turnos en slots disponibles |
| **CU-03** | Registrar Consulta Médica | Atender turno o urgencia; diagnóstico, receta y descuento de stock |
| **CU-04** | Gestionar Inventario y Stock | Alta de medicamentos, ingreso/egreso de stock, consulta de existencias |
| **CU-05** | Gestionar Alertas de Stock | Consultar y resolver alertas generadas por triggers de BD |
| **CU-06** | Gestionar Dueños y Mascotas | ABM de dueños, especies, razas y mascotas |
| **CU-07** | Consultar Historial Clínico | Ver consultas previas de una mascota |
| **CU-08** | Consultar Agenda del Día | Vista de turnos del día por veterinario |

---

## Estructura del proyecto

```
SIGVET/
├── src/main/java/com/sigvet/
│   ├── Main.java                 # Punto de entrada
│   ├── dao/                      # Acceso a datos (JDBC)
│   ├── service/                  # Lógica de negocio
│   ├── model/                    # Entidades y DTOs
│   ├── view/                     # Interfaz Swing (diálogos, ventana principal)
│   ├── util/                     # ConexionBD, DatabaseInitializer
│   └── exception/                # Excepciones de dominio
├── src/main/resources/
│   ├── sigvet_config.properties  # Configuración JDBC por defecto
│   ├── sigvet_ddl.sql            # Esquema y objetos de BD
│   └── sigvet_dml.sql            # Datos iniciales
├── target/                       # Salida de Maven (JARs)
├── pom.xml
├── EJECUTAR_SIGVET.bat           # Lanzador en Windows
└── README.md
```

---

## Tecnologías utilizadas

- **Java 17** (JDK)
- **Java Swing** — interfaz gráfica de escritorio
- **MySQL 8.0** — base de datos relacional
- **JDBC** — conectividad (`mysql-connector-j`)
- **Apache Maven** — dependencias y empaquetado
- **Gson** — utilidades JSON auxiliares

---

## Configuración de conexión

### Diálogo al iniciar

Si MySQL no está disponible o las credenciales son incorrectas, aparece **SIGVET — Configuración de Base de Datos**. Tras **Probar Conexión** exitosa, **Guardar y Continuar** persiste la configuración y reinicia el flujo de inicio.

### Archivo `sigvet_config.properties`

```properties
db.url=jdbc:mysql://localhost:3306/sigvet?useSSL=false&serverTimezone=America/Argentina/Buenos_Aires&allowPublicKeyRetrieval=true
db.usuario=root
db.password=
```

**Prioridad de lectura:**

1. `sigvet_config.properties` en el directorio de trabajo (generado por el diálogo).
2. Archivo embebido en el JAR / `src/main/resources`.
3. Valores por defecto (`localhost:3306`, usuario `root`, contraseña vacía).

---

## Solución de problemas

| Problema | Solución |
|----------|----------|
| **`mvn` no se reconoce** | Instalar Maven y agregar `bin` al PATH (ver sección 6). |
| **Java Exception al abrir el `.jar`** | Asociar `.jar` con JDK 17+ o usar `EJECUTAR_SIGVET.bat` / `java -jar`. |
| **`Access denied` (MySQL)** | Usuario o contraseña incorrectos; use el diálogo de configuración al iniciar. |
| **`Communications link failure`** | MySQL no está instalado, no está iniciado o el puerto/host son incorrectos. |
| **La BD no se crea** | El usuario MySQL necesita permiso `CREATE DATABASE`. Use `root` o un usuario con privilegios. |
| **Error al crear triggers/SPs** | Verifique MySQL 8.0+ y permisos `CREATE`, `TRIGGER`, `ROUTINE`. |
| **Contraseña distinta a la del README** | Normal en otra PC: el diálogo de inicio guarda la configuración local. |

---

## Autor

**Joaquín Ezequiel Winck**  
Licenciatura en Informática — Seminario Final

