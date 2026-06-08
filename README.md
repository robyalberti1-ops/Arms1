# ARMS v0.1-alpha

Companion Android sperimentale per KE3 PRO.

## Funzioni incluse

- Scansione e connessione BLE al KE3 PRO.
- Protocollo notifiche testato: Service `56FF`, Write `34F1`, Notify `34F2`.
- Invio notifica test `ARMS: HELLO`.
- NotificationListener per WhatsApp, Telegram, SMS, Messenger, Gmail, Instagram.
- Centro Benessere predisposto: batteria + parser iniziale per pacchetti salute `E5-11`.

## Compilazione APK con GitHub Actions

1. Carica tutti i file di questo ZIP nel repository GitHub.
2. Apri la scheda **Actions**.
3. Seleziona **Build ARMS APK**.
4. Premi **Run workflow**.
5. Scarica l'artifact **ARMS-v0.1-alpha-debug-apk**.

## Uso

1. Installa APK sul Redmi.
2. Concedi permessi Bluetooth.
3. Premi **Connetti KE3 PRO**.
4. Premi **Invia TEST HELLO**.
5. Abilita accesso notifiche per inviare WhatsApp reali all'orologio.

## Nota

Questa è una alpha sperimentale. Wake screen non implementato. Passi/SpO₂/sonno richiedono completamento del protocollo.
