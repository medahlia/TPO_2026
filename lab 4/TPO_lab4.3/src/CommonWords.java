import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;

/**
 * Пошук спільних слів у текстових документах за допомогою ForkJoin Framework.
 * Базується на прикладі з: https://www.oracle.com/technical-resources/articles/java/fork-join.html
 */
public class CommonWords {

    // Пул потоків для паралельного виконання
    private static final ForkJoinPool pool = new ForkJoinPool();

    public static int getThreadCount() {
        return pool.getParallelism();
    }

    // -------------------------------------------------------------------------
    // Модель даних
    // -------------------------------------------------------------------------

    static class Document {
        private final Path filePath;
        private final String rawText;

        Document(Path filePath) {
            this.filePath = filePath;
            this.rawText = null;
        }

        Document(String text) {
            this.rawText = text;
            this.filePath = null;
        }

        String readText() throws IOException {
            return (filePath != null) ? Files.readString(filePath) : rawText;
        }
    }

    static class Folder {
        final List<Folder> nestedFolders;
        final List<Document> files;

        Folder(List<Folder> nestedFolders, List<Document> files) {
            this.nestedFolders = nestedFolders;
            this.files = files;
        }

        static Folder load(File dir) {
            List<Document> fileList = new LinkedList<>();
            List<Folder> folderList = new LinkedList<>();
            for (File item : Objects.requireNonNull(dir.listFiles())) {
                if (item.isDirectory()) {
                    folderList.add(load(item));
                } else {
                    fileList.add(new Document(item.toPath()));
                }
            }
            return new Folder(folderList, fileList);
        }
    }

    // -------------------------------------------------------------------------
    // Послідовний (однопотоковий) алгоритм
    // -------------------------------------------------------------------------

    Set<String> findCommonWordsSequential(Folder root) {
        Set<String> result = new HashSet<>();
        traverseSequential(root, result);
        return result;
    }

    private void traverseSequential(Folder folder, Set<String> accumulated) {
        for (Folder sub : folder.nestedFolders) {
            traverseSequential(sub, accumulated);
        }
        for (Document file : folder.files) {
            mergeWordsFromFile(file, accumulated);
        }
    }

    private void mergeWordsFromFile(Document file, Set<String> accumulated) {
        try {
            Set<String> words = extractWords(file.readText());
            if (accumulated.isEmpty()) {
                accumulated.addAll(words);
            } else {
                accumulated.retainAll(words);
            }
        } catch (IOException e) {
            throw new RuntimeException("Помилка читання файлу", e);
        }
    }

    // -------------------------------------------------------------------------
    // Паралельний алгоритм (ForkJoin)
    // -------------------------------------------------------------------------

    Set<String> findCommonWordsParallel(Folder root) {
        return pool.invoke(new FolderTask(root));
    }

    /**
     * Задача для обробки однієї папки: форкує підзадачі для підпапок,
     * а файли у поточній папці обробляє безпосередньо.
     */
    static class FolderTask extends RecursiveTask<Set<String>> {
        private final Folder target;

        FolderTask(Folder target) {
            this.target = target;
        }

        @Override
        protected Set<String> compute() {
            // Спочатку запускаємо підзадачі для підпапок асинхронно
            List<FolderTask> subTasks = new ArrayList<>();
            for (Folder sub : target.nestedFolders) {
                FolderTask worker = new FolderTask(sub);
                subTasks.add(worker);
                worker.fork();
            }

            // Паралельно обробляємо файли поточної папки
            Set<String> current = null;
            for (Document file : target.files) {
                DocumentTask fw = new DocumentTask(file);
                Set<String> fileWords = fw.compute();
                if (current == null) {
                    current = fileWords;
                } else {
                    current.retainAll(fileWords);
                    if (current.isEmpty()) return current;
                }
            }

            // Збираємо результати підзадач
            for (FolderTask st : subTasks) {
                Set<String> subResult = st.join();
                if (current == null) {
                    current = subResult;
                } else {
                    current.retainAll(subResult);
                    if (current.isEmpty()) return current;
                }
            }

            return (current != null) ? current : Collections.emptySet();
        }
    }

    /**
     * Задача для обробки одного файлу. Якщо файл великий — ділить на шматки.
     */
    static class DocumentTask extends RecursiveTask<Set<String>> {
        private static final int CHUNK_SIZE = 10_000;
        private final Document file;

        DocumentTask(Document file) {
            this.file = file;
        }

        @Override
        protected Set<String> compute() {
            try {
                String text = file.readText();
                if (text.length() <= CHUNK_SIZE) {
                    return extractWords(text);
                }
                return processInChunks(text);
            } catch (IOException e) {
                throw new RuntimeException("Помилка читання файлу", e);
            }
        }

        private Set<String> processInChunks(String text) {
            List<String> chunks = splitByWords(text);
            List<DocumentTask> subtasks = new ArrayList<>();
            for (String chunk : chunks) {
                DocumentTask sub = new DocumentTask(new Document(chunk));
                subtasks.add(sub);
                sub.fork();
            }
            // Об'єднання: шматки одного файлу — union (всі слова файлу)
            Set<String> merged = new HashSet<>();
            for (DocumentTask sub : subtasks) {
                merged.addAll(sub.join());
            }
            return merged;
        }

        private List<String> splitByWords(String text) {
            List<String> chunks = new ArrayList<>();
            int start = 0;
            while (start < text.length()) {
                int end = Math.min(start + CHUNK_SIZE, text.length());
                // Не розривати слово посередині
                if (end < text.length() && text.charAt(end) != ' ') {
                    while (end > start && text.charAt(end) != ' ') end--;
                }
                chunks.add(text.substring(start, end).trim());
                start = end + 1;
            }
            return chunks;
        }
    }

    // -------------------------------------------------------------------------
    // Утиліта: витягнути унікальні слова з тексту
    // -------------------------------------------------------------------------

    static Set<String> extractWords(String text) {
        return Arrays.stream(text.split("\\W+"))
                .filter(w -> !w.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }
}