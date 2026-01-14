@echo off
mvn clean compile exec:java -Dexec.mainClass="com.nexaria.launcher.NexariaLauncher" -Dnexaria.dev=true
