#!/bin/bash
mvn clean compile exec:java -Dexec.mainClass="com.nexaria.launcher.core.NexariaLauncher" -Dnexaria.dev=true
