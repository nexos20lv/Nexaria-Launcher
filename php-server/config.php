<?php
// ============================================================
// Nexaria File Server — Configuration
// ============================================================

return [

    // ── Minecraft ──────────────────────────────────────────
    'mc_version' => '1.8.9', // Version de Minecraft

    // ── Loader (fabric / forge / neoforge / quilt / none) ─
    'loader' => 'forge', // Type de loader
    'loader_version' => '11.15.1.2318', // Version du loader

    // ── URL publique de CE serveur ─────────────────────────
    // Doit correspondre à l'URL où ce dossier est hébergé
    'server_url' => 'https://launcher.nexaria.site',

    // ── Options ───────────────────────────────────────────
    'allow_cors' => true, // Autoriser les requêtes cross-origin

];