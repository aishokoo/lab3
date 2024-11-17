import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;

public class z2 {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Отримуємо шлях до директорії та розширення файлів від користувача
        System.out.println("Введіть шлях до директорії:");
        String directoryPath = scanner.nextLine();

        System.out.println("Введіть розширення файлів для підрахунку (наприклад, .pdf):");
        String fileExtension = scanner.nextLine();

        scanner.close();

        // Перевіряємо, чи є вказаний шлях директорією
        File directory = new File(directoryPath);
        if (!directory.exists() || !directory.isDirectory()) {
            System.out.println("Невірний шлях до директорії. Будь ласка, спробуйте ще раз.");
            return;
        }

        // Використовуємо ForkJoinPool для підрахунку файлів
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        FileCountTask task = new FileCountTask(directory, fileExtension);

        try {
            // Запускаємо задачу та отримуємо результат
            int result = forkJoinPool.invoke(task);
            System.out.println("Кількість файлів з розширенням \"" + fileExtension + "\": " + result);
        } finally {
            // Завершуємо роботу ForkJoinPool
            forkJoinPool.shutdown();
        }
    }

    // Рекурсивне завдання для ForkJoin Framework
    static class FileCountTask extends RecursiveTask<Integer> {
        private final File directory;  // Директорія для обробки
        private final String fileExtension;  // Розширення файлів для пошуку

        public FileCountTask(File directory, String fileExtension) {
            this.directory = directory;
            this.fileExtension = fileExtension;
        }

        @Override
        protected Integer compute() {
            int count = 0;  // Лічильник знайдених файлів
            List<FileCountTask> subtasks = new ArrayList<>();  // Список підзадач

            // Отримуємо список файлів в директорії
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        // Якщо це підкаталог, створюємо нову підзадачу для нього
                        FileCountTask subtask = new FileCountTask(file, fileExtension);
                        subtasks.add(subtask);
                        subtask.fork();  // Асинхронно виконуємо підзадачу
                    } else if (file.getName().endsWith(fileExtension)) {
                        // Якщо файл має вказане розширення, збільшуємо лічильник
                        count++;
                    }
                }

                // Чекаємо завершення всіх підзадач та підсумовуємо їх результати
                for (FileCountTask subtask : subtasks) {
                    count += subtask.join();  // Отримуємо результат з підзадачі
                }
            }

            return count;  // Повертаємо загальну кількість знайдених файлів
        }
    }
}
