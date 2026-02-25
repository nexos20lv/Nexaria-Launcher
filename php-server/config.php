<?php
// ============================================================
// Nexaria File Server — Configuration
// ============================================================

return [

    // ── Minecraft ──────────────────────────────────────────
    'mc_version' => '1.21.11', // Version de Minecraft

    // ── Loader (fabric / forge / neoforge / quilt / none) ─
    'loader' => 'fabric', // Type de loader
    'loader_version' => '0.16.5', // Version du loader

    // ── URL publique de CE serveur ─────────────────────────
    // Doit correspondre à l'URL où ce dossier est hébergé
    'server_url' => 'https://launcher.nexaria.site',

    // ── Options ───────────────────────────────────────────
    'allow_cors' => true, // Autoriser les requêtes cross-origin

];