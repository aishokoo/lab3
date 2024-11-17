import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.*;

public class z1 {

    // Метод для генерації випадкового масиву заданого розміру з мінімальним і максимальним значеннями
    public static int[] generateRandomArray(int size, int min, int max) {
        Random random = new Random();
        return random.ints(size, min, max + 1).toArray();
    }

    // Реалізація паралельного обчислення суми за допомогою ForkJoin (Work Stealing)
    public static class PairwiseSumTask extends RecursiveTask<Long> {
        private static final int THRESHOLD = 1000; // Поріг, після якого задача розбивається на підзадачі
        private final int[] array;
        private final int start, end;

        public PairwiseSumTask(int[] array, int start, int end) {
            this.array = array;
            this.start = start;
            this.end = end;
        }

        @Override
        protected Long compute() {
            // Якщо розмір задачі менший за поріг, виконуємо обчислення послідовно
            if (end - start <= THRESHOLD) {
                long sum = 0;
                for (int i = start; i < end - 1; i++) {
                    sum += array[i] + array[i + 1];
                }
                return sum;
            } else {
                // Розбиваємо задачу на дві підзадачі
                int mid = (start + end) / 2;
                PairwiseSumTask leftTask = new PairwiseSumTask(array, start, mid);
                PairwiseSumTask rightTask = new PairwiseSumTask(array, mid, end);
                leftTask.fork(); // Запускаємо ліву задачу асинхронно
                return rightTask.compute() + leftTask.join(); // Чекаємо завершення лівої задачі та обчислюємо результат
            }
        }
    }

    // Реалізація паралельного обчислення суми за допомогою ExecutorService (Work Dealing)
    public static long pairwiseSumWithExecutorService(int[] array, int numThreads) throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(numThreads); // Створюємо пул потоків
        int chunkSize = Math.max(array.length / numThreads, 1); // Розбиваємо масив на частини
        CompletableFuture<Long>[] futures = new CompletableFuture[numThreads];

        for (int i = 0; i < numThreads; i++) {
            final int start = i * chunkSize;
            final int end = Math.min(array.length, start + chunkSize + 1); // Обчислюємо межі кожної частини
            futures[i] = CompletableFuture.supplyAsync(() -> {
                long sum = 0;
                for (int j = start; j < end - 1; j++) {
                    sum += array[j] + array[j + 1];
                }
                return sum;
            }, executor); // Передаємо задачу в пул потоків
        }

        // Чекаємо завершення всіх задач і підраховуємо загальну суму
        long totalSum = CompletableFuture.allOf(futures)
                .thenApply(v -> Arrays.stream(futures).mapToLong(f -> f.join()).sum())
                .get();

        executor.shutdown(); // Закриваємо пул потоків
        return totalSum;
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        Scanner scanner = new Scanner(System.in);

        // Отримуємо вхідні дані від користувача
        System.out.print("Введіть кількість елементів масиву: ");
        int arraySize = scanner.nextInt();
        System.out.print("Введіть мінімальне значення: ");
        int minValue = scanner.nextInt();
        System.out.print("Введіть максимальне значення: ");
        int maxValue = scanner.nextInt();

        // Перевірка коректності введених значень
        if (maxValue < minValue) {
            System.err.println("Помилка: максимальне значення не може бути меншим за мінімальне!");
            return;
        }

        // Генерація великого випадкового масиву
        int[] array = generateRandomArray(arraySize, minValue, maxValue);

        // Виведення згенерованого масиву (перші 10 елементів)
        System.out.println("Згенерований масив: " + Arrays.toString(Arrays.copyOf(array, Math.min(array.length, 10))) + "...");
        int numThreads = Runtime.getRuntime().availableProcessors(); // Кількість доступних процесорів

        // Вимірювання часу для Work Stealing
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        PairwiseSumTask task = new PairwiseSumTask(array, 0, array.length);
        long startTime = System.nanoTime();
        long workStealingResult = forkJoinPool.invoke(task); // Виконання задачі
        long workStealingTime = System.nanoTime() - startTime;

        System.out.printf("Результат Work Stealing: %d, Час виконання: %.3f мс%n", workStealingResult, workStealingTime / 1e6);

        // Вимірювання часу для Work Dealing
        startTime = System.nanoTime();
        long workDealingResult = pairwiseSumWithExecutorService(array, numThreads);
        long workDealingTime = System.nanoTime() - startTime;

        System.out.printf("Результат Work Dealing: %d, Час виконання: %.3f мс%n", workDealingResult, workDealingTime / 1e6);
    }
}
