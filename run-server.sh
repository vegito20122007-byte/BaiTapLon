#!/bin/bash
echo "========================================================"
echo "  STARTING AUCTION SERVER..."
echo "========================================================"

if ! command -v java &> /dev/null; then
    echo "[ERROR] Java is not installed! Please install JDK 17+."
    exit 1
fi

if [ ! -f "auction-server/target/auction-server-1.0.0-SNAPSHOT.jar" ]; then
    echo "First time run: Building project with Maven Wrapper..."
    chmod +x mvnw
    ./mvnw package -DskipTests
fi

java -cp "auction-server/target/auction-server-1.0.0-SNAPSHOT.jar:auction-server/target/lib/*" com.auction.server.ServerApp
