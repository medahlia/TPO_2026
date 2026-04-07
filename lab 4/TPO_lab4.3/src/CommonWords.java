import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;

/**
 * Finding common words in text documents using the ForkJoin Framework.
 * Based on the example from: https://www.oracle.com/technical-resources/articles/java/fork-join.html
 */
public class CommonWords {

    // Thread pool for parallel execution
    private static final ForkJoinPool pool = new ForkJoinPool();

    public static int getThreadCount() {
        return pool.getParallelism();
    }

    // -------------------------------------------------------------------------
    // Data model
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
    // Sequential (single-threaded) algorithm
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
            throw new RuntimeException("Error reading file", e);
        }
    }

    // -------------------------------------------------------------------------
    // Parallel algorithm (ForkJoin)
    // -------------------------------------------------------------------------

    Set<String> findCommonWordsParallel(Folder root) {
        return pool.invoke(new FolderTask(root));
    }

    /**
     * Task for processing a single folder: forks subtasks for subfolders,
     * processes files in the current folder directly.
     */
    static class FolderTask extends RecursiveTask<Set<String>> {
        private final Folder target;

        FolderTask(Folder target) {
            this.target = target;
        }

        @Override
        protected Set<String> compute() {
            // Fork subtasks for subfolders asynchronously first
            List<FolderTask> subTasks = new ArrayList<>();
            for (Folder sub : target.nestedFolders) {
                FolderTask worker = new FolderTask(sub);
                subTasks.add(worker);
                worker.fork();
            }

            // Process files in the current folder
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

            // Collect subtask results
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
     * Task for processing a single file. Splits into chunks if the file is large.
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
                throw new RuntimeException("Error reading file", e);
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
            // Merge chunks: union of all words in the file
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
                // Do not split words in the middle
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
    // Utility: extract unique words from text
    // -------------------------------------------------------------------------

    static Set<String> extractWords(String text) {
        return Arrays.stream(text.split("\\W+"))
                .map(String::toLowerCase)
                .filter(w -> !w.isEmpty())
                .filter(w -> w.matches("[a-z]+"))              // letters only, no digits
                .filter(w -> w.length() > 1 || w.equals("a")) // single letters: only article "a"
                .collect(Collectors.toSet());
    }
}