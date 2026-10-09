<!-- CYTECH_README_REFRESH:START -->
<div align="center">
<a href="https://github.com/CyoriaSMP-Team/Auto-Login-Mod"><img width="100%" alt="Auto-Login Mod banner" src="https://capsule-render.vercel.app/api?type=waving&color=0:122017,100:22C55E&height=210&section=header&text=Auto-Login%20Mod&fontSize=40&fontColor=ffffff&fontAlignY=36&desc=Prompt-aware%20Minecraft%20authentication%20helper&descAlignY=59&descSize=16"></a>

<p><img src="src/main/resources/assets/auto-login-mod/icon.png" alt="Auto-Login-Mod logo" width="136" /></p>

<img alt="Project: Minecraft Mod" src="https://img.shields.io/badge/PROJECT-Minecraft%20Mod-22C55E?style=flat-square&labelColor=122017"> <img alt="Stack: Fabric · Java" src="https://img.shields.io/badge/STACK-Fabric%20%C2%B7%20Java-22C55E?style=flat-square&labelColor=122017">

<a href="https://github.com/CyoriaSMP-Team/Auto-Login-Mod">Source</a> · <a href="https://github.com/CyoriaSMP-Team/Auto-Login-Mod/issues">Issues</a> · <a href="https://github.com/CyoriaSMP-Team/Auto-Login-Mod/releases">Releases</a>

</div>
<!-- CYTECH_README_REFRESH:END -->

---

A client-side Fabric utility that automatically authenticates you on Minecraft servers using `/login` or `/register`.

## How it works

1. Configure a global password or a per-server password once.
2. Join the server normally.
3. Auto-Login waits for the server's authentication prompt.
4. It detects whether the server asks for `/login` or `/register`.
5. After the configured delay, the command is sent automatically.
6. Duplicate chat/GUI detections are coordinated so one authentication attempt is sent at a time.

If Smart Mode is disabled, Auto-Login falls back to the older join-time login behavior for servers that do not send a detectable prompt.

## Security

- Stored credentials use PBKDF2-HMAC-SHA256 key derivation and AES-GCM encryption.
- A Master Password is **optional**.
- If you enable a Master Password, a successful unlock is remembered on that device by default, so restarting Minecraft does not turn Auto Login back into a manual login step.
- The remembered unlock is wrapped with a random per-device key stored locally in `config/alm-device.key`.
- You can disable **Remember Unlock** in the settings screen if you prefer to enter the Master Password each session.

> Device unlock is a convenience feature. Anyone who can fully read your Minecraft config directory may be able to access both the encrypted credentials and the local device key.

## Features

- Prompt-driven automatic `/login` and `/register`
- Per-server credentials
- Optional global credential
- Custom authentication triggers
- Chat and GUI prompt detection
- Duplicate-attempt protection
- Configurable randomized delay
- Optional Master Password
- Remembered device unlock
- Thai and English prompt detection
- F9 settings shortcut

## Commands

| Command | Description |
| --- | --- |
| `/alm` or `/alm gui` | Open settings |
| `/alm set <password>` | Set the global login password |
| `/alm add <ip> <password>` | Store a password for one server |
| `/alm remove <ip>` | Remove one server entry |
| `/alm list` | Show stored configuration |
| `/alm trigger add <word>` | Add a custom prompt trigger |
| `/alm trigger remove <word>` | Remove a custom prompt trigger |
| `/alm trigger list` | List custom prompt triggers |

## Upgrade note for 2.1.0

If you already used a Master Password before 2.1.0, Auto-Login cannot recover that password from its hash. Enter it **one final time** after upgrading; the mod will then remember the unlock on that device for future launches.

Version 2.1.0 also fixes the F9 keybinding, respects the configured minimum/maximum delay, and avoids duplicate login sends from JOIN + chat/GUI detection.

---
Created by CyoriaSMP Team / namnarak

---

<!-- CYTECH_STAR_HISTORY:START -->

## Star History

<a href="https://star-history.dera.page/#CyoriaSMP-Team/Auto-Login-Mod&type=date&legend=top-left">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://star-history.dera.page/svg?repos=CyoriaSMP-Team/Auto-Login-Mod&type=date&legend=top-left&theme=dark" />
    <source media="(prefers-color-scheme: light)" srcset="https://star-history.dera.page/svg?repos=CyoriaSMP-Team/Auto-Login-Mod&type=date&legend=top-left" />
    <img alt="GitHub star history for CyoriaSMP-Team/Auto-Login-Mod" src="https://star-history.dera.page/svg?repos=CyoriaSMP-Team/Auto-Login-Mod&type=date&legend=top-left" width="800" />
  </picture>
</a>

<!-- CYTECH_STAR_HISTORY:END -->
