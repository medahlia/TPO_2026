#!/bin/bash
# =============================================================================
# run_mpi.sh — компіляція та запуск паралельного МГУА через MPJ Express
# Структура проєкту:
#   GMDH/
#   ├── run_mpi.sh
#   ├── src/
#   │   ├── Matrix/   ← Matrix.java, MatrixMathematics.java, ...
#   │   └── mgua/     ← GMDHParallel.java
#   └── out/          ← створюється автоматично
# =============================================================================

MPJ_HOME=/Users/dasha/mpj-v0_44
NP=${1:-4}

echo "=================================================="
echo " МГУА MPI — MPJ Express v0.44"
echo " MPJ_HOME : $MPJ_HOME"
echo " Процесів : $NP"
echo "=================================================="

# --- 1. Перевірка MPJ ---
if [ ! -f "$MPJ_HOME/lib/mpj.jar" ]; then
    echo "[ПОМИЛКА] Не знайдено $MPJ_HOME/lib/mpj.jar"
    exit 1
fi

# --- 2. Компіляція ---
echo ""
echo "[1/3] Компіляція..."
mkdir -p out

javac -cp "$MPJ_HOME/lib/mpj.jar" \
      -d out \
      src/Matrix/Matrix.java \
      src/Matrix/MatrixMathematics.java \
      src/Matrix/NoSquareException.java \
      src/Matrix/IllegalDimensionException.java \
      src/mgua/GMDHParallel.java

if [ $? -ne 0 ]; then
    echo "[ПОМИЛКА] Компіляція не вдалась."
    exit 1
fi
echo "[OK] Компіляція успішна."

# --- 3. Запуск ---
echo ""
echo "[2/3] Запуск з $NP процесами..."
echo ""

export MPJ_HOME=$MPJ_HOME
export PATH=$MPJ_HOME/bin:$PATH

mpjrun.sh -np $NP -cp out mgua.GMDHParallel

echo ""
echo "[3/3] Готово. Результати у RESULTS_MPI.txt"

# --- 4. Показати результати ---
if [ -f "RESULTS_MPI.txt" ]; then
    echo ""
    echo "=== RESULTS_MPI.txt ==="
    cat RESULTS_MPI.txt
fi
