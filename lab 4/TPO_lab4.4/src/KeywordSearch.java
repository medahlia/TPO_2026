import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;

public class KeywordSearch {

    private static final ForkJoinPool POOL = new ForkJoinPool();

    public static int getThreadCount() {
        return POOL.getParallelism();
    }

    // ─── Модель даних ─────────────────────────────────────────────────────

    static class Document {
        private final Path   filePath;
        private final String rawText;

        Document(Path filePath) {
            this.filePath = filePath;
            this.rawText  = null;
        }

        Document(String text) {
            this.rawText  = text;
            this.filePath = null;
        }

        String read() throws IOException {
            return (filePath != null) ? Files.readString(filePath) : rawText;
        }
    }

    static class Folder {
        final List<Folder>   subFolders;
        final List<Document> files;

        Folder(List<Folder> subFolders, List<Document> files) {
            this.subFolders = subFolders;
            this.files      = files;
        }

        static Folder load(File dir) {
            List<Document> fileList   = new LinkedList<>();
            List<Folder>   folderList = new LinkedList<>();
            for (File item : Objects.requireNonNull(dir.listFiles())) {
                if (item.isDirectory()) folderList.add(load(item));
                else                   fileList.add(new Document(item.toPath()));
            }
            return new Folder(folderList, fileList);
        }
    }

    // ─── Послідовний алгоритм ─────────────────────────────────────────────

    List<String> findSequential(Folder root, List<String> keywords) {
        List<String> results = new ArrayList<>();
        searchFolder(root, keywords, results);
        return results;
    }

    private void searchFolder(Folder folder, List<String> keywords, List<String> results) {
        for (Folder sub : folder.subFolders) {
            searchFolder(sub, keywords, results);
        }
        for (Document file : folder.files) {
            searchFile(file, keywords, results);
        }
    }

    private void searchFile(Document file, List<String> keywords, List<String> results) {
        try {
            Set<String> words = extractWords(file.read());
            List<String> found = new ArrayList<>();
            for (String kw : keywords) {
                if (words.contains(kw)) found.add(kw);
            }
            if (!found.isEmpty()) {
                results.add(file.filePath + ": " + found);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // ─── Паралельний алгоритм (ForkJoin) ─────────────────────────────────

    List<String> findParallel(Folder root, List<String> keywords) {
        return POOL.invoke(new FolderTask(root, keywords));
    }

    // Задача для папки: форкає підпапки, обробляє файли у поточному потоці
    static class FolderTask extends RecursiveTask<List<String>> {
        private final Folder       folder;
        private final List<String> keywords;

        FolderTask(Folder folder, List<String> keywords) {
            this.folder   = folder;
            this.keywords = keywords;
        }

        @Override
        protected List<String> compute() {
            // Форкаємо підпапки асинхронно
            List<FolderTask> subTasks = new ArrayList<>();
            for (Folder sub : folder.subFolders) {
                FolderTask task = new FolderTask(sub, keywords);
                subTasks.add(task);
                task.fork();
            }

            // Файли поточної папки — обробляємо у поточному потоці
            List<String> results = new ArrayList<>();
            for (Document file : folder.files) {
                List<String> found = new FileTask(file, keywords).compute();
                if (!found.isEmpty()) {
                    results.add(file.filePath + ": " + found);
                }
            }

            // Збираємо результати підпапок
            for (FolderTask task : subTasks) {
                results.addAll(task.join());
            }

            return results;
        }
    }

    // Задача для файлу: ділить великі файли на шматки
    static class FileTask extends RecursiveTask<List<String>> {
        private static final int CHUNK_SIZE = 10_000;

        private final Document    file;
        private final List<String> keywords;

        FileTask(Document file, List<String> keywords) {
            this.file     = file;
            this.keywords = keywords;
        }

        @Override
        protected List<String> compute() {
            try {
                String text = file.read();

                // Базовий випадок — файл малий, обробляємо напряму
                if (text.length() <= CHUNK_SIZE) {
                    return matchKeywords(extractWords(text), keywords);
                }

                // Великий файл — ділимо на шматки, форкаємо
                List<FileTask> subTasks = new ArrayList<>();
                for (String chunk : splitText(text)) {
                    FileTask task = new FileTask(new Document(chunk), keywords);
                    subTasks.add(task);
                    task.fork();
                }

                // Об'єднуємо знайдені ключові слова з усіх шматків (без дублів)
                Set<String> found = new HashSet<>();
                for (FileTask task : subTasks) {
                    found.addAll(task.join());
                }
                return new ArrayList<>(found);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // Ділимо текст по пробілах (не розриваємо слова)
        private List<String> splitText(String text) {
            List<String> chunks = new ArrayList<>();
            int start = 0;
            while (start < text.length()) {
                int end = Math.min(start + CHUNK_SIZE, text.length());
                if (end < text.length() && text.charAt(end) != ' ') {
                    while (end > start && text.charAt(end) != ' ') end--;
                }
                chunks.add(text.substring(start, end).trim());
                start = end + 1;
            }
            return chunks;
        }
    }

    // ─── Допоміжні методи ─────────────────────────────────────────────────

    // Повертає список ключових слів, які є у наборі слів документа
    static List<String> matchKeywords(Set<String> words, List<String> keywords) {
        List<String> found = new ArrayList<>();
        for (String kw : keywords) {
            if (words.contains(kw)) found.add(kw);
        }
        return found;
    }

    // Витягує унікальні слова з тексту (lowercase, тільки букви)
    static Set<String> extractWords(String text) {
        return Arrays.stream(text.split("\\W+"))
                .map(String::toLowerCase)
                .filter(w -> !w.isEmpty())
                .collect(Collectors.toSet());
    }
}