# GeyserMenuBridge

## 🚀 ¡La Revolución de los Menús para Servidores Híbridos! 🚀

**GeyserMenuBridge** es un plugin innovador para servidores Spigot/Paper que utilizan GeyserMC. Su misión es permitir que los jugadores de Bedrock Edition vean e interactúen con los menús personalizados de Java Edition ¡exactamente como si fueran nativos de Java!

Olvídate de crear versiones separadas de tus menús o de usar formularios limitados de Bedrock. Con GeyserMenuBridge, un solo menú de Java funciona para todos.

## ✨ Características Principales (Fase Actual de Desarrollo)

*   **Detección Automática:** Identifica jugadores de Java y Bedrock (vía Floodgate).
*   **Interceptación Inteligente:** Captura los paquetes de apertura de menús de Java antes de que lleguen a los clientes Bedrock.
*   **Conversión Visual:** Presenta los menús de Java a los jugadores de Bedrock utilizando inventarios de Bukkit enviados a través de la API de Geyser, buscando la máxima fidelidad visual.
*   **Sincronización de Actualizaciones:** Los cambios en los menús de Java (ítems que se actualizan) se reflejan en tiempo real para los jugadores de Bedrock.
*   **Manejo Básico de Clics (Detección):** Detecta cuándo un jugador de Bedrock hace clic en un menú puenteado. La simulación completa de la acción del clic es la siguiente gran fase de desarrollo.
*   **Instalación Simple:** ¡Arrastrar y soltar! (Casi. Ver dependencias).

## 📦 Instalación

1.  Asegúrate de que tu servidor es compatible con Spigot/Paper (versión correspondiente a la API usada, ej. 1.20.1).
2.  Descarga la última versión de `GeyserMenuBridge.jar` de la sección de [Releases](https://github.com/YOUR_USERNAME/GeyserMenuBridge/releases) (¡reemplaza este enlace!).
3.  Coloca el archivo `GeyserMenuBridge.jar` en la carpeta `plugins` de tu servidor.
4.  **Dependencias (¡MUY IMPORTANTE!):** Debes tener instalados y funcionando los siguientes plugins:
    *   [**ProtocolLib**](https://www.spigotmc.org/resources/protocollib.1997/): Para la manipulación de paquetes.
    *   [**Geyser**](https://geysermc.org/): Para permitir conexiones de Bedrock. (Específicamente la versión para Spigot/Paper, ej. `Geyser-Spigot.jar`).
    *   [**Floodgate**](https://github.com/GeyserMC/Floodgate): Para la identificación de jugadores de Bedrock. (Generalmente viene con Geyser o se instala junto a él).
5.  Inicia o reinicia tu servidor. GeyserMenuBridge se cargará automáticamente.

## ⚙️ Comandos y Permisos

*   **/gmbdebug**
    *   Descripción: Activa o desactiva el modo de depuración global para GeyserMenuBridge. Esto mostrará más información en la consola, útil para diagnosticar problemas.
    *   Uso: `/gmbdebug`
    *   Permiso: `geysermenubridge.debug` (default: op)
*   **/testmenu**
    *   Descripción: Abre un menú de prueba con ítems variados para verificar la funcionalidad de GeyserMenuBridge.
    *   Uso: `/testmenu`
    *   Permiso: `geysermenubridge.testmenu` (default: op)

## 🛠️ Para Desarrolladores (Contribuciones)

¡Las contribuciones son bienvenidas! Si deseas mejorar GeyserMenuBridge:

1.  Haz un Fork del repositorio.
2.  Crea una nueva rama para tu feature (`git checkout -b feature/AmazingFeature`).
3.  Commitea tus cambios (`git commit -m 'Add some AmazingFeature'`).
4.  Pushea a la rama (`git push origin feature/AmazingFeature`).
5.  Abre un Pull Request.

El código está comentado en español para facilitar la comprensión de las partes más complejas.

## 🐛 Reporte de Bugs y Sugerencias

Si encuentras un bug o tienes una idea para mejorar el plugin, por favor abre un "Issue" en la [pestaña de Issues de GitHub](https://github.com/YOUR_USERNAME/GeyserMenuBridge/issues) (¡reemplaza este enlace!). Proporciona la mayor cantidad de detalles posible, incluyendo:
*   Versión del servidor (ej. Paper 1.20.1)
*   Versión de GeyserMenuBridge
*   Versión de Geyser y Floodgate
*   Versión de ProtocolLib
*   Lista de otros plugins (especialmente plugins de menús)
*   Pasos para reproducir el bug
*   Logs relevantes del servidor (especialmente con `gmbdebug` activado).

## ⚠️ Estado Actual y Limitaciones Conocidas

*   **Simulación de Clics:** La detección de clics está implementada, pero la *simulación* de la acción original del menú Java (ej. ejecutar un comando, comprar un ítem) es la principal característica en desarrollo activo. Actualmente, los clics en Bedrock son detectados y cancelados, pero no ejecutan la acción del menú Java.
*   **Compatibilidad:** Aunque se busca la máxima compatibilidad, algunos plugins de menús de Java muy complejos o que utilizan técnicas no estándar podrían no funcionar perfectamente de inmediato. Las pruebas exhaustivas están en curso.
*   **Traducción de Ítems:** Dependemos de Geyser para la traducción visual de los ítems. La mayoría de los metadatos comunes (nombres, lore, encantamientos) deberían funcionar bien.

---

*GeyserMenuBridge - Uniendo las experiencias de menú de Java y Bedrock en Minecraft.*
