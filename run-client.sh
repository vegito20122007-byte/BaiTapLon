#!/bin/bash
echo "========================================================"
echo "  STARTING AUCTION CLIENT JAVAFX..."
echo "========================================================"

if ! command -v java &> /dev/null; then
    echo "[ERROR] Java is not installed! Please install JDK 17+."
    exit 1
fi

if [ ! -f "auction-client/target/auction-client-1.0.0-SNAPSHOT.jar" ]; then
    echo "First time run: Building project with Maven Wrapper..."
    chmod +x mvnw
    ./mvnw package -DskipTests
fi

java -cp "auction-client/target/auction-client-1.0.0-SNAPSHOT.jar:auction-client/target/lib/*" com.auction.client.Launcher
