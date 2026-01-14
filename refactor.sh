#!/bin/bash

# 1. Update Package Declarations (Safe to re-run)
echo "Updating package declarations..."
find src/main/java/com/nexaria/launcher/core -name "*.java" -exec sed -i '' 's/^package com.nexaria.launcher;/package com.nexaria.launcher.core;/' {} +
find src/main/java/com/nexaria/launcher/services/auth -name "*.java" -exec sed -i '' 's/^package com.nexaria.launcher.auth;/package com.nexaria.launcher.services.auth;/' {} +
find src/main/java/com/nexaria/launcher/services/java -name "*.java" -exec sed -i '' 's/^package com.nexaria.launcher.java;/package com.nexaria.launcher.services.java;/' {} +
find src/main/java/com/nexaria/launcher/services/security -name "*.java" -exec sed -i '' 's/^package com.nexaria.launcher.security;/package com.nexaria.launcher.services.security;/' {} +
find src/main/java/com/nexaria/launcher/services/cache -name "*.java" -exec sed -i '' 's/^package com.nexaria.launcher.cache;/package com.nexaria.launcher.services.cache;/' {} +
find src/main/java/com/nexaria/launcher/util -name "*.java" -exec sed -i '' 's/^package com.nexaria.launcher.utils;/package com.nexaria.launcher.util;/' {} +

# Special cases
# UpdateService (renamed from GitHubModManager)
sed -i '' 's/^package com.nexaria.launcher.downloader;/package com.nexaria.launcher.services.update;/' src/main/java/com/nexaria/launcher/services/update/UpdateService.java
sed -i '' 's/public class GitHubModManager/public class UpdateService/' src/main/java/com/nexaria/launcher/services/update/UpdateService.java
sed -i '' 's/Logger logger = LoggerFactory.getLogger(GitHubModManager.class);/Logger logger = LoggerFactory.getLogger(UpdateService.class);/' src/main/java/com/nexaria/launcher/services/update/UpdateService.java
sed -i '' 's/public GitHubModManager(/public UpdateService(/' src/main/java/com/nexaria/launcher/services/update/UpdateService.java

# CacheCleanupService (moved from util to services.cache)
sed -i '' 's/^package com.nexaria.launcher.util;/package com.nexaria.launcher.services.cache;/' src/main/java/com/nexaria/launcher/services/cache/CacheCleanupService.java


# 2. Update Imports in ALL Java files
echo "Updating imports..."

# Auth
grep -r "import com.nexaria.launcher.auth" src/main/java | cut -d: -f1 | sort | uniq | xargs sed -i '' 's/import com.nexaria.launcher.auth\./import com.nexaria.launcher.services.auth\./g'

# Java
grep -r "import com.nexaria.launcher.java" src/main/java | cut -d: -f1 | sort | uniq | xargs sed -i '' 's/import com.nexaria.launcher.java\./import com.nexaria.launcher.services.java\./g'

# Security
grep -r "import com.nexaria.launcher.security" src/main/java | cut -d: -f1 | sort | uniq | xargs sed -i '' 's/import com.nexaria.launcher.security\./import com.nexaria.launcher.services.security\./g'

# Cache (ImageCache)
grep -r "import com.nexaria.launcher.cache" src/main/java | cut -d: -f1 | sort | uniq | xargs sed -i '' 's/import com.nexaria.launcher.cache\./import com.nexaria.launcher.services.cache\./g'

# Cache (CleanupService)
grep -r "import com.nexaria.launcher.util.CacheCleanupService" src/main/java | cut -d: -f1 | sort | uniq | xargs sed -i '' 's/import com.nexaria.launcher.util.CacheCleanupService/import com.nexaria.launcher.services.cache.CacheCleanupService/g'

# Utils (Old 'utils' -> New 'util')
grep -r "import com.nexaria.launcher.utils" src/main/java | cut -d: -f1 | sort | uniq | xargs sed -i '' 's/import com.nexaria.launcher.utils\./import com.nexaria.launcher.util\./g'

# Core (NexariaLauncher*)
# Note: Usually these were in root package, so they didn't have imports in other root files. 
# But classes in subpackages importing them need changes.
grep -r "import com.nexaria.launcher.NexariaLauncher" src/main/java | cut -d: -f1 | sort | uniq | xargs sed -i '' 's/import com.nexaria.launcher.NexariaLauncher/import com.nexaria.launcher.core.NexariaLauncher/g'

# UpdateService (GitHubModManager -> UpdateService)
grep -r "import com.nexaria.launcher.downloader.GitHubModManager" src/main/java | cut -d: -f1 | sort | uniq | xargs sed -i '' 's/import com.nexaria.launcher.downloader.GitHubModManager/import com.nexaria.launcher.services.update.UpdateService/g'

# Update code references to GitHubModManager -> UpdateService
# We need to be careful not to break strings, but usually it's safe in Java source code context
find src/main/java -name "*.java" -exec sed -i '' 's/GitHubModManager/UpdateService/g' {} +

echo "Refactoring complete."
