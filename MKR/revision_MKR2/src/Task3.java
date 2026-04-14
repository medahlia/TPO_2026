//Напишіть фрагмент коду OpenMPI/MPI Express, який виконує:
// 1) розсилку фрагментів масиву A, що зберігається в процесі-сервері, в процеси-сервери;
// 2) сортування переданих масивів в процесах-серверах;
// 3) передачу процесам сортованих з процесу master першого значення відсортованих масивів.

import mpi.*;
import java.util.Arrays;
import java.util.Random;

public class Task3 { //DistributedSort

    private static final int MASTER = 0;
    private static final int ARRAY_SIZE = 15;
    private static final int WORD_LENGTH = 5;

    public static void main(String[] args) throws Exception {

        MPI.Init(args);

        int numProcs = MPI.COMM_WORLD.Size();
        int rank = MPI.COMM_WORLD.Rank();

        if (numProcs < 2) {
            System.out.println("Need at least 2 processes");
            MPI.COMM_WORLD.Abort(1);
            MPI.Finalize();
            return;
        }

        // ── Крок 1: master генерує масив ──────────────────────────────────
        String[] fullArray = null;
        if (rank == MASTER) {
            fullArray = generateWords(ARRAY_SIZE, WORD_LENGTH);
            System.out.println("Original array:");
            printArray(fullArray);
        }

        // ── Крок 2: розраховуємо розподіл шматків між процесами ──────────
        int base  = ARRAY_SIZE / numProcs;  // базова кількість на процес
        int extra = ARRAY_SIZE % numProcs;  // залишок

        int[] chunkSizes = new int[numProcs];
        int[] offsets = new int[numProcs];

        for (int i = 0; i < numProcs; i++) {
            chunkSizes[i] = (i < extra) ? base + 1 : base;
            offsets[i] = (i == 0) ? 0 : offsets[i - 1] + chunkSizes[i - 1];
        }

        // ── Крок 3: Scatterv — розсилаємо шматки всім процесам ───────────
        int localSize = chunkSizes[rank];
        String[] localChunk = new String[localSize];

        MPI.COMM_WORLD.Scatterv(
                fullArray,   // джерело (читається тільки у master)
                0,           // зміщення у джерелі
                chunkSizes,  // розміри шматків для кожного процесу
                offsets,     // позиції початку кожного шматка
                MPI.OBJECT,  // тип даних
                localChunk,  // буфер-приймач поточного процесу
                0,           // зміщення у приймачі
                localSize,   // скільки елементів отримує цей процес
                MPI.OBJECT,  // тип даних
                MASTER);

        // ── Крок 4: кожен процес сортує свій шматок локально ─────────────
        Arrays.sort(localChunk);

        // ── Крок 5: кожен процес надсилає перший елемент до master ───────
        String[] firstElement   = { localChunk[0] };

        // Вихідний буфер виділяємо ТІЛЬКИ у master — решта передають null
        String[] firstElements = null;
        if (rank == MASTER) {
            firstElements = new String[numProcs];
        }

        MPI.COMM_WORLD.Gather(
                firstElement,  // що надсилаємо (1 елемент від кожного)
                0,             // зміщення у вхідному буфері
                1,             // скільки надсилаємо
                MPI.OBJECT,    // тип
                firstElements, // куди збираємо (null у не-master — це допустимо)
                0,             // зміщення у вихідному буфері
                1,             // скільки очікуємо від кожного
                MPI.OBJECT,    // тип
                MASTER    // хто збирає
        );

        // ── Крок 6: master виводить результат ────────────────────────────
        if (rank == MASTER) {
            System.out.println("First element of each sorted chunk:");
            printArray(firstElements);
        }

        MPI.Finalize();
    }

    // ── Генерація масиву випадкових слів фіксованої довжини ──────────────
    static String[] generateWords(int count, int wordLen) {
        Random rng      = new Random(42);
        String alphabet = "abcdefghijklmnopqrstuvwxyz";
        String[] result = new String[count];

        for (int i = 0; i < count; i++) {
            StringBuilder sb = new StringBuilder(wordLen);
            for (int j = 0; j < wordLen; j++) {
                sb.append(alphabet.charAt(rng.nextInt(alphabet.length())));
            }
            result[i] = sb.toString();
        }
        return result;
    }

    // ── Вивід масиву рядків в один рядок ─────────────────────────────────
    static void printArray(String[] array) {
        for (int i = 0; i < array.length; i++) {
            System.out.print(array[i]);
            if (i < array.length - 1) {
                System.out.print("  ");
            }
        }
        System.out.println();
    }
}


/////////////////////////////////////////////////////////////////////
private static final int MASTER = 0;

public static void main(String[] args) throws Exception {

    MPI.Init(args);

    int numProcs = MPI.COMM_WORLD.Size();
    int rank = MPI.COMM_WORLD.Rank();

    if (numProcs < 2) {
        MPI.COMM_WORLD.Abort(1);
        MPI.Finalize();
        return;
    }

    String[] fullArray = null;
    if (rank == MASTER) {
        fullArray = generateWords(ARRAY_SIZE, WORD_LENGTH);
    }

    int base = ARRAY_SIZE / numProcs;
    int extra = ARRAY_SIZE % numProcs;

    int[] chunkSizes = new int[numProcs];
    int[] offsets = new int[numProcs];

    for (int i = 0; i < numProcs; i++) {
        chunkSizes[i] = (i < extra) ? base + 1 : base;
        offsets[i] = (i == 0) ? 0 : offsets[i - 1] + chunkSizes[i - 1];
    }

    int localSize = chunkSizes[rank];
    String[] localChunk = new String[localSize];

    MPI.COMM_WORLD.Scatterv(fullArray, 0, chunkSizes, offsets, MPI.OBJECT, localChunk, 0, localSize, MPI.OBJECT, MASTER);

    Arrays.sort(localChunk);
    String[] firstElement   = { localChunk[0] };

    String[] firstElements = null;
    if (rank == MASTER) {
        firstElements = new String[numProcs];
    }

    MPI.COMM_WORLD.Gather(firstElement, 0, 1, MPI.OBJECT, firstElements, 0, 1, MPI.OBJECT, MASTER);

    MPI.Finalize();
}
