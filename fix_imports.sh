#!/bin/bash

echo "Fixing Fully Qualified Names..."

# 1. Auth FQNs
find src/main/java -name "*.java" -exec sed -i '' 's/com.nexaria.launcher.auth./com.nexaria.launcher.services.auth./g' {} +

# 2. Java FQNs
find src/main/java -name "*.java" -exec sed -i '' 's/com.nexaria.launcher.java./com.nexaria.launcher.services.java./g' {} +

# 3. Security FQNs
find src/main/java -name "*.java" -exec sed -i '' 's/com.nexaria.launcher.security./com.nexaria.launcher.services.security./g' {} +

# 4. Cache FQNs (ImageCache)
find src/main/java -name "*.java" -exec sed -i '' 's/com.nexaria.launcher.cache./com.nexaria.launcher.services.cache./g' {} +

# 5. CacheCleanupService FQN (was in util, now in services.cache)
find src/main/java -name "*.java" -exec sed -i '' 's/com.nexaria.launcher.util.CacheCleanupService/com.nexaria.launcher.services.cache.CacheCleanupService/g' {} +

# 6. Downloader/Update FQNs
find src/main/java -name "*.java" -exec sed -i '' 's/com.nexaria.launcher.downloader./com.nexaria.launcher.services.update./g' {} +

# Fix CacheCleanupService import issue (removing old package import if present inside the same package)
# If CacheCleanupService is in services.cache, it doesn't need to import com.nexaria.launcher.services.cache.ImageCache
# But keeping it is fine, except if it's the OLD import "com.nexaria.launcher.cache.ImageCache" which we alreadysed'd to new package
# Let's check specifically for any lingering "package com.nexaria.launcher.cache" in the wrong place

# Remove imports that are same as package (optional but clean)
# sed -i '' '/import com.nexaria.launcher.services.cache.ImageCache;/d' src/main/java/com/nexaria/launcher/services/cache/CacheCleanupService.java

echo "FQN fix complete."
