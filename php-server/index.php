<?php
// ============================================================
// Nexaria File Server — Point d'entrée
// 
// Routes :
//   GET /manifest.json    → Manifest de tous les fichiers
//   GET /info.json        → Infos version/loader
//   GET /files/...        → Téléchargement direct
// ============================================================

$config = require __DIR__ . '/config.php';

// ── CORS ──────────────────────────────────────────────────
if ($config['allow_cors']) {
    header('Access-Control-Allow-Origin: *');
    header('Access-Control-Allow-Methods: GET, HEAD');
}

header('Content-Type: application/json; charset=utf-8');

// ── Routing ───────────────────────────────────────────────
$uri = strtok($_SERVER['REQUEST_URI'], '?');
$uri = rtrim($uri, '/') ?: '/';

// Enlever le sous-dossier si le site est dans un sous-dossier
$scriptDir = dirname($_SERVER['SCRIPT_NAME']);
if ($scriptDir !== '/' && strpos($uri, $scriptDir) === 0) {
    $uri = substr($uri, strlen($scriptDir));
}

// Support ?action=manifest ou ?action=info
$action = $_GET['action'] ?? null;
if ($action === 'manifest')
    $uri = '/manifest.json';
if ($action === 'info')
    $uri = '/info.json';

switch ($uri) {
    case '/': // Racine = manifest par défaut
    case '/manifest.json':
    case '/manifest':
        echo json_encode(buildManifest($config), JSON_PRETTY_PRINT | JSON_UNESCAPED_SLASHES);
        break;

    case '/info.json':
    case '/info':
        echo json_encode([
            'mc_version' => $config['mc_version'],
            'loader' => $config['loader'],
            'loader_version' => $config['loader_version'],
            'server_url' => $config['server_url'],
        ], JSON_PRETTY_PRINT);
        break;

    default:
        http_response_code(404);
        echo json_encode(['error' => 'Route inconnue', 'path' => $uri]);
}

// ── Construire le manifest ────────────────────────────────
function buildManifest(array $config): array
{
    $serverUrl = rtrim($config['server_url'], '/');
    $files = [];

    // Dossiers à inclure automatiquement
    $folders = [
        'mods' => 'mods',
        'config' => 'config',
        'resourcepacks' => 'resourcepacks',
        'shaderpacks' => 'shaderpacks',
        'scripts' => 'scripts',
    ];

    foreach ($folders as $dir => $urlPath) {
        $fullDir = __DIR__ . '/files/' . $dir;
        if (is_dir($fullDir)) {
            $files = array_merge(
                $files,
                scanFolder($fullDir, $serverUrl . '/files/' . $urlPath, $urlPath)
            );
        }
    }

    // Ajouter le loader JAR si présent dans files/loader/
    $loaderDir = __DIR__ . '/files/loader';
    if (is_dir($loaderDir)) {
        $files = array_merge(
            $files,
            scanFolder($loaderDir, $serverUrl . '/files/loader', 'loader')
        );
    }

    return $files;
}

// ── Scanner un dossier récursivement ─────────────────────
function scanFolder(string $dir, string $baseUrl, string $relBase): array
{
    $entries = [];

    foreach (new RecursiveIteratorIterator(new RecursiveDirectoryIterator($dir)) as $file) {
        if ($file->isDir())
            continue;
        if (str_starts_with($file->getFilename(), '.'))
            continue; // Fichiers cachés

        $fullPath = $file->getPathname();
        $relPath = $relBase . '/' . ltrim(str_replace($dir, '', $fullPath), DIRECTORY_SEPARATOR);
        $relPath = str_replace('\\', '/', $relPath); // Windows compat
        $fileUrl = $baseUrl . '/' . ltrim(str_replace($dir, '', $fullPath), '/');
        $fileUrl = str_replace('\\', '/', $fileUrl);

        $entries[] = [
            'path' => $relPath,
            'url' => $fileUrl,
            'sha1' => sha1_file($fullPath),
            'size' => $file->getSize(),
        ];
    }

    return $entries;
}